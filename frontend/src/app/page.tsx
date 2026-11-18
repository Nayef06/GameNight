"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { ArrowRight, Gamepad2, Layers3, Sparkles, Users } from "lucide-react";
import { useAuth } from "@/components/AuthProvider";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

const features = [
  { icon: Layers3, title: "Bring your libraries", description: "Add your games or import your Steam collection in a few clicks." },
  { icon: Users, title: "Find the overlap", description: "See the games everyone owns, with platforms and preferences in one place." },
  { icon: Sparkles, title: "Make it a game night", description: "Pick candidates, vote together, and get to the good part faster." },
];

export default function Home() {
  const { username, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && username) router.replace("/library");
  }, [ready, username, router]);

  if (!ready || username) return <p className="text-sm text-muted-foreground">Loading...</p>;

  return (
    <div className="space-y-16 pb-12 sm:space-y-20">
      <section className="relative overflow-hidden rounded-3xl border bg-card px-6 py-16 sm:px-12 sm:py-24">
        <div className="pointer-events-none absolute -right-20 -top-24 size-96 rounded-full border border-border/60" />
        <div className="pointer-events-none absolute -right-4 top-10 size-64 rounded-full border border-border/40" />
        <div className="relative max-w-2xl space-y-7">
          <Badge variant="outline" className="px-3 py-1"><Gamepad2 className="mr-1 size-3.5" /> Your next game night starts here</Badge>
          <div className="space-y-4">
            <h1 className="text-4xl font-bold tracking-tight sm:text-6xl">Less deciding.<br /><span className="text-muted-foreground">More playing.</span></h1>
            <p className="max-w-xl text-base leading-7 text-muted-foreground sm:text-lg">Find the games your friends share, choose your favorites, and let everyone vote on what to play next.</p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button asChild size="lg"><Link href="/register">Create account <ArrowRight className="size-4" /></Link></Button>
            <Button asChild variant="outline" size="lg"><Link href="/login">Login</Link></Button>
          </div>
        </div>
      </section>
      <section className="space-y-6">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-muted-foreground">How it works</p>
          <h2 className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl">From backlog to playtime</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {features.map(({ icon: Icon, title, description }) => (
            <Card key={title}>
              <CardContent className="space-y-4 pt-1">
                <div className="flex size-11 items-center justify-center rounded-xl border bg-muted text-foreground"><Icon className="size-5" /></div>
                <div className="space-y-1.5"><h3 className="font-semibold">{title}</h3><p className="leading-6 text-muted-foreground">{description}</p></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>
    </div>
  );
}
