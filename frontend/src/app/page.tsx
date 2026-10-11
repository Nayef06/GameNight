"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/AuthProvider";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

export default function Home() {
  const { username, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && username) router.replace("/library");
  }, [ready, username, router]);

  if (!ready || username) return <p className="text-sm text-muted-foreground">Loading...</p>;

  return (
    <Card className="mx-auto max-w-md">
      <CardHeader>
        <CardTitle className="text-xl">GameNight</CardTitle>
        <CardDescription>Find the games everyone in your group owns.</CardDescription>
      </CardHeader>
      <CardContent className="flex gap-2">
        <Button asChild><Link href="/login">Login</Link></Button>
        <Button asChild variant="outline"><Link href="/register">Create account</Link></Button>
      </CardContent>
    </Card>
  );
}
