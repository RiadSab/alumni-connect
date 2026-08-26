// Alumni roster endpoints (ADMINISTRATOR only).

import { http } from "@/lib/http";
import type { Page } from "@/types/common";
import type {
  AlumniClaimDetailsDTO,
  AlumniImportResultDTO,
  AlumniRecordDTO,
  AlumniRecordFilters,
  ClaimAccountInput,
  ClaimInviteResultDTO,
} from "@/types/alumni";

export const alumniApi = {
  records: (filters?: AlumniRecordFilters) =>
    http.get<Page<AlumniRecordDTO>>("/admin/alumni", { query: filters }),

  importRoster: (file: File, dryRun: boolean) => {
    const form = new FormData();
    form.append("file", file);
    return http.post<AlumniImportResultDTO>("/admin/alumni/import", { form, query: { dryRun } });
  },

  invite: (promotionYear?: number) =>
    http.post<ClaimInviteResultDTO>("/admin/alumni/invite", { query: { promotionYear } }),

  link: (recordId: number, email: string) =>
    http.post<AlumniRecordDTO>(`/admin/alumni/${recordId}/link`, { json: { email } }),
};

// Public — the one-time token from the claim email is the credential.
export const alumniClaimApi = {
  details: (token: string) => http.get<AlumniClaimDetailsDTO>(`/alumni/claim/${token}`),

  claim: (body: ClaimAccountInput) => http.post<void>("/alumni/claim", { json: body }),

  optOut: (token: string) => http.post<void>(`/alumni/claim/${token}/opt-out`),
};
