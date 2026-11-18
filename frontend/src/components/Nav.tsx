"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, BookOpen, Gamepad2, LogOut, Settings2, Users } from "lucide-react";
import { useAuth } from "./AuthProvider";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const links = [
  { href: "/library", label: "Library", icon: BookOpen },
  { href: "/groups", label: "Groups", icon: Users },
  { href: "/notifications", label: "Notifications", icon: Bell },
  { href: "/preferences", label: "Preferences", icon: Settings2 },
];

export function Nav() {
  const { username, ready, logout } = useAuth();
  const pathname = usePathname();

  return (
    <header className="sticky top-0 z-40 border-b border-border/70 bg-background/85 backdrop-blur-xl">
      <nav className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3 sm:px-6" aria-label="Main navigation">
        <Link className="group flex items-center gap-2.5" href={ready && username ? "/library" : "/"}>
          <span className="flex size-9 items-center justify-center rounded-xl bg-primary text-primary-foreground transition-transform group-hover:-rotate-6">
            <Gamepad2 className="size-5" aria-hidden="true" />
          </span>
          <span className="text-base font-bold tracking-tight">Game<span className="text-primary">Night</span></span>
        </Link>
        {ready && username ? (
          <>
            <div className="order-3 flex w-full items-center gap-1 overflow-x-auto pb-0.5 lg:order-2 lg:w-auto lg:pb-0">
              {links.map(({ href, label, icon: Icon }) => {
                const active = pathname === href || pathname.startsWith(`${href}/`);
                return (
                  <Link key={href} href={href} aria-current={active ? "page" : undefined}
                    className={cn("inline-flex h-9 shrink-0 items-center gap-2 rounded-lg px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring", active && "bg-accent text-foreground shadow-sm")}>
                    <Icon className={cn("size-4", active && "text-primary")} aria-hidden="true" />
                    {label}
                  </Link>
                );
              })}
            </div>
            <div className="order-2 flex items-center gap-2 lg:order-3">
              <span className="flex size-8 items-center justify-center rounded-full border border-primary/30 bg-primary/15 text-xs font-bold uppercase text-primary" title={username} aria-label={`Signed in as ${username}`}>
                {username.slice(0, 2)}
              </span>
              <Button variant="ghost" size="icon" onClick={logout} aria-label="Logout" title="Logout"><LogOut className="size-4" /></Button>
            </div>
          </>
        ) : ready ? (
          <div className="flex items-center gap-2">
            <Button asChild variant="ghost" size="sm"><Link href="/login">Login</Link></Button>
            <Button asChild size="sm"><Link href="/register">Get started</Link></Button>
          </div>
        ) : null}
      </nav>
    </header>
  );
}
