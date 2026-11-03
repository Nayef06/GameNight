"use client";

import { useEffect, useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { Button } from "@/components/ui/button";
import { api, AppNotification } from "@/lib/api";

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
      <div className="max-w-xl space-y-5">
        <h1 className="text-2xl font-semibold">Notifications</h1>
        {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
        {loading ? <p>Loading notifications...</p> : notifications.length === 0 ? (
          <p className="text-sm text-muted-foreground">No notifications yet.</p>
        ) : (
          <ul className="divide-y divide-border">
            {notifications.map((notification) => (
              <li key={notification.id} className="flex items-center justify-between gap-4 py-3">
                <div>
                  <p className={notification.isRead ? "text-muted-foreground" : "font-medium"}>
                    {notification.message}
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
      </div>
    </RequireAuth>
  );
}
