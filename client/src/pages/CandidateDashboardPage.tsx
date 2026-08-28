// Candidate's dashboard: a compact job-search overview. Reached via /dashboard for a
// CANDIDATE. Deliberately uses its own condensed shapes (small stat tiles, tight list
// rows in side-by-side panels) rather than the full board cards, so it reads as a
// glanceable summary, not another list page.

import { Link } from "react-router-dom";
import { Bookmark, Briefcase, CalendarClock, CheckCircle2, FileUp, UserPen } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useMyApplications, useMyApplicationStats } from "@/features/jobApplications/hooks";
import { useRecommendedJobOffers } from "@/features/jobOffers/hooks";
import { useMyCandidateProfile } from "@/features/candidates/hooks";
import { CompanyLogo } from "@/features/jobOffers/CompanyLogo";
import { useT } from "@/features/i18n/lang-context";
import {
  useMarkNotificationRead,
  useUnreadNotifications,
} from "@/features/notifications/hooks";
import { notificationTextKeys } from "@/features/notifications/labels";
import type { NotificationDTO } from "@/types/notification";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { applicationStatusOptions } from "@/types/enums";
import type { ApplicationStatus } from "@/types/enums";
import type { JobApplicationDTO } from "@/types/jobApplication";
import type { JobOfferDTO } from "@/types/jobOffer";

// Tight status dot colours for the recent list (full badges belong on the list page).
const STATUS_DOT: Record<ApplicationStatus, string> = {
  APPLIED: "bg-[var(--color-stone)]",
  UNDER_REVIEW: "bg-[var(--color-link-blue)]",
  SCHEDULED_INTERVIEW: "bg-[var(--color-brand-purple-800)]",
  INTERVIEWED: "bg-[var(--color-brand-purple-800)]",
  ACCEPTED: "bg-[var(--color-brand-green)]",
  REJECTED: "bg-[var(--color-error)]",
  WITHDRAWN: "bg-[var(--color-stone)]",
};

function statusLabel(status: ApplicationStatus): string {
  return applicationStatusOptions.find((o) => o.value === status)?.label ?? status;
}

export function CandidateDashboardPage() {
  const { t } = useT();
  const { user } = useAuth();
  const stats = useMyApplicationStats();
  const recentQuery = useMyApplications({ size: 5 });
  const recent = recentQuery.data?.content ?? [];
  // Unread notifications only: opening one marks it read, which is what takes it off this page.
  const unread = useUnreadNotifications();
  const recommendedQuery = useRecommendedJobOffers({ size: 5 });
  const recommended = recommendedQuery.data?.content ?? [];
  const profile = useMyCandidateProfile();
  const needsResume = !!profile.data && !profile.data.resumeId;

  return (
    <div className="mx-auto max-w-5xl space-y-5">
      <h1 className="text-2xl font-semibold text-foreground">
        {t("dash.welcome", { name: user?.firstName ?? "" })}
      </h1>

      {(unread.data ?? []).slice(0, 3).map((notification) => (
        <NotificationBanner key={notification.id} notification={notification} />
      ))}
      {needsResume && <ResumeNudge />}

      {/* Compact stat tiles + inline shortcuts. */}
      <div className="grid gap-3 sm:grid-cols-3">
        <StatTile label={t("dash.stat.applications")} value={stats.isError ? "—" : stats.data?.total ?? 0}
          loading={stats.isLoading} />
        <StatTile label={t("dash.stat.active")} value={stats.isError ? "—" : stats.data?.active ?? 0}
          loading={stats.isLoading} />
        <StatTile label={t("dash.stat.accepted")} value={stats.isError ? "—" : stats.data?.accepted ?? 0}
          loading={stats.isLoading} />
      </div>

      <div className="flex flex-wrap gap-2">
        <Button size="sm" asChild>
          <Link to="/"><Briefcase className="size-4" /> {t("dash.browseJobs")}</Link>
        </Button>
        <Button size="sm" variant="outline" asChild>
          <Link to="/saved"><Bookmark className="size-4" /> {t("dash.savedJobs")}</Link>
        </Button>
        <Button size="sm" variant="outline" asChild>
          <Link to="/profile/edit"><UserPen className="size-4" /> {t("dash.editProfile")}</Link>
        </Button>
      </div>

      {/* Two short columns: recommended on the left, recent activity on the right. */}
      <div className="grid gap-5 lg:grid-cols-2">
        <Panel title={t("dash.recommended")}>
          {recommendedQuery.isLoading ? (
            <RowsSkeleton />
          ) : recommended.length === 0 ? (
            <EmptyRow text={t("dash.recommendedEmpty")} />
          ) : (
            <ul className="divide-y divide-border">
              {recommended.map((job) => (
                <RecommendedRow key={job.id} job={job} />
              ))}
            </ul>
          )}
        </Panel>

        <Panel
          title={t("dash.recentApplications")}
          action={recent.length > 0 ? { to: "/applications", label: t("dash.viewAll") } : undefined}
        >
          {recentQuery.isLoading ? (
            <RowsSkeleton />
          ) : recent.length === 0 ? (
            <EmptyRow text={t("dash.noApplications")} />
          ) : (
            <ul className="divide-y divide-border">
              {recent.map((application) => (
                <RecentRow key={application.id} application={application} />
              ))}
            </ul>
          )}
        </Panel>
      </div>
    </div>
  );
}

