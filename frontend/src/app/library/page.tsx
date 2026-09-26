"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Game } from "@/lib/api";

export default function LibraryPage() {
  const [games, setGames] = useState<Game[]>([]);
  const [title, setTitle] = useState("");
  const [genre, setGenre] = useState("Action");
  const [multiplayerSupport, setMultiplayerSupport] = useState("true");
  const [maxPlayers, setMaxPlayers] = useState("2");
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
      await api<Game>("/library", {
        method: "POST",
        body: {
          title,
          genre,
          multiplayerSupport: multiplayerSupport === "true",
          maxPlayers: Number(maxPlayers),
        },
      });
      setTitle("");
      setGenre("Action");
      setMultiplayerSupport("true");
      setMaxPlayers("2");
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
        <form className="game-form" onSubmit={addGame}>
          <label htmlFor="game-title">
            Game Title
            <input id="game-title" maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} required />
          </label>
          <label htmlFor="game-genre">
            Genre
            <select id="game-genre" value={genre} onChange={(e) => setGenre(e.target.value)} required>
              {['Action', 'Adventure', 'RPG', 'Strategy', 'Survival', 'Shooter', 'Sports', 'Simulation', 'Puzzle', 'Other']
                .map((option) => <option key={option}>{option}</option>)}
            </select>
          </label>
          <label htmlFor="game-multiplayer">
            Multiplayer Support
            <select id="game-multiplayer" value={multiplayerSupport} onChange={(e) => setMultiplayerSupport(e.target.value)} required>
              <option value="true">Yes</option>
              <option value="false">No</option>
            </select>
          </label>
          <label htmlFor="game-max-players">
            Maximum Players
            <input id="game-max-players" type="number" min="1" step="1" value={maxPlayers} onChange={(e) => setMaxPlayers(e.target.value)} required />
          </label>
          <button>Add Game</button>
        </form>
        {error && <p className="error">{error}</p>}
        <h2>My Games</h2>
        {games.length === 0 ? <p>Your library is empty.</p> : (
          <ul className="item-list">
            {games.map((game) => (
              <li key={game.id}>
                <div>
                  <strong>{game.title}</strong>
                  <div className="game-details">
                    Genre: {game.genre} | Multiplayer: {game.multiplayerSupport ? "Yes" : "No"} | Max Players: {game.maxPlayers}
                  </div>
                </div>
                <button className="small danger" onClick={() => removeGame(game.id)}>Remove</button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </RequireAuth>
  );
}
