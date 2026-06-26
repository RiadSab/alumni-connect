// React Query hooks for job applications (§6).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { jobApplicationsApi } from "@/api/jobApplications";
import { queryKeys } from "@/lib/queryKeys";
import type { MyApplicationFilters, ReviewApplicationDTO } from "@/types/jobApplication";
import type { JobOfferDTO } from "@/types/jobOffer";

// CANDIDATE — own application history. `enabled` lets callers skip the fetch for
// non-candidates (the endpoint is candidate-only).
export function useMyApplications(params?: MyApplicationFilters, options?: { enabled?: boolean }) {
  return useQuery({
    queryKey: queryKeys.jobApplications.mine(params),
    queryFn: () => jobApplicationsApi.mine(params),
    enabled: options?.enabled ?? true,
  });
}

// CANDIDATE — dashboard counts.
export function useMyApplicationStats() {
  return useQuery({
    queryKey: queryKeys.jobApplications.myStats(),
    queryFn: () => jobApplicationsApi.myStats(),
  });
}

// Applicant or posting company — single application.
export function useApplication(id: number) {
  return useQuery({
    queryKey: queryKeys.jobApplications.byId(id),
    queryFn: () => jobApplicationsApi.byId(id),
    enabled: Number.isFinite(id),
  });
}

// The résumé blob submitted with an application; enabled only when one exists.
export function useApplicationResume(id: number, enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.jobApplications.resume(id),
    queryFn: () => jobApplicationsApi.downloadResume(id),
    enabled,
    retry: false,
    staleTime: Infinity, // the submitted résumé is a snapshot — it never changes
  });
}

// OWNER/RECRUITER — the applicant's full candidate profile.
export function useApplicantProfile(id: number) {
  return useQuery({
    queryKey: queryKeys.jobApplications.applicant(id),
    queryFn: () => jobApplicationsApi.applicant(id),
    enabled: Number.isFinite(id),
  });
}

// CANDIDATE — withdraw own application.
export function useWithdrawApplication() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => jobApplicationsApi.withdraw(id),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.jobApplications.byId(updated.id), updated);
      queryClient.invalidateQueries({ queryKey: queryKeys.jobApplications.all() });
      // Withdrawing frees the candidate to re-apply, so the offer is no longer "applied".
      queryClient.setQueryData<JobOfferDTO>(queryKeys.jobOffers.byId(updated.jobOfferId), (prev) =>
        prev ? { ...prev, hasApplied: false } : prev,
      );
      queryClient.invalidateQueries({ queryKey: queryKeys.jobOffers.all() });
    },
  });
}

// OWNER/RECRUITER — review an application (status, note, priority, rating).
export function useReviewApplication() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: ReviewApplicationDTO }) =>
      jobApplicationsApi.review(input.id, input.body),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.jobApplications.byId(updated.id), updated);
      queryClient.invalidateQueries({ queryKey: queryKeys.jobApplications.all() });
      // the offer's applicant list also reflects this change
      queryClient.invalidateQueries({ queryKey: queryKeys.jobOffers.all() });
    },
  });
}
