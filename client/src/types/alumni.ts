// Alumni roster DTOs (admin-only). The roster is the school's own list of graduates.

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
