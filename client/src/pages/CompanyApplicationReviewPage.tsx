// Review one job application (OWNER/RECRUITER) at /company/applications/:appId.

import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowLeft, ExternalLink, FileText } from "lucide-react";
import {
  useApplication,
  useApplicantProfile,
  useReviewApplication,
} from "@/features/jobApplications/hooks";
import { jobApplicationsApi } from "@/api/jobApplications";
import { reviewApplicationSchema, type JobApplicationDTO } from "@/types/jobApplication";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  applicationStatusOptions,
  fieldsOptions,
  interviewModeOptions,
  priorityOptions,
  type ApplicationStatus,
} from "@/types/enums";
import { useAuth } from "@/features/auth/auth-context";

// Status options a recruiter sets (WITHDRAWN is a candidate-only action).
const reviewableStatuses = applicationStatusOptions.filter((o) => o.value !== "WITHDRAWN");

// <input type="datetime-local"> wants local wall time, while the API speaks UTC instants.
function toLocalInput(iso: string | null): string {
  if (iso === null) return "";
  const date = new Date(iso);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

function statusVariant(status: ApplicationStatus): "default" | "secondary" | "destructive" | "outline" {
  if (status === "ACCEPTED") return "default";
  if (status === "REJECTED") return "destructive";
  if (status === "WITHDRAWN") return "secondary";
  return "outline";
}

export function CompanyApplicationReviewPage() {
  const { t } = useT();
  const params = useParams();
  const appId = Number(params.appId);
  const { data: application, isLoading, isError } = useApplication(appId);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Skeleton className="h-28 w-full rounded-lg" />
        <Skeleton className="h-40 w-full rounded-lg" />
      </div>
    );
  }

  if (isError || !application) {
    return (
      <div className="mx-auto max-w-3xl text-center text-sm text-destructive">
        {t("board.error.body")}{" "}
        <Link to="/company/jobs" className="underline">{t("applicants.back")}</Link>.
      </div>
    );
  }

  const status =
    applicationStatusOptions.find((o) => o.value === application.applicationStatus)?.label ??
    application.applicationStatus;

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <Link
        to={`/company/jobs/${application.jobOfferId}/applications`}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-[var(--color-link-blue)] hover:text-[var(--color-link-blue-pressed)]"
      >
        <ArrowLeft className="size-4" /> {t("applicants.back")}
      </Link>

      <div className="rounded-lg border border-border bg-card p-6">
        <div className="flex flex-wrap items-center gap-2">
          <h1 className="text-2xl font-semibold leading-tight text-foreground">
            {application.applicantName}
          </h1>
          <Badge variant={statusVariant(application.applicationStatus)}>{status}</Badge>
        </div>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{application.jobOfferTitle}</p>
        <ResumeButton appId={application.id} hasResume={application.resumeStorageId != null} />
      </div>

      {application.coverLetter && (
        <Section title={t("review.coverLetter")}>
          <p className="whitespace-pre-line text-[15px] leading-relaxed text-[var(--color-charcoal)]">
            {application.coverLetter}
          </p>
        </Section>
      )}

      <ApplicantProfile appId={application.id} />
      <ReviewForm application={application} />
    </div>
  );
}

