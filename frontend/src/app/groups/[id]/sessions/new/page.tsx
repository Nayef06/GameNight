"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { api, GameNightSession, PLATFORMS, SharedGame } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { GameThumbnail } from "@/components/GameThumbnail";
import { ArrowLeft } from "lucide-react";

export default function CreateSessionPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [games, setGames] = useState<SharedGame[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [activeSession, setActiveSession] = useState<GameNightSession | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const [sharedGames, session] = await Promise.all([
          api<SharedGame[]>(`/groups/${id}/shared-games`),
          api<GameNightSession | undefined>(`/groups/${id}/sessions/active`),
        ]);
        setGames(sharedGames);
        setActiveSession(session ?? null);
      } catch (problem) {
        setError(problem instanceof Error ? problem.message : "Could not load shared games");
      } finally {
        setLoading(false);
      }
    }
    if (id) void load();
  }, [id]);

  function toggleGame(gameId: number) {
    setSelectedIds((current) => current.includes(gameId)
      ? current.filter((id) => id !== gameId)
      : [...current, gameId]);
  }

  async function create(event: FormEvent) {
    event.preventDefault();
    setError("");
    if (selectedIds.length < 2 || selectedIds.length > 5) {
      setError("Select between 2 and 5 candidate games.");
      return;
    }
    setSaving(true);
    try {
      const session = await api<GameNightSession>(`/groups/${id}/sessions`, {
        method: "POST",
        body: { gameIds: selectedIds },
      });
      router.push(`/groups/${id}/sessions/${session.id}`);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not create session");
    } finally {
      setSaving(false);
    }
  }

  return (
    <RequireAuth>
      <div className="max-w-3xl space-y-6">
        <Button asChild variant="ghost" size="sm"><Link href={`/groups/${id}`}><ArrowLeft className="size-4" /> Back to group</Link></Button>
        <div className="space-y-1"><h1 className="text-3xl font-semibold tracking-tight">Create GameNight Session</h1><p className="text-sm text-muted-foreground">Choose a short list of games for everyone to vote on.</p></div>
        {error && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
        {loading ? <p>Loading...</p> : activeSession ? (
          <Card><CardContent className="space-y-3">
            <p>This group already has an active GameNight session.</p>
            <Button asChild variant="outline"><Link href={`/groups/${id}/sessions/${activeSession.id}`}>View Session</Link></Button>
          </CardContent></Card>
        ) : !error || games.length > 0 ? (
          <Card>
            <CardHeader><CardTitle>Select games (2–5)</CardTitle><CardDescription>Choose at least two shared games. You can add up to five candidates.</CardDescription></CardHeader>
            <CardContent>
          <form onSubmit={create} className="space-y-5">
            <fieldset className="space-y-3">
              <legend className="sr-only">Select games (2–5)</legend>
              {games.length < 2 && <p className="text-sm text-muted-foreground">The group needs at least two shared games to create a session.</p>}
              {games.map((game) => (
                <label key={game.id} className="flex cursor-pointer items-center gap-3 rounded-xl border bg-muted/20 p-3 transition-colors hover:bg-muted/50 has-data-checked:border-foreground/60 has-data-checked:bg-muted">
                  <Checkbox checked={selectedIds.includes(game.id)} onCheckedChange={() => toggleGame(game.id)}
                    disabled={saving || (!selectedIds.includes(game.id) && selectedIds.length >= 5)} />
                  <GameThumbnail steamAppId={game.steamAppId} title={game.title} />
                  <span className="min-w-0 flex-1">
                    <span className="block font-medium">{game.title}</span>
                    <span className="text-sm text-muted-foreground">{game.platforms.map((platform) => PLATFORMS[platform]).join(", ")}</span>
                    {game.platforms.length > 1 && <span className="block text-sm text-muted-foreground">Members own this game on different platforms. Cross-play compatibility has not been verified.</span>}
                  </span>
                  <Badge variant="outline" className="hidden sm:inline-flex">Score {game.preferenceScore}</Badge>
                </label>
              ))}
            </fieldset>
            <Button type="submit" disabled={saving || games.length < 2 || selectedIds.length < 2 || selectedIds.length > 5}>
              {saving ? "Creating..." : "Create Session"}
            </Button>
          </form>
            </CardContent>
          </Card>
        ) : null}
      </div>
    </RequireAuth>
  );
}
