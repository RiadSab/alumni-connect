// React Query hooks for companies (§3).

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { companiesApi } from "@/api/companies";
import { queryKeys } from "@/lib/queryKeys";
import type { CompanyListParams, UpdateCompanyDTO } from "@/types/company";

// Public — browse active companies.
export function useCompanies(params?: CompanyListParams) {
  return useQuery({
    queryKey: queryKeys.companies.list(params),
    queryFn: () => companiesApi.list(params),
  });
}

// Public — name search for the "join an existing company" picker. Only queries once the
// term is long enough; keeps the prior matches on screen while the next ones load.
export function useCompanySearch(q: string) {
  const term = q.trim();
  return useQuery({
    queryKey: queryKeys.companies.list({ q: term, size: 8 }),
    queryFn: () => companiesApi.list({ q: term, size: 8 }),
    enabled: term.length >= 2,
    placeholderData: keepPreviousData,
  });
}

// Public — one company.
export function useCompany(id: number) {
  return useQuery({
    queryKey: queryKeys.companies.byId(id),
    queryFn: () => companiesApi.byId(id),
    enabled: Number.isFinite(id),
  });
}

// OWNER — edit own company.
export function useUpdateMyCompany() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateCompanyDTO) => companiesApi.updateMe(body),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.companies.byId(updated.id), updated);
      queryClient.invalidateQueries({ queryKey: queryKeys.companies.all() });
    },
  });
}

// OWNER — upload logo. Changes the company's logoId, so refetch companies.
export function useUploadCompanyLogo() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => companiesApi.uploadLogo(file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.companies.all() }),
  });
}
