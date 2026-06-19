// A company's "manage my postings" screen. Lists the caller's own company's job
// offers (any status) via /job-offers/me — OWNER/RECRUITER only, so a MEMBER who
// reaches this URL gets the error card (the backend 403s). Posting a new offer and
// editing/closing existing ones land in the next commits (/company/jobs/new,
// /company/jobs/:id/edit).

import { Link } from "react-router-dom";
import { Briefcase, CloudOff, Plus, RefreshCw, Users } from "lucide-react";
import { useMyJobOffers } from "@/features/jobOffers/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { daysFromNow, humanizeType } from "@/features/jobOffers/format";
import { jobStatusOptions } from "@/types/enums";
import type { JobStatus } from "@/types/enums";
import type { JobOfferDTO } from "@/types/jobOffer";

// Map an offer's status to a badge colour.
function statusVariant(status: JobStatus): "default" | "secondary" | "destructive" | "outline" {
  if (status === "OPEN") return "default";
  if (status === "CLOSED") return "secondary";
  if (status === "EXPIRED") return "destructive";
  return "outline"; // DRAFT
}

export function CompanyJobOffersPage() {
  const { t, tn } = useT();
  const { data, isLoading, isError, refetch } = useMyJobOffers();

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">{t("myJobs.title")}</h1>
          <p className="mt-1 text-sm text-[var(--color-slate)]">{t("myJobs.subtitle")}</p>
        </div>
        <Button asChild>
          <Link to="/company/jobs/new">
            <Plus className="size-4" /> {t("myJobs.post")}
          </Link>
        </Button>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-24 w-full rounded-lg" />
          <Skeleton className="h-24 w-full rounded-lg" />
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
            <Briefcase className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("myJobs.empty.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("myJobs.empty.body")}</p>
          <Button className="mt-5" asChild>
            <Link to="/company/jobs/new">
              <Plus className="size-4" /> {t("myJobs.post")}
            </Link>
          </Button>
        </div>
      ) : (
        <>
          <p className="mb-3 text-sm text-[var(--color-steel)]">
            {tn(data.totalElements, "myJobs.count.one", "myJobs.count.other")}
          </p>
          <div className="space-y-3">
            {data.content.map((offer) => (
              <OfferRow key={offer.id} offer={offer} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function OfferRow({ offer }: { offer: JobOfferDTO }) {
  const { t, tn } = useT();
  const status = jobStatusOptions.find((o) => o.value === offer.status)?.label ?? offer.status;
  const postedDaysAgo = Math.max(0, -daysFromNow(offer.createdAt));

  const applicants =
    offer.maxApplications != null
      ? t("card.applicantsMax", {
          current: offer.currentApplicationCount,
          max: offer.maxApplications,
        })
      : tn(offer.currentApplicationCount, "card.applicants.one", "card.applicants.other");

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-5">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">{offer.title}</h3>
          <Badge variant={statusVariant(offer.status)}>{status}</Badge>
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-2.5 text-sm text-[var(--color-slate)]">
          {offer.employmentType && <span>{humanizeType(offer.employmentType)}</span>}
          {/* The applicant count doubles as the link into the applicants screen. */}
          <Link
            to={`/company/jobs/${offer.id}/applications`}
            className="inline-flex items-center gap-1.5 hover:text-foreground hover:underline"
          >
            <Users className="size-3.5" /> {applicants}
          </Link>
          <span>{tn(postedDaysAgo, "card.posted.one", "card.posted.other")}</span>
        </div>
      </div>
      <div className="flex shrink-0 gap-2">
        <Button variant="ghost" size="sm" asChild>
          <Link to={`/jobs/${offer.id}`}>{t("myJobs.view")}</Link>
        </Button>
        <Button variant="outline" size="sm" asChild>
          <Link to={`/company/jobs/${offer.id}/edit`}>{t("myJobs.manage")}</Link>
        </Button>
      </div>
    </article>
  );
}
