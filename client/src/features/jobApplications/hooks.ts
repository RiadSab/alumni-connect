// React Query hooks for job applications (§6).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { jobApplicationsApi } from "@/api/jobApplications";
import { queryKeys } from "@/lib/queryKeys";
import type { PageParams } from "@/types/common";
import type { ReviewApplicationDTO } from "@/types/jobApplication";

// CANDIDATE — own application history. `enabled` lets callers skip the fetch for
// non-candidates (the endpoint is candidate-only).
export function useMyApplications(params?: PageParams, options?: { enabled?: boolean }) {
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
