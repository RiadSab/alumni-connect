// Job-application endpoints (§6).

import { http } from "@/lib/http";
import type { Page, PageParams } from "@/types/common";
import type { CandidateProfileDTO } from "@/types/candidate";
import type { JobApplicationDTO, ReviewApplicationDTO } from "@/types/jobApplication";

export const jobApplicationsApi = {
  // CANDIDATE — own application history.
  mine: (params?: PageParams) =>
    http.get<Page<JobApplicationDTO>>("/job-applications/me", { query: params }),

  // Applicant or posting company's OWNER/RECRUITER.
  byId: (id: number) => http.get<JobApplicationDTO>(`/job-applications/${id}`),

  // OWNER/RECRUITER — the applicant's full candidate profile.
  applicant: (id: number) => http.get<CandidateProfileDTO>(`/job-applications/${id}/applicant`),

  // Applicant or posting company's OWNER/RECRUITER.
  downloadResume: (id: number) => http.getBlob(`/job-applications/${id}/resume`),

  // CANDIDATE — withdraw own application.
  withdraw: (id: number) => http.post<JobApplicationDTO>(`/job-applications/${id}/withdraw`),

  // OWNER/RECRUITER — review (status, note, priority, rating).
  review: (id: number, body: ReviewApplicationDTO) =>
    http.patch<JobApplicationDTO>(`/job-applications/${id}`, { json: body }),
};
