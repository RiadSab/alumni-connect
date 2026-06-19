// Post a new job offer (OWNER/RECRUITER). Plain controlled inputs validated with
// createJobOfferSchema, then POST /job-offers via useCreateJobOffer. Reached at
// /company/jobs/new. Same form shape as the candidate forms; English labels for now
// (form i18n is a later pass).

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCreateJobOffer } from "@/features/jobOffers/hooks";
import { createJobOfferSchema } from "@/types/jobOffer";
import { employmentTypeOptions, jobCityOptions } from "@/types/enums";
import { isApiError } from "@/lib/http";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export function CompanyJobOfferCreatePage() {
  const navigate = useNavigate();
  const create = useCreateJobOffer();

  const [form, setForm] = useState({
    title: "",
    description: "",
    requirements: "", // one per line
    city: "",
    employmentType: "",
    minSalary: "",
    maxSalary: "",
    applicationDeadline: "", // date; sent as end-of-day date-time
    experienceYears: "",
    skillsRequired: "", // comma-separated
    maxApplications: "",
    contactEmail: "",
    isRemote: false,
    isUrgent: false,
  });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  function setField(name: keyof typeof form, value: string | boolean) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    const requirements = form.requirements.split("\n").map((s) => s.trim()).filter(Boolean);
    const skills = form.skillsRequired.split(",").map((s) => s.trim()).filter(Boolean);

    // Empty fields are omitted (undefined) so the backend leaves them unset.
    const payload = {
      title: form.title,
      description: form.description || undefined,
      requirements: requirements.length > 0 ? requirements : undefined,
      city: form.city || undefined,
      employmentType: form.employmentType || undefined,
      minSalary: form.minSalary === "" ? undefined : Number(form.minSalary),
      maxSalary: form.maxSalary === "" ? undefined : Number(form.maxSalary),
      // <input type="date"> gives a day only; pin it to end-of-day so it's a date-time.
      applicationDeadline:
        form.applicationDeadline === "" ? undefined : `${form.applicationDeadline}T23:59:59`,
      experienceYears: form.experienceYears === "" ? undefined : Number(form.experienceYears),
      skillsRequired: skills.length > 0 ? skills : undefined,
      maxApplications: form.maxApplications === "" ? undefined : Number(form.maxApplications),
      contactEmail: form.contactEmail || undefined,
      isRemote: form.isRemote,
      isUrgent: form.isUrgent,
    };

    const result = createJobOfferSchema.safeParse(payload);
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

    create.mutate(result.data, {
      onSuccess: () => navigate("/company/jobs"),
      onError: (error) => {
        setFormError(isApiError(error) ? error.message : "Something went wrong. Please try again.");
      },
    });
  }

  return (
    <div className="mx-auto max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>Post a job</CardTitle>
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
              <Button type="submit" disabled={create.isPending}>
                {create.isPending ? "Posting..." : "Post job"}
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
