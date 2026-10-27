"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { Button } from "@/components/ui/button";
import { api, GameNightSession } from "@/lib/api";

export default function SessionPage() {
  const { id, sessionId } = useParams<{ id: string; sessionId: string }>();
  const [session, setSession] = useState<GameNightSession | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (id && sessionId) {
      api<GameNightSession>(`/groups/${id}/sessions/${sessionId}`)
        .then(setSession)
        .catch((problem) => setError(problem instanceof Error ? problem.message : "Could not load session"));
    }
  }, [id, sessionId]);

  return (
    <RequireAuth>
      <div className="max-w-xl space-y-5">
        <Button asChild variant="ghost" size="sm"><Link href={`/groups/${id}`}>Back to group</Link></Button>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {!session ? !error && <p>Loading session...</p> : (
          <>
            <h1 className="text-2xl font-semibold">GameNight Session #{session.id}</h1>
            <p className="text-sm text-muted-foreground">Created: {new Date(session.createdAt).toLocaleDateString()}</p>
            <p className="text-sm text-muted-foreground">Status: {session.status}</p>
            <h2 className="font-medium">Candidate games</h2>
            <ul className="list-disc space-y-1 pl-5">
              {session.candidates.map((game) => <li key={game.id}>{game.title}</li>)}
            </ul>
          </>
        )}
      </div>
    </RequireAuth>
  );
}
