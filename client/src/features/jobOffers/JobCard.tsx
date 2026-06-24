// One job offer row for the board / saved list, with candidate Save/Apply actions.

import { Link } from "react-router-dom";
import { Bookmark, Calendar, Clock, MapPin, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useT } from "@/features/i18n/lang-context";
import { useAuth } from "@/features/auth/auth-context";
import { useSavedJobIds, useSaveJob, useUnsaveJob } from "@/features/savedJobs/hooks";
import { cn } from "@/lib/utils";
import { daysFromNow, formatMoney, humanizeType } from "@/features/jobOffers/format";
import { CompanyLogo } from "@/features/jobOffers/CompanyLogo";
import type { JobOfferDTO } from "@/types/jobOffer";

export function JobCard({ job }: { job: JobOfferDTO }) {
  const { t, tn } = useT();
  const { isAuthenticated, user } = useAuth();
  // Apply/save are candidate actions. Logged-out visitors keep them (the detail
  // page funnels them to login); authenticated companies/admins can't apply, so
  // they see no side actions — same rule as the detail page's ApplyAction.
  const canApply = !isAuthenticated || user?.userType === "CANDIDATE";
  const shownSkills = job.skillsRequired.slice(0, 4);
  const extraSkills = job.skillsRequired.length - shownSkills.length;

  const postedDaysAgo = Math.max(0, -daysFromNow(job.createdAt));
  const deadlineDays = job.applicationDeadline ? daysFromNow(job.applicationDeadline) : null;

  const applicants =
    job.maxApplications != null
      ? t("card.applicantsMax", {
          current: job.currentApplicationCount,
          max: job.maxApplications,
        })
      : tn(job.currentApplicationCount, "card.applicants.one", "card.applicants.other");

  return (
    <article className="grid grid-cols-[48px_1fr] gap-4 rounded-lg border border-border bg-card p-5 transition-shadow hover:shadow-md sm:grid-cols-[48px_1fr_auto]">
      {/* Logo */}
      <CompanyLogo
        companyId={job.companyId}
        companyName={job.companyName}
        logoId={job.logoId}
        className="size-12 rounded-md"
        textClassName="text-lg"
      />

      {/* Main */}
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-lg font-semibold leading-tight text-foreground">
            <Link to={`/jobs/${job.id}`} className="hover:text-primary">
              {job.title}
            </Link>
          </h3>
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
                <MapPin className="size-3.5 text-[var(--color-steel)]" />
                {job.city}
              </span>
            </>
          )}
          {job.employmentType && (
            <span className="rounded-sm border border-border bg-[var(--color-surface)] px-2 py-0.5 text-xs font-medium text-[var(--color-slate)]">
              {humanizeType(job.employmentType)}
            </span>
          )}
        </div>

        {job.minSalary != null && job.maxSalary != null && (
          <div className="mt-3 text-[15px] font-semibold text-[var(--color-charcoal)]">
            {formatMoney(job.minSalary)} – {formatMoney(job.maxSalary)} MAD{" "}
            <span className="text-[13px] font-normal text-[var(--color-steel)]">
              {t("card.perMonth")}
            </span>
          </div>
        )}

        {shownSkills.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-1.5">
            {shownSkills.map((skill) => (
              <span
                key={skill}
                className="rounded-sm bg-[var(--color-tint-lavender)] px-2 py-[3px] text-[12.5px] font-medium text-[var(--color-brand-purple-800)]"
              >
                {skill}
              </span>
            ))}
            {extraSkills > 0 && (
              <span className="rounded-sm bg-[var(--color-surface)] px-2 py-[3px] text-[12.5px] font-medium text-[var(--color-steel)]">
                {t("card.more", { n: extraSkills })}
              </span>
            )}
          </div>
        )}

        <div className="mt-3.5 flex flex-wrap items-center gap-3.5 text-[13px] text-[var(--color-steel)]">
          <span className="inline-flex items-center gap-1.5">
            <Users className="size-3.5" /> {applicants}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <Calendar className="size-3.5" />{" "}
            {tn(postedDaysAgo, "card.posted.one", "card.posted.other")}
          </span>
          {deadlineDays != null && deadlineDays >= 0 && (
            <span className="inline-flex items-center gap-1.5 font-medium text-[var(--color-brand-orange-deep)]">
              <Clock className="size-3.5" />{" "}
              {tn(deadlineDays, "card.closes.one", "card.closes.other")}
            </span>
          )}
        </div>
      </div>

      {/* Actions: a full-width row below on mobile, a side column from sm up. */}
      {canApply && (
        <div className="col-span-2 flex items-center gap-3 sm:col-span-1 sm:col-start-3 sm:row-start-1 sm:flex-col sm:items-end sm:justify-between sm:gap-3.5">
          <SaveButton jobId={job.id} />
          <Button asChild className="flex-1 sm:flex-none">
            <Link to={`/jobs/${job.id}`}>{t("card.apply")}</Link>
          </Button>
        </div>
      )}
    </article>
  );
}

// Bookmark toggle: candidates toggle saved state; logged-out visitors go to login.
function SaveButton({ jobId }: { jobId: number }) {
  const { t } = useT();
  const { isAuthenticated, user } = useAuth();
  const isCandidate = isAuthenticated && user?.userType === "CANDIDATE";

  const { data: savedIds } = useSavedJobIds();
  const save = useSaveJob();
  const unsave = useUnsaveJob();

  const btnClass =
    "grid size-9 place-items-center rounded-md border border-[var(--color-hairline-strong)] text-[var(--color-steel)] hover:border-[var(--color-stone)] hover:text-[var(--color-charcoal)]";

  if (!isCandidate) {
    return (
      <Link to="/login" state={{ from: `/jobs/${jobId}` }} aria-label={t("card.save")} className={btnClass}>
        <Bookmark className="size-4" />
      </Link>
    );
  }

  const isSaved = savedIds?.includes(jobId) ?? false;
  const pending = save.isPending || unsave.isPending;

  return (
    <button
      type="button"
      aria-label={isSaved ? t("card.saved") : t("card.save")}
      disabled={pending}
      onClick={() => (isSaved ? unsave : save).mutate(jobId)}
      className={cn(btnClass, isSaved && "border-primary text-primary")}
    >
      <Bookmark className={cn("size-4", isSaved && "fill-current")} />
    </button>
  );
}
