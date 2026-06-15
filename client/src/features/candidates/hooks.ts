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

export function useUploadResume() {
  return useMutation({
    mutationFn: (file: File) => candidatesApi.uploadResume(file),
  });
}

export function useUploadProfilePhoto() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => candidatesApi.uploadPhoto(file),
    // photo handle (profilePhotoId) lives on the profile — refetch it.
    onSuccess: () => qc.invalidateQueries({ queryKey: queryKeys.candidate.me() }),
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
