"use client";

import Link from "next/link";
import { useAuth } from "@/components/AuthProvider";

export default function Home() {
  const { username, ready } = useAuth();

  return (
    <section className="card hero">
      <h1>GameNight</h1>
      <p>Find the games that everyone in your group already owns.</p>
      {ready && username ? (
        <div className="actions">
          <Link className="button" href="/library">Manage my library</Link>
          <Link className="button secondary" href="/groups">View my groups</Link>
        </div>
      ) : ready ? (
        <div className="actions">
          <Link className="button" href="/register">Create account</Link>
          <Link className="button secondary" href="/login">Login</Link>
        </div>
      ) : null}
    </section>
  );
}

