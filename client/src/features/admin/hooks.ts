// React Query hooks for admin moderation (§7). All require ADMINISTRATOR.
// The 8 lifecycle mutations are written out one by one on purpose — each takes
// { id, body } and refetches the admin lists on success.

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "@/api/admin";
import { queryKeys } from "@/lib/queryKeys";
import type { PageParams } from "@/types/common";
import type {
  AdminCompanyFilters,
  AdminUserFilters,
  StatusChangeDTO,
} from "@/types/admin";

// --- Queries ---------------------------------------------------------------

export function usePendingUsers(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.admin.pendingUsers(params),
    queryFn: () => adminApi.pendingUsers(params),
  });
}

export function useAdminUsers(filters?: AdminUserFilters) {
  return useQuery({
    queryKey: queryKeys.admin.users(filters),
    queryFn: () => adminApi.users(filters),
  });
}

export function usePendingCompanies(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.admin.pendingCompanies(params),
    queryFn: () => adminApi.pendingCompanies(params),
  });
}

export function useAdminCompanies(filters?: AdminCompanyFilters) {
  return useQuery({
    queryKey: queryKeys.admin.companies(filters),
    queryFn: () => adminApi.companies(filters),
  });
}

// --- User lifecycle mutations ----------------------------------------------

export function useApproveUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.approveUser(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}

export function useRejectUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.rejectUser(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}

export function useSuspendUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.suspendUser(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}

export function useReactivateUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.reactivateUser(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}

// --- Company lifecycle mutations -------------------------------------------

export function useApproveCompany() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.approveCompany(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}

export function useRejectCompany() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.rejectCompany(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}

export function useSuspendCompany() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.suspendCompany(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}

export function useReactivateCompany() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      adminApi.reactivateCompany(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.admin.all() }),
  });
}
