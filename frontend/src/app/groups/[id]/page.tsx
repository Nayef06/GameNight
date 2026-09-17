"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Game, GroupDetails } from "@/lib/api";

export default function GroupPage() {
  const { id } = useParams<{ id: string }>();
  const [group, setGroup] = useState<GroupDetails | null>(null);
  const [sharedGames, setSharedGames] = useState<Game[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const [details, games] = await Promise.all([
          api<GroupDetails>(`/groups/${id}`),
          api<Game[]>(`/groups/${id}/shared-games`),
        ]);
        setGroup(details);
        setSharedGames(games);
      } catch (problem) {
        setError(problem instanceof Error ? problem.message : "Could not load group");
      }
    }
    if (id) void load();
  }, [id]);

  return (
    <RequireAuth>
      <section className="card">
        <Link href="/groups">Back to groups</Link>
        {error && <p className="error">{error}</p>}
        {!group ? !error && <p>Loading group...</p> : (
          <>
            <h1>{group.name}</h1>
            <p>Join code: <strong className="join-code">{group.joinCode}</strong></p>
            <h2>Members</h2>
            <ul className="simple-list">
              {group.members.map((member) => <li key={member}>{member}</li>)}
            </ul>
            <hr />
            <h2>Games Everyone Owns:</h2>
            {sharedGames.length === 0 ? (
              <p>No games are currently owned by everyone in this group.</p>
            ) : (
              <ul className="simple-list">{sharedGames.map((game) => <li key={game.id}>{game.title}</li>)}</ul>
            )}
          </>
        )}
      </section>
    </RequireAuth>
  );
}
