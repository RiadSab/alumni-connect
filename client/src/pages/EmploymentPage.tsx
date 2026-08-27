// The alumnus's own career timeline (/profile/employment): add, edit and remove periods.
// These rows are what the school's employment figures are aggregated from.

import { useState } from "react";
import { ArrowLeft, Briefcase, Pencil, Plus, Trash2 } from "lucide-react";
import { Link } from "react-router-dom";
import {
  useCreateEmploymentEntry,
  useDeleteEmploymentEntry,
  useMyEmployment,
  useUpdateEmploymentEntry,
} from "@/features/employment/hooks";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { employmentStatusOptions } from "@/types/enums";
import { saveEmploymentEntrySchema, type EmploymentEntryDTO } from "@/types/employment";

const emptyForm = {
  status: "EMPLOYED",
  employer: "",
  jobTitle: "",
  sector: "",
  city: "",
  startedAt: "",
  endedAt: "",
};

export function EmploymentPage() {
  const { t } = useT();
  const { data: entries, isLoading } = useMyEmployment();
  const [editing, setEditing] = useState<EmploymentEntryDTO | "new" | null>(null);

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <Link
        to="/profile"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-[var(--color-steel)] hover:text-foreground"
      >
        <ArrowLeft className="size-4" /> {t("employment.back")}
      </Link>

      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("employment.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("employment.subtitle")}</p>
      </div>

      {editing === null && (
        <Button size="sm" onClick={() => setEditing("new")}>
          <Plus className="size-4" /> {t("employment.add")}
        </Button>
      )}

      {editing !== null && (
        <EntryForm
          entry={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
        />
      )}

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 2 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-lg" />
          ))}
        </div>
      ) : !entries || entries.length === 0 ? (
        <p className="rounded-lg border border-border bg-card px-5 py-8 text-center text-sm text-[var(--color-slate)]">
          {t("employment.empty")}
        </p>
      ) : (
        <ul className="space-y-3">
          {entries.map((entry) => (
            <EntryRow key={entry.id} entry={entry} onEdit={() => setEditing(entry)} />
          ))}
        </ul>
      )}
    </div>
  );
}

function EntryRow({ entry, onEdit }: { entry: EmploymentEntryDTO; onEdit: () => void }) {
  const { t, lang } = useT();
  const remove = useDeleteEmploymentEntry();
  const [confirming, setConfirming] = useState(false);

  const status = employmentStatusOptions.find((o) => o.value === entry.status)?.label ?? entry.status;
  const period = `${formatMonth(entry.startedAt, lang)} — ${
    entry.endedAt === null ? t("employment.current") : formatMonth(entry.endedAt, lang)
  }`;

  return (
    <li className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-4">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-base font-semibold leading-tight text-foreground">
            {entry.status === "EMPLOYED" ? `${entry.jobTitle} · ${entry.employer}` : status}
          </h2>
          {entry.endedAt === null && <Badge>{t("employment.current")}</Badge>}
        </div>
        <p className="mt-1 text-sm text-[var(--color-steel)]">
          {period}
          {entry.city !== null && ` · ${entry.city}`}
          {entry.sector !== null && ` · ${entry.sector}`}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        <Button variant="outline" size="sm" onClick={onEdit}>
          <Pencil className="size-4" /> {t("employment.edit")}
        </Button>
        {confirming ? (
          <Button
            variant="destructive"
            size="sm"
            disabled={remove.isPending}
            onClick={() => remove.mutate(entry.id)}
          >
            {t("employment.deleteConfirm")}
          </Button>
        ) : (
          <Button variant="ghost" size="sm" onClick={() => setConfirming(true)}>
            <Trash2 className="size-4" />
          </Button>
        )}
      </div>
    </li>
  );
}

