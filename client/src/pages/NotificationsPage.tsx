// Every notification the user has had (/notifications). Unread ones are marked; reading one here
// also takes it off the dashboard, since the dashboard shows unread only.

import { useState } from "react";
import { Link } from "react-router-dom";
import { Bell, CalendarClock, CheckCircle2, XCircle } from "lucide-react";
import {
  useMarkAllNotificationsRead,
  useMarkNotificationRead,
  useNotifications,
} from "@/features/notifications/hooks";
import { notificationTextKeys } from "@/features/notifications/labels";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import type { NotificationDTO } from "@/types/notification";

const ICONS = {
  APPLICATION_ACCEPTED: CheckCircle2,
  APPLICATION_REJECTED: XCircle,
  INTERVIEW_SCHEDULED: CalendarClock,
};

export function NotificationsPage() {
  const { t } = useT();
  const [page, setPage] = useState(0);
  const { data, isLoading } = useNotifications({ page });
  const markAll = useMarkAllNotificationsRead();

  const hasUnread = (data?.content ?? []).some((n) => n.readAt === null);

  return (
    <div className="mx-auto max-w-2xl space-y-5">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold text-foreground">{t("notifications.title")}</h1>
        {hasUnread && (
          <Button variant="outline" size="sm" onClick={() => markAll.mutate()} disabled={markAll.isPending}>
            {t("notifications.markAll")}
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full rounded-lg" />
          ))}
        </div>
      ) : !data || data.empty ? (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-12 text-center">
          <Bell className="mb-3 size-6 text-[var(--color-stone)]" />
          <p className="text-sm text-[var(--color-slate)]">{t("notifications.empty")}</p>
        </div>
      ) : (
        <>
          <ul className="space-y-2">
            {data.content.map((notification) => (
              <NotificationRow key={notification.id} notification={notification} />
            ))}
          </ul>

          {data.totalPages > 1 && (
            <nav className="flex items-center justify-center gap-3">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage(page - 1)}>
                {t("pager.prev")}
              </Button>
              <span className="text-sm text-[var(--color-steel)]">
                {t("pager.page", { n: data.number + 1, total: data.totalPages })}
              </span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage(page + 1)}>
                {t("pager.next")}
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}

function NotificationRow({ notification }: { notification: NotificationDTO }) {
  const { t, lang } = useT();
  const markRead = useMarkNotificationRead();
  const unread = notification.readAt === null;
  const Icon = ICONS[notification.type];

  const text = t(notificationTextKeys[notification.type], {
    subject: notification.subject ?? "",
    context: notification.context ?? "",
  });
  const when = new Date(notification.createdAt).toLocaleDateString(lang, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });

  const body = (
    <div className="flex items-start gap-3">
      <Icon className={`mt-0.5 size-5 shrink-0 ${unread ? "text-[var(--color-brand-purple-800)]" : "text-[var(--color-stone)]"}`} />
      <div className="min-w-0 flex-1">
        <p className={`text-sm ${unread ? "font-semibold text-foreground" : "text-[var(--color-charcoal)]"}`}>
          {text}
        </p>
        <p className="mt-0.5 text-xs text-[var(--color-steel)]">{when}</p>
      </div>
      {unread && (
        <span className="shrink-0 rounded-full bg-[var(--color-tint-lavender)] px-2 py-0.5 text-[11px] font-semibold text-[var(--color-brand-purple-800)]">
          {t("notifications.new")}
        </span>
      )}
    </div>
  );

  const className = `block rounded-lg border p-4 transition-shadow hover:shadow-sm ${
    unread ? "border-[var(--color-brand-purple-800)] bg-card" : "border-border bg-card"
  }`;

  // Reading it is a side effect of opening it; a notification with no link is just marked.
  function open() {
    if (unread) markRead.mutate(notification.id);
  }

  return (
    <li>
      {notification.link === null ? (
        <button type="button" onClick={open} className={`${className} w-full text-left`}>
          {body}
        </button>
      ) : (
        <Link to={notification.link} onClick={open} className={className}>
          {body}
        </Link>
      )}
    </li>
  );
}
