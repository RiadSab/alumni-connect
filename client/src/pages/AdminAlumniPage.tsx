// Alumni roster (/admin/alumni): import the school's CSV of graduates and browse the result.

import { useState } from "react";
import { CloudOff, RefreshCw, Upload } from "lucide-react";
import { useAlumniRecords, useImportRoster } from "@/features/alumni/hooks";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { fieldsOptions } from "@/types/enums";
import type { AlumniRecordDTO, AlumniRecordFilters } from "@/types/alumni";

const CSV_COLUMNS = "student_id, first_name, last_name, field_of_study, promotion_year, email";

export function AdminAlumniPage() {
  const { t, tn } = useT();
  const [promotion, setPromotion] = useState("");
  const [page, setPage] = useState(0);

  const promotionYear = /^\d{4}$/.test(promotion) ? Number(promotion) : undefined;
  const filters: AlumniRecordFilters = { promotionYear, page };
  const { data, isLoading, isError, refetch } = useAlumniRecords(filters);

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("admin.alumni.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("admin.alumni.subtitle")}</p>
      </div>

      <ImportCard />

      <div className="flex flex-wrap items-center gap-3">
        <Input
          className="max-w-40"
          inputMode="numeric"
          placeholder={t("admin.alumni.promotionFilter")}
          value={promotion}
          onChange={(event) => {
            setPromotion(event.target.value);
            setPage(0);
          }}
        />
        {promotion !== "" && (
          <Button variant="ghost" size="sm" onClick={() => { setPromotion(""); setPage(0); }}>
            {t("admin.filter.clear")}
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full rounded-lg" />
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
          {t("admin.alumni.empty")}
        </p>
      ) : (
        <>
          <p className="text-sm text-[var(--color-steel)]">
            {tn(data.totalElements, "admin.alumni.count.one", "admin.alumni.count.other")}
          </p>
          <div className="space-y-3">
            {data.content.map((record) => (
              <RecordRow key={record.id} record={record} />
            ))}
          </div>

          {data.totalPages > 1 && (
            <nav className="flex items-center justify-center gap-3">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage(page - 1)}>
                {t("pager.prev")}
              </Button>
              <span className="text-sm text-[var(--color-steel)]">
                {t("pager.page", { n: data.number + 1, total: data.totalPages })}
              </span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage(page + 1)}>
                {t("pager.next")}
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}

function ImportCard() {
  const { t } = useT();
  const importRoster = useImportRoster();
  const [file, setFile] = useState<File | null>(null);
  const [dryRun, setDryRun] = useState(true);
  const [error, setError] = useState<string | null>(null);

  function submit() {
    if (file === null) return;
    setError(null);
    importRoster.mutate(
      { file, dryRun },
      { onError: (e) => setError(isApiError(e) ? e.message : t("admin.alumni.importError")) },
    );
  }

  const result = importRoster.data;

  return (
    <section className="rounded-lg border border-border bg-card p-5">
      <h2 className="text-sm font-semibold text-foreground">{t("admin.alumni.importTitle")}</h2>
      <p className="mt-1 text-sm text-[var(--color-slate)]">
        {t("admin.alumni.columns")}: <code className="text-[13px]">{CSV_COLUMNS}</code>
      </p>

      <div className="mt-4 flex flex-wrap items-center gap-3">
        <input
          type="file"
          accept=".csv,text/csv"
          onChange={(event) => {
            setFile(event.target.files?.[0] ?? null);
            importRoster.reset();
          }}
          className="text-sm file:mr-3 file:rounded-md file:border file:border-input file:bg-transparent file:px-3 file:py-1.5 file:text-sm file:font-medium"
        />
        <Button onClick={submit} disabled={file === null || importRoster.isPending}>
          <Upload className="size-4" />
          {importRoster.isPending ? t("admin.alumni.importing") : t("admin.alumni.import")}
        </Button>
      </div>

      <label className="mt-3 flex items-center gap-2 text-sm text-[var(--color-charcoal)]">
        <input type="checkbox" checked={dryRun} onChange={(event) => setDryRun(event.target.checked)} />
        {t("admin.alumni.dryRun")}
      </label>

      {error && <p className="mt-3 text-sm text-destructive">{error}</p>}

      {result && (
        <div className="mt-4 rounded-md border border-border bg-[var(--color-surface)] p-4">
          <p className="text-sm font-medium text-foreground">
            {t("admin.alumni.created", { n: result.created })} ·{" "}
            {t("admin.alumni.updated", { n: result.updated })}
          </p>
          {result.dryRun && (
            <p className="mt-1 text-sm text-[var(--color-slate)]">{t("admin.alumni.dryRunNotice")}</p>
          )}
          {result.errors.length > 0 && (
            <div className="mt-3">
              <p className="text-sm font-medium text-destructive">{t("admin.alumni.errorsTitle")}</p>
              <ul className="mt-1.5 max-h-48 space-y-1 overflow-y-auto text-sm text-[var(--color-charcoal)]">
                {result.errors.map((rowError) => (
                  <li key={rowError.line}>
                    {t("admin.alumni.line", { n: rowError.line })}: {rowError.message}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </section>
  );
}

function RecordRow({ record }: { record: AlumniRecordDTO }) {
  const { t } = useT();
  const field =
    fieldsOptions.find((o) => o.value === record.fieldOfStudy)?.label ?? record.fieldOfStudy;

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-4">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">
            {record.firstName} {record.lastName}
          </h3>
          <Badge variant="outline">{record.promotionYear}</Badge>
        </div>
        <p className="mt-1 text-sm text-[var(--color-slate)]">
          {field} · {record.studentId}
        </p>
        <p className="mt-0.5 text-sm text-[var(--color-steel)]">
          {record.email ?? t("admin.alumni.noEmail")}
        </p>
      </div>
      <Badge variant={record.claimed ? "default" : "secondary"} className="shrink-0">
        {record.claimed ? t("admin.alumni.claimed") : t("admin.alumni.unclaimed")}
      </Badge>
    </article>
  );
}
