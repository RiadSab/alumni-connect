// Edit the candidate's own profile. Loads /candidates/me, seeds the form from it,
// and PATCHes the changes. Plain controlled inputs validated with
// updateCandidateProfileSchema — same pattern as RegisterCandidatePage. Reached via
// /profile/edit when a CANDIDATE is logged in (ProfileEditPage dispatches by type).
//
// English labels for now (form i18n is a later pass, same as login/register).

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMyCandidateProfile, useUpdateMyCandidateProfile } from "@/features/candidates/hooks";
import { updateCandidateProfileSchema, type CandidateProfileDTO } from "@/types/candidate";
import { fieldsOptions } from "@/types/enums";
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
    JSON.stringify(changedFields({ a: 1, b: "x", s: [1, 2] }, { a: 1, b: "y", s: [1, 2] })) ===
      JSON.stringify({ b: "x" }),
    "changedFields keeps only edited entries",
  );
}

export function CandidateProfileEditPage() {
  const { data: profile, isLoading, isError } = useMyCandidateProfile();

  if (isLoading) {
    return (
      <div className="mx-auto max-w-lg">
        <Skeleton className="h-[32rem] w-full rounded-lg" />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="mx-auto max-w-lg text-center text-sm text-destructive">
        Couldn't load your profile. <Link to="/profile" className="underline">Go back</Link>.
      </div>
    );
  }

  return <EditForm profile={profile} />;
}

