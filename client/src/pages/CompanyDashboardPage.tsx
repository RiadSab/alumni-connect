// Company user's dashboard: overview of the company's postings plus shortcuts.
// Reached via /dashboard for a COMPANY_USER. Stats come from /job-offers/me/stats
// (OWNER/RECRUITER); a MEMBER 403s there, so the stats fall back to dashes.

import { Link } from "react-router-dom";
import { Briefcase, FilePlus2, Settings, Users, UsersRound } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useMyCompanyUserProfile } from "@/features/companyUsers/hooks";
import { useMyCompanyStats, useMyJobOffers } from "@/features/jobOffers/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { jobStatusOptions } from "@/types/enums";
import type { JobStatus } from "@/types/enums";
import type { JobOfferDTO } from "@/types/jobOffer";

function statusVariant(status: JobStatus): "default" | "secondary" | "destructive" | "outline" {
  if (status === "OPEN") return "default";
  if (status === "CLOSED") return "secondary";
  if (status === "EXPIRED") return "destructive";
  return "outline";
}

export function CompanyDashboardPage() {
  const { t } = useT();
  const { user } = useAuth();
  const me = useMyCompanyUserProfile();
  const stats = useMyCompanyStats();
  const offers = useMyJobOffers({ size: 5 });
  const recent = offers.data?.content ?? [];

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">
          {t("dash.welcome", { name: user?.firstName ?? "" })}
        </h1>
        {me.data && <p className="mt-1 text-sm text-[var(--color-slate)]">{me.data.companyName}</p>}
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <StatCard label={t("dash.stat.postings")} value={stats.isError ? "—" : stats.data?.totalPostings ?? 0}
          loading={stats.isLoading} />
        <StatCard label={t("dash.stat.open")} value={stats.isError ? "—" : stats.data?.openPostings ?? 0}
          loading={stats.isLoading} />
        <StatCard label={t("dash.stat.applicants")} value={stats.isError ? "—" : stats.data?.totalApplicants ?? 0}
          loading={stats.isLoading} />
      </div>

      {/* Quick actions */}
      <div className="flex flex-wrap gap-3">
        <Button asChild>
          <Link to="/company/jobs/new">
            <FilePlus2 className="size-4" /> {t("myJobs.post")}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/company/jobs">
            <Briefcase className="size-4" /> {t("dash.managePostings")}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/company/team">
            <UsersRound className="size-4" /> {t("dash.team")}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/profile">
            <Settings className="size-4" /> {t("dash.companyProfile")}
          </Link>
        </Button>
      </div>

      {/* Recent postings */}
      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
            {t("dash.recent")}
          </h2>
          {recent.length > 0 && (
            <Link to="/company/jobs" className="text-sm font-medium text-[var(--color-link-blue)] hover:underline">
              {t("dash.viewAll")}
            </Link>
          )}
        </div>

        {offers.isLoading ? (
          <Skeleton className="h-20 w-full rounded-lg" />
        ) : recent.length === 0 ? (
          <p className="text-sm text-[var(--color-slate)]">{t("dash.empty")}</p>
        ) : (
          <div className="space-y-3">
            {recent.map((offer) => (
              <RecentRow key={offer.id} offer={offer} />
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

function RecentRow({ offer }: { offer: JobOfferDTO }) {
  const { tn } = useT();
  const status = jobStatusOptions.find((o) => o.value === offer.status)?.label ?? offer.status;

  return (
    <article className="flex items-center justify-between gap-4 rounded-lg border border-border bg-card p-4">
      <div className="flex min-w-0 items-center gap-2">
        <Link to={`/company/jobs/${offer.id}/edit`} className="truncate text-sm font-semibold text-foreground hover:text-primary">
          {offer.title}
        </Link>
        <Badge variant={statusVariant(offer.status)}>{status}</Badge>
      </div>
      <Link
        to={`/company/jobs/${offer.id}/applications`}
        className="inline-flex shrink-0 items-center gap-1.5 text-sm text-[var(--color-slate)] hover:text-foreground hover:underline"
      >
        <Users className="size-3.5" />
        {tn(offer.currentApplicationCount, "card.applicants.one", "card.applicants.other")}
      </Link>
    </article>
  );
}
