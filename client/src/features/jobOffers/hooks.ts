// React Query hooks for job offers (§5).

import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { jobOffersApi } from "@/api/jobOffers";
import { queryKeys } from "@/lib/queryKeys";
import type { PageParams } from "@/types/common";
import type {
  CreateJobOfferDTO,
  JobOfferDTO,
  JobOfferFilters,
  UpdateJobOfferDTO,
} from "@/types/jobOffer";
import type { ApplicationFilters, ApplyToJobOfferInput } from "@/types/jobApplication";

// Public — browse OPEN offers with filters.
export function useJobOffers(filters?: JobOfferFilters) {
  return useQuery({
    queryKey: queryKeys.jobOffers.browse(filters),
    queryFn: () => jobOffersApi.browse(filters),
    // Keep the current page on screen while the next one loads (no skeleton flash).
    placeholderData: keepPreviousData,
  });
}

// OWNER/RECRUITER — own company's offers, any status.
export function useMyJobOffers(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.jobOffers.mine(params),
    queryFn: () => jobOffersApi.mine(params),
  });
}

// OWNER/RECRUITER — dashboard counts.
export function useMyCompanyStats() {
  return useQuery({
    queryKey: queryKeys.jobOffers.myStats(),
    queryFn: () => jobOffersApi.myStats(),
  });
}

// CANDIDATE — recommended offers.
export function useRecommendedJobOffers(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.jobOffers.recommended(params),
    queryFn: () => jobOffersApi.recommended(params),
  });
}

// Single offer.
export function useJobOffer(id: number) {
  return useQuery({
    queryKey: queryKeys.jobOffers.byId(id),
    queryFn: () => jobOffersApi.byId(id),
    enabled: Number.isFinite(id),
  });
}

// OWNER/RECRUITER — applicants for one offer.
export function useJobOfferApplications(id: number, filters?: ApplicationFilters) {
  return useQuery({
    queryKey: queryKeys.jobOffers.applications(id, filters),
    queryFn: () => jobOffersApi.applications(id, filters),
    enabled: Number.isFinite(id),
  });
}

// OWNER/RECRUITER — post a new offer.
export function useCreateJobOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: CreateJobOfferDTO) => jobOffersApi.create(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.jobOffers.all() }),
  });
}

// OWNER/RECRUITER — update an offer (incl. close/reopen).
export function useUpdateJobOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: UpdateJobOfferDTO }) =>
      jobOffersApi.update(input.id, input.body),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.jobOffers.byId(updated.id), updated);
      queryClient.invalidateQueries({ queryKey: queryKeys.jobOffers.all() });
    },
  });
}

// CANDIDATE — apply to an offer.
export function useApplyToJobOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; data: ApplyToJobOfferInput }) =>
      jobOffersApi.apply(input.id, input.data),
    onSuccess: (_result, input) => {
      // Mark this offer's cached detail applied right away, so a later visit (e.g.
      // from My Applications) shows "Already applied" without waiting on a refetch.
      queryClient.setQueryData<JobOfferDTO>(queryKeys.jobOffers.byId(input.id), (prev) =>
        prev ? { ...prev, hasApplied: true } : prev,
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.jobApplications.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.jobOffers.all() });
    },
  });
}
