// React Query hooks for the candidate's own profile (§2).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { candidatesApi } from "@/api/candidates";
import { queryKeys } from "@/lib/queryKeys";
import type { UpdateCandidateProfileDTO } from "@/types/candidate";

export function useMyCandidateProfile() {
  return useQuery({
    queryKey: queryKeys.candidate.me(),
    queryFn: () => candidatesApi.me(),
  });
}

export function useUpdateMyCandidateProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateCandidateProfileDTO) => candidatesApi.updateMe(body),
    onSuccess: (updated) => qc.setQueryData(queryKeys.candidate.me(), updated),
  });
}

// Presence + content of the candidate's own CV. The profile DTO has no résumé
// field, so "do I have one?" comes from this download: success = on file, a 404
// ApiError = none yet. retry:false so a 404 doesn't get retried.
export function useMyResume() {
  return useQuery({
    queryKey: queryKeys.candidate.resume(),
    queryFn: () => candidatesApi.downloadResume(),
    retry: false,
    staleTime: Infinity, // the blob only changes when we upload a new one
  });
}

export function useUploadResume() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => candidatesApi.uploadResume(file),
    // flips the "no résumé yet" state to "on file" and refetches the new blob.
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.candidate.resume() }),
  });
}

// The photo endpoint is authenticated, so the image can't go straight in an
// <img src> — we download the blob and turn it into an object URL. Gated on
// `enabled` so we only fetch when the profile says a photo exists.
export function useMyPhoto(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.candidate.photo(),
    queryFn: () => candidatesApi.downloadPhoto(),
    enabled,
    retry: false,
    staleTime: Infinity, // only changes when we upload a new one
  });
}

export function useUploadProfilePhoto() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => candidatesApi.uploadPhoto(file),
    onSuccess: () => {
      // profilePhotoId lives on the profile, the image blob is its own query.
      qc.invalidateQueries({ queryKey: queryKeys.candidate.me() });
      qc.invalidateQueries({ queryKey: queryKeys.candidate.photo() });
    },
  });
}

// ADMINISTRATOR — look up any candidate by id.
export function useCandidate(id: number) {
  return useQuery({
    queryKey: queryKeys.candidate.byId(id),
    queryFn: () => candidatesApi.byId(id),
    enabled: Number.isFinite(id),
  });
}
