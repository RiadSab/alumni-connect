// React Query hooks for authentication (§1). Login/register/change-password are
// all mutations. useLogin persists the JWT on success; routing/session state is
// handled by the caller (auth context lands in a later phase).

import { useMutation } from "@tanstack/react-query";
import { authApi } from "@/api/auth";
import type {
  ChangePasswordDTO,
  ForgotPasswordDTO,
  LoginRequestDTO,
  RegisterCandidateDTO,
  RegisterCompanyDTO,
  RegisterCompanyMemberDTO,
  ResetPasswordDTO,
} from "@/types/auth";

// Just calls the API and returns the LoginResponseDTO. The login page passes
// that result to the auth context's login(), which saves the token + user.
export function useLogin() {
  return useMutation({
    mutationFn: (body: LoginRequestDTO) => authApi.login(body),
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

export function useForgotPassword() {
  return useMutation({
    mutationFn: (body: ForgotPasswordDTO) => authApi.forgotPassword(body),
  });
}

export function useResetPassword() {
  return useMutation({
    mutationFn: (body: ResetPasswordDTO) => authApi.resetPassword(body),
  });
}
