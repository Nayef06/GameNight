"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { Button } from "@/components/ui/button";
import { api, GameNightSession, GroupDetails, SessionVotingResults } from "@/lib/api";

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

  const status = results?.status ?? session?.status;
  const showResults = results && (results.currentUserVoteGameId !== null || status === "CLOSED");
  const highestVotes = results?.results[0]?.votes ?? 0;
  const winners = highestVotes > 0
    ? results?.results.filter((game) => game.votes === highestVotes) ?? []
    : [];

  return (
    <RequireAuth>
      <div className="max-w-xl space-y-5">
        <Button asChild variant="ghost" size="sm"><Link href={`/groups/${id}`}>Back to group</Link></Button>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {message && <p role="status" className="text-sm">{message}</p>}
        {!session ? !error && <p>Loading session...</p> : (
          <>
            <h1 className="text-2xl font-semibold">GameNight Session #{session.id}</h1>
            <p className="text-sm text-muted-foreground">Created: {new Date(session.createdAt).toLocaleDateString()}</p>
            <p className="text-sm text-muted-foreground">Status: {status}</p>
            {status === "OPEN" && (
              <form className="space-y-3" onSubmit={submitVote}>
                <fieldset className="space-y-2" disabled={busy}>
                  <legend className="font-medium">Choose a Game:</legend>
                  {session.candidates.map((game) => (
                    <label className="flex items-center gap-2" key={game.id}>
                      <input type="radio" name="game" value={game.id}
                        checked={selectedGameId === game.id}
                        onChange={() => setSelectedGameId(game.id)} />
                      {game.title}
                    </label>
                  ))}
                </fieldset>
                <Button type="submit" disabled={busy || selectedGameId === null}>Submit Vote</Button>
              </form>
            )}
            {showResults && (
              <section className="space-y-3">
                <h2 className="font-medium">Current Results</h2>
                <ul className="space-y-1">
                  {results.results.map((game) => (
                    <li key={game.gameId}>{game.title} — {game.votes} {game.votes === 1 ? "vote" : "votes"}</li>
                  ))}
                </ul>
                <Button type="button" variant="outline" onClick={refreshResults} disabled={busy}>Refresh Results</Button>
              </section>
            )}
            {status === "CLOSED" && results && (
              <section className="space-y-2">
                <h2 className="font-medium">Voting Closed</h2>
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
              </section>
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
