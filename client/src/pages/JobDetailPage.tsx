// Single job offer detail. Public read-only view of one OPEN offer, with a gated
// Apply action: logged-out visitors are sent to login, candidates apply with their
// profile CV in one click, companies/admins see no apply button.

import { Link, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Bookmark,
  Briefcase,
  Calendar,
  CheckCircle2,
  Clock,
  Mail,
  MapPin,
  Star,
  Users,
} from "lucide-react";
import { useJobOffer, useApplyToJobOffer } from "@/features/jobOffers/hooks";
import { useSaveJob, useUnsaveJob } from "@/features/savedJobs/hooks";
import { cn } from "@/lib/utils";
import { daysFromNow, formatMoney, humanizeType } from "@/features/jobOffers/format";
import { CompanyLogo } from "@/features/jobOffers/CompanyLogo";
import { useT } from "@/features/i18n/lang-context";
import { useAuth } from "@/features/auth/auth-context";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { isApiError } from "@/lib/http";
import type { JobOfferDTO } from "@/types/jobOffer";

export function JobDetailPage() {
  const { t } = useT();
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const { data: job, isLoading, isError, error } = useJobOffer(id);

  const backLink = (
    <Link
      to="/"
      className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-[var(--color-link-blue)] hover:text-[var(--color-link-blue-pressed)]"
    >
      <ArrowLeft className="size-4" /> {t("detail.back")}
    </Link>
  );

  if (!Number.isFinite(id) || (isError && isApiError(error) && error.status === 404)) {
    return (
      <div className="mx-auto max-w-3xl">
        {backLink}
        <div className="rounded-lg border border-border bg-card px-8 py-16 text-center">
          <h1 className="text-lg font-semibold text-foreground">{t("detail.notFound.title")}</h1>
          <p className="mt-2 text-sm text-[var(--color-steel)]">{t("detail.notFound.body")}</p>
        </div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="mx-auto max-w-5xl">
        {backLink}
        <div className="grid gap-7 lg:grid-cols-[1fr_300px]">
          <div className="space-y-4">
            <Skeleton className="h-9 w-2/3" />
            <Skeleton className="h-5 w-1/3" />
            <Skeleton className="h-40 w-full" />
          </div>
          <Skeleton className="h-72 w-full" />
        </div>
      </div>
    );
  }

  if (!job) {
    // Network/server error that isn't a 404.
    return (
      <div className="mx-auto max-w-3xl">
        {backLink}
        <div className="rounded-lg border border-border bg-card px-8 py-16 text-center">
          <h1 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h1>
          <p className="mt-2 text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl">
      {backLink}
      <div className="grid items-start gap-7 lg:grid-cols-[1fr_300px]">
        <main className="min-w-0 space-y-6">
          <Header job={job} />
          <Description job={job} />
          <StringList title={t("detail.requirements")} items={job.requirements} />
          {job.skillsRequired.length > 0 && <Skills job={job} />}
        </main>
        <Sidebar job={job} />
      </div>
    </div>
  );
}

function Header({ job }: { job: JobOfferDTO }) {
  const { t } = useT();
  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <div className="flex gap-4">
        <CompanyLogo
          companyId={job.companyId}
          companyName={job.companyName}
          logoId={job.logoId}
          className="size-14 rounded-lg"
          textClassName="text-xl"
        />
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-semibold leading-tight text-foreground">{job.title}</h1>
            {job.isRemote && (
              <span className="rounded-full bg-[var(--color-tint-sky)] px-2 py-0.5 text-xs font-semibold text-[var(--color-link-blue-pressed)]">
                {t("card.remote")}
              </span>
            )}
            {job.isUrgent && (
              <span className="rounded-full bg-[var(--color-tint-peach)] px-2 py-0.5 text-xs font-semibold text-[var(--color-brand-orange-deep)]">
                {t("card.urgent")}
              </span>
            )}
          </div>
          <div className="mt-1.5 flex flex-wrap items-center gap-2.5 text-sm text-[var(--color-slate)]">
            <span className="font-medium text-[var(--color-charcoal)]">{job.companyName}</span>
            {job.city && (
              <>
                <span className="size-[3px] rounded-full bg-[var(--color-stone)]" />
                <span className="inline-flex items-center gap-1">
                  <MapPin className="size-3.5 text-[var(--color-steel)]" /> {job.city}
                </span>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function Description({ job }: { job: JobOfferDTO }) {
  const { t } = useT();
  return (
    <section className="rounded-lg border border-border bg-card p-6">
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {t("detail.description")}
      </h2>
      <p className="whitespace-pre-line text-[15px] leading-relaxed text-[var(--color-charcoal)]">
        {job.description ?? t("detail.noDescription")}
      </p>
    </section>
  );
}

function StringList({ title, items }: { title: string; items: string[] }) {
  if (items.length === 0) return null;
  return (
    <section className="rounded-lg border border-border bg-card p-6">
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {title}
      </h2>
      <ul className="space-y-2">
        {items.map((item) => (
          <li key={item} className="flex gap-2 text-[15px] text-[var(--color-charcoal)]">
            <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-[var(--color-brand-green)]" />
            {item}
          </li>
        ))}
      </ul>
    </section>
  );
}

function Skills({ job }: { job: JobOfferDTO }) {
  const { t } = useT();
  return (
    <section className="rounded-lg border border-border bg-card p-6">
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {t("detail.skills")}
      </h2>
      <div className="flex flex-wrap gap-1.5">
        {job.skillsRequired.map((skill) => (
          <span
            key={skill}
            className="rounded-sm bg-[var(--color-tint-lavender)] px-2.5 py-1 text-[13px] font-medium text-[var(--color-brand-purple-800)]"
          >
            {skill}
          </span>
        ))}
      </div>
    </section>
  );
}

function Sidebar({ job }: { job: JobOfferDTO }) {
  const { t, tn } = useT();
  const deadlineDays = job.applicationDeadline ? daysFromNow(job.applicationDeadline) : null;
  const applicants =
    job.maxApplications != null
      ? t("card.applicantsMax", {
          current: job.currentApplicationCount,
          max: job.maxApplications,
        })
      : tn(job.currentApplicationCount, "card.applicants.one", "card.applicants.other");

  return (
    <aside className="sticky top-6 space-y-4 rounded-lg border border-border bg-card p-6">
      {job.minSalary != null && job.maxSalary != null && (
        <Row icon={<Star className="size-4" />} label={t("detail.salary")}>
          {formatMoney(job.minSalary)} – {formatMoney(job.maxSalary)} MAD {t("card.perMonth")}
        </Row>
      )}
      {job.employmentType && (
        <Row icon={<Briefcase className="size-4" />} label={t("detail.type")}>
          {humanizeType(job.employmentType)}
        </Row>
      )}
      <Row icon={<MapPin className="size-4" />} label={t("detail.location")}>
        {job.isRemote ? t("detail.remoteLocation") : (job.city ?? "—")}
      </Row>
      {job.experienceYears != null && (
        <Row icon={<Clock className="size-4" />} label={t("detail.experience")}>
          {tn(job.experienceYears, "detail.experienceYears.one", "detail.experienceYears.other")}
        </Row>
      )}
      <Row icon={<Users className="size-4" />} label={t("detail.applicants")}>
        {applicants}
      </Row>
      {deadlineDays != null && (
        <Row icon={<Calendar className="size-4" />} label={t("detail.deadline")}>
          {tn(Math.max(0, deadlineDays), "card.closes.one", "card.closes.other")}
        </Row>
      )}
      {job.contactEmail && (
        <Row icon={<Mail className="size-4" />} label={t("detail.contact")}>
          <a className="text-[var(--color-link-blue)] hover:underline" href={`mailto:${job.contactEmail}`}>
            {job.contactEmail}
          </a>
        </Row>
      )}

      <ApplyAction job={job} />
      <SaveAction job={job} />
    </aside>
  );
}

function Row({ icon, label, children }: { icon: React.ReactNode; label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start gap-3">
      <span className="mt-0.5 text-[var(--color-steel)]">{icon}</span>
      <div className="min-w-0">
        <div className="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
          {label}
        </div>
        <div className="text-sm font-medium text-[var(--color-charcoal)]">{children}</div>
      </div>
    </div>
  );
}

function ApplyAction({ job }: { job: JobOfferDTO }) {
  const { t } = useT();
  const { isAuthenticated, user } = useAuth();
  const isCandidate = isAuthenticated && user?.userType === "CANDIDATE";
  const apply = useApplyToJobOffer();

  // Companies/admins can't apply — no button for them.
  if (isAuthenticated && !isCandidate) return null;

  if (!isAuthenticated) {
    return (
      <Button asChild className="w-full">
        <Link to="/login" state={{ from: `/jobs/${job.id}` }}>
          {t("detail.loginToApply")}
        </Link>
      </Button>
    );
  }

  if (apply.isSuccess || job.hasApplied) {
    return (
      <div className="flex items-center justify-center gap-2 rounded-md bg-[color-mix(in_srgb,var(--color-brand-green)_12%,#fff)] px-3 py-2 text-sm font-medium text-[var(--color-brand-green)]">
        <CheckCircle2 className="size-4" /> {apply.isSuccess ? t("detail.applied") : t("detail.alreadyApplied")}
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <Button
        className="w-full"
        disabled={apply.isPending}
        onClick={() => apply.mutate({ id: job.id, data: { useProfileResume: true } })}
      >
        {apply.isPending ? t("detail.applying") : t("card.apply")}
      </Button>
      <p className="text-center text-xs text-[var(--color-steel)]">{t("detail.applyHint")}</p>
      {apply.isError && (
        <p className="text-center text-xs text-destructive">
          {isApiError(apply.error) ? apply.error.message : t("detail.applyError")}
        </p>
      )}
    </div>
  );
}

// Bookmark toggle, candidates only (relies on the offer's isSaved flag).
function SaveAction({ job }: { job: JobOfferDTO }) {
  const { t } = useT();
  const { isAuthenticated, user } = useAuth();
  const isCandidate = isAuthenticated && user?.userType === "CANDIDATE";
  const save = useSaveJob();
  const unsave = useUnsaveJob();

  if (!isCandidate) return null;

  const isSaved = job.isSaved ?? false;
  const pending = save.isPending || unsave.isPending;

  return (
    <Button
      variant="outline"
      className="w-full"
      disabled={pending}
      onClick={() => (isSaved ? unsave : save).mutate(job.id)}
    >
      <Bookmark className={cn("size-4", isSaved && "fill-current")} />
      {isSaved ? t("detail.saved") : t("detail.save")}
    </Button>
  );
}
