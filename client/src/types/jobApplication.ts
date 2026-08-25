// Job-application DTOs (§6).

import { z } from "zod";
import { ApplicationStatus, InterviewMode, Priority } from "@/types/enums";
import type { AuditFields, PageParams } from "@/types/common";

// Output shape.
export interface JobApplicationDTO extends AuditFields {
  id: number;
  jobOfferId: number;
  jobOfferTitle: string;
  companyId: number;
  companyName: string;
  logoId: string | null;
  applicantId: number;
  applicantName: string;
  resumeStorageId: string | null;
  certificatesStorageIds: string[] | null;
  coverLetter: string | null;
  applicationStatus: ApplicationStatus;
  reviewedAt: string | null;
  reviewedById: number | null;
  reviewedByName: string | null;
  // Null for the candidate: the company's private assessment is stripped server-side.
  companyUserNote: string | null;
  priority: Priority | null;
  rating: number | null; // 0-10
  // Filled in once the application is moved to SCHEDULED_INTERVIEW.
  interviewMode: InterviewMode | null;
  interviewAt: string | null;
  interviewLink: string | null;
  interviewLocation: string | null;
  interviewerName: string | null;
}

// GET /api/job-applications/me/stats — candidate dashboard counts.
export interface MyApplicationStatsDTO {
  total: number;
  active: number;
  accepted: number;
}

// POST /api/job-offers/{id}/apply — multipart. Text/bool parts validated here;
// the file is attached separately by the api layer.
export const applyToJobOfferSchema = z.object({
  coverLetter: z.string().optional(),
  useProfileResume: z.boolean().optional(),
});
export type ApplyToJobOfferInput = z.infer<typeof applyToJobOfferSchema> & {
  resume?: File;
};

// PATCH /api/job-applications/{id} — company review, all optional.
export const reviewApplicationSchema = z.object({
  applicationStatus: z.enum(ApplicationStatus).optional(),
  companyUserNote: z.string().optional(),
  priority: z.enum(Priority).optional(),
  rating: z.number().int().min(0).max(10).optional(),
  // Required together when applicationStatus is SCHEDULED_INTERVIEW; which of link/location
  // is needed depends on the mode, so the server has the last word and its message is shown.
  interviewMode: z.enum(InterviewMode).optional(),
  interviewAt: z.string().optional(),
  interviewLink: z.string().optional(),
  interviewLocation: z.string().optional(),
  interviewerName: z.string().optional(),
});
export type ReviewApplicationDTO = z.infer<typeof reviewApplicationSchema>;

// Triage filters for GET /api/job-offers/{id}/applications.
export interface ApplicationFilters extends PageParams {
  status?: ApplicationStatus;
  reviewed?: boolean;
  minRating?: number;
}

// GET /api/job-applications/me — candidate's own list, optionally filtered to a set of statuses.
export interface MyApplicationFilters extends PageParams {
  status?: ApplicationStatus[];
}
