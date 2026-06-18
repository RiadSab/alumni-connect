// Candidate's own profile, read-only. Fetches /candidates/me and lays it out as
// cards. Editing and résumé/photo upload land in later commits.

import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { Camera, CloudOff, ExternalLink, FileText, Loader2, RefreshCw, Upload } from "lucide-react";
import {
  useMyCandidateProfile,
  useMyPhoto,
  useMyResume,
  useUploadProfilePhoto,
  useUploadResume,
} from "@/features/candidates/hooks";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { fieldsOptions } from "@/types/enums";
import type { CandidateProfileDTO } from "@/types/candidate";

export function ProfilePage() {
  const { t } = useT();
  const { data: profile, isLoading, isError, refetch } = useMyCandidateProfile();

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Skeleton className="h-28 w-full rounded-lg" />
        <Skeleton className="h-40 w-full rounded-lg" />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="mx-auto max-w-3xl">
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
          <Button className="mt-5" onClick={() => refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <ProfileHeader profile={profile} />
      <Resume />
      <Education profile={profile} />
      {profile.skills.length > 0 && <Skills profile={profile} />}
      <Links profile={profile} />
      {profile.bio && <About profile={profile} />}
    </div>
  );
}

// Turn the authenticated photo blob into an object URL for <img>, gated on
// whether the profile actually has a photo. Revokes the URL on change/unmount.
function useMyPhotoUrl(enabled: boolean): string | null {
  const photo = useMyPhoto(enabled);
  const blob = photo.data;
  const [url, setUrl] = useState<string | null>(null);
  // Mint a fresh object URL whenever the blob changes, and revoke it on the next
  // change/unmount. This MUST create the URL inside the effect (not useMemo): in
  // Strict Mode the effect runs setup→cleanup→setup, so a memoized URL would get
  // revoked while still rendered (blank photo after navigating back with the blob
  // already cached). Re-minting each setup keeps the URL in state valid.
  useEffect(() => {
    const objectUrl = blob ? URL.createObjectURL(blob) : null;
    setUrl(objectUrl); // eslint-disable-line react-hooks/set-state-in-effect -- syncing an external blob to a renderable URL; the extra render is intended
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [blob]);
  return url;
}

function ProfileHeader({ profile }: { profile: CandidateProfileDTO }) {
  const { t } = useT();
  const role = [profile.currentJobTitle, profile.currentCompany].filter(Boolean).join(" · ");

  const photoUrl = useMyPhotoUrl(profile.profilePhotoId != null);
  const upload = useUploadProfilePhoto();
  const inputRef = useRef<HTMLInputElement>(null);
  const [photoError, setPhotoError] = useState<string | null>(null);

  function onPhotoChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;
    if (!["image/png", "image/jpeg", "image/webp"].includes(file.type)) {
      setPhotoError(t("profile.photo.badType"));
      return;
    }
    setPhotoError(null);
    upload.mutate(file, {
      onError: (err) => setPhotoError(isApiError(err) ? err.message : t("profile.photo.error")),
    });
  }

  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 gap-4">
          <div className="relative size-16 shrink-0">
            {photoUrl ? (
              <img src={photoUrl} alt="" className="size-16 rounded-full object-cover" />
            ) : (
              <div className="grid size-16 place-items-center rounded-full bg-primary text-2xl font-bold text-primary-foreground">
                {profile.firstName.charAt(0)}
                {profile.lastName.charAt(0)}
              </div>
            )}
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={upload.isPending}
              aria-label={t("profile.photo.change")}
              className="absolute -bottom-1 -right-1 grid size-6 place-items-center rounded-full border border-border bg-card text-foreground shadow-sm hover:bg-muted disabled:opacity-60"
            >
              {upload.isPending ? (
                <Loader2 className="size-3 animate-spin" />
              ) : (
                <Camera className="size-3" />
              )}
            </button>
            <input
              ref={inputRef}
              type="file"
              accept="image/png,image/jpeg,image/webp"
              hidden
              onChange={onPhotoChange}
            />
          </div>
          <div className="min-w-0">
            <h1 className="text-2xl font-semibold leading-tight text-foreground">
              {profile.firstName} {profile.lastName}
            </h1>
            {role && <p className="mt-1 text-sm text-[var(--color-slate)]">{role}</p>}
            {photoError && <p className="mt-1 text-sm text-destructive">{photoError}</p>}
          </div>
        </div>
        <Button variant="outline" size="sm" asChild>
          <Link to="/profile/edit">{t("profile.edit")}</Link>
        </Button>
      </div>
      <div className="mt-5 grid gap-3 sm:grid-cols-2">
        <Row label={t("profile.email")}>
          <a className="text-[var(--color-link-blue)] hover:underline" href={`mailto:${profile.email}`}>
            {profile.email}
          </a>
        </Row>
        <Row label={t("profile.phone")}>{profile.phoneNumber}</Row>
      </div>
    </div>
  );
}