// A glanceable card with a small header; the body is a tight, divided list.
function Panel({
  title,
  action,
  children,
}: {
  title: string;
  action?: { to: string; label: string };
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-lg border border-border bg-card">
      <div className="flex items-center justify-between px-4 py-3">
        <h2 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-stone)]">{title}</h2>
        {action && (
          <Link to={action.to} className="text-xs font-medium text-[var(--color-link-blue)] hover:underline">
            {action.label}
          </Link>
        )}
      </div>
      <div className="border-t border-border px-4">{children}</div>
    </section>
  );
}

function RecommendedRow({ job }: { job: JobOfferDTO }) {
  return (
    <li>
      <Link to={`/jobs/${job.id}`} className="flex items-center gap-3 py-3 group">
        <CompanyLogo
          companyId={job.companyId}
          companyName={job.companyName}
          logoId={job.logoId}
          className="size-9 shrink-0 rounded-md"
          textClassName="text-sm"
        />
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-medium text-foreground group-hover:text-primary">
            {job.title}
          </div>
          <div className="truncate text-xs text-[var(--color-steel)]">
            {job.companyName}
            {job.city && ` · ${job.city}`}
          </div>
        </div>
      </Link>
    </li>
  );
}

function RecentRow({ application }: { application: JobApplicationDTO }) {
  return (
    <li>
      <Link to={`/applications/${application.id}`} className="flex items-center gap-3 py-3 group">
        <span className={`size-2 shrink-0 rounded-full ${STATUS_DOT[application.applicationStatus]}`} />
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-medium text-foreground group-hover:text-primary">
            {application.jobOfferTitle}
          </div>
          <div className="truncate text-xs text-[var(--color-steel)]">{application.companyName}</div>
        </div>
        <span className="shrink-0 text-xs text-[var(--color-steel)]">
          {statusLabel(application.applicationStatus)}
        </span>
      </Link>
    </li>
  );
}

// One unread notification, dismissed by opening it.
function NotificationBanner({ notification }: { notification: NotificationDTO }) {
  const { t } = useT();
  const markRead = useMarkNotificationRead();
  const accepted = notification.type === "APPLICATION_ACCEPTED";
  const tint = accepted
    ? "border-[var(--color-brand-green)] bg-[color-mix(in_srgb,var(--color-brand-green)_8%,#fff)]"
    : "border-[var(--color-brand-purple-800)] bg-[var(--color-tint-lavender)]";
  const iconColor = accepted ? "text-[var(--color-brand-green)]" : "text-[var(--color-brand-purple-800)]";
  const Icon = accepted ? CheckCircle2 : CalendarClock;

  return (
    <Link
      to={notification.link ?? "/notifications"}
      onClick={() => markRead.mutate(notification.id)}
      className={`flex items-center gap-3 rounded-lg border px-4 py-3 transition-shadow hover:shadow-sm ${tint}`}
    >
      <Icon className={`size-5 shrink-0 ${iconColor}`} />
      <span className="min-w-0 flex-1 text-sm font-semibold text-foreground">
        {t(notificationTextKeys[notification.type], {
          subject: notification.subject ?? "",
          context: notification.context ?? "",
        })}
      </span>
      <span className="shrink-0 text-xs font-medium text-[var(--color-link-blue)]">
        {t("dash.viewAll")}
      </span>
    </Link>
  );
}

// Shown until the candidate has a résumé on file — applying is much faster with one.
function ResumeNudge() {
  const { t } = useT();
  return (
    <div className="flex items-center gap-3 rounded-lg border border-[var(--color-brand-orange-deep)] bg-[var(--color-tint-peach)] px-4 py-3">
      <FileUp className="size-5 shrink-0 text-[var(--color-brand-orange-deep)]" />
      <div className="min-w-0 flex-1">
        <span className="text-sm font-semibold text-foreground">{t("dash.resumeNudge.title")}</span>
        <span className="ml-2 text-sm text-[var(--color-slate)]">{t("dash.resumeNudge.body")}</span>
      </div>
      <Button variant="outline" size="sm" asChild className="shrink-0">
        <Link to="/profile/edit">{t("dash.resumeNudge.cta")}</Link>
      </Button>
    </div>
  );
}

function StatTile({ label, value, loading }: { label: string; value: number | string; loading: boolean }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-border bg-card px-4 py-3">
      <span className="text-xs font-medium text-[var(--color-steel)]">{label}</span>
      {loading ? (
        <Skeleton className="h-6 w-8 rounded" />
      ) : (
        <span className="text-xl font-semibold text-foreground">{value}</span>
      )}
    </div>
  );
}

function EmptyRow({ text }: { text: string }) {
  return <p className="py-6 text-center text-sm text-[var(--color-slate)]">{text}</p>;
}

function RowsSkeleton() {
  return (
    <div className="divide-y divide-border">
      {Array.from({ length: 3 }).map((_, i) => (
        <div key={i} className="flex items-center gap-3 py-3">
          <Skeleton className="size-9 shrink-0 rounded-md" />
          <div className="flex-1 space-y-1.5">
            <Skeleton className="h-3.5 w-2/3 rounded" />
            <Skeleton className="h-3 w-1/3 rounded" />
          </div>
        </div>
      ))}
    </div>
  );
}
