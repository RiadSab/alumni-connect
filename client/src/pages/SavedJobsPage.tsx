// Candidate's bookmarked offers, newest-saved first. Reuses JobCard, whose own
// bookmark button lets the candidate unsave straight from this list.

import { Link } from "react-router-dom";
import { Bookmark, CloudOff, RefreshCw } from "lucide-react";
import { useSavedJobs } from "@/features/savedJobs/hooks";
import { useT } from "@/features/i18n/lang-context";
import { JobCard } from "@/features/jobOffers/JobCard";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { useState } from "react";

export function SavedJobsPage() {
  const { t, tn } = useT();
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, refetch } = useSavedJobs({ page });

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6">
        <h1 className="text-3xl font-semibold tracking-tight text-foreground">{t("saved.title")}</h1>
        <p className="mt-1.5 text-[15px] text-[var(--color-steel)]">{t("saved.subtitle")}</p>
      </div>

      {isLoading && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-40 w-full rounded-lg" />
          ))}
        </div>
      )}

      {isError && (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
          <Button className="mt-5" onClick={() => refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      )}

      {data && data.empty && (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[var(--color-surface)] text-[var(--color-steel)]">
            <Bookmark className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("saved.empty.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("saved.empty.body")}</p>
          <Button asChild className="mt-5">
            <Link to="/">{t("saved.empty.cta")}</Link>
          </Button>
        </div>
      )}

      {data && !data.empty && (
        <>
          <div className="mb-4 text-[15px] font-semibold text-foreground">
            {tn(data.totalElements, "saved.count.one", "saved.count.other")}
          </div>
          <div className="flex flex-col gap-3">
            {data.content.map((job) => (
              <JobCard key={job.id} job={job} />
            ))}
          </div>

          {data.totalPages > 1 && (
            <nav className="mt-6 flex items-center justify-center gap-1.5">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage(page - 1)}>
                {t("pager.prev")}
              </Button>
              <span className="px-2 text-sm text-[var(--color-steel)]">
                {t("pager.page", { n: data.number + 1, total: data.totalPages })}
              </span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage(page + 1)}>
                {t("pager.next")}
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}
