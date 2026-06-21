// React Query hooks for saved jobs (bookmarks). Candidate-only.

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { savedJobsApi } from "@/api/savedJobs";
import { useAuth } from "@/features/auth/auth-context";
import { queryKeys } from "@/lib/queryKeys";
import type { PageParams } from "@/types/common";

// The saved-jobs list page.
export function useSavedJobs(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.savedJobs.list(params),
    queryFn: () => savedJobsApi.list(params),
  });
}

// The saved offer ids, so a JobCard knows its bookmark state. One request for the
// whole board (React Query dedupes the shared key). Fetched only for candidates.
export function useSavedJobIds() {
  const { isAuthenticated, user } = useAuth();
  return useQuery({
    queryKey: queryKeys.savedJobs.ids(),
    queryFn: () => savedJobsApi.ids(),
    enabled: isAuthenticated && user?.userType === "CANDIDATE",
  });
}

// Save/unsave both refresh the saved ids/list and the affected offer detail (its isSaved flag).
export function useSaveJob() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offerId: number) => savedJobsApi.save(offerId),
    onSuccess: (_data, offerId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.savedJobs.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.jobOffers.byId(offerId) });
    },
  });
}

export function useUnsaveJob() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offerId: number) => savedJobsApi.unsave(offerId),
    onSuccess: (_data, offerId) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.savedJobs.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.jobOffers.byId(offerId) });
    },
  });
}
