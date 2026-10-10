"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";

export default function Home() {
  const { username, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && username) router.replace("/library");
  }, [ready, username, router]);

  if (!ready || username) return <p>Loading...</p>;

  return (
    <section className="card hero">
      <h1>GameNight</h1>
      <p>Find the games everyone in your group owns.</p>
      <div className="actions">
        <Link className="button" href="/login">Login</Link>
        <Link className="button secondary" href="/register">Create Account</Link>
      </div>
    </section>
  );
}
