export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

type ApiOptions = Omit<RequestInit, "body"> & { body?: unknown };

export async function api<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });

  if (!response.ok) {
    const data = await response.json().catch(() => ({ message: "Request failed" }));
    throw new Error(data.message ?? "Request failed");
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

export type AuthResponse = { token: string; username: string };
export type Game = {
  id: number;
  title: string;
  tags: string[];
  multiplayerSupport: boolean;
  maxPlayers: number;
};
export type SharedGame = Game & { preferenceScore: number };
export type GameSelection = {
  id: number;
  gameId: number;
  gameTitle: string;
  selectedAt: string;
};
export type GameNightSession = {
  id: number;
  groupId: number;
  createdBy: string;
  createdAt: string;
  status: "OPEN" | "CLOSED";
  candidates: { id: number; title: string }[];
};
export type SessionVotingResults = {
  status: "OPEN" | "CLOSED";
  currentUserVoteGameId: number | null;
  results: { gameId: number; title: string; votes: number }[];
};
export type Group = { id: number; name: string; joinCode: string; memberCount: number };
export type GroupMember = { id: number; username: string; creator: boolean };
export type GroupDetails = {
  id: number;
  name: string;
  joinCode: string;
  createdBy: string;
  createdAt: string;
  currentUserIsCreator: boolean;
  members: GroupMember[];
};
export type MultiplayerPreference = "NO_PREFERENCE" | "MULTIPLAYER" | "SINGLE_PLAYER";
export type Preferences = {
  preferredTags: string[];
  multiplayerPreference: MultiplayerPreference;
  preferredPlayerCount: number | null;
};
