"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { api, AuthResponse } from "@/lib/api";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Gamepad2 } from "lucide-react";

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
    <Card className="mx-auto mt-8 max-w-md">
      <CardHeader className="space-y-4 text-center">
        <div className="mx-auto flex size-12 items-center justify-center rounded-xl border bg-muted"><Gamepad2 className="size-6" aria-hidden="true" /></div>
        <div className="space-y-1"><CardTitle className="text-2xl font-semibold">Login</CardTitle>
        <CardDescription>Welcome back. Sign in to plan your next game night.</CardDescription></div>
      </CardHeader>
      <CardContent className="space-y-4">
        <form className="space-y-4" onSubmit={submit}>
          <div className="space-y-2"><Label htmlFor="username">Username</Label><Input id="username" autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} required /></div>
          <div className="space-y-2"><Label htmlFor="password">Password</Label><Input id="password" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
          {error && <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
          <Button size="lg" className="w-full" disabled={busy}>{busy ? "Logging in..." : "Login"}</Button>
        </form>
        <p className="text-center text-sm text-muted-foreground">No account? <Link className="font-medium text-foreground underline underline-offset-4" href="/register">Register</Link></p>
      </CardContent>
    </Card>
  );
}

