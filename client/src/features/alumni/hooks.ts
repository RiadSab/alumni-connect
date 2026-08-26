// React Query hooks for the alumni roster (ADMINISTRATOR only).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { alumniApi, alumniClaimApi } from "@/api/alumni";
import { queryKeys } from "@/lib/queryKeys";
import type { AlumniRecordFilters, ClaimAccountInput } from "@/types/alumni";

export function useAlumniRecords(filters?: AlumniRecordFilters) {
  return useQuery({
    queryKey: queryKeys.alumni.records(filters),
    queryFn: () => alumniApi.records(filters),
  });
}

export function useImportRoster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { file: File; dryRun: boolean }) =>
      alumniApi.importRoster(input.file, input.dryRun),
    // A dry run changes nothing, but refetching after one is harmless and keeps this simple.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.alumni.all() }),
  });
}

export function useInviteRoster() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (promotionYear?: number) => alumniApi.invite(promotionYear),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.alumni.all() }),
  });
}

export function useLinkAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { recordId: number; email: string }) =>
      alumniApi.link(input.recordId, input.email),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.alumni.all() }),
  });
}

// --- Public claim flow -----------------------------------------------------

export function useClaimDetails(token: string) {
  return useQuery({
    queryKey: queryKeys.alumni.claim(token),
    queryFn: () => alumniClaimApi.details(token),
    // A used or expired link is a 400, not a network hiccup.
    retry: false,
  });
}

export function useClaimAccount() {
  return useMutation({
    mutationFn: (body: ClaimAccountInput) => alumniClaimApi.claim(body),
  });
}

export function useOptOut() {
  return useMutation({
    mutationFn: (token: string) => alumniClaimApi.optOut(token),
  });
}
