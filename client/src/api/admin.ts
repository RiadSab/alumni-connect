// Admin endpoints (§7). All require ADMINISTRATOR. Lifecycle actions return a
// plain-text confirmation string.

import { http } from "@/lib/http";
import type { Page, PageParams } from "@/types/common";
import type {
  AdminCompanyDTO,
  AdminCompanyFilters,
  AdminStatsDTO,
  AdminUserDTO,
  AdminUserFilters,
  StatusChangeDTO,
} from "@/types/admin";

export const adminApi = {
  // Dashboard counts.
  stats: () => http.get<AdminStatsDTO>("/admin/stats"),

  // --- Users ---------------------------------------------------------------
  pendingUsers: (params?: PageParams) =>
    http.get<Page<AdminUserDTO>>("/admin/pending-users", { query: params }),

  users: (filters?: AdminUserFilters) =>
    http.get<Page<AdminUserDTO>>("/admin/users", { query: filters }),

  approveUser: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/users/${id}/approve`, { json: body }),
  rejectUser: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/users/${id}/reject`, { json: body }),
  suspendUser: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/users/${id}/suspend`, { json: body }),
  reactivateUser: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/users/${id}/reactivate`, { json: body }),

  // --- Companies -----------------------------------------------------------
  pendingCompanies: (params?: PageParams) =>
    http.get<Page<AdminCompanyDTO>>("/admin/pending-companies", { query: params }),

  companies: (filters?: AdminCompanyFilters) =>
    http.get<Page<AdminCompanyDTO>>("/admin/companies", { query: filters }),

  approveCompany: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/companies/${id}/approve`, { json: body }),
  rejectCompany: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/companies/${id}/reject`, { json: body }),
  suspendCompany: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/companies/${id}/suspend`, { json: body }),
  reactivateCompany: (id: number, body: StatusChangeDTO) =>
    http.postText(`/admin/companies/${id}/reactivate`, { json: body }),
};
