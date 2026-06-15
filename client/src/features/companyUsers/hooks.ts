// React Query hooks for company users / members (§4).

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { companyUsersApi } from "@/api/companyUsers";
import { queryKeys } from "@/lib/queryKeys";
import type { PageParams } from "@/types/common";
import type {
  ChangeMemberRoleDTO,
  UpdateCompanyUserProfileDTO,
} from "@/types/companyUser";

// Caller's own company roster.
export function useCompanyRoster(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.companyUsers.roster(params),
    queryFn: () => companyUsersApi.roster(params),
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
