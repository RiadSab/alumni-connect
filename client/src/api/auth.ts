// Authentication endpoints (§1).

import { http } from "@/lib/http";
import type {
  ChangePasswordDTO,
  LoginRequestDTO,
  LoginResponseDTO,
  RegisterCandidateDTO,
  RegisterCompanyDTO,
  RegisterCompanyMemberDTO,
} from "@/types/auth";

export const authApi = {
  registerCandidate: (body: RegisterCandidateDTO) =>
    http.post<void>("/auth/register/candidate", { json: body }),

  registerCompany: (body: RegisterCompanyDTO) =>
    http.post<void>("/auth/register/company", { json: body }),

  registerCompanyMember: (body: RegisterCompanyMemberDTO) =>
    http.post<void>("/auth/register/company-member", { json: body }),

  login: (body: LoginRequestDTO) => http.post<LoginResponseDTO>("/auth/login", { json: body }),

  changePassword: (body: ChangePasswordDTO) =>
    http.post<void>("/auth/change-password", { json: body }),
};