function EditForm({ profile }: { profile: CandidateProfileDTO }) {
  const navigate = useNavigate();
  const update = useUpdateMyCandidateProfile();

  // Seed the form from the loaded profile once.
  const [form, setForm] = useState(() => ({
    firstName: profile.firstName,
    lastName: profile.lastName,
    phoneNumber: profile.phoneNumber,
    isStudent: profile.isStudent,
    studentId: profile.studentId ?? "",
    fieldOfStudy: profile.fieldOfStudy as string,
    graduationYear: String(profile.graduationYear),
    dateOfBirth: profile.dateOfBirth ?? "",
    skills: profile.skills.join(", "),
    currentJobTitle: profile.currentJobTitle ?? "",
    currentCompany: profile.currentCompany ?? "",
    experienceYears: profile.experienceYears != null ? String(profile.experienceYears) : "",
    linkedinUrl: profile.linkedinUrl ?? "",
    githubUrl: profile.githubUrl ?? "",
    portfolioUrl: profile.portfolioUrl ?? "",
    bio: profile.bio ?? "",
  }));
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  function setField(name: keyof typeof form, value: string | boolean) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    // The form's current values, typed (blank numbers/date → undefined).
    const next: Record<string, unknown> = {
      firstName: form.firstName,
      lastName: form.lastName,
      phoneNumber: form.phoneNumber,
      isStudent: form.isStudent,
      studentId: form.studentId,
      fieldOfStudy: form.fieldOfStudy,
      graduationYear: form.graduationYear === "" ? undefined : Number(form.graduationYear),
      dateOfBirth: form.dateOfBirth === "" ? undefined : form.dateOfBirth,
      skills: form.skills.split(",").map((s) => s.trim()).filter(Boolean),
      currentJobTitle: form.currentJobTitle,
      currentCompany: form.currentCompany,
      experienceYears: form.experienceYears === "" ? undefined : Number(form.experienceYears),
      linkedinUrl: form.linkedinUrl,
      githubUrl: form.githubUrl,
      portfolioUrl: form.portfolioUrl,
      bio: form.bio,
    };

    // The values we loaded, normalized the same way (null → "" / undefined) so the
    // diff below only flags fields the user actually edited.
    const prev: Record<string, unknown> = {
      firstName: profile.firstName,
      lastName: profile.lastName,
      phoneNumber: profile.phoneNumber,
      isStudent: profile.isStudent,
      studentId: profile.studentId ?? "",
      fieldOfStudy: profile.fieldOfStudy,
      graduationYear: profile.graduationYear,
      dateOfBirth: profile.dateOfBirth ?? undefined,
      skills: profile.skills,
      currentJobTitle: profile.currentJobTitle ?? "",
      currentCompany: profile.currentCompany ?? "",
      experienceYears: profile.experienceYears ?? undefined,
      linkedinUrl: profile.linkedinUrl ?? "",
      githubUrl: profile.githubUrl ?? "",
      portfolioUrl: profile.portfolioUrl ?? "",
      bio: profile.bio ?? "",
    };

    // Backend: null/omitted = leave unchanged. Send only what changed.
    const changes = changedFields(next, prev);

    const result = updateCandidateProfileSchema.safeParse(changes);
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
          <CardTitle>Edit profile</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <div className="grid gap-4 sm:grid-cols-2">
              <TextField id="firstName" label="First name" value={form.firstName}
                onChange={(v) => setField("firstName", v)} error={fieldErrors.firstName} />
              <TextField id="lastName" label="Last name" value={form.lastName}
                onChange={(v) => setField("lastName", v)} error={fieldErrors.lastName} />
            </div>

            <TextField id="phoneNumber" label="Phone number" value={form.phoneNumber}
              onChange={(v) => setField("phoneNumber", v)} error={fieldErrors.phoneNumber} />

            <div className="flex items-center gap-3">
              <Switch id="isStudent" checked={form.isStudent}
                onCheckedChange={(checked) => setField("isStudent", checked)} />
              <label htmlFor="isStudent" className="text-sm font-medium">
                I am currently a student
              </label>
            </div>

            {form.isStudent && (
              <TextField id="studentId" label="Student ID" value={form.studentId}
                onChange={(v) => setField("studentId", v)} error={fieldErrors.studentId} />
            )}

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Field of study</label>
              <Select value={form.fieldOfStudy} onValueChange={(v) => setField("fieldOfStudy", v)}>
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
              {fieldErrors.fieldOfStudy && (
                <p className="text-sm text-destructive">{fieldErrors.fieldOfStudy}</p>
              )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <TextField id="graduationYear" label="Graduation year" type="number"
                value={form.graduationYear} onChange={(v) => setField("graduationYear", v)}
                error={fieldErrors.graduationYear} />
              <TextField id="dateOfBirth" label="Date of birth" type="date"
                value={form.dateOfBirth} onChange={(v) => setField("dateOfBirth", v)}
                error={fieldErrors.dateOfBirth} />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <TextField id="currentJobTitle" label="Current job title" value={form.currentJobTitle}
                onChange={(v) => setField("currentJobTitle", v)} error={fieldErrors.currentJobTitle} />
              <TextField id="currentCompany" label="Current company" value={form.currentCompany}
                onChange={(v) => setField("currentCompany", v)} error={fieldErrors.currentCompany} />
            </div>

            <TextField id="experienceYears" label="Years of experience" type="number"
              value={form.experienceYears} onChange={(v) => setField("experienceYears", v)}
              error={fieldErrors.experienceYears} />

            <TextField id="skills" label="Skills (comma-separated)" value={form.skills}
              onChange={(v) => setField("skills", v)} error={fieldErrors.skills} />

            <TextField id="linkedinUrl" label="LinkedIn URL" value={form.linkedinUrl}
              onChange={(v) => setField("linkedinUrl", v)} error={fieldErrors.linkedinUrl} />
            <TextField id="githubUrl" label="GitHub URL" value={form.githubUrl}
              onChange={(v) => setField("githubUrl", v)} error={fieldErrors.githubUrl} />
            <TextField id="portfolioUrl" label="Portfolio URL" value={form.portfolioUrl}
              onChange={(v) => setField("portfolioUrl", v)} error={fieldErrors.portfolioUrl} />

            <div className="flex flex-col gap-2">
              <label htmlFor="bio" className="text-sm font-medium">Bio</label>
              <textarea
                id="bio"
                rows={4}
                value={form.bio}
                onChange={(event) => setField("bio", event.target.value)}
                className="w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              />
              {fieldErrors.bio && <p className="text-sm text-destructive">{fieldErrors.bio}</p>}
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
