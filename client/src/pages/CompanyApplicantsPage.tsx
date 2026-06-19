// Applicants for one of the company's job offers (OWNER/RECRUITER). Lists the
// applications via GET /job-offers/{id}/applications. Reviewing a single applicant
// (status, note, rating, accept/reject) lives on the detail page reached from each
// row (/company/applications/:appId). A MEMBER who reaches this URL gets the error
// card (the backend 403s). Reached at /company/jobs/:id/applications.

import { Link, useParams } from "react-router-dom";
import { ArrowLeft, CloudOff, RefreshCw, Star, Users } from "lucide-react";
import { useJobOfferApplications } from "@/features/jobOffers/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { daysFromNow } from "@/features/jobOffers/format";
import { applicationStatusOptions, priorityOptions } from "@/types/enums";
import type { ApplicationStatus } from "@/types/enums";
import type { JobApplicationDTO } from "@/types/jobApplication";

// Map an application's status to a badge colour.
function statusVariant(
  status: ApplicationStatus,
): "default" | "secondary" | "destructive" | "outline" {
  if (status === "ACCEPTED") return "default";
  if (status === "REJECTED") return "destructive";
  if (status === "WITHDRAWN") return "secondary";
  return "outline";
}

export function CompanyApplicantsPage() {
  const { t, tn } = useT();
  const params = useParams();
  const offerId = Number(params.id);
  const { data, isLoading, isError, refetch } = useJobOfferApplications(offerId);

  // The offer title comes back on each application; show it once we have a row.
  const offerTitle = data?.content[0]?.jobOfferTitle;

  return (
    <div className="mx-auto max-w-3xl">
      <Link
        to="/company/jobs"
        className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-[var(--color-link-blue)] hover:text-[var(--color-link-blue-pressed)]"
      >
        <ArrowLeft className="size-4" /> {t("applicants.back")}
      </Link>

      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-foreground">{t("applicants.title")}</h1>
        {offerTitle && <p className="mt-1 text-sm text-[var(--color-slate)]">{offerTitle}</p>}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-lg" />
          <Skeleton className="h-20 w-full rounded-lg" />
        </div>
      ) : isError || !data ? (
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
      ) : data.content.length === 0 ? (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-muted text-muted-foreground">
            <Users className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("applicants.empty.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">
            {t("applicants.empty.body")}
          </p>
        </div>
      ) : (
        <>
          <p className="mb-3 text-sm text-[var(--color-steel)]">
            {tn(data.totalElements, "card.applicants.one", "card.applicants.other")}
          </p>
          <div className="space-y-3">
            {data.content.map((application) => (
              <ApplicantRow key={application.id} application={application} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function ApplicantRow({ application }: { application: JobApplicationDTO }) {
  const { t, tn } = useT();
  const status =
    applicationStatusOptions.find((o) => o.value === application.applicationStatus)?.label ??
    application.applicationStatus;
  const priority = application.priority
    ? priorityOptions.find((o) => o.value === application.priority)?.label
    : null;
  const appliedDaysAgo = Math.max(0, -daysFromNow(application.createdAt));

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-5">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">
            {application.applicantName}
          </h3>
          <Badge variant={statusVariant(application.applicationStatus)}>{status}</Badge>
          {priority && <Badge variant="outline">{priority}</Badge>}
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-2.5 text-sm text-[var(--color-slate)]">
          {application.rating != null && (
            <span className="inline-flex items-center gap-1">
              <Star className="size-3.5 text-[var(--color-brand-orange-deep)]" /> {application.rating}/10
            </span>
          )}
          <span>{tn(appliedDaysAgo, "card.posted.one", "card.posted.other")}</span>
        </div>
      </div>
      <Button variant="outline" size="sm" asChild>
        <Link to={`/company/applications/${application.id}`}>{t("applicants.review")}</Link>
      </Button>
    </article>
  );
}
