"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, MultiplayerPreference, Preferences } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";

export default function PreferencesPage() {
  const [availableTags, setAvailableTags] = useState<string[]>([]);
  const [preferredTags, setPreferredTags] = useState<string[]>([]);
  const [multiplayerPreference, setMultiplayerPreference] =
    useState<MultiplayerPreference>("NO_PREFERENCE");
  const [preferredPlayerCount, setPreferredPlayerCount] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadPreferences = useCallback(async () => {
    try {
      const [preferences, tags] = await Promise.all([
        api<Preferences>("/preferences"),
        api<string[]>("/games/tags"),
      ]);
      setAvailableTags(tags);
      setPreferredTags(preferences.preferredTags);
      setMultiplayerPreference(preferences.multiplayerPreference);
      setPreferredPlayerCount(preferences.preferredPlayerCount?.toString() ?? "");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load preferences");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void loadPreferences(); }, [loadPreferences]);

  function toggleTag(tag: string) {
    setPreferredTags((current) => current.includes(tag)
      ? current.filter((value) => value !== tag)
      : [...current, tag]);
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
          preferredTags,
          multiplayerPreference,
          preferredPlayerCount: preferredPlayerCount === "" ? null : Number(preferredPlayerCount),
        },
      });
      setPreferredTags(preferences.preferredTags);
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
      <div className="space-y-6">
        <h1 className="text-2xl font-semibold">Preferences</h1>
        <Card>
          <CardHeader><CardTitle>Game preferences</CardTitle><CardDescription>Used to rank shared games in your groups.</CardDescription></CardHeader>
          <CardContent>
            {loading ? <p className="text-sm text-muted-foreground">Loading preferences...</p> : (
              <form className="space-y-6" onSubmit={savePreferences}>
                <fieldset className="space-y-3">
                  <legend className="mb-3 text-sm font-medium">Preferred tags</legend>
                  <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-3">
                    {availableTags.map((tag) => (
                      <div className="flex items-center gap-2" key={tag}>
                        <Checkbox id={`tag-${tag}`} checked={preferredTags.includes(tag)} onCheckedChange={() => toggleTag(tag)} />
                        <Label htmlFor={`tag-${tag}`} className="font-normal">{tag}</Label>
                      </div>
                    ))}
                  </div>
                </fieldset>
                <fieldset className="space-y-3">
                  <legend className="mb-3 text-sm font-medium">Multiplayer preference</legend>
                  <RadioGroup value={multiplayerPreference} onValueChange={(value) => setMultiplayerPreference(value as MultiplayerPreference)}>
                    {[
                      ["NO_PREFERENCE", "No preference"],
                      ["MULTIPLAYER", "Prefer multiplayer"],
                      ["SINGLE_PLAYER", "Prefer single player"],
                    ].map(([value, label]) => (
                      <div className="flex items-center gap-2" key={value}>
                        <RadioGroupItem id={`multiplayer-${value}`} value={value} />
                        <Label htmlFor={`multiplayer-${value}`} className="font-normal">{label}</Label>
                      </div>
                    ))}
                  </RadioGroup>
                </fieldset>
                <div className="max-w-xs space-y-2">
                  <Label htmlFor="preferred-player-count">Preferred player count</Label>
                  <Input id="preferred-player-count" type="number" min="1" step="1" value={preferredPlayerCount} onChange={(event) => setPreferredPlayerCount(event.target.value)} />
                </div>
                <Button disabled={saving}>{saving ? "Saving..." : "Save preferences"}</Button>
              </form>
            )}
            {error && <p role="alert" className="mt-4 text-sm text-destructive">{error}</p>}
            {message && <p role="status" className="mt-4 text-sm text-muted-foreground">{message}</p>}
          </CardContent>
        </Card>
      </div>
    </RequireAuth>
  );
}
