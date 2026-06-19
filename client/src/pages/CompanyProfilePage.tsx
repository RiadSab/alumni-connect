// A company user's own company profile, read-only. There is no GET /companies/me,
// so we read the logged-in company user first (/company-users/me) to get the
// companyId, then load that company (/companies/{id}). Reached via /profile when a
// COMPANY_USER is logged in (ProfilePage dispatches by user type). Editing and logo
// upload (OWNER only) land in the next commit.

import { Link } from "react-router-dom";
import { Building2, CloudOff, ExternalLink, RefreshCw } from "lucide-react";
import { useMyCompanyUserProfile } from "@/features/companyUsers/hooks";
import { useCompany } from "@/features/companies/hooks";
import { companiesApi } from "@/api/companies";
import { useT } from "@/features/i18n/lang-context";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { companySizeOptions, companyStatusOptions, fieldsOptions } from "@/types/enums";
import type { CompanyDTO } from "@/types/company";

export function CompanyProfilePage() {
  const { t } = useT();
  const me = useMyCompanyUserProfile();
  // Until `me` resolves, companyId is undefined; useCompany is disabled for a
  // non-finite id, so it won't fire until we have a real id.
  const company = useCompany(me.data?.companyId ?? NaN);

  const isLoading = me.isLoading || company.isLoading;
  const isError = me.isError || company.isError;

  if (isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Skeleton className="h-28 w-full rounded-lg" />
        <Skeleton className="h-40 w-full rounded-lg" />
      </div>
    );
  }

  if (isError || !company.data) {
    return (
      <div className="mx-auto max-w-3xl">
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("company.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("company.error.body")}</p>
          <Button className="mt-5" onClick={() => company.refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      </div>
    );
  }

  const profile = company.data;
  // Only the company OWNER can edit; the backend enforces this too (403).
  const isOwner = me.data?.companyRole === "OWNER";

  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <CompanyHeader profile={profile} isOwner={isOwner} />
      <Details profile={profile} />
      {profile.description && <About profile={profile} />}
    </div>
  );
}

function CompanyHeader({ profile, isOwner }: { profile: CompanyDTO; isOwner: boolean }) {
  const { t } = useT();
  const field = fieldsOptions.find((o) => o.value === profile.field)?.label ?? profile.field;

  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="flex min-w-0 items-start gap-4">
          <div className="size-16 shrink-0">
            {profile.logoId ? (
              // The logo endpoint is public, so it can go straight into <img src>
              // (no auth header / blob dance like the candidate photo needs).
              <img
                src={companiesApi.logoUrl(profile.id)}
                alt=""
                className="size-16 rounded-lg border border-border object-cover"
              />
            ) : (
              <div className="grid size-16 place-items-center rounded-lg bg-primary text-primary-foreground">
                <Building2 className="size-7" />
              </div>
            )}
          </div>
          <div className="min-w-0">
            <h1 className="text-2xl font-semibold leading-tight text-foreground">{profile.name}</h1>
            <p className="mt-1 text-sm text-[var(--color-slate)]">{field}</p>
          </div>
        </div>
        {isOwner && (
          <Button variant="outline" size="sm" asChild>
            <Link to="/profile/edit">{t("profile.edit")}</Link>
          </Button>
        )}
      </div>
      <div className="mt-5 grid gap-3 sm:grid-cols-2">
        <Row label={t("profile.email")}>
          <a className="text-[var(--color-link-blue)] hover:underline" href={`mailto:${profile.email}`}>
            {profile.email}
          </a>
        </Row>
        {profile.phone && <Row label={t("profile.phone")}>{profile.phone}</Row>}
      </div>
    </div>
  );
}

function Details({ profile }: { profile: CompanyDTO }) {
  const { t } = useT();
  const size = companySizeOptions.find((o) => o.value === profile.size)?.label ?? profile.size;
  const status = companyStatusOptions.find((o) => o.value === profile.status)?.label ?? profile.status;

  return (
    <Section title={t("company.details")}>
      <div className="grid gap-3 sm:grid-cols-2">
        {profile.size && <Row label={t("company.size")}>{size}</Row>}
        {profile.address && <Row label={t("company.address")}>{profile.address}</Row>}
        {profile.website && (
          <Row label={t("company.website")}>
            <a
              href={profile.website}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 text-[var(--color-link-blue)] hover:underline"
            >
              <ExternalLink className="size-4" /> {profile.website}
            </a>
          </Row>
        )}
        <Row label={t("company.status")}>{status}</Row>
      </div>
    </Section>
  );
}

function About({ profile }: { profile: CompanyDTO }) {
  const { t } = useT();
  return (
    <Section title={t("profile.about")}>
      <p className="whitespace-pre-line text-[15px] leading-relaxed text-[var(--color-charcoal)]">
        {profile.description}
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
