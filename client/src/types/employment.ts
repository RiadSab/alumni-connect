// Employment history DTOs. The alumnus owns these rows; the report aggregates them.

import { z } from "zod";
import { EmploymentStatus } from "@/types/enums";

export interface EmploymentEntryDTO {
  id: number;
  status: EmploymentStatus;
  employer: string | null;
  jobTitle: string | null;
  sector: string | null;
  city: string | null;
  startedAt: string; // ISO date
  endedAt: string | null; // null = this is where they are now
  lastConfirmedAt: string | null;
}

// POST/PATCH /api/employment/me — employer and jobTitle are required for EMPLOYED, which the
// server enforces and reports; the form mirrors it so the user finds out before the round trip.
export const saveEmploymentEntrySchema = z.object({
  status: z.enum(EmploymentStatus),
  employer: z.string().optional(),
  jobTitle: z.string().optional(),
  sector: z.string().optional(),
  city: z.string().optional(),
  startedAt: z.string().min(1),
  endedAt: z.string().optional(),
});
export type SaveEmploymentEntryInput = z.infer<typeof saveEmploymentEntrySchema>;

// GET /api/employment/confirm/{token} — the yearly nudge landing page.
export interface EmploymentConfirmDetailsDTO {
  firstName: string;
  status: EmploymentStatus;
  employer: string | null;
  jobTitle: string | null;
  startedAt: string;
}
