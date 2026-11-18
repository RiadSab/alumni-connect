import { Bell, CalendarClock, CalendarMinus, CalendarPlus, CalendarX, CheckCircle2, XCircle } from "lucide-react";
import type { LucideIcon } from "lucide-react";
import type { TKey } from "@/features/i18n/dictionary";
import type { NotificationType } from "@/types/notification";

const notificationPresentation = {
  APPLICATION_ACCEPTED: { textKey: "notif.accepted", icon: CheckCircle2 },
  APPLICATION_REJECTED: { textKey: "notif.rejected", icon: XCircle },
  INTERVIEW_SCHEDULED: { textKey: "notif.interview", icon: CalendarClock },
  MENTORING_SLOT_BOOKED: { textKey: "notif.mentoringBooked", icon: CalendarPlus },
  MENTORING_SESSION_CANCELLED: { textKey: "notif.mentoringCancelled", icon: CalendarX },
  MENTORING_SESSION_RELEASED: { textKey: "notif.mentoringReleased", icon: CalendarMinus },
} satisfies Record<NotificationType, { textKey: TKey; icon: LucideIcon }>;

export function getNotificationPresentation(type: string) {
  if (Object.hasOwn(notificationPresentation, type)) {
    return notificationPresentation[type as NotificationType];
  }
  return { textKey: "notif.generic", icon: Bell } as const;
}
