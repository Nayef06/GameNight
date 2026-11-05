import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import GroupPage from "./page";
import { api, GroupStatistics } from "@/lib/api";

vi.mock("next/navigation", () => ({ useParams: () => ({ id: "1" }), useRouter: () => ({ push: vi.fn() }) }));
vi.mock("next/link", () => ({ default: ({ children, href }: { children: React.ReactNode; href: string }) => <a href={href}>{children}</a> }));
vi.mock("@/components/RequireAuth", () => ({ RequireAuth: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("@/components/ui/button", () => ({ Button: ({ children, asChild, ...props }: {
  children: React.ReactNode; asChild?: boolean; [key: string]: unknown;
}) => asChild ? children : <button {...props}>{children}</button> }));
vi.mock("@/lib/api", async (original) => ({ ...await original<typeof import("@/lib/api")>(), api: vi.fn() }));

const apiMock = vi.mocked(api);
let creator: boolean;
let mixedPlatforms: boolean;
let statistics: GroupStatistics;
let statisticsFailure: boolean;

beforeEach(() => {
  creator = true;
  mixedPlatforms = true;
  statistics = { completedSessions: 8, gamesSelected: 12, mostSelectedGame: { title: "Terraria", count: 5 } };
  statisticsFailure = false;
  apiMock.mockReset();
  apiMock.mockImplementation(async (path, options) => {
    if (path === "/groups/1") return {
      id: 1, name: "Friday", joinCode: "ABC234", members: [], currentUserIsCreator: creator,
      discordWebhookConfigured: true,
    } as never;
    if (path === "/groups/1/discord-webhook") return { discordWebhookConfigured: options?.method !== "DELETE" } as never;
    if (path === "/groups/1/statistics") {
      if (statisticsFailure) throw new Error("Could not load group statistics");
      return statistics as never;
    }
    if (path === "/groups/1/selections") {
      statistics = { ...statistics, gamesSelected: 13, mostSelectedGame: { title: "Terraria", count: 6 } };
      return { id: 1, gameId: 10, gameTitle: "Terraria", selectedAt: "2026-11-05T20:00:00Z" } as never;
    }
    if (path.endsWith("/shared-games")) return [{
      id: 10, title: "Terraria", tags: ["Survival"], multiplayerSupport: true, maxPlayers: 8,
      preferenceScore: 0, platforms: mixedPlatforms ? ["STEAM", "XBOX"] : ["STEAM"],
    }] as never;
    if (path.endsWith("/active")) return undefined as never;
    return [] as never;
  });
});
afterEach(cleanup);

test("creator configures and removes the webhook without displaying the saved secret", async () => {
  render(<GroupPage />);
  await screen.findByText("A Discord webhook is configured.");
  const field = screen.getByLabelText("Webhook URL") as HTMLInputElement;
  expect(field.value).toBe("");
  const webhookUrl = "https://discord.com/api/webhooks/123456789012345678/secret_token_abcdefghijklmnopqrstuvwxyz";
  fireEvent.change(field, { target: { value: webhookUrl } });
  fireEvent.click(screen.getByRole("button", { name: "Save" }));
  await screen.findByText("Discord webhook saved.");
  expect(apiMock).toHaveBeenCalledWith("/groups/1/discord-webhook", { method: "PUT", body: { webhookUrl } });
  expect(field.value).toBe("");
  fireEvent.click(screen.getByRole("button", { name: "Remove" }));
  await screen.findByText("No Discord webhook is configured.");
  expect(apiMock).toHaveBeenCalledWith("/groups/1/discord-webhook", { method: "DELETE" });
});

test("members see only configuration status and differing platforms carry the cross-play note", async () => {
  creator = false;
  render(<GroupPage />);
  await screen.findByText("A Discord webhook is configured.");
  expect(screen.queryByLabelText("Webhook URL")).toBeNull();
  expect(screen.getByText("Platforms: Steam, Xbox")).toBeTruthy();
  expect(screen.getByText("Members own this game on different platforms. Cross-play compatibility has not been verified.")).toBeTruthy();
});

test("a single platform shows its label without the different-platform warning", async () => {
  mixedPlatforms = false;
  render(<GroupPage />);
  await waitFor(() => expect(screen.getByText("Platforms: Steam")).toBeTruthy());
  expect(screen.queryByText(/Cross-play compatibility/)).toBeNull();
});

test("shows the three group statistics from the API", async () => {
  render(<GroupPage />);
  await screen.findByText("Group Statistics");
  expect(screen.getByText("Completed Sessions: 8")).toBeTruthy();
  expect(screen.getByText("Games Selected: 12")).toBeTruthy();
  expect(screen.getByText("Most Selected Game: Terraria (5 times)")).toBeTruthy();
});

test("groups without activity show zero counts and None yet", async () => {
  statistics = { completedSessions: 0, gamesSelected: 0, mostSelectedGame: null };
  render(<GroupPage />);
  await screen.findByText("Completed Sessions: 0");
  expect(screen.getByText("Games Selected: 0")).toBeTruthy();
  expect(screen.getByText("Most Selected Game: None yet")).toBeTruthy();
});

test("refreshes statistics after recording a game selection", async () => {
  render(<GroupPage />);
  await screen.findByText("Games Selected: 12");
  fireEvent.click(screen.getByRole("button", { name: "Choose game" }));
  await screen.findByText("Games Selected: 13");
  expect(screen.getByText("Most Selected Game: Terraria (6 times)")).toBeTruthy();
  expect(screen.getByText("Completed Sessions: 8")).toBeTruthy();
});

test("uses singular time for one recorded selection", async () => {
  statistics = { completedSessions: 0, gamesSelected: 1, mostSelectedGame: { title: "Terraria", count: 1 } };
  render(<GroupPage />);
  await screen.findByText("Most Selected Game: Terraria (1 time)");
});

test("statistics errors preserve the group page and do not display misleading zero counts", async () => {
  statisticsFailure = true;
  render(<GroupPage />);
  await screen.findByRole("heading", { name: "Friday" });
  expect(screen.getByRole("alert").textContent).toBe("Could not load group statistics");
  expect(screen.queryByText("Completed Sessions: 0")).toBeNull();
  expect(screen.getByRole("button", { name: "Choose game" })).toBeTruthy();
});
