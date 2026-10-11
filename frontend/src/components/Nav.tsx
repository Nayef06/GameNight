"use client";

import Link from "next/link";
import { useAuth } from "./AuthProvider";
import { Button } from "@/components/ui/button";

export function Nav() {
  const { username, ready, logout } = useAuth();

  return (
    <header className="border-b">
      <nav className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link className="text-base font-semibold" href={ready && username ? "/library" : "/"}>GameNight</Link>
        {ready && username ? (
          <div className="flex flex-wrap items-center gap-1">
            <Button asChild variant="ghost" size="sm"><Link href="/library">Library</Link></Button>
            <Button asChild variant="ghost" size="sm"><Link href="/groups">Groups</Link></Button>
            <Button asChild variant="ghost" size="sm"><Link href="/preferences">Preferences</Link></Button>
            <Button variant="ghost" size="sm" onClick={logout}>Logout</Button>
          </div>
        ) : ready ? (
          <div className="flex items-center gap-1">
            <Button asChild variant="ghost" size="sm"><Link href="/login">Login</Link></Button>
            <Button asChild variant="outline" size="sm"><Link href="/register">Register</Link></Button>
          </div>
        ) : null}
      </nav>
    </header>
  );
}
