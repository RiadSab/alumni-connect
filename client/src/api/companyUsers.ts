// Company-user (member) endpoints (§4).

import { http } from "@/lib/http";
import type { Page, PageParams } from "@/types/common";
import type {
  ChangeMemberRoleDTO,
  CompanyUserProfileDTO,
  UpdateCompanyUserProfileDTO,
} from "@/types/companyUser";
import type { StatusChangeDTO } from "@/types/admin";

export const companyUsersApi = {
  // Caller's own company roster.
  roster: (params?: PageParams) =>
    http.get<Page<CompanyUserProfileDTO>>("/company-users", { query: params }),

  // OWNER only — members of the caller's company awaiting approval.
  pending: (params?: PageParams) =>
    http.get<Page<CompanyUserProfileDTO>>("/company-users/pending", { query: params }),

  // OWNER only — approve / reject a pending member.
  approve: (id: number, body: StatusChangeDTO) =>
    http.post<CompanyUserProfileDTO>(`/company-users/${id}/approve`, { json: body }),
  reject: (id: number, body: StatusChangeDTO) =>
    http.post<CompanyUserProfileDTO>(`/company-users/${id}/reject`, { json: body }),

  me: () => http.get<CompanyUserProfileDTO>("/company-users/me"),

  updateMe: (body: UpdateCompanyUserProfileDTO) =>
    http.patch<CompanyUserProfileDTO>("/company-users/me", { json: body }),

  // ADMINISTRATOR only.
  byId: (id: number) => http.get<CompanyUserProfileDTO>(`/company-users/${id}`),

  // OWNER only — change another member's platform role.
  changeRole: (id: number, body: ChangeMemberRoleDTO) =>
    http.patch<CompanyUserProfileDTO>(`/company-users/${id}/role`, { json: body }),
};
