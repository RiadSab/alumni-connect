// React Query hooks for employment history.

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { employmentApi } from "@/api/employment";
import { queryKeys } from "@/lib/queryKeys";
import type { SaveEmploymentEntryInput } from "@/types/employment";

export function useMyEmployment() {
  return useQuery({
    queryKey: queryKeys.employment.mine(),
    queryFn: () => employmentApi.mine(),
  });
}

export function useCreateEmploymentEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: SaveEmploymentEntryInput) => employmentApi.create(body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.employment.all() }),
  });
}

export function useUpdateEmploymentEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: SaveEmploymentEntryInput }) =>
      employmentApi.update(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.employment.all() }),
  });
}

export function useDeleteEmploymentEntry() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => employmentApi.remove(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.employment.all() }),
  });
}

// Public: the token in the link is the authentication, so these run logged out.
export function useEmploymentConfirmDetails(token: string) {
  return useQuery({
    queryKey: queryKeys.employment.confirm(token),
    queryFn: () => employmentApi.confirmDetails(token),
    retry: false,
  });
}

export function useConfirmEmployment() {
  return useMutation({
    mutationFn: (token: string) => employmentApi.confirm(token),
  });
}
