// Notification DTOs. The server stores the type and its two values; the text is rendered here so
// it follows the reader's language.

import type { PageParams } from "@/types/common";

export const NotificationType = [
  "APPLICATION_ACCEPTED",
  "APPLICATION_REJECTED",
  "INTERVIEW_SCHEDULED",
] as const;
export type NotificationType = (typeof NotificationType)[number];

export interface NotificationDTO {
  id: number;
  type: NotificationType;
  subject: string | null; // the job title
  context: string | null; // the company
  link: string | null;
  createdAt: string;
  readAt: string | null; // null = still unread, so still on the dashboard
}

export type NotificationFilters = PageParams;
