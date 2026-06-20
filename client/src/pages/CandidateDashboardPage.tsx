// Candidate's dashboard: job-search overview plus shortcuts. Reached via /dashboard
// for a CANDIDATE. Counts come from the stats endpoint; the recent list is a small
// page of the latest applications.

import { Link } from "react-router-dom";
import { Briefcase, FileText, UserPen } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useMyApplications, useMyApplicationStats } from "@/features/jobApplications/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { applicationStatusOptions } from "@/types/enums";
import type { ApplicationStatus } from "@/types/enums";
import type { JobApplicationDTO } from "@/types/jobApplication";

function statusVariant(status: ApplicationStatus): "default" | "secondary" | "destructive" | "outline" {
  if (status === "ACCEPTED") return "default";
  if (status === "REJECTED") return "destructive";
  if (status === "WITHDRAWN") return "outline";
  return "secondary";
}

export function CandidateDashboardPage() {
  const { t } = useT();
  const { user } = useAuth();
  const stats = useMyApplicationStats();
  // Just the latest few for the recent list; the counts come from stats above.
  const recentQuery = useMyApplications({ size: 5 });
  const recent = recentQuery.data?.content ?? [];

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">
          {t("dash.welcome", { name: user?.firstName ?? "" })}
        </h1>
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label={t("dash.stat.applications")} value={stats.isError ? "—" : stats.data?.total ?? 0}
          loading={stats.isLoading} />
        <StatCard label={t("dash.stat.active")} value={stats.isError ? "—" : stats.data?.active ?? 0}
          loading={stats.isLoading} />
        <StatCard label={t("dash.stat.accepted")} value={stats.isError ? "—" : stats.data?.accepted ?? 0}
          loading={stats.isLoading} />
      </div>

      {/* Quick actions */}
      <div className="flex flex-wrap gap-3">
        <Button asChild>
          <Link to="/">
            <Briefcase className="size-4" /> {t("dash.browseJobs")}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/applications">
            <FileText className="size-4" /> {t("nav.myApplications")}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/profile/edit">
            <UserPen className="size-4" /> {t("dash.editProfile")}
          </Link>
        </Button>
      </div>

      {/* Recent applications */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
            {t("dash.recentApplications")}
          </h2>
          {recent.length > 0 && (
            <Link to="/applications" className="text-sm font-medium text-[var(--color-link-blue)] hover:underline">
              {t("dash.viewAll")}
            </Link>
          )}
        </div>

        {recentQuery.isLoading ? (
          <Skeleton className="h-20 w-full rounded-lg" />
        ) : recent.length === 0 ? (
          <p className="text-sm text-[var(--color-slate)]">{t("dash.noApplications")}</p>
        ) : (
          <div className="space-y-3">
            {recent.map((application) => (
              <RecentRow key={application.id} application={application} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function StatCard({ label, value, loading }: { label: string; value: number | string; loading: boolean }) {
  return (
    <div className="rounded-lg border border-border bg-card p-5">
      <div className="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {label}
      </div>
      {loading ? (
        <Skeleton className="mt-2 h-8 w-12 rounded" />
      ) : (
        <div className="mt-1 text-3xl font-semibold text-foreground">{value}</div>
      )}
    </div>
  );
}

function RecentRow({ application }: { application: JobApplicationDTO }) {
  const status =
    applicationStatusOptions.find((o) => o.value === application.applicationStatus)?.label ??
    application.applicationStatus;

  return (
    <article className="flex items-center justify-between gap-4 rounded-lg border border-border bg-card p-4">
      <Link
        to={`/jobs/${application.jobOfferId}`}
        className="truncate text-sm font-semibold text-foreground hover:text-primary"
      >
        {application.jobOfferTitle}
      </Link>
      <Badge variant={statusVariant(application.applicationStatus)}>{status}</Badge>
    </article>
  );
}
