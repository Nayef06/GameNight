"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Game } from "@/lib/api";

export default function LibraryPage() {
  const [games, setGames] = useState<Game[]>([]);
  const [title, setTitle] = useState("");
  const [error, setError] = useState("");

  const loadGames = useCallback(async () => {
    try {
      setGames(await api<Game[]>("/library"));
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load library");
    }
  }, []);

  useEffect(() => { void loadGames(); }, [loadGames]);

  async function addGame(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      await api<Game>("/library", { method: "POST", body: { title } });
      setTitle("");
      await loadGames();
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not add game");
    }
  }

  async function removeGame(gameId: number) {
    setError("");
    try {
      await api<void>(`/library/${gameId}`, { method: "DELETE" });
      await loadGames();
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not remove game");
    }
  }

  return (
    <RequireAuth>
      <section className="card">
        <h1>My Library</h1>
        <form className="inline-form" onSubmit={addGame}>
          <input aria-label="Game title" placeholder="Game title, e.g. Terraria" maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} required />
          <button>Add game</button>
        </form>
        {error && <p className="error">{error}</p>}
        {games.length === 0 ? <p>Your library is empty.</p> : (
          <ul className="item-list">
            {games.map((game) => (
              <li key={game.id}><span>{game.title}</span><button className="small danger" onClick={() => removeGame(game.id)}>Remove</button></li>
            ))}
          </ul>
        )}
      </section>
    </RequireAuth>
  );
}

