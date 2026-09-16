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
            <Link href="/library">My Library</Link>
            <Link href="/groups">Groups</Link>
            <span>{username}</span>
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

