// Central query-key factory. Every cached query gets its key from here so reads
// and invalidations always use the same key. Each `all()` returns the top-level
// prefix, which invalidates every query under that feature at once.

import type { PageParams } from "@/types/common";
import type { CompanyListParams } from "@/types/company";
import type { JobOfferFilters } from "@/types/jobOffer";
import type { ApplicationFilters, MyApplicationFilters } from "@/types/jobApplication";
import type { AlumniRecordFilters } from "@/types/alumni";
import type { AdminUserFilters, AdminCompanyFilters } from "@/types/admin";

export const queryKeys = {
  candidate: {
    me: () => ["candidate", "me"] as const,
    resume: () => ["candidate", "me", "resume"] as const,
    photo: () => ["candidate", "me", "photo"] as const,
    byId: (id: number) => ["candidate", "byId", id] as const,
  },

  companies: {
    all: () => ["companies"] as const,
    list: (params?: CompanyListParams) => ["companies", "list", params] as const,
    byId: (id: number) => ["companies", "byId", id] as const,
  },

  companyUsers: {
    all: () => ["companyUsers"] as const,
    roster: (params?: PageParams) => ["companyUsers", "roster", params] as const,
    pending: (params?: PageParams) => ["companyUsers", "pending", params] as const,
    me: () => ["companyUsers", "me"] as const,
    byId: (id: number) => ["companyUsers", "byId", id] as const,
  },

  jobOffers: {
    all: () => ["jobOffers"] as const,
    browse: (filters?: JobOfferFilters) => ["jobOffers", "browse", filters] as const,
    mine: (params?: PageParams) => ["jobOffers", "mine", params] as const,
    myStats: () => ["jobOffers", "myStats"] as const,
    recommended: (params?: PageParams) => ["jobOffers", "recommended", params] as const,
    byId: (id: number) => ["jobOffers", "byId", id] as const,
    applications: (id: number, filters?: ApplicationFilters) =>
      ["jobOffers", "byId", id, "applications", filters] as const,
  },

  savedJobs: {
    all: () => ["savedJobs"] as const,
    list: (params?: PageParams) => ["savedJobs", "list", params] as const,
    ids: () => ["savedJobs", "ids"] as const,
  },

  jobApplications: {
    all: () => ["jobApplications"] as const,
    mine: (params?: MyApplicationFilters) => ["jobApplications", "mine", params] as const,
    myStats: () => ["jobApplications", "myStats"] as const,
    byId: (id: number) => ["jobApplications", "byId", id] as const,
    applicant: (id: number) => ["jobApplications", "byId", id, "applicant"] as const,
    resume: (id: number) => ["jobApplications", "byId", id, "resume"] as const,
  },

  reports: {
    all: () => ["reports"] as const,
    promotions: () => ["reports", "promotions"] as const,
    employment: (promotionYear: number | null) => ["reports", "employment", promotionYear] as const,
  },

  employment: {
    all: () => ["employment"] as const,
    mine: () => ["employment", "mine"] as const,
    confirm: (token: string) => ["employment", "confirm", token] as const,
  },

  alumni: {
    all: () => ["alumni"] as const,
    records: (filters?: AlumniRecordFilters) => ["alumni", "records", filters] as const,
    claim: (token: string) => ["alumni", "claim", token] as const,
  },

  admin: {
    all: () => ["admin"] as const,
    stats: () => ["admin", "stats"] as const,
    pendingUsers: (params?: PageParams) => ["admin", "pendingUsers", params] as const,
    users: (filters?: AdminUserFilters) => ["admin", "users", filters] as const,
    pendingCompanies: (params?: PageParams) => ["admin", "pendingCompanies", params] as const,
    companies: (filters?: AdminCompanyFilters) => ["admin", "companies", filters] as const,
  },
};
