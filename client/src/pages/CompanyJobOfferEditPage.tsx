// Edit one of the company's job offers (OWNER/RECRUITER), and close/reopen it.
// Loads GET /job-offers/{id} (which falls back to the posting company's
// OWNER/RECRUITER for non-OPEN offers), seeds the form, and PATCHes only the changed
// fields. Closing/reopening is just PATCH { status }. Reached at
// /company/jobs/:id/edit. English labels for now (form i18n is a later pass).

import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useJobOffer, useUpdateJobOffer } from "@/features/jobOffers/hooks";
import { updateJobOfferSchema, type JobOfferDTO } from "@/types/jobOffer";
import { employmentTypeOptions, jobCityOptions, jobStatusOptions } from "@/types/enums";
import { isApiError } from "@/lib/http";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

// Keep only entries whose value differs from the original. Arrays are compared by
// value. Backend treats null/omitted fields as "leave unchanged", so unchanged
// fields must not be sent.
function changedFields(
  next: Record<string, unknown>,
  prev: Record<string, unknown>,
): Record<string, unknown> {
  const out: Record<string, unknown> = {};
  for (const key of Object.keys(next)) {
    const a = next[key];
    const b = prev[key];
    const same = Array.isArray(a) ? JSON.stringify(a) === JSON.stringify(b) : a === b;
    if (!same) out[key] = a;
  }
  return out;
}

if (import.meta.env.DEV) {
  console.assert(
    JSON.stringify(changedFields({ a: 1, s: [1, 2] }, { a: 1, s: [1, 3] })) ===
      JSON.stringify({ s: [1, 2] }),
    "changedFields keeps only edited entries",
  );
}

export function CompanyJobOfferEditPage() {
  const params = useParams();
  const id = Number(params.id);
  const { data: offer, isLoading, isError } = useJobOffer(id);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-lg">
        <Skeleton className="h-[32rem] w-full rounded-lg" />
      </div>
    );
  }

  if (isError || !offer) {
    return (
      <div className="mx-auto max-w-lg text-center text-sm text-destructive">
        Couldn't load this offer. <Link to="/company/jobs" className="underline">Go back</Link>.
      </div>
    );
  }

  return <EditForm offer={offer} />;
}

