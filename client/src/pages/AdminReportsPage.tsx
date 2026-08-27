// Employment report per promotion (/admin/reports). The response rate is shown first and next to
// every other figure: an unclaimed graduate is unknown, not unemployed.

import { useState } from "react";
import { CloudOff, RefreshCw } from "lucide-react";
import { useEmploymentReport, usePromotionYears } from "@/features/reports/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { EmploymentReportDTO } from "@/types/report";

function percent(value: number | null): string {
  return value === null ? "—" : `${Math.round(value * 100)}%`;
}

export function AdminReportsPage() {
  const { t } = useT();
  const promotions = usePromotionYears();
  const [selected, setSelected] = useState<number | null>(null);
  // Derived rather than synced into state: until one is picked, show the most recent promotion.
  const year = selected ?? promotions.data?.[0] ?? null;

  const report = useEmploymentReport(year);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("reports.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("reports.subtitle")}</p>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Select
          value={year === null ? "" : String(year)}
          onValueChange={(value) => setSelected(Number(value))}
        >
          <SelectTrigger className="min-w-40">
            <SelectValue placeholder={t("reports.promotion")} />
          </SelectTrigger>
          <SelectContent>
            {(promotions.data ?? []).map((promotion) => (
              <SelectItem key={promotion} value={String(promotion)}>
                {promotion}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {promotions.data?.length === 0 ? (
        <p className="rounded-lg border border-border bg-card px-5 py-8 text-center text-sm text-[var(--color-slate)]">
          {t("reports.noRoster")}
        </p>
      ) : report.isLoading || year === null ? (
        <div className="space-y-3">
          <Skeleton className="h-24 w-full rounded-lg" />
          <Skeleton className="h-40 w-full rounded-lg" />
        </div>
      ) : report.isError || !report.data ? (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-12 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <Button className="mt-5" onClick={() => report.refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      ) : (
        <Report report={report.data} />
      )}
    </div>
  );
}

function Report({ report }: { report: EmploymentReportDTO }) {
  const { t } = useT();

  return (
    <div className="space-y-5">
      {/* The coverage line comes first: every figure below is only as good as this one. */}
      <section className="rounded-lg border border-border bg-[var(--color-tint-sky)] p-5">
        <p className="text-sm font-semibold text-foreground">
          {t("reports.coverage", {
            responded: report.responded,
            total: report.totalGraduates,
            rate: percent(report.responseRate),
          })}
        </p>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("reports.coverageNote")}</p>
      </section>

      <div className="grid gap-3 sm:grid-cols-3">
        <Tile
          label={t("reports.employmentRate")}
          value={percent(report.employmentRate)}
          note={t("reports.ofResponded", { n: report.responded })}
        />
        <Tile
          label={t("reports.medianTime")}
          value={
            report.medianMonthsToFirstJob === null
              ? "—"
              : t("reports.months", { n: report.medianMonthsToFirstJob })
          }
          note={t("reports.sinceGraduation")}
        />
        <Tile
          label={t("reports.claimed")}
          value={`${report.claimed}/${report.totalGraduates}`}
          note={t("reports.claimedNote")}
        />
      </div>

      <section className="rounded-lg border border-border bg-card p-5">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
          {t("reports.breakdown")}
        </h2>
        <dl className="mt-3 grid gap-2 sm:grid-cols-2">
          <Line label={t("reports.employed")} value={report.employed} />
          <Line label={t("reports.studying")} value={report.studying} />
          <Line label={t("reports.seeking")} value={report.seeking} />
          <Line label={t("reports.unknownNow")} value={report.noCurrentPeriod} />
          <Line label={t("reports.silent")} value={report.totalGraduates - report.responded} />
        </dl>
      </section>

      <section className="rounded-lg border border-border bg-card p-5">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
          {t("reports.topEmployers")}
        </h2>
        {report.topEmployers.length === 0 ? (
          <p className="mt-3 text-sm text-[var(--color-slate)]">{t("reports.noEmployers")}</p>
        ) : (
          <ul className="mt-3 divide-y divide-border">
            {report.topEmployers.map((employer) => (
              <li key={employer.employer} className="flex items-center justify-between py-2">
                <span className="text-sm font-medium text-[var(--color-charcoal)]">
                  {employer.employer}
                </span>
                <span className="text-sm text-[var(--color-steel)]">{employer.count}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function Tile({ label, value, note }: { label: string; value: string; note: string }) {
  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <div className="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {label}
      </div>
      <div className="mt-1 text-2xl font-semibold text-foreground">{value}</div>
      <div className="mt-0.5 text-xs text-[var(--color-slate)]">{note}</div>
    </div>
  );
}

function Line({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center justify-between gap-4">
      <dt className="text-sm text-[var(--color-steel)]">{label}</dt>
      <dd className="text-sm font-medium text-[var(--color-charcoal)]">{value}</dd>
    </div>
  );
}