// Fetches the application's résumé PDF on click (it's an authenticated endpoint, so
// we read it as a blob and open it in a new tab).
function ResumeButton({ appId, hasResume }: { appId: number; hasResume: boolean }) {
  const { t } = useT();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function view() {
    setError(null);
    setLoading(true);
    try {
      const blob = await jobApplicationsApi.downloadResume(appId);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank");
      // The new tab has loaded the blob by then.
      setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (e) {
      setError(isApiError(e) ? e.message : t("review.resumeError"));
    } finally {
      setLoading(false);
    }
  }

  if (!hasResume) {
    return <p className="mt-4 text-sm text-[var(--color-slate)]">{t("review.noResume")}</p>;
  }

  return (
    <div className="mt-4">
      <Button variant="outline" size="sm" onClick={view} disabled={loading}>
        <FileText className="size-4" /> {loading ? t("review.opening") : t("review.viewResume")}
      </Button>
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
    </div>
  );
}

function ApplicantProfile({ appId }: { appId: number }) {
  const { t, tn } = useT();
  const { data: profile, isLoading, isError } = useApplicantProfile(appId);

  if (isLoading) {
    return <Skeleton className="h-40 w-full rounded-lg" />;
  }
  if (isError || !profile) {
    return null; // profile is supplementary; don't block the review if it fails
  }

  const field = fieldsOptions.find((o) => o.value === profile.fieldOfStudy)?.label ?? profile.fieldOfStudy;
  const links = [
    { url: profile.linkedinUrl, label: "LinkedIn" },
    { url: profile.githubUrl, label: "GitHub" },
    { url: profile.portfolioUrl, label: "Portfolio" },
  ].filter((l) => l.url);

  return (
    <Section title={t("review.applicant")}>
      <div className="grid gap-3 sm:grid-cols-2">
        <Row label={t("profile.email")}>
          <a className="text-[var(--color-link-blue)] hover:underline" href={`mailto:${profile.email}`}>
            {profile.email}
          </a>
        </Row>
        <Row label={t("profile.phone")}>{profile.phoneNumber}</Row>
        <Row label={t("profile.fieldOfStudy")}>{field}</Row>
        <Row label={t("profile.graduationYear")}>{profile.graduationYear}</Row>
        {profile.experienceYears != null && (
          <Row label={t("detail.experience")}>
            {tn(profile.experienceYears, "detail.experienceYears.one", "detail.experienceYears.other")}
          </Row>
        )}
      </div>

      {profile.skills.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-1.5">
          {profile.skills.map((skill) => (
            <span
              key={skill}
              className="rounded-sm bg-[var(--color-tint-lavender)] px-2.5 py-1 text-[13px] font-medium text-[var(--color-brand-purple-800)]"
            >
              {skill}
            </span>
          ))}
        </div>
      )}

      {links.length > 0 && (
        <div className="mt-4 flex flex-wrap gap-4">
          {links.map((l) => (
            <a
              key={l.label}
              href={l.url!}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-[var(--color-link-blue)] hover:underline"
            >
              <ExternalLink className="size-4" /> {l.label}
            </a>
          ))}
        </div>
      )}

      {profile.bio && (
        <p className="mt-4 whitespace-pre-line text-[15px] leading-relaxed text-[var(--color-charcoal)]">
          {profile.bio}
        </p>
      )}
    </Section>
  );
}

