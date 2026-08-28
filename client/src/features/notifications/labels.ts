// Which sentence each notification type renders as. The server sends the type and the two values.

import type { NotificationType } from "@/types/notification";

export const notificationTextKeys = {
  APPLICATION_ACCEPTED: "notif.accepted",
  APPLICATION_REJECTED: "notif.rejected",
  INTERVIEW_SCHEDULED: "notif.interview",
} as const satisfies Record<NotificationType, string>;
