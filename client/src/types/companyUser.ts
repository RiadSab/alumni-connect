// Company-user (member) DTOs (§4).

import { z } from "zod";
import {
  CompanyRole,
  CompanyUserPosition,
  type UserStatus,
} from "@/types/enums";
import type { AuditFields } from "@/types/common";
import { nonBlank } from "@/lib/validation";

// Output of the /api/company-users endpoints.
export interface CompanyUserProfileDTO extends AuditFields {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string | null;
  userStatus: UserStatus;
  companyId: number;
  companyName: string;
  companyRole: CompanyRole;
  position: CompanyUserPosition;
}

// PATCH /api/company-users/me — all optional (omit = leave unchanged).
export const updateCompanyUserProfileSchema = z.object({
  firstName: nonBlank().optional(),
  lastName: nonBlank().optional(),
  phoneNumber: z.string().optional(),
  position: z.enum(CompanyUserPosition).optional(),
});
export type UpdateCompanyUserProfileDTO = z.infer<typeof updateCompanyUserProfileSchema>;

// PATCH /api/company-users/{id}/role — service rejects OWNER (v1: MEMBER <-> RECRUITER).
export const changeMemberRoleSchema = z.object({
  role: z.enum(CompanyRole),
});
export type ChangeMemberRoleDTO = z.infer<typeof changeMemberRoleSchema>;
