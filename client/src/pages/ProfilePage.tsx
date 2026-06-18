// Candidate's own profile, read-only. Fetches /candidates/me and lays it out as
// cards. Editing and résumé/photo upload land in later commits.

import { CloudOff, ExternalLink, RefreshCw } from "lucide-react";
import { useMyCandidateProfile } from "@/features/candidates/hooks";
import { useT } from "@/features/i18n/lang-context";
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
      <Education profile={profile} />
      {profile.skills.length > 0 && <Skills profile={profile} />}
      <Links profile={profile} />
      {profile.bio && <About profile={profile} />}
    </div>
  );
}

function ProfileHeader({ profile }: { profile: CandidateProfileDTO }) {
  const { t } = useT();
  const role = [profile.currentJobTitle, profile.currentCompany].filter(Boolean).join(" · ");
  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <div className="flex gap-4">
        <div className="grid size-16 shrink-0 place-items-center rounded-full bg-primary text-2xl font-bold text-primary-foreground">
          {profile.firstName.charAt(0)}
          {profile.lastName.charAt(0)}
        </div>
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold leading-tight text-foreground">
            {profile.firstName} {profile.lastName}
          </h1>
          {role && <p className="mt-1 text-sm text-[var(--color-slate)]">{role}</p>}
        </div>
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
