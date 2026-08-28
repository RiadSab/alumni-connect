// Notification endpoints. Everything is scoped to the caller by the server.

import { http } from "@/lib/http";
import type { Page } from "@/types/common";
import type { NotificationDTO, NotificationFilters } from "@/types/notification";

export const notificationsApi = {
  mine: (params?: NotificationFilters) =>
    http.get<Page<NotificationDTO>>("/notifications", { query: params }),

  unread: () => http.get<NotificationDTO[]>("/notifications/unread"),

  unreadCount: () => http.get<{ count: number }>("/notifications/unread-count"),

  markRead: (id: number) => http.post<void>(`/notifications/${id}/read`),

  markAllRead: () => http.post<{ read: number }>("/notifications/read-all"),
};
