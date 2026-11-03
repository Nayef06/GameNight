import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import GroupPage from "./page";
import { api } from "@/lib/api";

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

beforeEach(() => {
  creator = true;
  mixedPlatforms = true;
  apiMock.mockReset();
  apiMock.mockImplementation(async (path, options) => {
    if (path === "/groups/1") return {
      id: 1, name: "Friday", joinCode: "ABC234", members: [], currentUserIsCreator: creator,
      discordWebhookConfigured: true,
    } as never;
    if (path === "/groups/1/discord-webhook") return { discordWebhookConfigured: options?.method !== "DELETE" } as never;
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
