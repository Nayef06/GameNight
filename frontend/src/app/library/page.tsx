"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Game, GamingPlatform, LibraryGame, PLATFORMS, SteamConnection, SteamSyncResult } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select";
import { GameThumbnail } from "@/components/GameThumbnail";
import { Badge } from "@/components/ui/badge";
import { BookOpen, Download, Plus } from "lucide-react";

export default function LibraryPage() {
  const [games, setGames] = useState<LibraryGame[]>([]);
  const [catalog, setCatalog] = useState<Game[]>([]);
  const [gameId, setGameId] = useState("");
  const [gameSearch, setGameSearch] = useState("");
  const [platform, setPlatform] = useState<GamingPlatform>("STEAM");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [steamId, setSteamId] = useState("");
  const [savedSteamId, setSavedSteamId] = useState<string | null>(null);
  const [steamBusy, setSteamBusy] = useState(false);

  const loadGames = useCallback(async () => {
    try {
      setGames(await api<LibraryGame[]>("/library"));
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load library");
    }
  }, []);

  useEffect(() => {
    async function load() {
      try {
        const [library, availableGames, steamConnection] = await Promise.all([
          api<LibraryGame[]>("/library"),
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
      await api<LibraryGame>("/library", {
        method: "POST",
        body: { gameId: Number(gameId), platform },
      });
      setGameId("");
      await loadGames();
      setMessage("Game added.");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not add game");
    }
  }

  const ownedGameIds = new Set(games.filter((owned) => owned.platform === platform).map((owned) => owned.id));
  const searchTerm = gameSearch.trim().toLocaleLowerCase();
  const visibleGames = catalog.filter((game) =>
    game.title.toLocaleLowerCase().includes(searchTerm)
    || game.tags.some((tag) => tag.toLocaleLowerCase().includes(searchTerm)));
  const selectedGame = catalog.find((game) => game.id === Number(gameId));

  async function removeGame(gameId: number, platform: GamingPlatform) {
    setError("");
    setMessage("");
    try {
      await api<void>(`/library/${gameId}?platform=${platform}`, { method: "DELETE" });
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
        <div className="space-y-1"><h1 className="text-3xl font-semibold tracking-tight">My Library</h1><p className="text-sm text-muted-foreground">Keep your collection ready for the next game night.</p></div>
        <div className="grid gap-3 sm:grid-cols-3">
          <Card size="sm"><CardContent><p className="text-xs text-muted-foreground">Games owned</p><p className="mt-1 text-2xl font-semibold">{games.length}</p></CardContent></Card>
          <Card size="sm"><CardContent><p className="text-xs text-muted-foreground">Platforms</p><p className="mt-1 text-2xl font-semibold">{new Set(games.map((game) => game.platform)).size}</p></CardContent></Card>
          <Card size="sm"><CardContent><p className="text-xs text-muted-foreground">Catalog games</p><p className="mt-1 text-2xl font-semibold">{catalog.length}</p></CardContent></Card>
        </div>
        <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
        <Card>
          <CardHeader><CardTitle className="flex items-center gap-2"><Plus className="size-4" /> Add a game</CardTitle><CardDescription>Browse the catalog and select a game to add.</CardDescription></CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={addGame}>
              <div className="space-y-2">
                <Label htmlFor="game-platform">Platform</Label>
                <NativeSelect id="game-platform" className="w-full" value={platform} onChange={(event) => {
                  setPlatform(event.target.value as GamingPlatform);
                  setGameId("");
                }}>
                  {Object.entries(PLATFORMS).map(([value, label]) => <NativeSelectOption key={value} value={value}>{label}</NativeSelectOption>)}
                </NativeSelect>
              </div>
              <div className="space-y-2">
                <Label htmlFor="game-search">Search games</Label>
                <Input id="game-search" type="search" placeholder="Search by title or tag" value={gameSearch} onChange={(event) => {
                  setGameSearch(event.target.value);
                  setGameId("");
                }} />
              </div>
              <div role="group" aria-label="Game catalog" className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                {visibleGames.map((game) => {
                  const owned = ownedGameIds.has(game.id);
                  const selected = gameId === String(game.id);
                  return (
                    <button
                      key={game.id}
                      type="button"
                      aria-label={owned ? `${game.title} already in library on ${PLATFORMS[platform]}` : `Select ${game.title}`}
                      aria-pressed={selected}
                      disabled={owned}
                      onClick={() => setGameId(String(game.id))}
                      className={`flex h-full min-w-0 flex-col overflow-hidden rounded-xl border bg-muted/20 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring ${selected ? "border-primary bg-muted ring-2 ring-primary" : "border-border hover:border-foreground/50 hover:bg-muted/50"} ${owned ? "cursor-not-allowed opacity-50" : "cursor-pointer"}`}
                    >
                      <GameThumbnail steamAppId={game.steamAppId} title={game.title} width={320} height={160} className="h-24 w-full rounded-none border-0 object-cover sm:h-28" />
                      <span className="flex w-full flex-1 items-start justify-between gap-2 p-2 text-sm font-medium">
                        <span className="min-w-0">{game.title}</span>
                        {selected && <Badge variant="secondary">Selected</Badge>}
                        {owned && <Badge variant="outline">Owned</Badge>}
                      </span>
                    </button>
                  );
                })}
              </div>
              {visibleGames.length === 0 && <p className="text-sm text-muted-foreground">No games match your search.</p>}
              {selectedGame && <div className="flex items-center gap-3"><GameThumbnail steamAppId={selectedGame.steamAppId} title={selectedGame.title} /><p className="text-sm text-muted-foreground">Tags: {selectedGame.tags.join(", ")} · {selectedGame.multiplayerSupport ? "Multiplayer" : "Single player"} · Up to {selectedGame.maxPlayers} {selectedGame.maxPlayers === 1 ? "player" : "players"}</p></div>}
              <Button disabled={!gameId || ownedGameIds.has(Number(gameId))}>Add to library</Button>
            </form>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle className="flex items-center gap-2"><Download className="size-4" /> Steam Library</CardTitle><CardDescription>Enter your SteamID64 to import accessible owned games.</CardDescription></CardHeader>
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
        </div>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {message && <p role="status" className="text-sm text-muted-foreground">{message}</p>}
        <Card>
          <CardHeader><CardTitle className="flex items-center gap-2"><BookOpen className="size-4" /> My games</CardTitle><CardDescription>Your collection across all platforms.</CardDescription></CardHeader>
          <CardContent>
            {games.length === 0 ? <p className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">You have not added any games yet.</p> : (
              <ul className="divide-y divide-border">
                {games.map((game) => (
                  <li className="flex flex-wrap items-center justify-between gap-3 py-3 first:pt-0 last:pb-0" key={`${game.id}-${game.platform}`}>
                    <div className="flex min-w-0 items-center gap-3">
                      <GameThumbnail steamAppId={game.steamAppId} title={game.title} />
                      <div className="min-w-0 space-y-1">
                        <p className="font-medium">{game.title} — {PLATFORMS[game.platform]}</p>
                        <p className="text-sm text-muted-foreground">Tags: {game.tags.join(", ")} · {game.multiplayerSupport ? "Multiplayer" : "Single player"} · Up to {game.maxPlayers} {game.maxPlayers === 1 ? "player" : "players"}</p>
                      </div>
                    </div>
                    <Button type="button" size="sm" variant="destructive" onClick={() => removeGame(game.id, game.platform)}>Remove</Button>
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
