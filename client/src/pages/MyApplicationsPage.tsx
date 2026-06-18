// Candidate's own application history. Lists every job they've applied to with
// its current status, newest first, linking each back to the job detail.

import { Link } from "react-router-dom";
import { CloudOff, FileText, RefreshCw } from "lucide-react";
import { useMyApplications } from "@/features/jobApplications/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { applicationStatusOptions, type ApplicationStatus } from "@/types/enums";
import type { JobApplicationDTO } from "@/types/jobApplication";
import { useState } from "react";

// Badge colour per status. Labels stay English (enum-label translation deferred,
// same as employment types) via applicationStatusOptions.
const STATUS_STYLES: Record<ApplicationStatus, string> = {
  APPLIED: "bg-[var(--color-surface)] text-[var(--color-slate)]",
  UNDER_REVIEW: "bg-[var(--color-tint-sky)] text-[var(--color-link-blue-pressed)]",
  SCHEDULED_INTERVIEW: "bg-[var(--color-tint-lavender)] text-[var(--color-brand-purple-800)]",
  INTERVIEWED: "bg-[var(--color-tint-lavender)] text-[var(--color-brand-purple-800)]",
  ACCEPTED: "bg-[color-mix(in_srgb,var(--color-brand-green)_14%,#fff)] text-[var(--color-brand-green)]",
  REJECTED: "bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]",
  WITHDRAWN: "bg-[var(--color-surface)] text-[var(--color-stone)]",
};

function statusLabel(status: ApplicationStatus): string {
  return applicationStatusOptions.find((o) => o.value === status)?.label ?? status;
}

export function MyApplicationsPage() {
  const { t, tn, lang } = useT();
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, refetch } = useMyApplications({ page });

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6">
        <h1 className="text-3xl font-semibold tracking-tight text-foreground">{t("apps.title")}</h1>
        <p className="mt-1.5 text-[15px] text-[var(--color-steel)]">{t("apps.subtitle")}</p>
      </div>

      {isLoading && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-lg" />
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
            <FileText className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("apps.empty.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("apps.empty.body")}</p>
          <Button asChild className="mt-5">
            <Link to="/">{t("apps.empty.cta")}</Link>
          </Button>
        </div>
      )}

      {data && !data.empty && (
        <>
          <div className="mb-4 text-[15px] font-semibold text-foreground">
            {tn(data.totalElements, "apps.count.one", "apps.count.other")}
          </div>
          <div className="flex flex-col gap-3">
            {data.content.map((app) => (
              <ApplicationRow
                key={app.id}
                app={app}
                appliedLabel={t("apps.applied", {
                  date: new Date(app.createdAt).toLocaleDateString(lang, {
                    day: "numeric",
                    month: "short",
                    year: "numeric",
                  }),
                })}
              />
            ))}
          </div>

          {/* ponytail: simple Prev/Next — candidates rarely span many pages; reuse
              HomePage's numbered pager if these lists ever grow. */}
          {data.totalPages > 1 && (
            <nav className="mt-6 flex items-center justify-center gap-1.5">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage(page - 1)}>
                Prev
              </Button>
              <span className="px-2 text-sm text-[var(--color-steel)]">
                {data.number + 1} / {data.totalPages}
              </span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage(page + 1)}>
                Next
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}

function ApplicationRow({ app, appliedLabel }: { app: JobApplicationDTO; appliedLabel: string }) {
  return (
    <Link
      to={`/jobs/${app.jobOfferId}`}
      className="flex items-center justify-between gap-4 rounded-lg border border-border bg-card p-5 hover:border-[var(--color-stone)]"
    >
      <div className="min-w-0">
        <div className="truncate font-semibold text-foreground">{app.jobOfferTitle}</div>
        <div className="mt-1 text-sm text-[var(--color-steel)]">{appliedLabel}</div>
      </div>
      <span
        className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[app.applicationStatus]}`}
      >
        {statusLabel(app.applicationStatus)}
      </span>
    </Link>
  );
}
