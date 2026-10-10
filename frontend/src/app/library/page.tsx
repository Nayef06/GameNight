"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Game } from "@/lib/api";

export default function LibraryPage() {
  const [games, setGames] = useState<Game[]>([]);
  const [catalog, setCatalog] = useState<Game[]>([]);
  const [gameId, setGameId] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadGames = useCallback(async () => {
    try {
      setGames(await api<Game[]>("/library"));
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load library");
    }
  }, []);

  useEffect(() => {
    async function load() {
      try {
        const [library, availableGames] = await Promise.all([
          api<Game[]>("/library"),
          api<Game[]>("/games"),
        ]);
        setGames(library);
        setCatalog(availableGames);
      } catch (problem) {
        setError(problem instanceof Error ? problem.message : "Could not load games");
      }
    }
    void load();
  }, []);

  async function addGame(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    try {
      await api<Game>("/library", {
        method: "POST",
        body: { gameId: Number(gameId) },
      });
      setGameId("");
      await loadGames();
      setMessage("Game added.");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not add game");
    }
  }

  const availableGames = catalog.filter((game) => !games.some((owned) => owned.id === game.id));
  const selectedGame = catalog.find((game) => game.id === Number(gameId));

  async function removeGame(gameId: number) {
    setError("");
    setMessage("");
    try {
      await api<void>(`/library/${gameId}`, { method: "DELETE" });
      await loadGames();
      setMessage("Game removed.");
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
            Select a Game
            <select id="game-title" value={gameId} onChange={(e) => setGameId(e.target.value)} required>
              <option value="">Choose a game</option>
              {availableGames.map((game) => (
                <option key={game.id} value={game.id}>{game.title}</option>
              ))}
            </select>
          </label>
          {selectedGame && (
            <p className="game-details">
              Tags: {selectedGame.tags.join(", ")} | {selectedGame.multiplayerSupport ? "Multiplayer" : "Single Player"} | Up to {selectedGame.maxPlayers} {selectedGame.maxPlayers === 1 ? "player" : "players"}
            </p>
          )}
          <button disabled={!gameId}>Add to My Library</button>
        </form>
        {error && <p className="error">{error}</p>}
        {message && <p className="success">{message}</p>}
        <h2>My Games</h2>
        {games.length === 0 ? <p>You have not added any games yet.</p> : (
          <ul className="item-list">
            {games.map((game) => (
              <li key={game.id}>
                <div>
                  <strong>{game.title}</strong>
                  <div className="game-details">
                    Tags: {game.tags.join(", ")} | {game.multiplayerSupport ? "Multiplayer" : "Single Player"} | Up to {game.maxPlayers} {game.maxPlayers === 1 ? "player" : "players"}
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
