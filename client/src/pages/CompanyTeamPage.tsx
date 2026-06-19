// The company's team roster. Any company user can view it (GET /company-users,
// scoped to the caller's company). An OWNER can change another member's role between
// MEMBER and RECRUITER (PATCH /company-users/{id}/role) — never OWNER, never their
// own. Reached at /company/team.

import { useState } from "react";
import { CloudOff, RefreshCw } from "lucide-react";
import { useCompanyRoster, useMyCompanyUserProfile, useChangeMemberRole } from "@/features/companyUsers/hooks";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
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
import { companyRoleOptions, companyUserPositionOptions } from "@/types/enums";
import type { CompanyRole } from "@/types/enums";
import type { CompanyUserProfileDTO } from "@/types/companyUser";

// An OWNER can only assign these (OWNER is granted separately, not here).
const assignableRoles = companyRoleOptions.filter((o) => o.value !== "OWNER");

function roleVariant(role: CompanyRole): "default" | "secondary" | "outline" {
  if (role === "OWNER") return "default";
  if (role === "RECRUITER") return "outline";
  return "secondary";
}

export function CompanyTeamPage() {
  const { t, tn } = useT();
  const me = useMyCompanyUserProfile();
  // ponytail: size 100 loads the whole roster in one shot; paginate if a company
  // ever has more than that many members.
  const roster = useCompanyRoster({ size: 100 });

  const isOwner = me.data?.companyRole === "OWNER";
  const myId = me.data?.id;

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-foreground">{t("team.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("team.subtitle")}</p>
      </div>

      {roster.isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-lg" />
          <Skeleton className="h-20 w-full rounded-lg" />
        </div>
      ) : roster.isError || !roster.data ? (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-16 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
          <Button className="mt-5" onClick={() => roster.refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      ) : (
        <>
          <p className="mb-3 text-sm text-[var(--color-steel)]">
            {tn(roster.data.totalElements, "team.count.one", "team.count.other")}
          </p>
          <div className="space-y-3">
            {roster.data.content.map((member) => (
              <MemberRow
                key={member.id}
                member={member}
                canManage={isOwner === true && member.id !== myId && member.companyRole !== "OWNER"}
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

function MemberRow({ member, canManage }: { member: CompanyUserProfileDTO; canManage: boolean }) {
  const { t } = useT();
  const changeRole = useChangeMemberRole();
  const [error, setError] = useState<string | null>(null);

  const role = companyRoleOptions.find((o) => o.value === member.companyRole)?.label ?? member.companyRole;
  const position =
    companyUserPositionOptions.find((o) => o.value === member.position)?.label ?? member.position;

  function onRoleChange(value: string) {
    setError(null);
    changeRole.mutate(
      { id: member.id, body: { role: value as CompanyRole } },
      { onError: (e) => setError(isApiError(e) ? e.message : t("team.roleError")) },
    );
  }

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-5">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">
            {member.firstName} {member.lastName}
          </h3>
          <Badge variant={roleVariant(member.companyRole)}>{role}</Badge>
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-2.5 text-sm text-[var(--color-slate)]">
          <a className="hover:underline" href={`mailto:${member.email}`}>{member.email}</a>
          <span>{position}</span>
        </div>
        {error && <p className="mt-1.5 text-sm text-destructive">{error}</p>}
      </div>

      {canManage && (
        <div className="w-40 shrink-0">
          <Select
            value={member.companyRole}
            onValueChange={onRoleChange}
            disabled={changeRole.isPending}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {assignableRoles.map((option) => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}
    </article>
  );
}
