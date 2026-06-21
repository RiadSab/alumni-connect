// Job board filter sidebar. The page owns the filter values; the only local state
// is the in-progress skill text before it's committed to a chip.

import { useState } from "react";
import { Search, SlidersHorizontal, X } from "lucide-react";
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
  const [skillDraft, setSkillDraft] = useState("");

  function addSkill() {
    const skill = skillDraft.trim();
    setSkillDraft("");
    if (!skill || values.skills.some((s) => s.toLowerCase() === skill.toLowerCase())) return;
    onChange({ skills: [...values.skills, skill] });
  }

  function removeSkill(skill: string) {
    onChange({ skills: values.skills.filter((s) => s !== skill) });
  }

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

      <div className="mb-4">
        <label htmlFor="skill" className="mb-2 block text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
          {t("filters.skills")}
        </label>
        <Input
          id="skill"
          placeholder={t("filters.skillsPlaceholder")}
          value={skillDraft}
          onChange={(event) => setSkillDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === ",") {
              event.preventDefault();
              addSkill();
            }
          }}
          onBlur={addSkill}
        />
        {values.skills.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {values.skills.map((skill) => (
              <span
                key={skill}
                className="inline-flex items-center gap-1 rounded-sm bg-[var(--color-tint-lavender)] px-2 py-[3px] text-[12.5px] font-medium text-[var(--color-brand-purple-800)]"
              >
                {skill}
                <button
                  type="button"
                  aria-label={t("filters.removeSkill", { skill })}
                  onClick={() => removeSkill(skill)}
                  className="text-[var(--color-brand-purple-800)] hover:text-[var(--color-charcoal)]"
                >
                  <X className="size-3" />
                </button>
              </span>
            ))}
          </div>
        )}
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
