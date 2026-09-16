"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, AuthResponse } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const { saveLogin } = useAuth();
  const router = useRouter();

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const result = await api<AuthResponse>("/auth/login", {
        method: "POST",
        body: { username, password },
      });
      saveLogin(result.token, result.username);
      router.push("/library");
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Login failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card form-card">
      <h1>Login</h1>
      <form onSubmit={submit}>
        <label>Username<input value={username} onChange={(e) => setUsername(e.target.value)} required /></label>
        <label>Password<input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required /></label>
        {error && <p className="error">{error}</p>}
        <button disabled={busy}>{busy ? "Logging in..." : "Login"}</button>
      </form>
      <p>No account? <Link href="/register">Register</Link></p>
    </section>
  );
}

