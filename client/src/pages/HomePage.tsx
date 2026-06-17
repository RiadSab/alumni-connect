// Public job board. Fetches OPEN offers and renders them as cards. Filters and
// pagination come in later parts; this just shows page 1 with loading/empty/error
// states matching the design.

import { CloudOff, RefreshCw, SearchX } from "lucide-react";
import { useJobOffers } from "@/features/jobOffers/hooks";
import { JobCard } from "@/features/jobOffers/JobCard";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";

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
  const { data, isLoading, isError, refetch } = useJobOffers();

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-6">
        <h1 className="text-3xl font-semibold tracking-tight text-foreground">Job Board</h1>
        <p className="mt-1.5 text-[15px] text-[var(--color-steel)]">
          Opportunities posted by companies in the Alumni Connect network.
        </p>
      </div>

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
          <h3 className="text-lg font-semibold text-foreground">No open jobs right now</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">
            There are no open positions at the moment. Check back soon — new roles are posted regularly.
          </p>
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
    </div>
  );
}
