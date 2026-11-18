"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Group } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { ArrowRight, Plus, Users } from "lucide-react";

export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [name, setName] = useState("");
  const [joinCode, setJoinCode] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const loadGroups = useCallback(async () => {
    try {
      setGroups(await api<Group[]>("/groups"));
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load groups");
    }
  }, []);

  useEffect(() => {
    const parameters = new URLSearchParams(window.location.search);
    if (parameters.get("left") === "true") {
      setMessage("You left the group.");
      window.history.replaceState({}, "", "/groups");
    }
    void loadGroups();
  }, [loadGroups]);

  async function createGroup(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    try {
      await api<Group>("/groups", { method: "POST", body: { name } });
      setName("");
      await loadGroups();
      setMessage("Group created.");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not create group");
    }
  }

  async function joinGroup(event: FormEvent) {
    event.preventDefault();
    setError("");
    setMessage("");
    try {
      await api<Group>("/groups/join", { method: "POST", body: { joinCode } });
      setJoinCode("");
      await loadGroups();
      setMessage("Joined group successfully.");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not join group");
    }
  }

  return (
    <RequireAuth>
      <div className="space-y-6">
        <div className="space-y-1"><h1 className="text-3xl font-semibold tracking-tight">My Groups</h1><p className="text-sm text-muted-foreground">Your friends, shared games, and upcoming sessions.</p></div>
        {error && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
        {message && <p role="status" className="rounded-lg border bg-muted/40 p-3 text-sm">{message}</p>}
        <Card>
          <CardHeader><CardTitle className="flex items-center gap-2"><Users className="size-4" /> Groups</CardTitle><CardDescription>{groups.length} {groups.length === 1 ? "group" : "groups"} in your circle</CardDescription></CardHeader>
          <CardContent>
            {groups.length === 0 ? <p className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">You have not joined any groups yet.</p> : (
              <ul className="grid gap-3 sm:grid-cols-2">
                {groups.map((group) => (
                  <li className="flex items-center justify-between gap-3 rounded-xl border bg-muted/20 p-4" key={group.id}>
                    <div><p className="font-medium">{group.name}</p><p className="text-sm text-muted-foreground">{group.memberCount} {group.memberCount === 1 ? "member" : "members"}</p></div>
                    <Button asChild size="sm" variant="outline"><Link href={`/groups/${group.id}`}>Open <ArrowRight className="size-3.5" /></Link></Button>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
        <div className="grid gap-6 sm:grid-cols-2">
          <Card>
            <CardHeader><CardTitle className="flex items-center gap-2"><Plus className="size-4" /> Create group</CardTitle><CardDescription>Start a group for your friends.</CardDescription></CardHeader>
            <CardContent><form className="space-y-4" onSubmit={createGroup}>
              <div className="space-y-2"><Label htmlFor="group-name">Group name</Label><Input id="group-name" maxLength={80} value={name} onChange={(e) => setName(e.target.value)} required /></div>
              <Button>Create group</Button>
            </form></CardContent>
          </Card>
          <Card>
            <CardHeader><CardTitle>Join group</CardTitle><CardDescription>Enter the code shared by a group member.</CardDescription></CardHeader>
            <CardContent><form className="space-y-4" onSubmit={joinGroup}>
              <div className="space-y-2"><Label htmlFor="join-code">6-character join code</Label><Input id="join-code" minLength={6} maxLength={6} value={joinCode} onChange={(e) => setJoinCode(e.target.value.toUpperCase())} required /></div>
              <Button>Join</Button>
            </form></CardContent>
          </Card>
        </div>
      </div>
    </RequireAuth>
  );
}