function EntryForm({ entry, onClose }: { entry: EmploymentEntryDTO | null; onClose: () => void }) {
  const { t } = useT();
  const create = useCreateEmploymentEntry();
  const update = useUpdateEmploymentEntry();
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState(() =>
    entry === null
      ? emptyForm
      : {
          status: entry.status as string,
          employer: entry.employer ?? "",
          jobTitle: entry.jobTitle ?? "",
          sector: entry.sector ?? "",
          city: entry.city ?? "",
          startedAt: entry.startedAt,
          endedAt: entry.endedAt ?? "",
        },
  );

  function setField(name: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  const employed = form.status === "EMPLOYED";
  const pending = create.isPending || update.isPending;

  function submit() {
    setError(null);
    const parsed = saveEmploymentEntrySchema.safeParse({
      ...form,
      employer: employed ? form.employer : undefined,
      jobTitle: employed ? form.jobTitle : undefined,
      sector: form.sector === "" ? undefined : form.sector,
      city: form.city === "" ? undefined : form.city,
      endedAt: form.endedAt === "" ? undefined : form.endedAt,
    });
    if (!parsed.success) {
      setError(t("employment.error"));
      return;
    }

    const options = {
      onSuccess: () => onClose(),
      onError: (e: unknown) => setError(isApiError(e) ? e.message : t("employment.error")),
    };
    if (entry === null) create.mutate(parsed.data, options);
    else update.mutate({ id: entry.id, body: parsed.data }, options);
  }

  return (
    <section className="rounded-lg border border-border bg-card p-5">
      <div className="flex flex-col gap-4">
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium">Status</label>
            <Select value={form.status} onValueChange={(v) => setField("status", v)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {employmentStatusOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {employed && (
            <div className="flex flex-col gap-2">
              <label htmlFor="employer" className="text-sm font-medium">Employer</label>
              <Input
                id="employer"
                value={form.employer}
                onChange={(event) => setField("employer", event.target.value)}
              />
            </div>
          )}
        </div>

        {employed && (
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-2">
              <label htmlFor="jobTitle" className="text-sm font-medium">Job title</label>
              <Input
                id="jobTitle"
                value={form.jobTitle}
                onChange={(event) => setField("jobTitle", event.target.value)}
              />
            </div>
            <div className="flex flex-col gap-2">
              <label htmlFor="sector" className="text-sm font-medium">Sector</label>
              <Input
                id="sector"
                placeholder="IT, banking, telecom…"
                value={form.sector}
                onChange={(event) => setField("sector", event.target.value)}
              />
            </div>
          </div>
        )}

        <div className="grid gap-4 sm:grid-cols-3">
          <div className="flex flex-col gap-2">
            <label htmlFor="city" className="text-sm font-medium">City</label>
            <Input
              id="city"
              value={form.city}
              onChange={(event) => setField("city", event.target.value)}
            />
          </div>
          <div className="flex flex-col gap-2">
            <label htmlFor="startedAt" className="text-sm font-medium">Started</label>
            <Input
              id="startedAt"
              type="date"
              value={form.startedAt}
              onChange={(event) => setField("startedAt", event.target.value)}
            />
          </div>
          <div className="flex flex-col gap-2">
            <label htmlFor="endedAt" className="text-sm font-medium">Ended</label>
            <Input
              id="endedAt"
              type="date"
              value={form.endedAt}
              onChange={(event) => setField("endedAt", event.target.value)}
            />
            <span className="text-xs text-[var(--color-slate)]">Leave empty if it's current</span>
          </div>
        </div>

        {error !== null && <p className="text-sm text-destructive">{error}</p>}

        <div className="flex gap-3">
          <Button onClick={submit} disabled={pending}>
            <Briefcase className="size-4" />
            {pending ? t("employment.saving") : t("employment.save")}
          </Button>
          <Button variant="ghost" onClick={onClose} disabled={pending}>
            {t("employment.cancel")}
          </Button>
        </div>
      </div>
    </section>
  );
}

function formatMonth(iso: string, lang: string): string {
  return new Date(iso).toLocaleDateString(lang, { month: "short", year: "numeric" });
}
