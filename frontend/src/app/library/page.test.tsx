import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import LibraryPage from "./page";
import { api } from "@/lib/api";

vi.mock("@/components/RequireAuth", () => ({ RequireAuth: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("@/lib/api", async (original) => ({ ...await original<typeof import("@/lib/api")>(), api: vi.fn() }));

const apiMock = vi.mocked(api);
const minecraft = { id: 1, title: "Minecraft", tags: ["Survival"], multiplayerSupport: true, maxPlayers: 8 };

beforeEach(() => {
  apiMock.mockReset();
  apiMock.mockImplementation(async (path) => {
    if (path === "/games") return [minecraft] as never;
    if (path === "/library") return [{ ...minecraft, platform: "STEAM" }] as never;
    return { steamId: null } as never;
  });
});
afterEach(cleanup);

test("shows ownership platform and allows the same title on a different platform", async () => {
  render(<LibraryPage />);
  expect(await screen.findByText("Minecraft — Steam")).toBeTruthy();
  expect(screen.queryByRole("option", { name: "Minecraft" })).toBeNull();
  fireEvent.change(screen.getByLabelText("Platform"), { target: { value: "XBOX" } });
  fireEvent.change(screen.getByLabelText("Game"), { target: { value: "1" } });
  fireEvent.click(screen.getByRole("button", { name: "Add to library" }));
  await waitFor(() => expect(apiMock).toHaveBeenCalledWith("/library", {
    method: "POST", body: { gameId: 1, platform: "XBOX" },
  }));
});

test("removes only the displayed platform's ownership", async () => {
  render(<LibraryPage />);
  await screen.findByText("Minecraft — Steam");
  fireEvent.click(screen.getByRole("button", { name: "Remove" }));
  await waitFor(() => expect(apiMock).toHaveBeenCalledWith("/library/1?platform=STEAM", { method: "DELETE" }));
});
