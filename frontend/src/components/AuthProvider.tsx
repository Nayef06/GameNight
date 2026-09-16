"use client";

import { createContext, ReactNode, useContext, useEffect, useState } from "react";
import { useRouter } from "next/navigation";

type AuthContextValue = {
  username: string | null;
  ready: boolean;
  saveLogin: (token: string, username: string) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(null);
  const [ready, setReady] = useState(false);
  const router = useRouter();

  useEffect(() => {
    setUsername(localStorage.getItem("username"));
    setReady(true);
  }, []);

  function saveLogin(token: string, nextUsername: string) {
    localStorage.setItem("token", token);
    localStorage.setItem("username", nextUsername);
    setUsername(nextUsername);
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    setUsername(null);
    router.push("/login");
  }

  return (
    <AuthContext.Provider value={{ username, ready, saveLogin, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}

