import { http } from "@/lib/http";
import type { Page, PageParams } from "@/types/common";
import type { JobOfferDTO } from "@/types/jobOffer";

export const savedJobsApi = {
  // The candidate's saved offers, newest-saved first.
  list: (params?: PageParams) => http.get<Page<JobOfferDTO>>("/saved-jobs", { query: params }),

  // Just the saved offer ids — lets the board mark each card in one request.
  ids: () => http.get<number[]>("/saved-jobs/ids"),

  save: (offerId: number) => http.post<JobOfferDTO>(`/saved-jobs/${offerId}`),
  unsave: (offerId: number) => http.del<void>(`/saved-jobs/${offerId}`),
};