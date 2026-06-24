// Admin DTOs (§7). The "pending" and browse-all endpoints both return these DTOs.

import { z } from "zod";
import type {
  CompanySize,
  CompanyStatus,
  Fields,
  UserStatus,
  UserType,
} from "@/types/enums";
import type { AuditFields, PageParams } from "@/types/common";
import { nonBlank } from "@/lib/validation";

// Moderation view — GET /api/admin/users.
export interface AdminUserDTO extends AuditFields {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string | null;
  userType: UserType;
  userStatus: UserStatus;
  statusChangeReason: string | null;
  emailVerified: boolean;
}

// Moderation view — GET /api/admin/companies.
export interface AdminCompanyDTO extends AuditFields {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  field: Fields;
  description: string | null;
  website: string | null;
  address: string | null;
  size: CompanySize | null;
  status: CompanyStatus;
  statusChangeReason: string | null;
  // The company's owner — present on the pending queue, null on the browse-all list.
  owner: { firstName: string; lastName: string; email: string } | null;
}

// The pending and browse-all endpoints share these shapes; no entity types needed.

// Body for all 8 user/company lifecycle actions.
export const statusChangeSchema = z.object({
  reason: nonBlank(),
});
export type StatusChangeDTO = z.infer<typeof statusChangeSchema>;

// Filters.
export interface AdminUserFilters extends PageParams {
  status?: UserStatus;
  type?: UserType;
}

export interface AdminCompanyFilters extends PageParams {
  status?: CompanyStatus;
}

// GET /api/admin/stats — admin dashboard counts.
export interface AdminStatsDTO {
  pendingUsers: number;
  pendingCompanies: number;
}
