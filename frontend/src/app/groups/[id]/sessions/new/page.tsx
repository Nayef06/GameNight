"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { api, GameNightSession, SharedGame } from "@/lib/api";

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
      <div className="max-w-xl space-y-6">
        <Button asChild variant="ghost" size="sm"><Link href={`/groups/${id}`}>Back to group</Link></Button>
        <h1 className="text-2xl font-semibold">Create GameNight Session</h1>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {loading ? <p>Loading...</p> : activeSession ? (
          <div className="space-y-3">
            <p>This group already has an active GameNight session.</p>
            <Button asChild variant="outline"><Link href={`/groups/${id}/sessions/${activeSession.id}`}>View Session</Link></Button>
          </div>
        ) : !error || games.length > 0 ? (
          <form onSubmit={create} className="space-y-4">
            <fieldset className="space-y-3">
              <legend className="font-medium">Select games (2–5)</legend>
              {games.length < 2 && <p className="text-sm text-muted-foreground">The group needs at least two shared games to create a session.</p>}
              {games.map((game) => (
                <label key={game.id} className="flex items-center gap-2">
                  <Checkbox checked={selectedIds.includes(game.id)} onCheckedChange={() => toggleGame(game.id)}
                    disabled={saving || (!selectedIds.includes(game.id) && selectedIds.length >= 5)} />
                  <span>{game.title}</span>
                </label>
              ))}
            </fieldset>
            <Button type="submit" disabled={saving || games.length < 2 || selectedIds.length < 2 || selectedIds.length > 5}>
              {saving ? "Creating..." : "Create Session"}
            </Button>
          </form>
        ) : null}
      </div>
    </RequireAuth>
  );
}
