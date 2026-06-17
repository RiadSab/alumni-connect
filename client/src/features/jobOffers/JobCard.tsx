// One job offer, rendered to match the Job Board design. Presentational only —
// it takes a JobOfferDTO and shows it. Apply/save are wired in a later phase.

import { Bookmark, Calendar, Clock, MapPin, Users } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { JobOfferDTO } from "@/types/jobOffer";

// Logo background palette from the design, picked by company id.
const LOGO_COLORS = ["#5645d4", "#dd5b00", "#2a9d99", "#0075de", "#a02e6d", "#1aae39"];

function logoColor(companyId: number): string {
  return LOGO_COLORS[companyId % LOGO_COLORS.length];
}

function formatMoney(value: number): string {
  return value.toLocaleString("en-US");
}

// Whole days between now and an ISO date-time. Positive = in the future.
function daysFromNow(iso: string): number {
  const diff = new Date(iso).getTime() - Date.now();
  return Math.round(diff / 86_400_000);
}

function humanizeType(value: string): string {
  return value.replace(/_/g, " ");
}

export function JobCard({ job }: { job: JobOfferDTO }) {
  const shownSkills = job.skillsRequired.slice(0, 4);
  const extraSkills = job.skillsRequired.length - shownSkills.length;

  const postedDaysAgo = Math.max(0, -daysFromNow(job.createdAt));
  const deadlineDays = job.applicationDeadline ? daysFromNow(job.applicationDeadline) : null;

  const applicants =
    job.maxApplications != null
      ? `${job.currentApplicationCount} / ${job.maxApplications} applicants`
      : `${job.currentApplicationCount} applicants`;

  return (
    <article className="grid grid-cols-[48px_1fr_auto] gap-4 rounded-lg border border-border bg-card p-5 transition-shadow hover:shadow-md">
      {/* Logo */}
      <div
        className="grid size-12 place-items-center rounded-md text-lg font-bold text-white"
        style={{ background: logoColor(job.companyId) }}
      >
        {job.companyName.charAt(0)}
      </div>

      {/* Main */}
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-lg font-semibold leading-tight text-foreground">{job.title}</h3>
          {job.isRemote && (
            <span className="rounded-full bg-[var(--color-tint-sky)] px-2 py-0.5 text-xs font-semibold text-[var(--color-link-blue-pressed)]">
              Remote
            </span>
          )}
          {job.isUrgent && (
            <span className="rounded-full bg-[var(--color-tint-peach)] px-2 py-0.5 text-xs font-semibold text-[var(--color-brand-orange-deep)]">
              Urgent
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
            <span className="text-[13px] font-normal text-[var(--color-steel)]">/ month</span>
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
                +{extraSkills} more
              </span>
            )}
          </div>
        )}

        <div className="mt-3.5 flex flex-wrap items-center gap-3.5 text-[13px] text-[var(--color-steel)]">
          <span className="inline-flex items-center gap-1.5">
            <Users className="size-3.5" /> {applicants}
          </span>
          <span className="inline-flex items-center gap-1.5">
            <Calendar className="size-3.5" /> Posted {postedDaysAgo} {postedDaysAgo === 1 ? "day" : "days"} ago
          </span>
          {deadlineDays != null && deadlineDays >= 0 && (
            <span className="inline-flex items-center gap-1.5 font-medium text-[var(--color-brand-orange-deep)]">
              <Clock className="size-3.5" /> Closes in {deadlineDays} {deadlineDays === 1 ? "day" : "days"}
            </span>
          )}
        </div>
      </div>

      {/* Side actions */}
      <div className="flex flex-col items-end justify-between gap-3.5">
        <button
          type="button"
          aria-label="Save job"
          className="grid size-9 place-items-center rounded-md border border-[var(--color-hairline-strong)] text-[var(--color-steel)] hover:border-[var(--color-stone)] hover:text-[var(--color-charcoal)]"
        >
          <Bookmark className="size-4" />
        </button>
        <Button>Apply</Button>
      </div>
    </article>
  );
}
