// Administrator's dashboard: the count of accounts awaiting approval plus shortcuts
// to the moderation screens. Reached via /dashboard when an ADMINISTRATOR is logged
// in (DashboardPage dispatches by user type). Counts come from the two "pending"
// endpoints; only their totals are needed here, so we request a single row.

import { Link } from "react-router-dom";
import { Building2, ClipboardCheck, Users } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { usePendingUsers, usePendingCompanies } from "@/features/admin/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

export function AdminDashboardPage() {
  const { t } = useT();
  const { user } = useAuth();
  // Only the totals matter on this screen, so ask for one row and read totalElements.
  const pendingUsers = usePendingUsers({ size: 1 });
  const pendingCompanies = usePendingCompanies({ size: 1 });

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">
          {t("dash.welcome", { name: user?.firstName ?? "" })}
        </h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("admin.dash.subtitle")}</p>
      </div>

      {/* Pending counts */}
      <div className="grid gap-4 sm:grid-cols-2">
        <StatCard
          label={t("admin.dash.pendingUsers")}
          value={pendingUsers.isError ? "—" : pendingUsers.data?.totalElements ?? 0}
          loading={pendingUsers.isLoading}
        />
        <StatCard
          label={t("admin.dash.pendingCompanies")}
          value={pendingCompanies.isError ? "—" : pendingCompanies.data?.totalElements ?? 0}
          loading={pendingCompanies.isLoading}
        />
      </div>

      {/* Moderation shortcuts */}
      <div className="flex flex-wrap gap-3">
        <Button asChild>
          <Link to="/admin/pending">
            <ClipboardCheck className="size-4" /> {t("admin.dash.reviewApprovals")}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/admin/users">
            <Users className="size-4" /> {t("admin.dash.allUsers")}
          </Link>
        </Button>
        <Button variant="outline" asChild>
          <Link to="/admin/companies">
            <Building2 className="size-4" /> {t("admin.dash.allCompanies")}
          </Link>
        </Button>
      </div>
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
