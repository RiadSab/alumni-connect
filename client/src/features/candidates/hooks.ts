// React Query hooks for the candidate's own profile (§2).

import { useEffect, useState } from "react";
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

// The candidate's own CV blob; success = on file, a 404 = none yet (retry:false).
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

// Authenticated photo blob (can't go straight in an <img>); enabled only when a photo exists.
export function useMyPhoto(enabled: boolean) {
  return useQuery({
    queryKey: queryKeys.candidate.photo(),
    queryFn: () => candidatesApi.downloadPhoto(),
    enabled,
    retry: false,
    staleTime: Infinity, // only changes when we upload a new one
  });
}

// Photo blob → object URL, minted in the effect (not useMemo) so Strict Mode doesn't revoke a live URL.
export function useMyPhotoUrl(enabled: boolean): string | null {
  const photo = useMyPhoto(enabled);
  const blob = photo.data;
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    const objectUrl = blob ? URL.createObjectURL(blob) : null;
    setUrl(objectUrl); // eslint-disable-line react-hooks/set-state-in-effect -- syncing an external blob to a renderable URL; the extra render is intended
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [blob]);
  return url;
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
