// Authentication endpoints (§1).

import { http } from "@/lib/http";
import type {
  ChangePasswordDTO,
  ForgotPasswordDTO,
  LoginRequestDTO,
  LoginResponseDTO,
  RegisterCandidateDTO,
  RegisterCompanyDTO,
  RegisterCompanyMemberDTO,
  ResetPasswordDTO,
} from "@/types/auth";

export const authApi = {
  registerCandidate: (body: RegisterCandidateDTO) =>
    http.post<void>("/auth/register/candidate", { json: body }),

  registerCompany: (body: RegisterCompanyDTO) =>
    http.post<void>("/auth/register/company", { json: body }),

  registerCompanyMember: (body: RegisterCompanyMemberDTO) =>
    http.post<void>("/auth/register/company-member", { json: body }),

  login: (body: LoginRequestDTO) => http.post<LoginResponseDTO>("/auth/login", { json: body }),

  logout: () => http.post<void>("/auth/logout"),

  changePassword: (body: ChangePasswordDTO) =>
    http.post<void>("/auth/change-password", { json: body }),

  forgotPassword: (body: ForgotPasswordDTO) =>
    http.post<void>("/auth/forgot-password", { json: body }),

  resetPassword: (body: ResetPasswordDTO) =>
    http.post<void>("/auth/reset-password", { json: body }),
};
