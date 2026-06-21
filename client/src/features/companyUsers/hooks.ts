// React Query hooks for company users / members (§4).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { companyUsersApi } from "@/api/companyUsers";
import { queryKeys } from "@/lib/queryKeys";
import type { PageParams } from "@/types/common";
import type {
  ChangeMemberRoleDTO,
  UpdateCompanyUserProfileDTO,
} from "@/types/companyUser";
import type { StatusChangeDTO } from "@/types/admin";

// Caller's own company roster.
export function useCompanyRoster(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.companyUsers.roster(params),
    queryFn: () => companyUsersApi.roster(params),
  });
}

// OWNER — members awaiting approval.
export function usePendingMembers(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.companyUsers.pending(params),
    queryFn: () => companyUsersApi.pending(params),
  });
}

// OWNER — approve a pending member. Invalidates the whole company-users tree so the
// member leaves the pending list and the roster reflects the now-active account.
export function useApproveMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      companyUsersApi.approve(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.companyUsers.all() }),
  });
}

export function useRejectMember() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: StatusChangeDTO }) =>
      companyUsersApi.reject(input.id, input.body),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.companyUsers.all() }),
  });
}

// Caller's own company-user profile.
export function useMyCompanyUserProfile() {
  return useQuery({
    queryKey: queryKeys.companyUsers.me(),
    queryFn: () => companyUsersApi.me(),
  });
}

// Edit own profile.
export function useUpdateMyCompanyUserProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdateCompanyUserProfileDTO) => companyUsersApi.updateMe(body),
    onSuccess: (updated) => queryClient.setQueryData(queryKeys.companyUsers.me(), updated),
  });
}

// ADMINISTRATOR — look up any company user.
export function useCompanyUser(id: number) {
  return useQuery({
    queryKey: queryKeys.companyUsers.byId(id),
    queryFn: () => companyUsersApi.byId(id),
    enabled: Number.isFinite(id),
  });
}

// OWNER — change a member's role.
export function useChangeMemberRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { id: number; body: ChangeMemberRoleDTO }) =>
      companyUsersApi.changeRole(input.id, input.body),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.companyUsers.byId(updated.id), updated);
      queryClient.invalidateQueries({ queryKey: queryKeys.companyUsers.all() });
    },
  });
}
