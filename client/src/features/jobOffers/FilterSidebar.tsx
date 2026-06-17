// Job board filter sidebar. Holds no state of its own — the page owns the values
// and this renders the controls. Skills filter is deferred until there's a real
// source for the skill list.

import { Search, SlidersHorizontal } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { jobCityOptions, employmentTypeOptions } from "@/types/enums";
import { useT } from "@/features/i18n/lang-context";
import type { JobFilterValues } from "@/features/jobOffers/filters";

interface FilterSidebarProps {
  values: JobFilterValues;
  onChange: (patch: Partial<JobFilterValues>) => void;
  onClear: () => void;
}

export function FilterSidebar({ values, onChange, onClear }: FilterSidebarProps) {
  const { t } = useT();
  return (
    <aside className="sticky top-6 rounded-lg border border-border bg-card p-5">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <SlidersHorizontal className="size-4 text-[var(--color-steel)]" /> {t("filters.title")}
        </h2>
        <button
          type="button"
          onClick={onClear}
          className="text-[13px] font-medium text-[var(--color-link-blue)] hover:text-[var(--color-link-blue-pressed)]"
        >
          {t("filters.clearAll")}
        </button>
      </div>

      <div className="mb-4">
        <label htmlFor="kw" className="mb-2 block text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
          {t("filters.keyword")}
        </label>
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-[var(--color-steel)]" />
          <Input
            id="kw"
            className="pl-9"
            placeholder={t("filters.keywordPlaceholder")}
            value={values.q}
            onChange={(event) => onChange({ q: event.target.value })}
          />
        </div>
      </div>

      <div className="mb-4">
        <label className="mb-2 block text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
          {t("filters.city")}
        </label>
        <Select value={values.city} onValueChange={(city) => onChange({ city })}>
          <SelectTrigger className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t("filters.allCities")}</SelectItem>
            {jobCityOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="mb-4">
        <label className="mb-2 block text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
          {t("filters.type")}
        </label>
        <Select
          value={values.employmentType}
          onValueChange={(employmentType) => onChange({ employmentType })}
        >
          <SelectTrigger className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t("filters.allTypes")}</SelectItem>
            {employmentTypeOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-[var(--color-charcoal)]">
          {t("filters.remoteOnly")}
        </span>
        <Switch
          checked={values.isRemote}
          onCheckedChange={(isRemote) => onChange({ isRemote })}
        />
      </div>
    </aside>
  );
}
