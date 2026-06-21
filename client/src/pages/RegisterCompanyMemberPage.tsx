// Join an existing company: registers a new COMPANY_USER as a MEMBER of a company that
// already exists (the owner registered it earlier). Same await-approval flow as the other
// sign-ups. English labels, matching the other auth forms.

import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Search, X } from "lucide-react";
import { useRegisterCompanyMember } from "@/features/auth/hooks";
import { useCompanySearch } from "@/features/companies/hooks";
import { registerCompanyMemberSchema } from "@/types/auth";
import { companyUserPositionOptions, fieldsOptions } from "@/types/enums";
import { isApiError } from "@/lib/http";
import type { CompanyDTO } from "@/types/company";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
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
      if (!company) errors.companyId = "Select your company from the list above.";
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
            ? "That company can't be joined right now. Pick another, or check with your company owner."
            : isApiError(error)
              ? error.message
              : "Something went wrong. Please try again.",
        );
      },
    });
  }

  return (
    <div className="mx-auto max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>Join your company</CardTitle>
          <CardDescription>
            For employees of a company already on Alumni Connect. Your account awaits admin approval.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <h2 className="text-sm font-semibold text-muted-foreground">Your company</h2>
            <CompanyPicker selected={company} onSelect={setCompany} error={fieldErrors.companyId} />

            <h2 className="mt-2 text-sm font-semibold text-muted-foreground">Your account</h2>

            <Field id="firstName" label="First name" value={form.firstName} onChange={(v) => setField("firstName", v)} error={fieldErrors.firstName} />
            <Field id="lastName" label="Last name" value={form.lastName} onChange={(v) => setField("lastName", v)} error={fieldErrors.lastName} />
            <Field id="email" label="Email" type="email" value={form.email} onChange={(v) => setField("email", v)} error={fieldErrors.email} />
            <Field id="password" label="Password" type="password" value={form.password} onChange={(v) => setField("password", v)} error={fieldErrors.password} />
            <Field id="phoneNumber" label="Phone (optional)" value={form.phoneNumber} onChange={(v) => setField("phoneNumber", v)} error={fieldErrors.phoneNumber} />

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Your position</label>
              <Select value={form.position} onValueChange={(value) => setField("position", value)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select your position" />
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

            <Button type="submit" disabled={register.isPending}>
              {register.isPending ? "Joining..." : "Join company"}
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              Registering a new company?{" "}
              <Link to="/register/company" className="underline">
                Create one
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

// Label + input + error trio (the form's repeated text-field shape).
function Field({
  id,
  label,
  value,
  onChange,
  error,
  type = "text",
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  type?: string;
}) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <Input id={id} type={type} value={value} onChange={(event) => onChange(event.target.value)} aria-invalid={error !== undefined} />
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}

// Search-and-select for an existing active company. Once one is chosen it collapses to a
// chip with a "Change" button; selecting supplies the companyId the form submits.
function CompanyPicker({
  selected,
  onSelect,
  error,
}: {
  selected: CompanyDTO | null;
  onSelect: (company: CompanyDTO | null) => void;
  error?: string;
}) {
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
          <X className="size-4" /> Change
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          className="pl-9"
          placeholder="Search your company by name"
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          aria-invalid={error !== undefined}
        />
      </div>

      {searching && (
        <div className="rounded-lg border border-border">
          {isFetching && !data && <Skeleton className="m-2 h-9" />}
          {data && data.empty && (
            <p className="px-3 py-3 text-sm text-muted-foreground">No active companies match.</p>
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
