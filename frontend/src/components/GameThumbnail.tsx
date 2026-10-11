import Image from "next/image";
import { Gamepad2 } from "lucide-react";
import { cn } from "@/lib/utils";

const thumbnails: Record<string, string> = {
  "Among Us": "/games/among-us.jpg",
  "Baldur's Gate 3": "/games/baldurs-gate-3.jpg",
  Celeste: "/games/celeste.jpg",
  "Counter-Strike 2": "/games/counter-strike-2.jpg",
  "Dead by Daylight": "/games/dead-by-daylight.jpg",
  "Deep Rock Galactic": "/games/deep-rock-galactic.jpg",
  Hades: "/games/hades.jpg",
  "It Takes Two": "/games/it-takes-two.jpg",
  "Lethal Company": "/games/lethal-company.jpg",
  "Mario Kart 8 Deluxe": "/games/mario-kart-8-deluxe.jpg",
  Minecraft: "/games/minecraft.jpg",
  "Overcooked! 2": "/games/overcooked-2.jpg",
  Phasmophobia: "/games/phasmophobia.jpg",
  "Portal 2": "/games/portal-2.jpg",
  "Rocket League": "/games/rocket-league.jpg",
  "Sea of Thieves": "/games/sea-of-thieves.jpg",
  "Slay the Spire": "/games/slay-the-spire.jpg",
  "Stardew Valley": "/games/stardew-valley.jpg",
  Terraria: "/games/terraria.jpg",
  Valorant: "/games/valorant.png",
};

export function GameThumbnail({ title, className }: { title: string; className?: string }) {
  const src = thumbnails[title];
  const styles = cn("h-14 w-28 shrink-0 rounded-md border border-border bg-muted object-cover", className);

  if (!src) {
    return <div className={cn(styles, "flex items-center justify-center")} role="img" aria-label={`No thumbnail for ${title}`}><Gamepad2 className="size-5 text-muted-foreground" aria-hidden="true" /></div>;
  }

  return <Image src={src} alt={`${title} thumbnail`} width={112} height={56} className={styles} />;
}
