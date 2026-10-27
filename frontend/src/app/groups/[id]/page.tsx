"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, GameNightSession, GameSelection, GroupDetails, SharedGame } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { NativeSelect, NativeSelectOption } from "@/components/ui/native-select";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { GameThumbnail } from "@/components/GameThumbnail";

export default function GroupPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [group, setGroup] = useState<GroupDetails | null>(null);
  const [sharedGames, setSharedGames] = useState<SharedGame[]>([]);
  const [recentSelections, setRecentSelections] = useState<GameSelection[]>([]);
  const [activeSession, setActiveSession] = useState<GameNightSession | null>(null);
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
        const [details, games, selections, tags, session] = await Promise.all([
          api<GroupDetails>(`/groups/${id}`),
          api<SharedGame[]>(`/groups/${id}/shared-games`),
          api<GameSelection[]>(`/groups/${id}/selections/recent`),
          api<string[]>("/games/tags"),
          api<GameNightSession | undefined>(`/groups/${id}/sessions/active`),
        ]);
        setGroup(details);
        setSharedGames(games);
        setRecentSelections(selections);
        setAvailableTags(tags);
        setActiveSession(session ?? null);
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
      <div className="space-y-6">
        <Button asChild variant="ghost" size="sm"><Link href="/groups">← Back to groups</Link></Button>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {message && <p role="status" className="text-sm text-muted-foreground">{message}</p>}
        {!group ? !error && <p className="text-sm text-muted-foreground">Loading group...</p> : (
          <>
            <div className="space-y-1">
              <h1 className="text-2xl font-semibold">{group.name}</h1>
              <p className="text-sm text-muted-foreground">Join code: <strong className="font-mono text-foreground">{group.joinCode}</strong></p>
            </div>
            <Card>
              <CardHeader><CardTitle>Members</CardTitle></CardHeader>
              <CardContent className="space-y-4">
                <ul className="divide-y divide-border">
                  {group.members.map((member) => (
                    <li className="flex items-center justify-between gap-3 py-2 first:pt-0 last:pb-0" key={member.id}>
                      <span>{member.username}{member.creator && <Badge variant="outline" className="ml-2">Creator</Badge>}</span>
                      {group.currentUserIsCreator && !member.creator && (
                        <AlertDialog>
                          <AlertDialogTrigger asChild><Button size="sm" variant="destructive">Remove</Button></AlertDialogTrigger>
                          <AlertDialogContent>
                            <AlertDialogHeader><AlertDialogTitle>Remove member?</AlertDialogTitle><AlertDialogDescription>Remove {member.username} from this group?</AlertDialogDescription></AlertDialogHeader>
                            <AlertDialogFooter><AlertDialogCancel>Cancel</AlertDialogCancel><AlertDialogAction variant="destructive" onClick={() => removeMember(member.id, member.username)}>Remove</AlertDialogAction></AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      )}
                    </li>
                  ))}
                </ul>
                {group.currentUserIsCreator ? <p className="text-sm text-muted-foreground">The group creator cannot leave the group.</p> : (
                  <AlertDialog>
                    <AlertDialogTrigger asChild><Button variant="destructive">Leave group</Button></AlertDialogTrigger>
                    <AlertDialogContent>
                      <AlertDialogHeader><AlertDialogTitle>Leave group?</AlertDialogTitle><AlertDialogDescription>You will no longer have access to this group.</AlertDialogDescription></AlertDialogHeader>
                      <AlertDialogFooter><AlertDialogCancel>Cancel</AlertDialogCancel><AlertDialogAction variant="destructive" onClick={leaveGroup}>Leave group</AlertDialogAction></AlertDialogFooter>
                    </AlertDialogContent>
                  </AlertDialog>
                )}
              </CardContent>
            </Card>
            <Card>
              <CardHeader><CardTitle>GameNight session</CardTitle></CardHeader>
              <CardContent className="space-y-3">
                {activeSession ? (
                  <>
                    <p className="text-sm text-muted-foreground">This group already has an active GameNight session.</p>
                    <h2 className="font-medium">Active GameNight Session #{activeSession.id}</h2>
                    <p className="text-sm text-muted-foreground">Created: {new Date(activeSession.createdAt).toLocaleDateString()}</p>
                    <p className="text-sm font-medium">Candidate games</p>
                    <ul className="list-disc space-y-1 pl-5">
                      {activeSession.candidates.map((game) => <li key={game.id}>{game.title}</li>)}
                    </ul>
                    <Button asChild variant="outline"><Link href={`/groups/${id}/sessions/${activeSession.id}`}>View Session</Link></Button>
                  </>
                ) : (
                  <Button asChild><Link href={`/groups/${id}/sessions/new`}>Create GameNight Session</Link></Button>
                )}
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>Shared games</CardTitle>
                <CardDescription>Games owned by every member. Each matching preferred tag adds 1 point per member. Matching multiplayer and player count preferences add 1 point each. Recently chosen games lose 2 points. Highest scores appear first.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <form className="space-y-4" onSubmit={applyFilters}>
                  <fieldset className="space-y-3">
                    <legend className="mb-1 text-sm font-medium">Tags</legend>
                    <p className="text-sm text-muted-foreground">Games must have every selected tag.</p>
                    <div className="grid gap-3 sm:grid-cols-2 md:grid-cols-3">
                      {availableTags.map((tag) => (
                        <div className="flex items-center gap-2" key={tag}>
                          <Checkbox id={`filter-tag-${tag}`} checked={selectedTags.includes(tag)} onCheckedChange={() => toggleTag(tag)} />
                          <Label htmlFor={`filter-tag-${tag}`} className="font-normal">{tag}</Label>
                        </div>
                      ))}
                    </div>
                  </fieldset>
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div className="space-y-2">
                      <Label htmlFor="filter-multiplayer">Multiplayer</Label>
                      <NativeSelect id="filter-multiplayer" className="w-full" value={multiplayerSupport} onChange={(e) => setMultiplayerSupport(e.target.value)}>
                        <NativeSelectOption value="">Any</NativeSelectOption>
                        <NativeSelectOption value="true">Multiplayer</NativeSelectOption>
                        <NativeSelectOption value="false">Single player</NativeSelectOption>
                      </NativeSelect>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="filter-min-players">Minimum player capacity</Label>
                      <Input id="filter-min-players" type="number" min="1" step="1" value={minPlayers} onChange={(e) => setMinPlayers(e.target.value)} />
                    </div>
                  </div>
                  <div className="flex gap-2"><Button type="submit">Apply filters</Button><Button type="button" variant="outline" onClick={clearFilters}>Clear filters</Button></div>
                </form>
                <div className="border-t pt-4">
                  {sharedGames.length === 0 ? <p className="text-sm text-muted-foreground">{appliedFilterQuery ? "No shared games match these filters." : "No games are currently owned by everyone in this group."}</p> : (
                    <ul className="space-y-2">{sharedGames.map((game) => (
                      <li className={`flex flex-wrap items-center justify-between gap-4 rounded-lg border p-3 ${game.preferenceScore === sharedGames[0].preferenceScore ? "border-green-800 bg-green-950/30" : "border-border"}`} key={game.id}>
                        <div className="flex min-w-0 items-center gap-3">
                          <GameThumbnail title={game.title} />
                          <div className="min-w-0 space-y-1">
                            <div className="flex flex-wrap items-center gap-2"><strong className="font-medium">{game.title}</strong>{game.preferenceScore === sharedGames[0].preferenceScore && <Badge className="border-green-800 bg-green-950 text-green-300" variant="outline">Top score</Badge>}</div>
                            <p className="text-sm text-muted-foreground">Tags: {game.tags.join(", ")} · Multiplayer: {game.multiplayerSupport ? "Yes" : "No"} · Max players: {game.maxPlayers}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-sm">Score: <strong>{game.preferenceScore}</strong></span>
                          <Button type="button" size="sm" disabled={choosingGameId !== null} onClick={() => chooseGame(game)}>{choosingGameId === game.id ? "Choosing..." : "Choose game"}</Button>
                        </div>
                      </li>
                    ))}</ul>
                  )}
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardHeader><CardTitle>Recently chosen</CardTitle></CardHeader>
              <CardContent>{recentSelections.length === 0 ? <p className="text-sm text-muted-foreground">No games have been chosen yet.</p> : <ul className="divide-y divide-border">{recentSelections.map((selection) => <li className="flex items-center gap-3 py-2 first:pt-0 last:pb-0" key={selection.id}><GameThumbnail title={selection.gameTitle} /><span>{selection.gameTitle}</span></li>)}</ul>}</CardContent>
            </Card>
          </>
        )}
      </div>
    </RequireAuth>
  );
}
