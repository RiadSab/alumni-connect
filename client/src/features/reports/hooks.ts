// React Query hooks for the employment report.

import { useQuery } from "@tanstack/react-query";
import { reportsApi } from "@/api/reports";
import { queryKeys } from "@/lib/queryKeys";

export function usePromotionYears() {
  return useQuery({
    queryKey: queryKeys.reports.promotions(),
    queryFn: () => reportsApi.promotions(),
  });
}

export function useEmploymentReport(promotionYear: number | null) {
  return useQuery({
    queryKey: queryKeys.reports.employment(promotionYear),
    queryFn: () => reportsApi.employment(promotionYear as number),
    enabled: promotionYear !== null,
  });
}
