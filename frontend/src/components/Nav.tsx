"use client";

import Link from "next/link";
import { useAuth } from "./AuthProvider";

export function Nav() {
  const { username, ready, logout } = useAuth();

  return (
    <header>
      <nav>
        <Link className="brand" href="/">GameNight</Link>
        {ready && username ? (
          <div className="nav-links">
            <span className="separator">|</span>
            <Link href="/library">My Library</Link>
            <span className="separator">|</span>
            <Link href="/groups">My Groups</Link>
            <span className="separator">|</span>
            <Link href="/preferences">Preferences</Link>
            <span className="separator">|</span>
            <button className="link-button" onClick={logout}>Logout</button>
          </div>
        ) : ready ? (
          <div className="nav-links">
            <Link href="/login">Login</Link>
            <Link href="/register">Register</Link>
          </div>
        ) : null}
      </nav>
    </header>
  );
}
