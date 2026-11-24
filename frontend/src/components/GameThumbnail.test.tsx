import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, expect, test } from "vitest";
import { GameThumbnail } from "./GameThumbnail";

afterEach(cleanup);

test("loads Steam artwork for a game outside the preset catalog", () => {
  render(<GameThumbnail title="An imported game" steamAppId={620} />);
  const image = screen.getByAltText("An imported game thumbnail");
  expect(decodeURIComponent(image.getAttribute("src")!)).toContain("/steam/apps/620/header.jpg");
  fireEvent.error(image);
  expect(screen.getByRole("img", { name: "No thumbnail for An imported game" })).toBeTruthy();
});

test("falls back from Steam to a bundled image, then to the game icon", () => {
  render(<GameThumbnail title="Terraria" steamAppId={105600} />);
  const image = screen.getByAltText("Terraria thumbnail");
  fireEvent.error(image);
  expect(decodeURIComponent(image.getAttribute("src")!)).toContain("/games/terraria.jpg");
  fireEvent.error(image);
  expect(screen.getByRole("img", { name: "No thumbnail for Terraria" })).toBeTruthy();
});

test("uses local artwork when no Steam app ID is available", () => {
  render(<GameThumbnail title="Minecraft" />);
  expect(decodeURIComponent(screen.getByAltText("Minecraft thumbnail").getAttribute("src")!)).toContain("/games/minecraft.jpg");
});

test("loads new artwork when the selected game changes after an image failure", () => {
  const { rerender } = render(<GameThumbnail title="First game" steamAppId={620} />);
  fireEvent.error(screen.getByAltText("First game thumbnail"));
  rerender(<GameThumbnail title="Second game" steamAppId={400} />);
  expect(decodeURIComponent(screen.getByAltText("Second game thumbnail").getAttribute("src")!)).toContain("/steam/apps/400/header.jpg");
});
