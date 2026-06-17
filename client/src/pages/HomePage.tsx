// Public job board. Fetches OPEN offers (optionally filtered) and renders them as
// cards. Pagination comes in the next part; this shows page 1 with loading / empty
// / error states matching the design.

import { useState } from "react";
import { CloudOff, RefreshCw, RotateCcw, SearchX } from "lucide-react";
import { useJobOffers } from "@/features/jobOffers/hooks";
import { JobCard } from "@/features/jobOffers/JobCard";
import { FilterSidebar } from "@/features/jobOffers/FilterSidebar";
import { EMPTY_FILTERS, type JobFilterValues } from "@/features/jobOffers/filters";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import type { JobOfferFilters } from "@/types/jobOffer";
import type { EmploymentType, JobCity } from "@/types/enums";

function JobCardSkeleton() {
  return (
    <div className="grid grid-cols-[48px_1fr] gap-4 rounded-lg border border-border bg-card p-5">
      <Skeleton className="size-12 rounded-md" />
      <div className="space-y-3">
        <Skeleton className="h-[18px] w-[46%]" />
        <Skeleton className="h-[13px] w-[62%]" />
        <Skeleton className="h-[15px] w-[30%]" />
        <div className="flex gap-1.5">
          <Skeleton className="h-[22px] w-16" />
          <Skeleton className="h-[22px] w-20" />
          <Skeleton className="h-[22px] w-14" />
        </div>
      </div>
    </div>
  );
}

export function HomePage() {
  const [values, setValues] = useState<JobFilterValues>(EMPTY_FILTERS);

  function patch(part: Partial<JobFilterValues>) {
    setValues((prev) => ({ ...prev, ...part }));
  }

  const filters: JobOfferFilters = {
    q: values.q.trim() || undefined,
    city: values.city === "ALL" ? undefined : (values.city as JobCity),
    employmentType:
      values.employmentType === "ALL" ? undefined : (values.employmentType as EmploymentType),
    isRemote: values.isRemote || undefined,
  };

  const hasActiveFilters =
    filters.q !== undefined ||
    filters.city !== undefined ||
    filters.employmentType !== undefined ||
    filters.isRemote !== undefined;

  const { data, isLoading, isError, refetch } = useJobOffers(filters);

  return (
    <div className="mx-auto max-w-6xl">
      <div className="mb-6">
        <h1 className="text-3xl font-semibold tracking-tight text-foreground">Job Board</h1>
        <p className="mt-1.5 text-[15px] text-[var(--color-steel)]">
          Opportunities posted by companies in the Alumni Connect network.
        </p>
      </div>

      <div className="grid items-start gap-7 lg:grid-cols-[264px_1fr]">
        <FilterSidebar
          values={values}
          onChange={patch}
          onClear={() => setValues(EMPTY_FILTERS)}
        />

        <section>
          {isLoading && (
            <div className="flex flex-col gap-3">
              {Array.from({ length: 5 }).map((_, i) => (
                <JobCardSkeleton key={i} />
              ))}
            </div>
          )}

          {isError && (
            <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
              <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
                <CloudOff className="size-6" />
              </div>
              <h3 className="text-lg font-semibold text-foreground">Couldn't load jobs</h3>
              <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">
                Something went wrong reaching the job board. Check your connection and try again.
              </p>
              <Button className="mt-5" onClick={() => refetch()}>
                <RefreshCw className="size-4" /> Retry
              </Button>
            </div>
          )}

          {data && data.empty && (
            <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
              <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[var(--color-surface)] text-[var(--color-steel)]">
                <SearchX className="size-6" />
              </div>
              <h3 className="text-lg font-semibold text-foreground">
                {hasActiveFilters ? "No jobs match your filters" : "No open jobs right now"}
              </h3>
              <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">
                {hasActiveFilters
                  ? "Try removing a filter or two — broadening the city or clearing the type usually surfaces more roles."
                  : "There are no open positions at the moment. Check back soon — new roles are posted regularly."}
              </p>
              {hasActiveFilters && (
                <Button className="mt-5" onClick={() => setValues(EMPTY_FILTERS)}>
                  <RotateCcw className="size-4" /> Clear filters
                </Button>
              )}
            </div>
          )}

          {data && !data.empty && (
            <>
              <div className="mb-4 text-[15px] font-semibold text-foreground">
                {data.totalElements} {data.totalElements === 1 ? "job" : "jobs"}
              </div>
              <div className="flex flex-col gap-3">
                {data.content.map((job) => (
                  <JobCard key={job.id} job={job} />
                ))}
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
