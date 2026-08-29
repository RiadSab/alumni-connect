// Mentoring DTOs. A slot is an alumnus's offered time; a booking is a student's claim on one.

import { z } from "zod";
import type { Fields, MentoringTrack } from "@/types/enums";
import type { PageParams } from "@/types/common";

export type SlotStatus = "OPEN" | "BOOKED" | "CANCELLED";

export type BookingState =
  | "BOOKED"
  | "CONFIRMED"
  | "RELEASED_UNCONFIRMED"
  | "CANCELLED_BY_STUDENT"
  | "CANCELLED_BY_MENTOR"
  | "ATTENDED"
  | "NO_SHOW";

// No meetingLink here on purpose: the server never puts it in a browsable list.
export interface MentoringSlotDTO {
  id: number;
  mentorName: string;
  mentorPromotion: number | null;
  track: MentoringTrack;
  targetField: Fields | null; // null = open to any filière
  startsAt: string;
  durationMinutes: number;
  status: SlotStatus;
}

export interface MentoringBookingDTO {
  id: number;
  state: BookingState;
  slot: MentoringSlotDTO;
  studentName: string;
  meetingLink: string | null; // only the two parties get it
}

export const publishSlotSchema = z.object({
  startsAt: z.string().min(1),
  durationMinutes: z.number().int().min(10).max(90).optional(),
  track: z.string().min(1),
  targetField: z.string().optional(),
  meetingLink: z.string().min(1),
});
export type PublishSlotInput = z.infer<typeof publishSlotSchema>;

export interface SlotFilters extends PageParams {
  track?: MentoringTrack;
  field?: Fields;
}
