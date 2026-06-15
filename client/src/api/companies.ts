// Company endpoints (§3).

import { http, apiUrl } from "@/lib/http";
import { singleFileForm } from "@/lib/form";
import type { Page, PageParams, StoredFileDTO } from "@/types/common";
import type { CompanyDTO, UpdateCompanyDTO } from "@/types/company";

export const companiesApi = {
  // Public — active companies only.
  list: (params?: PageParams) => http.get<Page<CompanyDTO>>("/companies", { query: params }),

  // Public.
  byId: (id: number) => http.get<CompanyDTO>(`/companies/${id}`),

  // OWNER only.
  updateMe: (body: UpdateCompanyDTO) => http.patch<CompanyDTO>("/companies/me", { json: body }),

  // OWNER only.
  uploadLogo: (file: File) =>
    http.post<StoredFileDTO>("/companies/me/logo", { form: singleFileForm(file) }),

  // Public image URL for <img src> (no auth required).
  logoUrl: (id: number) => apiUrl(`/companies/${id}/logo`),
};
