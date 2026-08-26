// Alumni roster DTOs (admin-only). The roster is the school's own list of graduates.

import { z } from "zod";
import { nonBlank } from "@/lib/validation";
import type { Fields } from "@/types/enums";
import type { PageParams } from "@/types/common";

export interface AlumniRecordDTO {
  id: number;
  studentId: string;
  firstName: string;
  lastName: string;
  fieldOfStudy: Fields;
  promotionYear: number;
  email: string | null;
  claimed: boolean;
  claimedAt: string | null;
  optedOutAt: string | null;
}

// POST /api/admin/alumni/import — valid rows import even when others fail.
export interface AlumniImportResultDTO {
  dryRun: boolean;
  created: number;
  updated: number;
  errors: { line: number; message: string }[];
}

export interface AlumniRecordFilters extends PageParams {
  promotionYear?: number;
}

// GET /api/alumni/claim/{token} — the school's facts, shown before the password is set.
export interface AlumniClaimDetailsDTO {
  firstName: string;
  lastName: string;
  promotionYear: number;
  fieldOfStudy: Fields;
  email: string;
}

// POST /api/alumni/claim
export const claimAccountSchema = z.object({
  token: nonBlank(),
  password: nonBlank(),
  phoneNumber: z.string().optional(),
});
export type ClaimAccountInput = z.infer<typeof claimAccountSchema>;

// POST /api/admin/alumni/invite
export interface ClaimInviteResultDTO {
  sent: number;
  skipped: number;
}
