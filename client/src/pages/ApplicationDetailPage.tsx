// A candidate's view of one of their own applications: the offer it's for, its
// current status, the résumé and cover letter they submitted, and a guarded
// withdraw action. The My Applications rows link here.

import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, Briefcase, Calendar, CloudOff, FileText, MapPin, RefreshCw } from "lucide-react";
import {
  useApplication,
  useApplicationResume,
  useWithdrawApplication,
} from "@/features/jobApplications/hooks";
import { useJobOffer } from "@/features/jobOffers/hooks";
import { formatMoney, humanizeType } from "@/features/jobOffers/format";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { applicationStatusOptions, type ApplicationStatus } from "@/types/enums";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

// Badge colour per status (kept in step with MyApplicationsPage; labels stay English
// via applicationStatusOptions, same as the rest of the app).
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

// Withdraw makes no sense once the application has reached a terminal outcome.
const TERMINAL: ApplicationStatus[] = ["WITHDRAWN", "ACCEPTED", "REJECTED"];

export function ApplicationDetailPage() {
  const { t, lang } = useT();
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  const application = useApplication(id);
  const app = application.data;
  const offer = useJobOffer(app?.jobOfferId ?? Number.NaN); // enabled once the application loads
  const resume = useApplicationResume(id, !!app?.resumeStorageId);
  const withdraw = useWithdrawApplication();
  const [confirmOpen, setConfirmOpen] = useState(false);

  if (application.isLoading) {
    return (
      <div className="mx-auto max-w-3xl">
        <Skeleton className="mb-6 h-8 w-40 rounded" />
        <Skeleton className="h-64 w-full rounded-lg" />
      </div>
    );
  }

  if (application.isError || !app) {
    return (
      <div className="mx-auto max-w-3xl">
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
          <Button className="mt-5" onClick={() => application.refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      </div>
    );
  }

  const canWithdraw = !TERMINAL.includes(app.applicationStatus);
  const appliedLabel = t("apps.applied", {
    date: new Date(app.createdAt).toLocaleDateString(lang, {
      day: "numeric",
      month: "short",
      year: "numeric",
    }),
  });

  function viewResume() {
    if (!resume.data) return;
    const url = URL.createObjectURL(resume.data);
    window.open(url, "_blank");
    // ponytail: revoke after a minute — the new tab has loaded the blob by then.
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        to="/applications"
        className="mb-5 inline-flex items-center gap-1.5 text-sm font-medium text-[var(--color-steel)] hover:text-foreground"
      >
        <ArrowLeft className="size-4" /> {t("appDetail.back")}
      </Link>

      <div className="rounded-lg border border-border bg-card p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <h1 className="truncate text-2xl font-semibold tracking-tight text-foreground">
              {app.jobOfferTitle}
            </h1>
            <p className="mt-1 text-sm text-[var(--color-steel)]">{appliedLabel}</p>
          </div>
          <span
            className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLES[app.applicationStatus]}`}
          >
            {statusLabel(app.applicationStatus)}
          </span>
        </div>

        <OfferSummary query={offer} />

        {/* Résumé submitted with this application (a snapshot — independent of the profile CV). */}
        <Section title={t("appDetail.resumeTitle")}>
          {app.resumeStorageId ? (
            <Button variant="outline" size="sm" onClick={viewResume} disabled={resume.isLoading}>
              <FileText className="size-4" />
              {resume.isLoading ? t("appDetail.resumeLoading") : t("appDetail.viewResume")}
            </Button>
          ) : (
            <p className="text-sm text-[var(--color-slate)]">{t("appDetail.noResume")}</p>
          )}
          {resume.isError && (
            <p className="mt-1.5 text-sm text-destructive">{t("appDetail.resumeError")}</p>
          )}
        </Section>

        {/* Cover letter the candidate submitted. */}
        <Section title={t("appDetail.coverLetterTitle")}>
          {app.coverLetter ? (
            <p className="whitespace-pre-wrap text-sm text-[var(--color-charcoal)]">
              {app.coverLetter}
            </p>
          ) : (
            <p className="text-sm text-[var(--color-slate)]">{t("appDetail.noCoverLetter")}</p>
          )}
        </Section>

        {canWithdraw && (
          <div className="mt-6 border-t border-border pt-5">
            <Dialog
              open={confirmOpen}
              onOpenChange={(next) => {
                setConfirmOpen(next);
                if (!next) withdraw.reset();
              }}
            >
              <DialogTrigger asChild>
                <Button variant="outline" size="sm">
                  {t("appDetail.withdraw")}
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogTitle>{t("appDetail.withdrawTitle")}</DialogTitle>
                <DialogDescription>{t("appDetail.withdrawPrompt")}</DialogDescription>
                {withdraw.isError && (
                  <p className="mt-2 text-sm text-destructive">
                    {isApiError(withdraw.error) ? withdraw.error.message : t("appDetail.withdrawError")}
                  </p>
                )}
                <DialogFooter>
                  <DialogClose asChild>
                    <Button variant="ghost" size="sm">
                      {t("appDetail.cancel")}
                    </Button>
                  </DialogClose>
                  <Button
                    variant="destructive"
                    size="sm"
                    disabled={withdraw.isPending}
                    onClick={() =>
                      withdraw.mutate(app.id, { onSuccess: () => setConfirmOpen(false) })
                    }
                  >
                    {withdraw.isPending ? t("appDetail.withdrawing") : t("appDetail.withdrawConfirm")}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        )}
      </div>
    </div>
  );
}

// A compact, live read of the offer this application is for. Applicants are allowed
// to read their offer even after it closes (except DRAFT), so this works for closed/
// expired offers too; if it's unreachable, we just say so.
function OfferSummary({ query }: { query: ReturnType<typeof useJobOffer> }) {
  const { t, lang } = useT();
  const o = query.data;

  return (
    <Section title={t("appDetail.offerTitle")}>
      {query.isLoading ? (
        <Skeleton className="h-16 w-full rounded" />
      ) : o ? (
        <div>
          <div className="font-medium text-[var(--color-charcoal)]">{o.companyName}</div>
          <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1.5 text-sm text-[var(--color-steel)]">
            <span className="inline-flex items-center gap-1.5">
              <MapPin className="size-4" /> {o.isRemote ? t("detail.remoteLocation") : (o.city ?? "—")}
            </span>
            {o.employmentType && (
              <span className="inline-flex items-center gap-1.5">
                <Briefcase className="size-4" /> {humanizeType(o.employmentType)}
              </span>
            )}
            {o.minSalary != null && o.maxSalary != null && (
              <span>
                {formatMoney(o.minSalary)} – {formatMoney(o.maxSalary)} MAD
              </span>
            )}
            {o.applicationDeadline && (
              <span className="inline-flex items-center gap-1.5">
                <Calendar className="size-4" />
                {new Date(o.applicationDeadline).toLocaleDateString(lang, {
                  day: "numeric",
                  month: "short",
                  year: "numeric",
                })}
              </span>
            )}
          </div>
        </div>
      ) : (
        <p className="text-sm text-[var(--color-slate)]">{t("appDetail.offerUnavailable")}</p>
      )}
    </Section>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mt-6 border-t border-border pt-5">
      <h2 className="mb-2 text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {title}
      </h2>
      {children}
    </div>
  );
}
