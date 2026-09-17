"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { api, Group } from "@/lib/api";

export default function GroupsPage() {
  const [groups, setGroups] = useState<Group[]>([]);
  const [name, setName] = useState("");
  const [joinCode, setJoinCode] = useState("");
  const [error, setError] = useState("");

  const loadGroups = useCallback(async () => {
    try {
      setGroups(await api<Group[]>("/groups"));
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not load groups");
    }
  }, []);

  useEffect(() => { void loadGroups(); }, [loadGroups]);

  async function createGroup(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      await api<Group>("/groups", { method: "POST", body: { name } });
      setName("");
      await loadGroups();
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not create group");
    }
  }

  async function joinGroup(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      await api<Group>("/groups/join", { method: "POST", body: { joinCode } });
      setJoinCode("");
      await loadGroups();
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not join group");
    }
  }

  return (
    <RequireAuth>
      <section className="card">
        <h1>My Groups</h1>
        {error && <p className="error">{error}</p>}
        {groups.length === 0 ? <p>You have not created or joined a group yet.</p> : (
          <ul className="item-list">
            {groups.map((group) => (
              <li key={group.id}>
                <Link href={`/groups/${group.id}`}>{group.name}</Link>
                <span className="muted">Join code: <strong>{group.joinCode}</strong></span>
              </li>
            ))}
          </ul>
        )}
        <div className="two-columns">
          <form onSubmit={createGroup}>
            <h2>Create Group</h2>
            <label>Group name<input maxLength={80} value={name} onChange={(e) => setName(e.target.value)} required /></label>
            <button>Create</button>
          </form>
          <form onSubmit={joinGroup}>
            <h2>Join Group</h2>
            <label>6-character join code<input className="code-input" minLength={6} maxLength={6} value={joinCode} onChange={(e) => setJoinCode(e.target.value.toUpperCase())} required /></label>
            <button>Join</button>
          </form>
        </div>
      </section>
    </RequireAuth>
  );
}
