// Admin DTOs (§7). Includes the two raw JPA entity shapes that the "pending"
// endpoints serialize instead of a clean DTO.

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
}

// Raw User entity — GET /api/admin/pending-users (passwordHash never serialized).
export interface UserEntity extends AuditFields {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string | null;
  userType: UserType;
  userStatus: UserStatus;
  emailVerified: boolean;
  preferredLanguage: string; // default "en"
  statusChangeReason: string | null;
  version: number;
}

// Raw Company entity — GET /api/admin/pending-companies.
export interface CompanyEntity extends AuditFields {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  field: Fields;
  description: string | null;
  website: string | null;
  address: string | null;
  logoId: string | null;
  videoPresentationId: string | null;
  status: CompanyStatus;
  size: CompanySize | null;
  statusChangeReason: string | null;
  version: number;
}

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
