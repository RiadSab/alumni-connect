// Company-user (member) endpoints (§4).

import { http } from "@/lib/http";
import type { Page, PageParams } from "@/types/common";
import type {
  ChangeMemberRoleDTO,
  CompanyUserProfileDTO,
  UpdateCompanyUserProfileDTO,
} from "@/types/companyUser";

export const companyUsersApi = {
  // Caller's own company roster.
  roster: (params?: PageParams) =>
    http.get<Page<CompanyUserProfileDTO>>("/company-users", { query: params }),

  me: () => http.get<CompanyUserProfileDTO>("/company-users/me"),

  updateMe: (body: UpdateCompanyUserProfileDTO) =>
    http.patch<CompanyUserProfileDTO>("/company-users/me", { json: body }),

  // ADMINISTRATOR only.
  byId: (id: number) => http.get<CompanyUserProfileDTO>(`/company-users/${id}`),

  // OWNER only — change another member's platform role.
  changeRole: (id: number, body: ChangeMemberRoleDTO) =>
    http.patch<CompanyUserProfileDTO>(`/company-users/${id}/role`, { json: body }),
};
