// Job-offer endpoints (§5).

import { http } from "@/lib/http";
import { appendIfPresent } from "@/lib/form";
import type { Page, PageParams } from "@/types/common";
import type {
  CompanyOfferStatsDTO,
  CreateJobOfferDTO,
  JobOfferDTO,
  JobOfferFilters,
  UpdateJobOfferDTO,
} from "@/types/jobOffer";
import type {
  ApplicationFilters,
  ApplyToJobOfferInput,
  JobApplicationDTO,
} from "@/types/jobApplication";

function buildApplyForm(input: ApplyToJobOfferInput): FormData {
  const form = new FormData();
  appendIfPresent(form, "coverLetter", input.coverLetter);
  appendIfPresent(form, "resume", input.resume);
  appendIfPresent(form, "useProfileResume", input.useProfileResume);
  return form;
}

export const jobOffersApi = {
  // Public browse of OPEN offers with filters.
  browse: (filters?: JobOfferFilters) =>
    http.get<Page<JobOfferDTO>>("/job-offers", { query: filters }),

  // OWNER/RECRUITER.
  create: (body: CreateJobOfferDTO) => http.post<JobOfferDTO>("/job-offers", { json: body }),

  // OWNER/RECRUITER — own company's offers, any status.
  mine: (params?: PageParams) => http.get<Page<JobOfferDTO>>("/job-offers/me", { query: params }),

  // OWNER/RECRUITER — dashboard counts.
  myStats: () => http.get<CompanyOfferStatsDTO>("/job-offers/me/stats"),

  // CANDIDATE — skill-matched OPEN offers.
  recommended: (params?: PageParams) =>
    http.get<Page<JobOfferDTO>>("/job-offers/recommended", { query: params }),

  byId: (id: number) => http.get<JobOfferDTO>(`/job-offers/${id}`),

  // OWNER/RECRUITER — partial update incl. status (close/reopen).
  update: (id: number, body: UpdateJobOfferDTO) =>
    http.patch<JobOfferDTO>(`/job-offers/${id}`, { json: body }),

  // OWNER/RECRUITER — applicants for one offer.
  applications: (id: number, filters?: ApplicationFilters) =>
    http.get<Page<JobApplicationDTO>>(`/job-offers/${id}/applications`, { query: filters }),

  // CANDIDATE — apply (multipart).
  apply: (id: number, input: ApplyToJobOfferInput) =>
    http.post<JobApplicationDTO>(`/job-offers/${id}/apply`, { form: buildApplyForm(input) }),
};