function ReviewForm({ application }: { application: JobApplicationDTO }) {
  const { t } = useT();
  const { user } = useAuth();
  const review = useReviewApplication();
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState(() => ({
    status: application.applicationStatus as string,
    priority: (application.priority as string) ?? "",
    rating: application.rating != null ? String(application.rating) : "",
    note: application.companyUserNote ?? "",
    interviewMode: (application.interviewMode as string) ?? "ONLINE",
    interviewAt: toLocalInput(application.interviewAt),
    interviewLink: application.interviewLink ?? "",
    interviewLocation: application.interviewLocation ?? "",
    // Whoever schedules is usually the one conducting it.
    interviewerName: application.interviewerName ?? `${user?.firstName ?? ""} ${user?.lastName ?? ""}`.trim(),
  }));

  function setField(name: keyof typeof form, value: string) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function submit(statusOverride?: ApplicationStatus) {
    setError(null);
    const status = statusOverride ?? form.status;
    if (statusOverride) setField("status", statusOverride); // keep the Select in sync

    const online = form.interviewMode === "ONLINE";
    const payload = {
      applicationStatus: status,
      priority: form.priority === "" ? undefined : form.priority,
      rating: form.rating === "" ? undefined : Number(form.rating),
      companyUserNote: form.note,
      // Sent only when scheduling; the server ignores them otherwise.
      ...(status === "SCHEDULED_INTERVIEW" && {
        interviewMode: form.interviewMode,
        interviewAt: form.interviewAt === "" ? undefined : new Date(form.interviewAt).toISOString(),
        interviewLink: online ? form.interviewLink : undefined,
        interviewLocation: online ? undefined : form.interviewLocation,
        interviewerName: form.interviewerName,
      }),
    };

    const result = reviewApplicationSchema.safeParse(payload);
    if (!result.success) {
      setError(result.error.issues[0]?.message ?? t("review.error"));
      return;
    }

    review.mutate(
      { id: application.id, body: result.data },
      {
        onError: (e) => setError(isApiError(e) ? e.message : t("review.error")),
      },
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t("review.title")}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex flex-col gap-4">
          <div className="flex gap-3">
            <Button onClick={() => submit("ACCEPTED")} disabled={review.isPending}>
              {t("review.accept")}
            </Button>
            <Button variant="outline" onClick={() => submit("REJECTED")} disabled={review.isPending}>
              {t("review.reject")}
            </Button>
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium">{t("review.status")}</label>
            <Select value={form.status} onValueChange={(v) => setField("status", v)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {reviewableStatuses.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {form.status === "SCHEDULED_INTERVIEW" && (
            <div className="flex flex-col gap-4 rounded-lg border border-border bg-[var(--color-tint-lavender)]/30 p-4">
              <p className="text-sm font-semibold text-foreground">Interview details</p>

              <div className="grid gap-4 sm:grid-cols-2">
                <div className="flex flex-col gap-2">
                  <label className="text-sm font-medium">Mode</label>
                  <Select
                    value={form.interviewMode}
                    onValueChange={(v) => setField("interviewMode", v)}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {interviewModeOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex flex-col gap-2">
                  <label htmlFor="interviewAt" className="text-sm font-medium">Date and time</label>
                  <Input
                    id="interviewAt"
                    type="datetime-local"
                    value={form.interviewAt}
                    onChange={(event) => setField("interviewAt", event.target.value)}
                  />
                </div>
              </div>

              {form.interviewMode === "ONLINE" ? (
                <div className="flex flex-col gap-2">
                  <label htmlFor="interviewLink" className="text-sm font-medium">Meeting link</label>
                  <Input
                    id="interviewLink"
                    type="url"
                    placeholder="https://meet.google.com/abc-defg-hij"
                    value={form.interviewLink}
                    onChange={(event) => setField("interviewLink", event.target.value)}
                  />
                </div>
              ) : (
                <div className="flex flex-col gap-2">
                  <label htmlFor="interviewLocation" className="text-sm font-medium">Location</label>
                  <Input
                    id="interviewLocation"
                    placeholder="12 Rue des Fleurs, Casablanca"
                    value={form.interviewLocation}
                    onChange={(event) => setField("interviewLocation", event.target.value)}
                  />
                </div>
              )}

              <div className="flex flex-col gap-2">
                <label htmlFor="interviewerName" className="text-sm font-medium">Interviewer</label>
                <Input
                  id="interviewerName"
                  value={form.interviewerName}
                  onChange={(event) => setField("interviewerName", event.target.value)}
                />
              </div>

              <p className="text-xs text-[var(--color-slate)]">
                Saving emails these details to the candidate.
              </p>
            </div>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">{t("review.priority")}</label>
              <Select value={form.priority} onValueChange={(v) => setField("priority", v)}>
                <SelectTrigger>
                  <SelectValue placeholder={t("review.priorityNone")} />
                </SelectTrigger>
                <SelectContent>
                  {priorityOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="flex flex-col gap-2">
              <label htmlFor="rating" className="text-sm font-medium">{t("review.rating")}</label>
              <Input
                id="rating"
                type="number"
                min={0}
                max={10}
                value={form.rating}
                onChange={(event) => setField("rating", event.target.value)}
              />
            </div>
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="note" className="text-sm font-medium">{t("review.note")}</label>
            <textarea
              id="note"
              rows={4}
              value={form.note}
              onChange={(event) => setField("note", event.target.value)}
              className="w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
          {review.isSuccess && !error && (
            <p className="text-sm text-[var(--color-brand-green)]">{t("review.saved")}</p>
          )}

          <div>
            <Button onClick={() => submit()} disabled={review.isPending}>
              {review.isPending ? t("review.saving") : t("review.save")}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-lg border border-border bg-card p-6">
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {title}
      </h2>
      {children}
    </section>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="min-w-0">
      <div className="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {label}
      </div>
      <div className="text-sm font-medium text-[var(--color-charcoal)]">{children}</div>
    </div>
  );
}
