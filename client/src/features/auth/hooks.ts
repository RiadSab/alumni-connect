// React Query hooks for authentication (§1). Login/register/change-password are
// all mutations. useLogin persists the JWT on success; routing/session state is
// handled by the caller (auth context lands in a later phase).

import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api/auth";
import { setToken } from "@/lib/auth";
import type {
  ChangePasswordDTO,
  LoginRequestDTO,
  RegisterCandidateDTO,
  RegisterCompanyDTO,
  RegisterCompanyMemberDTO,
} from "@/types/auth";

export function useLogin() {
  return useMutation({
    mutationFn: (body: LoginRequestDTO) => authApi.login(body),
    onSuccess: (data) => setToken(data.token),
  });
}

export function useRegisterCandidate() {
  return useMutation({
    mutationFn: (body: RegisterCandidateDTO) => authApi.registerCandidate(body),
  });
}

export function useRegisterCompany() {
  return useMutation({
    mutationFn: (body: RegisterCompanyDTO) => authApi.registerCompany(body),
  });
}

export function useRegisterCompanyMember() {
  return useMutation({
    mutationFn: (body: RegisterCompanyMemberDTO) => authApi.registerCompanyMember(body),
  });
}

export function useChangePassword() {
  return useMutation({
    mutationFn: (body: ChangePasswordDTO) => authApi.changePassword(body),
  });
}
