"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Game, GroupDetails } from "@/lib/api";

export default function GroupPage() {
  const { id } = useParams<{ id: string }>();
  const [group, setGroup] = useState<GroupDetails | null>(null);
  const [sharedGames, setSharedGames] = useState<Game[]>([]);
  const [genre, setGenre] = useState("");
  const [multiplayerSupport, setMultiplayerSupport] = useState("");
  const [minPlayers, setMinPlayers] = useState("");
  const [filtersApplied, setFiltersApplied] = useState(false);
  const [error, setError] = useState("");

  const loadSharedGames = useCallback(async (query = "") => {
    setSharedGames(await api<Game[]>(`/groups/${id}/shared-games${query}`));
  }, [id]);

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

  async function applyFilters(event: FormEvent) {
    event.preventDefault();
    setError("");
    const parameters = new URLSearchParams();
    if (genre) parameters.set("genre", genre);
    if (multiplayerSupport) parameters.set("multiplayerSupport", multiplayerSupport);
    if (minPlayers) parameters.set("minPlayers", minPlayers);
    try {
      await loadSharedGames(parameters.size ? `?${parameters.toString()}` : "");
      setFiltersApplied(parameters.size > 0);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not filter shared games");
    }
  }

  async function clearFilters() {
    setGenre("");
    setMultiplayerSupport("");
    setMinPlayers("");
    setError("");
    try {
      await loadSharedGames();
      setFiltersApplied(false);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load shared games");
    }
  }

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
            <h2>Shared Games</h2>
            <p className="muted">Games owned by every member of this group.</p>
            <form className="filter-form" onSubmit={applyFilters}>
              <label htmlFor="filter-genre">
                Genre
                <select id="filter-genre" value={genre} onChange={(e) => setGenre(e.target.value)}>
                  <option value="">All Genres</option>
                  {['Action', 'Adventure', 'RPG', 'Strategy', 'Survival', 'Shooter', 'Sports', 'Simulation', 'Puzzle', 'Other']
                    .map((option) => <option key={option}>{option}</option>)}
                </select>
              </label>
              <label htmlFor="filter-multiplayer">
                Multiplayer
                <select id="filter-multiplayer" value={multiplayerSupport} onChange={(e) => setMultiplayerSupport(e.target.value)}>
                  <option value="">Any</option>
                  <option value="true">Multiplayer</option>
                  <option value="false">Single Player</option>
                </select>
              </label>
              <label htmlFor="filter-min-players">
                Minimum Player Capacity
                <input id="filter-min-players" type="number" min="1" step="1" value={minPlayers} onChange={(e) => setMinPlayers(e.target.value)} />
              </label>
              <div className="filter-actions">
                <button type="submit">Apply Filters</button>
                <button type="button" className="secondary" onClick={clearFilters}>Clear Filters</button>
              </div>
            </form>
            {sharedGames.length === 0 ? (
              <p>{filtersApplied ? "No shared games match these filters." : "No games are currently owned by everyone in this group."}</p>
            ) : (
              <ul className="simple-list">{sharedGames.map((game) => (
                <li key={game.id}>
                  <strong>{game.title}</strong>
                  <div className="game-details">
                    Genre: {game.genre} | Multiplayer: {game.multiplayerSupport ? "Yes" : "No"} | Max Players: {game.maxPlayers}
                  </div>
                </li>
              ))}</ul>
            )}
          </>
        )}
      </section>
    </RequireAuth>
  );
}
