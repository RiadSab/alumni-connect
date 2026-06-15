// Candidate endpoints (§2).

import { http } from "@/lib/http";
import { singleFileForm } from "@/lib/form";
import type { StoredFileDTO } from "@/types/common";
import type { CandidateProfileDTO, UpdateCandidateProfileDTO } from "@/types/candidate";

export const candidatesApi = {
  me: () => http.get<CandidateProfileDTO>("/candidates/me"),

  updateMe: (body: UpdateCandidateProfileDTO) =>
    http.patch<CandidateProfileDTO>("/candidates/me", { json: body }),

  uploadResume: (file: File) =>
    http.post<StoredFileDTO>("/candidates/me/resume", { form: singleFileForm(file) }),

  downloadResume: () => http.getBlob("/candidates/me/resume"),

  uploadPhoto: (file: File) =>
    http.post<StoredFileDTO>("/candidates/me/photo", { form: singleFileForm(file) }),

  downloadPhoto: () => http.getBlob("/candidates/me/photo"),

  // ADMINISTRATOR only.
  byId: (id: number) => http.get<CandidateProfileDTO>(`/candidates/${id}`),
};
