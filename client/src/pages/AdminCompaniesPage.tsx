// Admin moderation list for every company account (/admin/companies). Filter by
// status, page through the results, and act on a row: PENDING companies can be
// approved or rejected, ACTIVE/PARTNER companies suspended, SUSPENDED ones
// reactivated. Each action opens the reason dialog and invalidates the admin lists
// on success, so the row's status and available actions update automatically.

import { useState } from "react";
import { CloudOff, RefreshCw } from "lucide-react";
import {
  useAdminCompanies,
  useApproveCompany,
  useRejectCompany,
  useSuspendCompany,
  useReactivateCompany,
} from "@/features/admin/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ReasonDialog } from "@/components/admin/ReasonDialog";
import { companyStatusOptions, fieldsOptions, companySizeOptions } from "@/types/enums";
import type { CompanyStatus } from "@/types/enums";
import type { AdminCompanyDTO, AdminCompanyFilters } from "@/types/admin";

// Badge colour per status. Labels come from companyStatusOptions (English for now,
// like the rest of the enum labels).
const STATUS_STYLES: Record<CompanyStatus, string> = {
  ACTIVE: "bg-[color-mix(in_srgb,var(--color-brand-green)_14%,#fff)] text-[var(--color-brand-green)]",
  PENDING: "bg-[var(--color-tint-sky)] text-[var(--color-link-blue-pressed)]",
  PARTNER: "bg-[var(--color-tint-lavender)] text-[var(--color-brand-purple-800)]",
  SUSPENDED: "bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]",
  REJECTED: "bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]",
};

export function AdminCompaniesPage() {
  const { t, tn } = useT();
  const [status, setStatus] = useState<CompanyStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const filters: AdminCompanyFilters = {
    status: status === "ALL" ? undefined : status,
    page,
  };
  const hasActiveFilters = status !== "ALL";

  const { data, isLoading, isError, refetch } = useAdminCompanies(filters);

  function clearFilters() {
    setStatus("ALL");
    setPage(0);
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("admin.companies.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("admin.companies.subtitle")}</p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        <Select
          value={status}
          onValueChange={(value) => {
            setStatus(value as CompanyStatus | "ALL");
            setPage(0);
          }}
        >
          <SelectTrigger className="min-w-40">
            <SelectValue placeholder={t("admin.filter.status")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t("admin.filter.allStatuses")}</SelectItem>
            {companyStatusOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            {t("admin.filter.clear")}
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-lg" />
          ))}
        </div>
      ) : isError || !data ? (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-12 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
          <Button className="mt-5" onClick={() => refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      ) : data.empty ? (
        <p className="rounded-lg border border-border bg-card px-5 py-8 text-center text-sm text-[var(--color-slate)]">
          {hasActiveFilters ? t("admin.companies.emptyFiltered") : t("admin.companies.empty")}
        </p>
      ) : (
        <>
          <p className="text-sm text-[var(--color-steel)]">
            {tn(data.totalElements, "admin.companies.count.one", "admin.companies.count.other")}
          </p>
          <div className="space-y-3">
            {data.content.map((company) => (
              <CompanyRow key={company.id} company={company} />
            ))}
          </div>

          {data.totalPages > 1 && (
            <nav className="flex items-center justify-center gap-3">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage(page - 1)}>
                {t("admin.pager.prev")}
              </Button>
              <span className="text-sm text-[var(--color-steel)]">
                {t("admin.pager.page", { n: data.number + 1, total: data.totalPages })}
              </span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage(page + 1)}>
                {t("admin.pager.next")}
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}

function CompanyRow({ company }: { company: AdminCompanyDTO }) {
  const field = fieldsOptions.find((o) => o.value === company.field)?.label ?? company.field;
  const size = company.size
    ? companySizeOptions.find((o) => o.value === company.size)?.label ?? company.size
    : null;
  const statusLabel =
    companyStatusOptions.find((o) => o.value === company.status)?.label ?? company.status;

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-5">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">{company.name}</h3>
          <Badge variant="outline">{field}</Badge>
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[company.status]}`}>
            {statusLabel}
          </span>
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-2.5 text-sm text-[var(--color-slate)]">
          <a className="hover:underline" href={`mailto:${company.email}`}>{company.email}</a>
          {size && <span>{size}</span>}
        </div>
      </div>

      <div className="flex shrink-0 gap-2">
        <CompanyRowActions company={company} />
      </div>
    </article>
  );
}

// Which lifecycle actions a row offers depends on the company's current status. All
// four hooks are created unconditionally (rules of hooks); only the relevant
// dialogs render.
function CompanyRowActions({ company }: { company: AdminCompanyDTO }) {
  const { t } = useT();
  const approve = useApproveCompany();
  const reject = useRejectCompany();
  const suspend = useSuspendCompany();
  const reactivate = useReactivateCompany();

  if (company.status === "PENDING") {
    return (
      <>
        <ReasonDialog
          id={company.id}
          action={approve}
          triggerLabel={t("admin.approve")}
          title={t("admin.approveCompany")}
          confirmLabel={t("admin.approve")}
        />
        <ReasonDialog
          id={company.id}
          action={reject}
          triggerLabel={t("admin.reject")}
          triggerVariant="destructive"
          title={t("admin.rejectCompany")}
          confirmLabel={t("admin.reject")}
          confirmVariant="destructive"
        />
      </>
    );
  }

  if (company.status === "ACTIVE" || company.status === "PARTNER") {
    return (
      <ReasonDialog
        id={company.id}
        action={suspend}
        triggerLabel={t("admin.suspend")}
        triggerVariant="destructive"
        title={t("admin.suspendCompany")}
        confirmLabel={t("admin.suspend")}
        confirmVariant="destructive"
      />
    );
  }

  if (company.status === "SUSPENDED") {
    return (
      <ReasonDialog
        id={company.id}
        action={reactivate}
        triggerLabel={t("admin.reactivate")}
        title={t("admin.reactivateCompany")}
        confirmLabel={t("admin.reactivate")}
      />
    );
  }

  return null;
}
