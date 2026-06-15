// Authentication DTOs (§1). Request bodies are zod schemas; the login response
// is a plain output type.

import { z } from "zod";
import { CompanySize, CompanyUserPosition, Fields, type UserType } from "@/types/enums";
import { nonBlank, optionalUrl } from "@/lib/validation";

// POST /api/auth/register/candidate
export const registerCandidateSchema = z
  .object({
    firstName: nonBlank(),
    lastName: nonBlank(),
    email: z.email(),
    password: nonBlank(),
    phoneNumber: nonBlank(),
    isStudent: z.boolean(),
    studentId: z.string().trim().optional(),
    fieldOfStudy: z.enum(Fields),
    graduationYear: z.number().int(),
    skills: z.array(nonBlank()).optional(),
    currentJobTitle: z.string().optional(),
    currentCompany: z.string().optional(),
    experienceYears: z.number().int().min(0).optional(),
    linkedinUrl: optionalUrl,
    githubUrl: optionalUrl,
    portfolioUrl: optionalUrl,
    bio: z.string().optional(),
  })
  .refine((d) => !d.isStudent || !!d.studentId?.trim(), {
    message: "Student ID is required when you are a student",
    path: ["studentId"],
  });
export type RegisterCandidateDTO = z.infer<typeof registerCandidateSchema>;

// POST /api/auth/register/company
export const registerCompanySchema = z.object({
  firstName: nonBlank(),
  lastName: nonBlank(),
  email: z.email(),
  password: nonBlank(),
  phoneNumber: z.string().optional(),
  position: z.enum(CompanyUserPosition),
  companyName: nonBlank(),
  companyEmail: z.email(),
  companyPhone: z.string().optional(),
  companyField: z.enum(Fields),
  companyDescription: z.string().optional(),
  companyWebsite: z.string().optional(),
  companyAddress: z.string().optional(),
  companySize: z.enum(CompanySize).optional(),
});
export type RegisterCompanyDTO = z.infer<typeof registerCompanySchema>;

// POST /api/auth/register/company-member
export const registerCompanyMemberSchema = z.object({
  firstName: nonBlank(),
  lastName: nonBlank(),
  email: z.email(),
  password: nonBlank(),
  phoneNumber: z.string().optional(),
  position: z.enum(CompanyUserPosition),
  companyId: z.number().int(),
});
export type RegisterCompanyMemberDTO = z.infer<typeof registerCompanyMemberSchema>;

// POST /api/auth/login
export const loginSchema = z.object({
  email: nonBlank(),
  password: nonBlank(),
});
export type LoginRequestDTO = z.infer<typeof loginSchema>;

// POST /api/auth/change-password
export const changePasswordSchema = z.object({
  oldPassword: nonBlank(),
  newPassword: nonBlank(),
});
export type ChangePasswordDTO = z.infer<typeof changePasswordSchema>;

// Response of POST /api/auth/login
export interface LoginResponseDTO {
  token: string;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  userType: UserType;
}
