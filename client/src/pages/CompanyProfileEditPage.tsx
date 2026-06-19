// Edit the company's own profile (OWNER only). Loads the caller's company the same
// two-step way the view does (/company-users/me → companyId → /companies/{id}),
// seeds the form, and PATCHes the changes via /companies/me. Plain controlled inputs
// validated with updateCompanySchema — same pattern as CandidateProfileEditPage.
// Reached via /profile/edit when a COMPANY_USER is logged in (ProfileEditPage
// dispatches by type). Logo upload lands in the next commit.
//
// English labels for now (form i18n is a later pass, same as the candidate form).

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMyCompanyUserProfile } from "@/features/companyUsers/hooks";
import { useCompany, useUpdateMyCompany } from "@/features/companies/hooks";
import { updateCompanySchema, type CompanyDTO } from "@/types/company";
import { companySizeOptions, fieldsOptions } from "@/types/enums";
import { isApiError } from "@/lib/http";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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

// Keep only entries whose value differs from the original. Backend treats
// null/omitted fields as "leave unchanged", so unchanged fields must not be sent.
function changedFields(
  next: Record<string, unknown>,
  prev: Record<string, unknown>,
): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const key of Object.keys(next)) {
    if (next[key] !== prev[key]) out[key] = next[key];
  }
  return out;
}

if (import.meta.env.DEV) {
  console.assert(
    JSON.stringify(changedFields({ a: 1, b: "x" }, { a: 1, b: "y" })) === JSON.stringify({ b: "x" }),
    "changedFields keeps only edited entries",
  );
}

export function CompanyProfileEditPage() {
  const me = useMyCompanyUserProfile();
  const company = useCompany(me.data?.companyId ?? NaN);

  const isLoading = me.isLoading || company.isLoading;
  const isError = me.isError || company.isError;

  if (isLoading) {
    return (
      <div className="mx-auto max-w-lg">
        <Skeleton className="h-[28rem] w-full rounded-lg" />
      </div>
    );
  }

  if (isError || !company.data || !me.data) {
    return (
      <div className="mx-auto max-w-lg text-center text-sm text-destructive">
        Couldn't load your company. <Link to="/profile" className="underline">Go back</Link>.
      </div>
    );
  }

  // Only the OWNER can edit; the backend would 403 anyone else. Don't show the form
  // to a RECRUITER/MEMBER who reached this URL directly.
  if (me.data.companyRole !== "OWNER") {
    return (
      <div className="mx-auto max-w-lg text-center text-sm text-muted-foreground">
        Only the company owner can edit the company profile.{" "}
        <Link to="/profile" className="underline">Go back</Link>.
      </div>
    );
  }

  return <EditForm company={company.data} />;
}

function EditForm({ company }: { company: CompanyDTO }) {
  const navigate = useNavigate();
  const update = useUpdateMyCompany();

  // Seed the form from the loaded company once.
  const [form, setForm] = useState(() => ({
    name: company.name,
    phone: company.phone ?? "",
    field: company.field as string,
    size: (company.size as string) ?? "",
    website: company.website ?? "",
    address: company.address ?? "",
    description: company.description ?? "",
  }));
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  function setField(name: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    // The form's current values (blank size → undefined, i.e. leave unchanged).
    const next: Record<string, unknown> = {
      name: form.name,
      phone: form.phone,
      field: form.field,
      size: form.size === "" ? undefined : form.size,
      website: form.website,
      address: form.address,
      description: form.description,
    };

    // The values we loaded, normalized the same way (null → "" / undefined) so the
    // diff below only flags fields the user actually edited.
    const prev: Record<string, unknown> = {
      name: company.name,
      phone: company.phone ?? "",
      field: company.field,
      size: company.size ?? undefined,
      website: company.website ?? "",
      address: company.address ?? "",
      description: company.description ?? "",
    };

    // Backend: null/omitted = leave unchanged. Send only what changed.
    const changes = changedFields(next, prev);

    const result = updateCompanySchema.safeParse(changes);
    if (!result.success) {
      const errors: Record<string, string> = {};
      for (const issue of result.error.issues) {
        const field = issue.path[0];
        if (typeof field === "string" && errors[field] === undefined) {
          errors[field] = issue.message;
        }
      }
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    if (Object.keys(result.data).length === 0) {
      navigate("/profile"); // nothing edited
      return;
    }

    update.mutate(result.data, {
      onSuccess: () => navigate("/profile"),
      onError: (error) => {
        setFormError(isApiError(error) ? error.message : "Something went wrong. Please try again.");
      },
    });
  }

  return (
    <div className="mx-auto max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>Edit company profile</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <TextField id="name" label="Company name" value={form.name}
              onChange={(v) => setField("name", v)} error={fieldErrors.name} />

            <TextField id="phone" label="Phone" value={form.phone}
              onChange={(v) => setField("phone", v)} error={fieldErrors.phone} />

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Field</label>
              <Select value={form.field} onValueChange={(v) => setField("field", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a field" />
                </SelectTrigger>
                <SelectContent>
                  {fieldsOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fieldErrors.field && <p className="text-sm text-destructive">{fieldErrors.field}</p>}
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Company size</label>
              <Select value={form.size} onValueChange={(v) => setField("size", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a size" />
                </SelectTrigger>
                <SelectContent>
                  {companySizeOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fieldErrors.size && <p className="text-sm text-destructive">{fieldErrors.size}</p>}
            </div>

            <TextField id="website" label="Website" value={form.website}
              onChange={(v) => setField("website", v)} error={fieldErrors.website} />
            <TextField id="address" label="Address" value={form.address}
              onChange={(v) => setField("address", v)} error={fieldErrors.address} />

            <div className="flex flex-col gap-2">
              <label htmlFor="description" className="text-sm font-medium">Description</label>
              <textarea
                id="description"
                rows={5}
                value={form.description}
                onChange={(event) => setField("description", event.target.value)}
                className="w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              />
              {fieldErrors.description && (
                <p className="text-sm text-destructive">{fieldErrors.description}</p>
              )}
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <div className="flex gap-3">
              <Button type="submit" disabled={update.isPending}>
                {update.isPending ? "Saving..." : "Save changes"}
              </Button>
              <Button type="button" variant="outline" asChild>
                <Link to="/profile">Cancel</Link>
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

// label + input + error trio, repeated for every text field.
function TextField({
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
      <Input
        id={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={error !== undefined}
      />
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
