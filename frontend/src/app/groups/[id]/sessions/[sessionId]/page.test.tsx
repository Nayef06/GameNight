import { act, cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import SessionPage from "./page";
import { api } from "@/lib/api";

vi.mock("next/navigation", () => ({ useParams: () => ({ id: "1", sessionId: "2" }) }));
vi.mock("next/link", () => ({ default: ({ children, href }: { children: React.ReactNode; href: string }) =>
  <a href={href}>{children}</a> }));
vi.mock("@/components/RequireAuth", () => ({ RequireAuth: ({ children }: { children: React.ReactNode }) =>
  <>{children}</> }));
vi.mock("@/components/ui/button", () => ({ Button: ({ children, asChild, ...props }: {
  children: React.ReactNode; asChild?: boolean; [key: string]: unknown;
}) => asChild ? children : <button {...props}>{children}</button> }));
vi.mock("@/lib/api", () => ({ api: vi.fn() }));

const apiMock = vi.mocked(api);
let resultRequests: number;
let closeOnSecondRequest: boolean;

beforeEach(() => {
  vi.useFakeTimers();
  resultRequests = 0;
  closeOnSecondRequest = false;
  apiMock.mockReset();
  apiMock.mockImplementation(async (path) => {
    if (path.endsWith("/results")) {
      resultRequests++;
      return {
        status: closeOnSecondRequest && resultRequests > 1 ? "CLOSED" : "OPEN",
        currentUserVoteGameId: 11,
        results: [
          { gameId: 11, title: "Terraria", votes: resultRequests },
          { gameId: 19, title: "Minecraft", votes: 0 },
        ],
      } as never;
    }
    if (path === "/groups/1") return { currentUserIsCreator: false } as never;
    return {
      id: 2, groupId: 1, createdBy: "alice", createdAt: "2026-11-02T12:00:00Z",
      status: "OPEN", candidates: [{ id: 11, title: "Terraria" }, { id: 19, title: "Minecraft" }],
    } as never;
  });
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
});

async function showSession() {
  render(<SessionPage />);
  await act(async () => { await Promise.resolve(); });
  expect(screen.getByText(/Terraria.*1 vote/)).toBeTruthy();
  expect(resultRequests).toBe(1);
}

test("updates vote counts after five seconds without navigating", async () => {
  await showSession();
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });

  expect(screen.getByText(/Terraria.*2 votes/)).toBeTruthy();
  expect(resultRequests).toBe(2);
});

test("clears polling when the session page is left", async () => {
  const view = render(<SessionPage />);
  await act(async () => { await Promise.resolve(); });
  expect(resultRequests).toBe(1);
  view.unmount();

  await act(async () => { await vi.advanceTimersByTimeAsync(15000); });
  expect(resultRequests).toBe(1);
});

test("stops polling when results report that voting closed", async () => {
  closeOnSecondRequest = true;
  await showSession();
  await act(async () => { await vi.advanceTimersByTimeAsync(5000); });
  expect(screen.getByText("Status: CLOSED")).toBeTruthy();

  await act(async () => { await vi.advanceTimersByTimeAsync(15000); });
  expect(resultRequests).toBe(2);
});
