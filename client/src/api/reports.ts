// Employment report endpoints (ADMINISTRATOR only).

import { http } from "@/lib/http";
import type { EmploymentReportDTO } from "@/types/report";

export const reportsApi = {
  promotions: () => http.get<number[]>("/admin/reports/promotions"),

  employment: (promotionYear: number) =>
    http.get<EmploymentReportDTO>("/admin/reports/employment", { query: { promotionYear } }),
};