function EditForm({ offer }: { offer: JobOfferDTO }) {
  const navigate = useNavigate();
  const save = useUpdateJobOffer();
  const statusChange = useUpdateJobOffer();
  const [statusError, setStatusError] = useState<string | null>(null);

  // Seed the form from the loaded offer once. The deadline input is date-only.
  const [form, setForm] = useState(() => ({
    title: offer.title,
    description: offer.description ?? "",
    requirements: offer.requirements.join("\n"),
    city: (offer.city as string) ?? "",
    employmentType: (offer.employmentType as string) ?? "",
    minSalary: offer.minSalary != null ? String(offer.minSalary) : "",
    maxSalary: offer.maxSalary != null ? String(offer.maxSalary) : "",
    applicationDeadline: offer.applicationDeadline ? offer.applicationDeadline.slice(0, 10) : "",
    experienceYears: offer.experienceYears != null ? String(offer.experienceYears) : "",
    skillsRequired: offer.skillsRequired.join(", "),
    maxApplications: offer.maxApplications != null ? String(offer.maxApplications) : "",
    contactEmail: offer.contactEmail ?? "",
    isRemote: offer.isRemote,
    isUrgent: offer.isUrgent,
  }));
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  function setField(name: keyof typeof form, value: string | boolean) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  const status = jobStatusOptions.find((o) => o.value === offer.status)?.label ?? offer.status;
  const isOpen = offer.status === "OPEN";

  function toggleStatus() {
    setStatusError(null);
    statusChange.mutate(
      { id: offer.id, body: { status: isOpen ? "CLOSED" : "OPEN" } },
      {
        onError: (error) =>
          setStatusError(isApiError(error) ? error.message : "Couldn't change the status."),
      },
    );
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    const requirements = form.requirements.split("\n").map((s) => s.trim()).filter(Boolean);
    const skills = form.skillsRequired.split(",").map((s) => s.trim()).filter(Boolean);

    // Current values, normalized. Enums/email/numbers use undefined when blank (they
    // can't be sent empty); free text and arrays keep "" / [] so they can be cleared.
    const next: Record<string, unknown> = {
      title: form.title,
      description: form.description,
      requirements,
      city: form.city || undefined,
      employmentType: form.employmentType || undefined,
      minSalary: form.minSalary === "" ? undefined : Number(form.minSalary),
      maxSalary: form.maxSalary === "" ? undefined : Number(form.maxSalary),
      applicationDeadline: form.applicationDeadline || undefined, // date-only for the diff
      experienceYears: form.experienceYears === "" ? undefined : Number(form.experienceYears),
      skillsRequired: skills,
      maxApplications: form.maxApplications === "" ? undefined : Number(form.maxApplications),
      contactEmail: form.contactEmail || undefined,
      isRemote: form.isRemote,
      isUrgent: form.isUrgent,
    };

    // The loaded offer, normalized the same way so the diff only flags real edits.
    const prev: Record<string, unknown> = {
      title: offer.title,
      description: offer.description ?? "",
      requirements: offer.requirements,
      city: offer.city ?? undefined,
      employmentType: offer.employmentType ?? undefined,
      minSalary: offer.minSalary ?? undefined,
      maxSalary: offer.maxSalary ?? undefined,
      applicationDeadline: offer.applicationDeadline ? offer.applicationDeadline.slice(0, 10) : undefined,
      experienceYears: offer.experienceYears ?? undefined,
      skillsRequired: offer.skillsRequired,
      maxApplications: offer.maxApplications ?? undefined,
      contactEmail: offer.contactEmail ?? undefined,
      isRemote: offer.isRemote,
      isUrgent: offer.isUrgent,
    };

    const changes = changedFields(next, prev);

    // If the deadline changed, pin the date to end-of-day so it's a date-time.
    if (typeof changes.applicationDeadline === "string") {
      changes.applicationDeadline = `${changes.applicationDeadline}T23:59:59`;
    }

    const result = updateJobOfferSchema.safeParse(changes);
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
      navigate("/company/jobs"); // nothing edited
      return;
    }

    save.mutate(
      { id: offer.id, body: result.data },
      {
        onSuccess: () => navigate("/company/jobs"),
        onError: (error) => {
          setFormError(isApiError(error) ? error.message : "Something went wrong. Please try again.");
        },
      },
    );
  }

  return (
    <div className="mx-auto max-w-lg space-y-4">
      {/* Status / close-reopen */}
      <div className="flex items-center justify-between gap-4 rounded-lg border border-border bg-card p-4">
        <div className="text-sm">
          <span className="text-[var(--color-steel)]">Status: </span>
          <span className="font-medium text-foreground">{status}</span>
          {statusError && <p className="mt-1 text-destructive">{statusError}</p>}
        </div>
        <Button
          type="button"
          variant={isOpen ? "outline" : "default"}
          size="sm"
          onClick={toggleStatus}
          disabled={statusChange.isPending}
        >
          {statusChange.isPending ? "Saving..." : isOpen ? "Close posting" : "Reopen posting"}
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Edit job offer</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <TextField id="title" label="Title" value={form.title}
              onChange={(v) => setField("title", v)} error={fieldErrors.title} />

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

            <div className="flex flex-col gap-2">
              <label htmlFor="requirements" className="text-sm font-medium">
                Requirements (one per line)
              </label>
              <textarea
                id="requirements"
                rows={4}
                value={form.requirements}
                onChange={(event) => setField("requirements", event.target.value)}
                className="w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              />
              {fieldErrors.requirements && (
                <p className="text-sm text-destructive">{fieldErrors.requirements}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">City</label>
              <Select value={form.city} onValueChange={(v) => setField("city", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a city" />
                </SelectTrigger>
                <SelectContent>
                  {jobCityOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fieldErrors.city && <p className="text-sm text-destructive">{fieldErrors.city}</p>}
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Employment type</label>
              <Select value={form.employmentType} onValueChange={(v) => setField("employmentType", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select a type" />
                </SelectTrigger>
                <SelectContent>
                  {employmentTypeOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fieldErrors.employmentType && (
                <p className="text-sm text-destructive">{fieldErrors.employmentType}</p>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <TextField id="minSalary" label="Min salary (MAD)" type="number" value={form.minSalary}
                onChange={(v) => setField("minSalary", v)} error={fieldErrors.minSalary} />
              <TextField id="maxSalary" label="Max salary (MAD)" type="number" value={form.maxSalary}
                onChange={(v) => setField("maxSalary", v)} error={fieldErrors.maxSalary} />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <TextField id="applicationDeadline" label="Application deadline" type="date"
                value={form.applicationDeadline}
                onChange={(v) => setField("applicationDeadline", v)}
                error={fieldErrors.applicationDeadline} />
              <TextField id="experienceYears" label="Years of experience" type="number"
                value={form.experienceYears} onChange={(v) => setField("experienceYears", v)}
                error={fieldErrors.experienceYears} />
            </div>

            <TextField id="skillsRequired" label="Skills (comma-separated)" value={form.skillsRequired}
              onChange={(v) => setField("skillsRequired", v)} error={fieldErrors.skillsRequired} />

            <div className="grid gap-4 sm:grid-cols-2">
              <TextField id="maxApplications" label="Max applications" type="number"
                value={form.maxApplications} onChange={(v) => setField("maxApplications", v)}
                error={fieldErrors.maxApplications} />
              <TextField id="contactEmail" label="Contact email" type="email" value={form.contactEmail}
                onChange={(v) => setField("contactEmail", v)} error={fieldErrors.contactEmail} />
            </div>

            <div className="flex items-center gap-3">
              <Switch id="isRemote" checked={form.isRemote}
                onCheckedChange={(checked) => setField("isRemote", checked)} />
              <label htmlFor="isRemote" className="text-sm font-medium">Remote</label>
            </div>
            <div className="flex items-center gap-3">
              <Switch id="isUrgent" checked={form.isUrgent}
                onCheckedChange={(checked) => setField("isUrgent", checked)} />
              <label htmlFor="isUrgent" className="text-sm font-medium">Urgent</label>
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <div className="flex gap-3">
              <Button type="submit" disabled={save.isPending}>
                {save.isPending ? "Saving..." : "Save changes"}
              </Button>
              <Button type="button" variant="outline" asChild>
                <Link to="/company/jobs">Cancel</Link>
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
