"use client";

import { ReactNode, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "./AuthProvider";

export function RequireAuth({ children }: { children: ReactNode }) {
  const { username, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && !username) router.replace("/login");
  }, [ready, username, router]);

  if (!ready || !username) return <p>Loading...</p>;
  return children;
}

