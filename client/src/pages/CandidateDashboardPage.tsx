// Candidate's dashboard: a quick overview of their job search plus shortcuts to the
// main candidate screens. Reached via /dashboard when a CANDIDATE is logged in
// (DashboardPage dispatches by user type). Stats are computed client-side from the
// candidate's own applications.

import { Link } from "react-router-dom";
import { Briefcase, FileText, UserPen } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useMyApplications } from "@/features/jobApplications/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { applicationStatusOptions } from "@/types/enums";
import type { ApplicationStatus } from "@/types/enums";
import type { JobApplicationDTO } from "@/types/jobApplication";

// An application is "in progress" until it reaches a terminal state.
const TERMINAL: ApplicationStatus[] = ["ACCEPTED", "REJECTED", "WITHDRAWN"];

function statusVariant(status: ApplicationStatus): "default" | "secondary" | "destructive" | "outline" {
  if (status === "ACCEPTED") return "default";
  if (status === "REJECTED") return "destructive";
  if (status === "WITHDRAWN") return "outline";
  return "secondary";
}

export function CandidateDashboardPage() {
  const { t } = useT();
  const { user } = useAuth();
  // size 100 covers the stats in one shot; add a backend summary endpoint
  // if a candidate ever applies to more than that.
  const apps = useMyApplications({ size: 100 });

  const list = apps.data?.content ?? [];
  const total = apps.data?.totalElements ?? 0;
  const active = list.filter((a) => !TERMINAL.includes(a.applicationStatus)).length;
  const accepted = list.filter((a) => a.applicationStatus === "ACCEPTED").length;
  const recent = list.slice(0, 5);

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">
          {t("dash.welcome", { name: user?.firstName ?? "" })}
        </h1>
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label={t("dash.stat.applications")} value={apps.isError ? "—" : total}
          loading={apps.isLoading} />
        <StatCard label={t("dash.stat.active")} value={apps.isError ? "—" : active}
          loading={apps.isLoading} />
        <StatCard label={t("dash.stat.accepted")} value={apps.isError ? "—" : accepted}
          loading={apps.isLoading} />
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

        {apps.isLoading ? (
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
