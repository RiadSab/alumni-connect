// Company DTOs (§3).

import { z } from "zod";
import { CompanySize, Fields, type CompanyStatus } from "@/types/enums";
import type { AuditFields, PageParams } from "@/types/common";
import { nonBlank } from "@/lib/validation";

// GET /api/companies — paginated browse with an optional name search (q).
export interface CompanyListParams extends PageParams {
  q?: string;
}

// Public company view — output of GET /api/companies, GET /api/companies/{id},
// PATCH /api/companies/me.
export interface CompanyDTO extends AuditFields {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  field: Fields;
  description: string | null;
  website: string | null;
  address: string | null;
  size: CompanySize | null;
  logoId: string | null;
  status: CompanyStatus;
}

// PATCH /api/companies/me — all optional (omit = leave unchanged).
export const updateCompanySchema = z.object({
  name: nonBlank().optional(),
  phone: z.string().optional(),
  field: z.enum(Fields).optional(),
  description: z.string().max(2000).optional(),
  website: z.string().optional(),
  address: z.string().optional(),
  size: z.enum(CompanySize).optional(),
});
export type UpdateCompanyDTO = z.infer<typeof updateCompanySchema>;
