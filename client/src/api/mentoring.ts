// Mentoring endpoints. Publishing needs a roster row, booking needs a student profile — both
// enforced server-side.

import { http } from "@/lib/http";
import type { Page } from "@/types/common";
import type {
  MentoringBookingDTO,
  MentoringSlotDTO,
  PublishSlotInput,
  SlotFilters,
} from "@/types/mentoring";

export const mentoringApi = {
  slots: (filters?: SlotFilters) =>
    http.get<Page<MentoringSlotDTO>>("/mentoring/slots", { query: filters }),

  mySlots: () => http.get<MentoringSlotDTO[]>("/mentoring/slots/me"),

  publish: (body: PublishSlotInput) =>
    http.post<MentoringSlotDTO>("/mentoring/slots", { json: body }),

  cancelSlot: (id: number) => http.del<void>(`/mentoring/slots/${id}`),

  book: (slotId: number) => http.post<MentoringBookingDTO>(`/mentoring/slots/${slotId}/book`),

  myBookings: () => http.get<MentoringBookingDTO[]>("/mentoring/bookings/me"),

  remaining: () => http.get<{ remaining: number }>("/mentoring/bookings/me/remaining"),

  cancelBooking: (id: number) => http.del<void>(`/mentoring/bookings/${id}`),

  // Mentor side: who booked which of my slots, and did they turn up.
  mentorBookings: () => http.get<MentoringBookingDTO[]>("/mentoring/bookings/mentor"),

  markAttendance: (id: number, attended: boolean) =>
    http.post<void>(`/mentoring/bookings/${id}/attendance`, { query: { attended } }),

  // Public: the token from the confirmation email is the authentication.
  confirmDetails: (token: string) => http.get<MentoringBookingDTO>(`/mentoring/confirm/${token}`),

  confirm: (token: string) => http.post<void>(`/mentoring/confirm/${token}`),
};
