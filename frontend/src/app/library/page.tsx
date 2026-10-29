"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Game, SteamConnection, SteamSyncResult } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select";
import { GameThumbnail } from "@/components/GameThumbnail";

export default function LibraryPage() {
  const [games, setGames] = useState<Game[]>([]);
  const [catalog, setCatalog] = useState<Game[]>([]);
  const [gameId, setGameId] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [steamId, setSteamId] = useState("");
  const [savedSteamId, setSavedSteamId] = useState<string | null>(null);
  const [steamBusy, setSteamBusy] = useState(false);

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
        const [library, availableGames, steamConnection] = await Promise.all([
          api<Game[]>("/library"),
          api<Game[]>("/games"),
          api<SteamConnection>("/users/me/steam"),
        ]);
        setGames(library);
        setCatalog(availableGames);
        setSavedSteamId(steamConnection.steamId);
        setSteamId(steamConnection.steamId ?? "");
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

  async function syncSteam() {
    setError("");
    setMessage("");
    setSteamBusy(true);
    try {
      const result = await api<SteamSyncResult>("/library/steam/sync", { method: "POST" });
      await loadGames();
      setMessage(`Imported ${result.imported} new ${result.imported === 1 ? "game" : "games"}. ${result.alreadyInLibrary} ${result.alreadyInLibrary === 1 ? "game was" : "games were"} already in your library.`);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not sync Steam library");
    } finally {
      setSteamBusy(false);
    }
  }

  async function saveAndImport(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    setSteamBusy(true);
    try {
      const connection = await api<SteamConnection>("/users/me/steam", {
        method: "PUT",
        body: { steamId },
      });
      setSavedSteamId(connection.steamId);
      setSteamId(connection.steamId ?? "");
      const result = await api<SteamSyncResult>("/library/steam/sync", { method: "POST" });
      await loadGames();
      setMessage(`Imported ${result.imported} new ${result.imported === 1 ? "game" : "games"}. ${result.alreadyInLibrary} ${result.alreadyInLibrary === 1 ? "game was" : "games were"} already in your library.`);
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not import Steam library");
    } finally {
      setSteamBusy(false);
    }
  }

  return (
    <RequireAuth>
      <div className="space-y-6">
        <h1 className="text-2xl font-semibold">My Library</h1>
        <Card>
          <CardHeader><CardTitle>Add a game</CardTitle><CardDescription>Select a game from the catalog.</CardDescription></CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={addGame}>
              <div className="space-y-2">
                <Label htmlFor="game-title">Game</Label>
                <NativeSelect id="game-title" className="w-full" value={gameId} onChange={(e) => setGameId(e.target.value)} required>
                  <NativeSelectOption value="">Choose a game</NativeSelectOption>
                  {availableGames.map((game) => <NativeSelectOption key={game.id} value={game.id}>{game.title}</NativeSelectOption>)}
                </NativeSelect>
              </div>
              {selectedGame && <div className="flex items-center gap-3"><GameThumbnail title={selectedGame.title} /><p className="text-sm text-muted-foreground">Tags: {selectedGame.tags.join(", ")} · {selectedGame.multiplayerSupport ? "Multiplayer" : "Single player"} · Up to {selectedGame.maxPlayers} {selectedGame.maxPlayers === 1 ? "player" : "players"}</p></div>}
              <Button disabled={!gameId}>Add to library</Button>
            </form>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle>Steam Library</CardTitle><CardDescription>Enter your SteamID64 to import accessible owned games.</CardDescription></CardHeader>
          <CardContent className="space-y-4">
            <form className="space-y-4" onSubmit={saveAndImport}>
              <div className="space-y-2">
                <Label htmlFor="steam-id">SteamID64</Label>
                <Input id="steam-id" inputMode="numeric" pattern="7656119[0-9]{10}" maxLength={17} value={steamId} onChange={(event) => setSteamId(event.target.value)} required />
              </div>
              <Button disabled={steamBusy}>Save and Import</Button>
            </form>
            {savedSteamId && <Button type="button" variant="outline" disabled={steamBusy} onClick={syncSteam}>Sync Steam Library</Button>}
          </CardContent>
        </Card>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {message && <p role="status" className="text-sm text-muted-foreground">{message}</p>}
        <Card>
          <CardHeader><CardTitle>My games</CardTitle></CardHeader>
          <CardContent>
            {games.length === 0 ? <p className="text-sm text-muted-foreground">You have not added any games yet.</p> : (
              <ul className="divide-y divide-border">
                {games.map((game) => (
                  <li className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0" key={game.id}>
                    <div className="flex min-w-0 items-center gap-3">
                      <GameThumbnail title={game.title} />
                      <div className="min-w-0 space-y-1">
                        <p className="font-medium">{game.title}</p>
                        <p className="text-sm text-muted-foreground">Tags: {game.tags.join(", ")} · {game.multiplayerSupport ? "Multiplayer" : "Single player"} · Up to {game.maxPlayers} {game.maxPlayers === 1 ? "player" : "players"}</p>
                      </div>
                    </div>
                    <Button type="button" size="sm" variant="destructive" onClick={() => removeGame(game.id)}>Remove</Button>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </RequireAuth>
  );
}
