"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { Button } from "@/components/ui/button";
import { api, GameNightSession, GroupDetails, SessionVotingResults } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { ArrowLeft, RefreshCw } from "lucide-react";

export default function SessionPage() {
  const { id, sessionId } = useParams<{ id: string; sessionId: string }>();
  const [session, setSession] = useState<GameNightSession | null>(null);
  const [results, setResults] = useState<SessionVotingResults | null>(null);
  const [isGroupCreator, setIsGroupCreator] = useState(false);
  const [selectedGameId, setSelectedGameId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!id || !sessionId) return;
    Promise.all([
      api<GameNightSession>(`/groups/${id}/sessions/${sessionId}`),
      api<SessionVotingResults>(`/groups/${id}/sessions/${sessionId}/results`),
      api<GroupDetails>(`/groups/${id}`),
    ]).then(([loadedSession, loadedResults, group]) => {
      setSession(loadedSession);
      setResults(loadedResults);
      setSelectedGameId(loadedResults.currentUserVoteGameId);
      setIsGroupCreator(group.currentUserIsCreator);
    }).catch((problem) => setError(problem instanceof Error ? problem.message : "Could not load session"));
  }, [id, sessionId]);

  const status = results?.status ?? session?.status;

  useEffect(() => {
    if (!id || !sessionId || status !== "OPEN") return;

    let active = true;
    let inFlight = false;
    const interval = window.setInterval(() => {
      if (inFlight) return;
      inFlight = true;
      api<SessionVotingResults>(`/groups/${id}/sessions/${sessionId}/results`)
        .then((updated) => {
          if (active) {
            setResults((previous) => previous?.status === "CLOSED" ? previous : updated);
          }
        })
        .catch(() => {
          // The next interval retries a temporary network failure.
        })
        .finally(() => { inFlight = false; });
    }, 5000);

    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [id, sessionId, status]);

  async function refreshResults() {
    setError("");
    try {
      const updated = await api<SessionVotingResults>(`/groups/${id}/sessions/${sessionId}/results`);
      setResults(updated);
      setSelectedGameId(updated.currentUserVoteGameId);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not refresh results");
    }
  }

  async function submitVote(event: FormEvent) {
    event.preventDefault();
    if (selectedGameId === null) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await api<void>(`/groups/${id}/sessions/${sessionId}/vote`, {
        method: "POST",
        body: { gameId: selectedGameId },
      });
      setMessage("Vote submitted.");
      await refreshResults();
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not submit vote");
    } finally {
      setBusy(false);
    }
  }

  async function closeVoting() {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await api<void>(`/groups/${id}/sessions/${sessionId}/close`, { method: "POST" });
      const [updatedSession, updatedResults] = await Promise.all([
        api<GameNightSession>(`/groups/${id}/sessions/${sessionId}`),
        api<SessionVotingResults>(`/groups/${id}/sessions/${sessionId}/results`),
      ]);
      setSession(updatedSession);
      setResults(updatedResults);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not close voting");
    } finally {
      setBusy(false);
    }
  }

  const showResults = results && (results.currentUserVoteGameId !== null || status === "CLOSED");
  const highestVotes = results?.results[0]?.votes ?? 0;
  const winners = highestVotes > 0
    ? results?.results.filter((game) => game.votes === highestVotes) ?? []
    : [];

  return (
    <RequireAuth>
      <div className="max-w-3xl space-y-6">
        <Button asChild variant="ghost" size="sm"><Link href={`/groups/${id}`}><ArrowLeft className="size-4" /> Back to group</Link></Button>
        {error && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
        {message && <p role="status" className="rounded-lg border bg-muted/40 p-3 text-sm">{message}</p>}
        {!session ? !error && <p>Loading session...</p> : (
          <>
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div className="space-y-1"><h1 className="text-3xl font-semibold tracking-tight">GameNight Session #{session.id}</h1><p className="text-sm text-muted-foreground">Created: {new Date(session.createdAt).toLocaleDateString()}</p></div>
              <Badge variant={status === "OPEN" ? "secondary" : "outline"}>Status: {status}</Badge>
            </div>
            {status === "OPEN" && (
              <Card><CardHeader><CardTitle>Choose a Game</CardTitle><CardDescription>Select one candidate. You can update your vote while voting is open.</CardDescription></CardHeader><CardContent>
              <form className="space-y-4" onSubmit={submitVote}>
                <fieldset className="space-y-3" disabled={busy}>
                  <legend className="sr-only">Choose a Game:</legend>
                  <RadioGroup value={selectedGameId?.toString()} onValueChange={(value) => setSelectedGameId(Number(value))}>
                  {session.candidates.map((game) => (
                    <label className="flex cursor-pointer items-center gap-3 rounded-xl border bg-muted/20 p-4 transition-colors hover:bg-muted/50 has-data-checked:border-foreground/60 has-data-checked:bg-muted" key={game.id}>
                      <RadioGroupItem value={String(game.id)} disabled={busy} />
                      <span className="font-medium">{game.title}</span>
                    </label>
                  ))}
                  </RadioGroup>
                </fieldset>
                <Button type="submit" disabled={busy || selectedGameId === null}>Submit Vote</Button>
              </form>
              </CardContent></Card>
            )}
            {showResults && (
              <Card><CardHeader><CardTitle>Current Results</CardTitle><CardDescription>Votes refresh automatically while the session is open.</CardDescription></CardHeader><CardContent className="space-y-4">
                <ul className="space-y-3">
                  {results.results.map((game) => (
                    <li key={game.gameId} className="space-y-2"><div className="flex justify-between gap-3 text-sm"><span>{game.title} — {game.votes} {game.votes === 1 ? "vote" : "votes"}</span></div><div className="h-2 overflow-hidden rounded-full bg-muted"><div className="h-full rounded-full bg-primary transition-all" style={{ width: `${highestVotes ? (game.votes / highestVotes) * 100 : 0}%` }} /></div></li>
                  ))}
                </ul>
                <Button type="button" variant="outline" onClick={refreshResults} disabled={busy}><RefreshCw className="size-4" /> Refresh Results</Button>
              </CardContent></Card>
            )}
            {status === "CLOSED" && results && (
              <Card><CardHeader><CardTitle>Voting Closed</CardTitle></CardHeader><CardContent className="space-y-2">
                {winners.length === 0 ? <p>No votes were submitted.</p> : winners.length === 1 ? (
                  <>
                    <p>Most Voted Game: {winners[0].title}</p>
                    <p>Votes: {winners[0].votes}</p>
                  </>
                ) : (
                  <>
                    <p>Tie:</p>
                    <ul className="space-y-1">
                      {winners.map((game) => <li key={game.gameId}>{game.title} — {game.votes} votes</li>)}
                    </ul>
                  </>
                )}
              </CardContent></Card>
            )}
            {status === "OPEN" && isGroupCreator && (
              <Button type="button" variant="outline" onClick={closeVoting} disabled={busy}>Close Voting</Button>
            )}
          </>
        )}
      </div>
    </RequireAuth>
  );
}
