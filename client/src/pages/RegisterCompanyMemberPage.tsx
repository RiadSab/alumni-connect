// Join an existing company: registers a new COMPANY_USER as a MEMBER of a company
// that already exists (the owner registered it earlier). Same await-approval flow
// as the other sign-ups.

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Mail, Phone, Search, User, X } from "lucide-react";
import { useRegisterCompanyMember } from "@/features/auth/hooks";
import { useCompanySearch } from "@/features/companies/hooks";
import { AuthScreen } from "@/features/auth/AuthScreen";
import { AuthBrand, AuthField, AuthPasswordField } from "@/features/auth/fields";
import { useT } from "@/features/i18n/lang-context";
import { registerCompanyMemberSchema } from "@/types/auth";
import { companyUserPositionOptions, fieldsOptions } from "@/types/enums";
import { isApiError } from "@/lib/http";
import type { CompanyDTO } from "@/types/company";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const emptyForm = {
  firstName: "",
  lastName: "",
  email: "",
  password: "",
  phoneNumber: "",
  position: "",
};

export function RegisterCompanyMemberPage() {
  const { t } = useT();
  const register = useRegisterCompanyMember();
  const navigate = useNavigate();

  const [form, setForm] = useState(emptyForm);
  const [company, setCompany] = useState<CompanyDTO | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  function setField(name: keyof typeof emptyForm, value: string) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    const payload = {
      firstName: form.firstName,
      lastName: form.lastName,
      email: form.email,
      password: form.password,
      phoneNumber: form.phoneNumber || undefined,
      position: form.position,
      companyId: company?.id ?? Number.NaN,
    };

    const result = registerCompanyMemberSchema.safeParse(payload);
    if (!result.success) {
      const errors: Record<string, string> = {};
      for (const issue of result.error.issues) {
        const field = issue.path[0];
        if (typeof field === "string" && errors[field] === undefined) {
          errors[field] = issue.message;
        }
      }
      if (!company) errors.companyId = t("auth.member.selectCompany");
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    register.mutate(result.data, {
      onSuccess: () => navigate("/login", { state: { justRegistered: true } }),
      // A 400 here means the company isn't found / isn't active anymore.
      onError: (error) => {
        setFormError(
          isApiError(error) && error.status === 400
            ? t("auth.member.cantJoin")
            : isApiError(error)
              ? error.message
              : t("auth.error.generic"),
        );
      },
    });
  }

  return (
    <AuthScreen>
      <Card className="w-full max-w-lg bg-card/95 shadow-[var(--shadow-2)] backdrop-blur-sm">
        <CardContent className="flex flex-col gap-6 p-7">
          <AuthBrand title={t("auth.member.title")} subtitle={t("auth.member.subtitle")} />

          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <h2 className="text-sm font-semibold text-muted-foreground">
              {t("auth.company.yourCompany")}
            </h2>
            <CompanyPicker selected={company} onSelect={setCompany} error={fieldErrors.companyId} />

            <h2 className="mt-2 text-sm font-semibold text-muted-foreground">
              {t("auth.company.yourAccount")}
            </h2>

            <AuthField id="firstName" label={t("auth.field.firstName")} icon={User} value={form.firstName} onChange={(e) => setField("firstName", e.target.value)} error={fieldErrors.firstName} />
            <AuthField id="lastName" label={t("auth.field.lastName")} icon={User} value={form.lastName} onChange={(e) => setField("lastName", e.target.value)} error={fieldErrors.lastName} />
            <AuthField id="email" label={t("auth.field.email")} icon={Mail} type="email" value={form.email} onChange={(e) => setField("email", e.target.value)} error={fieldErrors.email} />
            <AuthPasswordField id="password" label={t("auth.field.password")} value={form.password} onChange={(e) => setField("password", e.target.value)} error={fieldErrors.password} />
            <AuthField id="phoneNumber" label={t("auth.field.phoneOptional")} icon={Phone} value={form.phoneNumber} onChange={(e) => setField("phoneNumber", e.target.value)} error={fieldErrors.phoneNumber} />

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium">{t("auth.field.position")}</label>
              <Select value={form.position} onValueChange={(value) => setField("position", value)}>
                <SelectTrigger>
                  <SelectValue placeholder={t("auth.placeholder.selectPosition")} />
                </SelectTrigger>
                <SelectContent>
                  {companyUserPositionOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fieldErrors.position && <p className="text-sm text-destructive">{fieldErrors.position}</p>}
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <Button type="submit" className="h-10" disabled={register.isPending}>
              {register.isPending ? t("auth.member.joining") : t("auth.member.submit")}
            </Button>
          </form>

          <div className="border-t border-border pt-4 text-center text-sm text-muted-foreground">
            {t("auth.member.registeringNew")}{" "}
            <Link to="/register/company" className="font-medium text-primary underline-offset-4 hover:underline">
              {t("auth.member.createOne")}
            </Link>
          </div>
        </CardContent>
      </Card>
    </AuthScreen>
  );
}

// Search-and-select for an existing active company. Once one is chosen it collapses
// to a chip with a "Change" button; selecting supplies the companyId the form submits.
function CompanyPicker({
  selected,
  onSelect,
  error,
}: {
  selected: CompanyDTO | null;
  onSelect: (company: CompanyDTO | null) => void;
  error?: string;
}) {
  const { t } = useT();
  const [term, setTerm] = useState("");
  const debounced = useDebounced(term);
  const { data, isFetching } = useCompanySearch(debounced);
  const searching = debounced.trim().length >= 2;

  if (selected) {
    return (
      <div className="flex items-center justify-between rounded-lg border border-border bg-muted/40 px-3 py-2">
        <div className="min-w-0">
          <div className="truncate text-sm font-medium text-foreground">{selected.name}</div>
          <div className="text-xs text-muted-foreground">{fieldLabel(selected.field)}</div>
        </div>
        <Button type="button" variant="ghost" size="sm" onClick={() => onSelect(null)}>
          <X className="size-4" /> {t("auth.member.change")}
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          className="h-10 pl-9"
          placeholder={t("auth.member.searchPlaceholder")}
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          aria-invalid={error !== undefined}
        />
      </div>

      {searching && (
        <div className="rounded-lg border border-border">
          {isFetching && !data && <Skeleton className="m-2 h-9" />}
          {data && data.empty && (
            <p className="px-3 py-3 text-sm text-muted-foreground">{t("auth.member.noMatch")}</p>
          )}
          {data &&
            data.content.map((c) => (
              <button
                key={c.id}
                type="button"
                onClick={() => onSelect(c)}
                className="flex w-full flex-col items-start border-b border-border px-3 py-2 text-left last:border-b-0 hover:bg-muted"
              >
                <span className="text-sm font-medium text-foreground">{c.name}</span>
                <span className="text-xs text-muted-foreground">{fieldLabel(c.field)}</span>
              </button>
            ))}
        </div>
      )}

      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

function fieldLabel(field: CompanyDTO["field"]): string {
  return fieldsOptions.find((o) => o.value === field)?.label ?? field;
}

// Debounce the search term so typing doesn't fire a request per keystroke.
function useDebounced(value: string, ms = 300): string {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), ms);
    return () => clearTimeout(timer);
  }, [value, ms]);
  return debounced;
}
