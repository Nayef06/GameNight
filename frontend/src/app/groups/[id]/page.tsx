"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, GameSelection, GroupDetails, SharedGame } from "@/lib/api";

export default function GroupPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [group, setGroup] = useState<GroupDetails | null>(null);
  const [sharedGames, setSharedGames] = useState<SharedGame[]>([]);
  const [recentSelections, setRecentSelections] = useState<GameSelection[]>([]);
  const [choosingGameId, setChoosingGameId] = useState<number | null>(null);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [availableTags, setAvailableTags] = useState<string[]>([]);
  const [multiplayerSupport, setMultiplayerSupport] = useState("");
  const [minPlayers, setMinPlayers] = useState("");
  const [appliedFilterQuery, setAppliedFilterQuery] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadSharedGames = useCallback(async (query = "") => {
    setSharedGames(await api<SharedGame[]>(`/groups/${id}/shared-games${query}`));
  }, [id]);

  const loadRecentSelections = useCallback(async () => {
    setRecentSelections(await api<GameSelection[]>(`/groups/${id}/selections/recent`));
  }, [id]);

  useEffect(() => {
    async function load() {
      try {
        const [details, games, selections, tags] = await Promise.all([
          api<GroupDetails>(`/groups/${id}`),
          api<SharedGame[]>(`/groups/${id}/shared-games`),
          api<GameSelection[]>(`/groups/${id}/selections/recent`),
          api<string[]>("/games/tags"),
        ]);
        setGroup(details);
        setSharedGames(games);
        setRecentSelections(selections);
        setAvailableTags(tags);
      } catch (problem) {
        setError(problem instanceof Error ? problem.message : "Could not load group");
      }
    }
    if (id) void load();
  }, [id]);

  async function applyFilters(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    const query = selectedFilterQuery();
    try {
      await loadSharedGames(query);
      setAppliedFilterQuery(query);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not filter shared games");
    }
  }

  async function clearFilters() {
    setSelectedTags([]);
    setMultiplayerSupport("");
    setMinPlayers("");
    setError("");
    setMessage("");
    try {
      await loadSharedGames();
      setAppliedFilterQuery("");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load shared games");
    }
  }

  function selectedFilterQuery() {
    const parameters = new URLSearchParams();
    selectedTags.forEach((tag) => parameters.append("tags", tag));
    if (multiplayerSupport) parameters.set("multiplayerSupport", multiplayerSupport);
    if (minPlayers) parameters.set("minPlayers", minPlayers);
    return parameters.size ? `?${parameters.toString()}` : "";
  }

  function toggleTag(tag: string) {
    setSelectedTags((current) => current.includes(tag)
      ? current.filter((value) => value !== tag)
      : [...current, tag]);
  }

  async function removeMember(userId: number, username: string) {
    if (!window.confirm(`Remove ${username} from this group?`)) return;
    setError("");
    setMessage("");
    try {
      await api<void>(`/groups/${id}/members/${userId}`, { method: "DELETE" });
      const [details] = await Promise.all([
        api<GroupDetails>(`/groups/${id}`),
        loadSharedGames(appliedFilterQuery),
      ]);
      setGroup(details);
      setMessage("Member removed.");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not remove member");
    }
  }

  async function leaveGroup() {
    if (!window.confirm("Leave this group?")) return;
    setError("");
    setMessage("");
    try {
      await api<void>(`/groups/${id}/leave`, { method: "POST" });
      router.push("/groups?left=true");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not leave group");
    }
  }

  async function chooseGame(game: SharedGame) {
    setError("");
    setMessage("");
    setChoosingGameId(game.id);
    try {
      await api<GameSelection>(`/groups/${id}/selections`, {
        method: "POST",
        body: { gameId: game.id },
      });
      await Promise.all([
        loadSharedGames(appliedFilterQuery),
        loadRecentSelections(),
      ]);
      setMessage(`${game.title} marked as played.`);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not choose game");
    } finally {
      setChoosingGameId(null);
    }
  }

  return (
    <RequireAuth>
      <section className="card">
        <Link href="/groups">Back to groups</Link>
        {error && <p className="error">{error}</p>}
        {message && <p className="success">{message}</p>}
        {!group ? !error && <p>Loading group...</p> : (
          <>
            <h1>{group.name}</h1>
            <p>Join code: <strong className="join-code">{group.joinCode}</strong></p>
            <h2>Members</h2>
            <ul className="simple-list">
              {group.members.map((member) => (
                <li className="member-row" key={member.id}>
                  <span>{member.username}{member.creator ? " (Creator)" : ""}</span>
                  {group.currentUserIsCreator && !member.creator && (
                    <button className="small danger" onClick={() => removeMember(member.id, member.username)}>
                      Remove
                    </button>
                  )}
                </li>
              ))}
            </ul>
            {group.currentUserIsCreator ? (
              <p className="muted">The group creator cannot leave the group.</p>
            ) : (
              <button className="danger section-action" onClick={leaveGroup}>Leave Group</button>
            )}
            <hr />
            <h2>Shared Games</h2>
            <p className="muted">Games owned by every member of this group.</p>
            <p className="muted">Each matching preferred tag adds 1 point per member. Matching multiplayer and player-count preferences add 1 point each. Recently chosen games lose 2 points. Highest scores appear first.</p>
            <form className="filter-form" onSubmit={applyFilters}>
              <fieldset className="tag-filter">
                <legend>Tags</legend>
                <p className="muted">Games must have every selected tag.</p>
                <div className="tag-options">
                  {availableTags.map((tag) => (
                    <label className="tag-option" key={tag}>
                      <input type="checkbox" checked={selectedTags.includes(tag)}
                        onChange={() => toggleTag(tag)} />
                      {tag}
                    </label>
                  ))}
                </div>
              </fieldset>
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
              <p>{appliedFilterQuery ? "No shared games match these filters." : "No games are currently owned by everyone in this group."}</p>
            ) : (
              <ul className="simple-list">{sharedGames.map((game) => (
                <li className={`shared-game-row${game.preferenceScore === sharedGames[0].preferenceScore ? " top-scoring-game" : ""}`} key={game.id}>
                  <div>
                    <div className="shared-game-title">
                      <strong>{game.title}</strong>
                      {game.preferenceScore === sharedGames[0].preferenceScore && <span className="top-score-label">Top score</span>}
                    </div>
                    <div className="game-details">
                      Tags: {game.tags.join(", ")} | Multiplayer: {game.multiplayerSupport ? "Yes" : "No"} | Max Players: {game.maxPlayers}
                    </div>
                  </div>
                  <div className="shared-game-actions">
                    <span className="preference-score">Score: <strong>{game.preferenceScore}</strong></span>
                    <button type="button" className="small" disabled={choosingGameId !== null}
                      onClick={() => chooseGame(game)}>
                      {choosingGameId === game.id ? "Choosing..." : "Choose Game"}
                    </button>
                  </div>
                </li>
              ))}</ul>
            )}
            <h2>Recently Chosen</h2>
            {recentSelections.length === 0 ? (
              <p className="muted">No games have been chosen yet.</p>
            ) : (
              <ul className="simple-list recent-selections">
                {recentSelections.map((selection) => (
                  <li key={selection.id}>{selection.gameTitle}</li>
                ))}
              </ul>
            )}
          </>
        )}
      </section>
    </RequireAuth>
  );
}
