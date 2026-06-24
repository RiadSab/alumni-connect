// Job-offer DTOs (§5).

import { z } from "zod";
import { EmploymentType, JobCity, JobStatus } from "@/types/enums";
import type { AuditFields, PageParams } from "@/types/common";
import { nonBlank } from "@/lib/validation";

// Output shape (list + detail).
export interface JobOfferDTO extends AuditFields {
  id: number;
  title: string;
  description: string | null;
  requirements: string[];
  companyId: number;
  companyName: string;
  logoId: string | null;
  postedById: number;
  postedByName: string;
  city: JobCity | null;
  minSalary: number | null;
  maxSalary: number | null;
  employmentType: EmploymentType | null;
  applicationDeadline: string | null; // ISO date-time
  status: JobStatus;
  experienceYears: number | null;
  skillsRequired: string[];
  isRemote: boolean;
  isUrgent: boolean;
  maxApplications: number | null;
  currentApplicationCount: number;
  contactEmail: string | null;
  hasApplied: boolean | null; // detail view only; null in list responses
  isSaved: boolean | null; // detail view + saved-jobs list; null elsewhere
}

// GET /api/job-offers/me/stats — company dashboard counts.
export interface CompanyOfferStatsDTO {
  totalPostings: number;
  openPostings: number;
  totalApplicants: number;
}

// POST /api/job-offers
export const createJobOfferSchema = z.object({
  title: nonBlank(),
  description: z.string().optional(),
  requirements: z.array(z.string()).optional(),
  city: z.enum(JobCity).optional(),
  minSalary: z.number().int().optional(),
  maxSalary: z.number().int().optional(),
  employmentType: z.enum(EmploymentType).optional(),
  applicationDeadline: z.string().optional(), // ISO date-time
  experienceYears: z.number().int().min(0).optional(),
  skillsRequired: z.array(z.string()).optional(),
  isRemote: z.boolean().optional(),
  isUrgent: z.boolean().optional(),
  maxApplications: z.number().int().optional(),
  contactEmail: z.email().optional(),
});
export type CreateJobOfferDTO = z.infer<typeof createJobOfferSchema>;

// PATCH /api/job-offers/{id} — all optional, incl. status (close/reopen).
export const updateJobOfferSchema = createJobOfferSchema.partial().extend({
  status: z.enum(JobStatus).optional(),
});
export type UpdateJobOfferDTO = z.infer<typeof updateJobOfferSchema>;

// Query filters for GET /api/job-offers (public browse).
export interface JobOfferFilters extends PageParams {
  q?: string;
  city?: JobCity;
  employmentType?: EmploymentType;
  isRemote?: boolean;
  skills?: string[];
}