// Résumé upload/replace. The CV is what the Apply button reuses (useProfileResume),
// so without one here, applying fails. Presence comes from useMyResume: success =
// on file (and we can open the blob), a 404 = none yet.
function Resume() {
  const { t } = useT();
  const resume = useMyResume();
  const upload = useUploadResume();
  const inputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  function pickFile() {
    setError(null);
    inputRef.current?.click();
  }

  function onFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = ""; // let the user re-pick the same filename next time
    if (!file) return;
    if (file.type !== "application/pdf") {
      setError(t("profile.resume.notPdf"));
      return;
    }
    setError(null);
    upload.mutate(file, {
      onError: (err) => setError(isApiError(err) ? err.message : t("profile.resume.error")),
    });
  }

  function view() {
    if (!resume.data) return;
    const url = URL.createObjectURL(resume.data);
    window.open(url, "_blank");
    // ponytail: revoke after a minute — the new tab has loaded the blob by then.
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

  const hasResume = resume.isSuccess;

  return (
    <Section title={t("profile.resume.title")}>
      <div className="flex flex-wrap items-center gap-3">
        {resume.isLoading ? (
          <Skeleton className="h-5 w-32 rounded" />
        ) : hasResume ? (
          <>
            <span className="inline-flex items-center gap-1.5 text-sm font-medium text-[var(--color-charcoal)]">
              <FileText className="size-4" /> {t("profile.resume.onFile")}
            </span>
            <button
              type="button"
              onClick={view}
              className="text-sm font-medium text-[var(--color-link-blue)] hover:underline"
            >
              {t("profile.resume.view")}
            </button>
          </>
        ) : (
          <span className="text-sm text-[var(--color-slate)]">{t("profile.resume.none")}</span>
        )}

        <Button
          variant="outline"
          size="sm"
          className="ml-auto"
          onClick={pickFile}
          disabled={upload.isPending}
        >
          <Upload className="size-4" />
          {upload.isPending
            ? t("profile.resume.uploading")
            : hasResume
              ? t("profile.resume.replace")
              : t("profile.resume.upload")}
        </Button>
        <input ref={inputRef} type="file" accept="application/pdf" hidden onChange={onFileChange} />
      </div>
      {error && <p className="mt-2 text-sm text-destructive">{error}</p>}
    </Section>
  );
}

function Education({ profile }: { profile: CandidateProfileDTO }) {
  const { t, tn, lang } = useT();
  const field = fieldsOptions.find((o) => o.value === profile.fieldOfStudy)?.label ?? profile.fieldOfStudy;
  return (
    <Section title={t("profile.education")}>
      <div className="grid gap-3 sm:grid-cols-2">
        <Row label={t("profile.fieldOfStudy")}>{field}</Row>
        <Row label={t("profile.graduationYear")}>{profile.graduationYear}</Row>
        {profile.studentId && <Row label={t("profile.studentId")}>{profile.studentId}</Row>}
        {profile.experienceYears != null && (
          <Row label={t("detail.experience")}>
            {tn(profile.experienceYears, "detail.experienceYears.one", "detail.experienceYears.other")}
          </Row>
        )}
        {profile.dateOfBirth && (
          <Row label={t("profile.dateOfBirth")}>
            {new Date(profile.dateOfBirth).toLocaleDateString(lang, {
              day: "numeric",
              month: "short",
              year: "numeric",
            })}
          </Row>
        )}
      </div>
    </Section>
  );
}

function Skills({ profile }: { profile: CandidateProfileDTO }) {
  const { t } = useT();
  return (
    <Section title={t("detail.skills")}>
      <div className="flex flex-wrap gap-1.5">
        {profile.skills.map((skill) => (
          <span
            key={skill}
            className="rounded-sm bg-[var(--color-tint-lavender)] px-2.5 py-1 text-[13px] font-medium text-[var(--color-brand-purple-800)]"
          >
            {skill}
          </span>
        ))}
      </div>
    </Section>
  );
}

function Links({ profile }: { profile: CandidateProfileDTO }) {
  const { t } = useT();
  const links = [
    { url: profile.linkedinUrl, label: "LinkedIn" },
    { url: profile.githubUrl, label: "GitHub" },
    { url: profile.portfolioUrl, label: "Portfolio" },
  ].filter((l) => l.url);

  if (links.length === 0) return null;
  return (
    <Section title={t("profile.links")}>
      <div className="flex flex-wrap gap-4">
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
    </Section>
  );
}

function About({ profile }: { profile: CandidateProfileDTO }) {
  const { t } = useT();
  return (
    <Section title={t("profile.about")}>
      <p className="whitespace-pre-line text-[15px] leading-relaxed text-[var(--color-charcoal)]">
        {profile.bio}
      </p>
    </Section>
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
