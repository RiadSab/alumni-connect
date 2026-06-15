// Candidate DTOs (§2).

import { z } from "zod";
import { Fields, type UserStatus } from "@/types/enums";
import type { AuditFields } from "@/types/common";
import { nonBlank, optionalUrl } from "@/lib/validation";

// Output of GET/PATCH /api/candidates/me and GET /api/candidates/{id}
export interface CandidateProfileDTO extends AuditFields {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  userStatus: UserStatus;
  isStudent: boolean;
  studentId: string | null;
  fieldOfStudy: Fields;
  graduationYear: number;
  dateOfBirth: string | null; // YYYY-MM-DD
  skills: string[];
  currentJobTitle: string | null;
  currentCompany: string | null;
  experienceYears: number | null;
  linkedinUrl: string | null;
  githubUrl: string | null;
  portfolioUrl: string | null;
  bio: string | null;
  profilePhotoId: string | null;
}

// PATCH /api/candidates/me — all fields optional (omit = leave unchanged).
export const updateCandidateProfileSchema = z.object({
  firstName: nonBlank().optional(),
  lastName: nonBlank().optional(),
  phoneNumber: nonBlank().optional(),
  isStudent: z.boolean().optional(),
  studentId: z.string().optional(),
  fieldOfStudy: z.enum(Fields).optional(),
  graduationYear: z.number().int().optional(),
  dateOfBirth: z.string().optional(), // YYYY-MM-DD
  skills: z.array(nonBlank()).optional(),
  currentJobTitle: z.string().optional(),
  currentCompany: z.string().optional(),
  experienceYears: z.number().int().min(0).optional(),
  linkedinUrl: optionalUrl,
  githubUrl: optionalUrl,
  portfolioUrl: optionalUrl,
  bio: z.string().optional(),
});
export type UpdateCandidateProfileDTO = z.infer<typeof updateCandidateProfileSchema>;
