// Notification DTOs. The server stores the type and its two values; the text is rendered here so
// it follows the reader's language.

import type { PageParams } from "@/types/common";

export const NotificationType = [
  "APPLICATION_ACCEPTED",
  "APPLICATION_REJECTED",
  "INTERVIEW_SCHEDULED",
  "MENTORING_SLOT_BOOKED",
  "MENTORING_SESSION_CANCELLED",
  "MENTORING_SESSION_RELEASED",
] as const;
export type NotificationType = (typeof NotificationType)[number];

export interface NotificationDTO {
  id: number;
  type: string; // The API may send newer event types than this client knows.
  subject: string | null;
  context: string | null;
  link: string | null;
  createdAt: string;
  readAt: string | null; // null = still unread, so still on the dashboard
}

export type NotificationFilters = PageParams;
