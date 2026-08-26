// Alumni roster endpoints (ADMINISTRATOR only).

import { http } from "@/lib/http";
import type { Page } from "@/types/common";
import type { AlumniImportResultDTO, AlumniRecordDTO, AlumniRecordFilters } from "@/types/alumni";

export const alumniApi = {
  records: (filters?: AlumniRecordFilters) =>
    http.get<Page<AlumniRecordDTO>>("/admin/alumni", { query: filters }),

  importRoster: (file: File, dryRun: boolean) => {
    const form = new FormData();
    form.append("file", file);
    return http.post<AlumniImportResultDTO>("/admin/alumni/import", { form, query: { dryRun } });
  },
};
