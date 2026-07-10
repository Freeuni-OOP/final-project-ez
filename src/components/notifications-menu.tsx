// Bell icon in the header: shows an unread-count badge, opens a dropdown of
// recent notifications, and lets you mark them read (individually, by
// clicking through to what they're about, or all at once). Signed-out users
// never see this - the header only mounts it when a user is present.

import { useEffect, useRef, useState } from "react";
import { Link } from "@tanstack/react-router";
import { Bell, Heart, MessageCircle, UserPlus } from "lucide-react";
import { formatDistanceToNowStrict } from "date-fns";

import type { Notification } from "@/lib/api";
import {
  useMarkNotificationRead,
  useNotifications,
  useUnreadNotificationCount,
} from "@/lib/queries";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

const ICONS = {
  LIKE: Heart,
  COMMENT: MessageCircle,
  FOLLOW: UserPlus,
} as const;

export function NotificationsMenu({ username }: { username: string }) {
  const { t } = useI18n();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const { data: unreadCount } = useUnreadNotificationCount(username);
  const { data: notifications } = useNotifications(open ? username : undefined);
  const markRead = useMarkNotificationRead();

  // Close on outside click or Escape.
  useEffect(() => {
    if (!open) return;
    const onPointerDown = (e: PointerEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  const hasUnread = !!unreadCount && unreadCount > 0;
  const unread = (notifications ?? []).filter((n) => !n.read);

  const markAllRead = () => {
    for (const n of unread) markRead.mutate(n.id);
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={t("notifications")}
        aria-expanded={open}
        className="relative rounded-md p-2 text-foreground/70 transition-colors hover:bg-accent hover:text-foreground"
      >
        <Bell className="h-4 w-4" />
        {hasUnread && (
          <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-semibold leading-none text-primary-foreground">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-40 mt-2 w-80 rounded-xl border border-border bg-background shadow-xl">
          <div className="flex items-center justify-between border-b border-border px-3 py-2">
            <span className="text-sm font-semibold text-foreground">{t("notifications")}</span>
            {unread.length > 0 && (
              <button
                type="button"
                onClick={markAllRead}
                className="text-xs font-medium text-primary hover:underline"
              >
                {t("mark_all_read")}
              </button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {!notifications || notifications.length === 0 ? (
              <p className="px-3 py-6 text-center text-sm text-muted-foreground">
                {t("notifications_empty")}
              </p>
            ) : (
              notifications.map((n) => (
                <NotificationRow
                  key={n.id}
                  notification={n}
                  onOpen={() => {
                    if (!n.read) markRead.mutate(n.id);
                    setOpen(false);
                  }}
                />
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function NotificationRow({
  notification: n,
  onOpen,
}: {
  notification: Notification;
  onOpen: () => void;
}) {
  const { t } = useI18n();
  const Icon = ICONS[n.type];

  const rowClass = cn(
    "flex items-start gap-3 px-3 py-3 text-sm transition-colors hover:bg-accent",
    !n.read && "bg-primary/5",
  );

  const iconClass = cn(
    "mt-0.5 h-4 w-4 shrink-0",
    n.type === "LIKE" && "text-red-500",
    n.type === "COMMENT" && "text-primary",
    n.type === "FOLLOW" && "text-foreground/70",
  );

  const body = (
    <>
      <Icon className={iconClass} />
      <span className="flex-1 text-foreground">
        <span className="font-semibold">{n.actor}</span>{" "}
        {n.type === "FOLLOW" ? (
          t("notif_followed")
        ) : (
          <>
            {n.type === "LIKE" ? t("notif_liked") : t("notif_commented")}{" "}
            <span className="font-medium">
              {n.compositionTitle ? `"${n.compositionTitle}"` : ""}
            </span>
          </>
        )}
        <span className="mt-0.5 block text-xs text-muted-foreground">
          {formatDistanceToNowStrict(new Date(n.createdAt), { addSuffix: true })}
        </span>
      </span>
      {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary" />}
    </>
  );

  if (n.type === "FOLLOW") {
    return (
      <Link to="/u/$username" params={{ username: n.actor }} onClick={onOpen} className={rowClass}>
        {body}
      </Link>
    );
  }

  if (n.compositionSlug) {
    return (
      <Link
        to="/c/$slug"
        params={{ slug: n.compositionSlug }}
        onClick={onOpen}
        className={rowClass}
      >
        {body}
      </Link>
    );
  }

  // No composition slug (shouldn't normally happen - likes/comments only
  // land on public compositions) - render as a plain, non-navigating row.
  return (
    <div onClick={onOpen} className={cn(rowClass, "cursor-pointer")}>
      {body}
    </div>
  );
}
