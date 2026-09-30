"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, MultiplayerPreference, Preferences } from "@/lib/api";

const GENRES = [
  "Action", "Adventure", "RPG", "Strategy", "Survival",
  "Shooter", "Sports", "Simulation", "Puzzle", "Other",
];

export default function PreferencesPage() {
  const [preferredGenres, setPreferredGenres] = useState<string[]>([]);
  const [multiplayerPreference, setMultiplayerPreference] =
    useState<MultiplayerPreference>("NO_PREFERENCE");
  const [preferredPlayerCount, setPreferredPlayerCount] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadPreferences = useCallback(async () => {
    try {
      const preferences = await api<Preferences>("/preferences");
      setPreferredGenres(preferences.preferredGenres);
      setMultiplayerPreference(preferences.multiplayerPreference);
      setPreferredPlayerCount(preferences.preferredPlayerCount?.toString() ?? "");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load preferences");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void loadPreferences(); }, [loadPreferences]);

  function toggleGenre(genre: string) {
    setPreferredGenres((current) => current.includes(genre)
      ? current.filter((value) => value !== genre)
      : [...current, genre]);
  }

  async function savePreferences(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    setSaving(true);
    try {
      const preferences = await api<Preferences>("/preferences", {
        method: "PUT",
        body: {
          preferredGenres,
          multiplayerPreference,
          preferredPlayerCount: preferredPlayerCount === "" ? null : Number(preferredPlayerCount),
        },
      });
      setPreferredGenres(preferences.preferredGenres);
      setMultiplayerPreference(preferences.multiplayerPreference);
      setPreferredPlayerCount(preferences.preferredPlayerCount?.toString() ?? "");
      setMessage("Preferences saved.");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not save preferences");
    } finally {
      setSaving(false);
    }
  }

  return (
    <RequireAuth>
      <section className="card">
        <h1>Preferences</h1>
        {loading ? <p>Loading preferences...</p> : (
          <form className="preferences-form" onSubmit={savePreferences}>
            <fieldset>
              <legend>Preferred Genres</legend>
              <div className="checkbox-grid">
                {GENRES.map((genre) => (
                  <label className="choice-label" key={genre}>
                    <input
                      type="checkbox"
                      checked={preferredGenres.includes(genre)}
                      onChange={() => toggleGenre(genre)}
                    />
                    {genre}
                  </label>
                ))}
              </div>
            </fieldset>

            <fieldset>
              <legend>Multiplayer Preference</legend>
              <div className="choice-list">
                {[
                  ["NO_PREFERENCE", "No Preference"],
                  ["MULTIPLAYER", "Prefer Multiplayer"],
                  ["SINGLE_PLAYER", "Prefer Single Player"],
                ].map(([value, label]) => (
                  <label className="choice-label" key={value}>
                    <input
                      type="radio"
                      name="multiplayerPreference"
                      value={value}
                      checked={multiplayerPreference === value}
                      onChange={() => setMultiplayerPreference(value as MultiplayerPreference)}
                    />
                    {label}
                  </label>
                ))}
              </div>
            </fieldset>

            <label className="player-count-label" htmlFor="preferred-player-count">
              Preferred Player Count
              <input
                id="preferred-player-count"
                type="number"
                min="1"
                step="1"
                value={preferredPlayerCount}
                onChange={(event) => setPreferredPlayerCount(event.target.value)}
              />
            </label>

            <button disabled={saving}>{saving ? "Saving..." : "Save Preferences"}</button>
          </form>
        )}
        {error && <p className="error">{error}</p>}
        {message && <p className="success">{message}</p>}
      </section>
    </RequireAuth>
  );
}
