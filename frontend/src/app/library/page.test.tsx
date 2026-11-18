import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import LibraryPage from "./page";
import { api } from "@/lib/api";

vi.mock("@/components/RequireAuth", () => ({ RequireAuth: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("@/lib/api", async (original) => ({ ...await original<typeof import("@/lib/api")>(), api: vi.fn() }));

const apiMock = vi.mocked(api);
const minecraft = { id: 1, title: "Minecraft", tags: ["Survival"], multiplayerSupport: true, maxPlayers: 8 };
const terraria = { id: 2, title: "Terraria", tags: ["Adventure"], multiplayerSupport: true, maxPlayers: 8 };

beforeEach(() => {
  apiMock.mockReset();
  apiMock.mockImplementation(async (path) => {
    if (path === "/games") return [minecraft, terraria] as never;
    if (path === "/library") return [{ ...minecraft, platform: "STEAM" }] as never;
    return { steamId: null } as never;
  });
});
afterEach(cleanup);

test("shows ownership platform and allows the same title on a different platform", async () => {
  render(<LibraryPage />);
  expect(await screen.findByText("Minecraft — Steam")).toBeTruthy();
  const catalog = screen.getByRole("group", { name: "Game catalog" });
  expect(within(catalog).getByRole("button", { name: "Minecraft already in library on Steam" }).hasAttribute("disabled")).toBe(true);
  expect(within(catalog).getByAltText("Minecraft thumbnail")).toBeTruthy();
  expect(within(catalog).getByAltText("Terraria thumbnail")).toBeTruthy();
  fireEvent.change(screen.getByLabelText("Platform"), { target: { value: "XBOX" } });
  fireEvent.click(within(catalog).getByRole("button", { name: "Select Minecraft" }));
  fireEvent.click(screen.getByRole("button", { name: "Add to library" }));
  await waitFor(() => expect(apiMock).toHaveBeenCalledWith("/library", {
    method: "POST", body: { gameId: 1, platform: "XBOX" },
  }));
});

test("searches catalog thumbnails by title or tag", async () => {
  render(<LibraryPage />);
  const catalog = await screen.findByRole("group", { name: "Game catalog" });
  await within(catalog).findByAltText("Terraria thumbnail");
  const search = screen.getByLabelText("Search games");

  fireEvent.change(search, { target: { value: "terra" } });
  expect(within(catalog).getByAltText("Terraria thumbnail")).toBeTruthy();
  expect(within(catalog).queryByAltText("Minecraft thumbnail")).toBeNull();

  fireEvent.change(search, { target: { value: "survival" } });
  expect(within(catalog).getByAltText("Minecraft thumbnail")).toBeTruthy();
  expect(within(catalog).queryByAltText("Terraria thumbnail")).toBeNull();

  fireEvent.change(search, { target: { value: "not-a-game" } });
  expect(screen.getByText("No games match your search.")).toBeTruthy();
});

test("removes only the displayed platform's ownership", async () => {
  render(<LibraryPage />);
  await screen.findByText("Minecraft — Steam");
  fireEvent.click(screen.getByRole("button", { name: "Remove" }));
  await waitFor(() => expect(apiMock).toHaveBeenCalledWith("/library/1?platform=STEAM", { method: "DELETE" }));
});
