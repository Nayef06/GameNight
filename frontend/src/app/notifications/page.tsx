"use client";

import { useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { Button } from "@/components/ui/button";
import { api, AppNotification } from "@/lib/api";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Bell } from "lucide-react";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [markingId, setMarkingId] = useState<number | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    api<AppNotification[]>("/notifications")
      .then((loaded) => { if (active) setNotifications(loaded); })
      .catch((problem) => {
        if (active) setError(problem instanceof Error ? problem.message : "Could not load notifications");
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  async function markRead(id: number) {
    setMarkingId(id);
    setError("");
    try {
      const updated = await api<AppNotification>(`/notifications/${id}/read`, { method: "PATCH" });
      setNotifications((current) => current.map((notification) =>
        notification.id === id ? updated : notification));
    } catch (problem) {
      setError(problem instanceof Error ? problem.message : "Could not mark notification as read");
    } finally {
      setMarkingId(null);
    }
  }

  return (
    <RequireAuth>
      <div className="max-w-3xl space-y-6">
        <div className="space-y-1"><h1 className="text-3xl font-semibold tracking-tight">Notifications</h1><p className="text-sm text-muted-foreground">Updates from your groups and game nights.</p></div>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        <Card>
          <CardHeader><CardTitle className="flex items-center gap-2"><Bell className="size-4" /> Inbox</CardTitle><CardDescription>{notifications.filter((notification) => !notification.isRead).length} unread</CardDescription></CardHeader>
          <CardContent>
        {loading ? <p className="text-sm text-muted-foreground">Loading notifications...</p> : notifications.length === 0 ? (
          <div className="rounded-lg border border-dashed p-8 text-center text-sm text-muted-foreground">No notifications yet.</div>
        ) : (
          <ul className="divide-y divide-border">
            {notifications.map((notification) => (
              <li key={notification.id} className="flex flex-wrap items-center justify-between gap-4 py-4 first:pt-0 last:pb-0">
                <div>
                  <p className={notification.isRead ? "text-muted-foreground" : "font-medium"}>
                    {notification.message} {!notification.isRead && <Badge variant="secondary" className="ml-1">New</Badge>}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {new Date(notification.createdAt).toLocaleString()}
                  </p>
                </div>
                {!notification.isRead && (
                  <Button type="button" variant="outline" size="sm"
                    disabled={markingId === notification.id}
                    onClick={() => markRead(notification.id)}>Mark as Read</Button>
                )}
              </li>
            ))}
          </ul>
        )}
          </CardContent>
        </Card>
      </div>
    </RequireAuth>
  );
}
