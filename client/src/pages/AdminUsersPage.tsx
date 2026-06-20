// Admin moderation list for every user account (/admin/users). Filter by status
// and/or type, page through the results, and act on a row: PENDING users can be
// approved or rejected, ACTIVE users suspended, SUSPENDED users reactivated. Each
// action opens the reason dialog and invalidates the admin lists on success, so the
// row's status (and available actions) update automatically.

import { useState } from "react";
import { CloudOff, RefreshCw } from "lucide-react";
import {
  useAdminUsers,
  useApproveUser,
  useRejectUser,
  useSuspendUser,
  useReactivateUser,
} from "@/features/admin/hooks";
import { useT } from "@/features/i18n/lang-context";
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
import { ReasonDialog } from "@/components/admin/ReasonDialog";
import { userStatusOptions, userTypeOptions } from "@/types/enums";
import type { UserStatus, UserType } from "@/types/enums";
import type { AdminUserDTO, AdminUserFilters } from "@/types/admin";

// Badge colour per status. Labels come from userStatusOptions (English for now,
// like the rest of the enum labels).
const STATUS_STYLES: Record<UserStatus, string> = {
  ACTIVE: "bg-[color-mix(in_srgb,var(--color-brand-green)_14%,#fff)] text-[var(--color-brand-green)]",
  PENDING: "bg-[var(--color-tint-sky)] text-[var(--color-link-blue-pressed)]",
  SUSPENDED: "bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]",
  REJECTED: "bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]",
  INACTIVE: "bg-[var(--color-surface)] text-[var(--color-stone)]",
  DELETED: "bg-[var(--color-surface)] text-[var(--color-stone)]",
};

export function AdminUsersPage() {
  const { t, tn } = useT();
  const [status, setStatus] = useState<UserStatus | "ALL">("ALL");
  const [type, setType] = useState<UserType | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const filters: AdminUserFilters = {
    status: status === "ALL" ? undefined : status,
    type: type === "ALL" ? undefined : type,
    page,
  };
  const hasActiveFilters = status !== "ALL" || type !== "ALL";

  const { data, isLoading, isError, refetch } = useAdminUsers(filters);

  function clearFilters() {
    setStatus("ALL");
    setType("ALL");
    setPage(0);
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("admin.users.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("admin.users.subtitle")}</p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        <Select
          value={status}
          onValueChange={(value) => {
            setStatus(value as UserStatus | "ALL");
            setPage(0);
          }}
        >
          <SelectTrigger className="min-w-40">
            <SelectValue placeholder={t("admin.filter.status")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t("admin.filter.allStatuses")}</SelectItem>
            {userStatusOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={type}
          onValueChange={(value) => {
            setType(value as UserType | "ALL");
            setPage(0);
          }}
        >
          <SelectTrigger className="min-w-40">
            <SelectValue placeholder={t("admin.filter.type")} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">{t("admin.filter.allTypes")}</SelectItem>
            {userTypeOptions.map((option) => (
              <SelectItem key={option.value} value={option.value}>
                {option.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={clearFilters}>
            {t("admin.filter.clear")}
          </Button>
        )}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-lg" />
          ))}
        </div>
      ) : isError || !data ? (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-12 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
          <Button className="mt-5" onClick={() => refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      ) : data.empty ? (
        <p className="rounded-lg border border-border bg-card px-5 py-8 text-center text-sm text-[var(--color-slate)]">
          {hasActiveFilters ? t("admin.users.emptyFiltered") : t("admin.users.empty")}
        </p>
      ) : (
        <>
          <p className="text-sm text-[var(--color-steel)]">
            {tn(data.totalElements, "admin.users.count.one", "admin.users.count.other")}
          </p>
          <div className="space-y-3">
            {data.content.map((user) => (
              <UserRow key={user.id} user={user} />
            ))}
          </div>

          {data.totalPages > 1 && (
            <nav className="flex items-center justify-center gap-3">
              <Button variant="outline" size="sm" disabled={data.first} onClick={() => setPage(page - 1)}>
                {t("admin.pager.prev")}
              </Button>
              <span className="text-sm text-[var(--color-steel)]">
                {t("admin.pager.page", { n: data.number + 1, total: data.totalPages })}
              </span>
              <Button variant="outline" size="sm" disabled={data.last} onClick={() => setPage(page + 1)}>
                {t("admin.pager.next")}
              </Button>
            </nav>
          )}
        </>
      )}
    </div>
  );
}

function UserRow({ user }: { user: AdminUserDTO }) {
  const type = userTypeOptions.find((o) => o.value === user.userType)?.label ?? user.userType;
  const statusLabel =
    userStatusOptions.find((o) => o.value === user.userStatus)?.label ?? user.userStatus;

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-5">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">
            {user.firstName} {user.lastName}
          </h3>
          <Badge variant="outline">{type}</Badge>
          <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[user.userStatus]}`}>
            {statusLabel}
          </span>
        </div>
        <a className="mt-1.5 block text-sm text-[var(--color-slate)] hover:underline" href={`mailto:${user.email}`}>
          {user.email}
        </a>
      </div>

      <div className="flex shrink-0 gap-2">
        <UserRowActions user={user} />
      </div>
    </article>
  );
}

// Which lifecycle actions a row offers depends on the user's current status. All
// four hooks are created unconditionally (rules of hooks); only the relevant
// dialogs render.
function UserRowActions({ user }: { user: AdminUserDTO }) {
  const { t } = useT();
  const approve = useApproveUser();
  const reject = useRejectUser();
  const suspend = useSuspendUser();
  const reactivate = useReactivateUser();

  if (user.userStatus === "PENDING") {
    return (
      <>
        <ReasonDialog
          id={user.id}
          action={approve}
          triggerLabel={t("admin.approve")}
          title={t("admin.approveUser")}
          confirmLabel={t("admin.approve")}
        />
        <ReasonDialog
          id={user.id}
          action={reject}
          triggerLabel={t("admin.reject")}
          triggerVariant="destructive"
          title={t("admin.rejectUser")}
          confirmLabel={t("admin.reject")}
          confirmVariant="destructive"
        />
      </>
    );
  }

  if (user.userStatus === "ACTIVE") {
    return (
      <ReasonDialog
        id={user.id}
        action={suspend}
        triggerLabel={t("admin.suspend")}
        triggerVariant="destructive"
        title={t("admin.suspendUser")}
        confirmLabel={t("admin.suspend")}
        confirmVariant="destructive"
      />
    );
  }

  if (user.userStatus === "SUSPENDED") {
    return (
      <ReasonDialog
        id={user.id}
        action={reactivate}
        triggerLabel={t("admin.reactivate")}
        title={t("admin.reactivateUser")}
        confirmLabel={t("admin.reactivate")}
      />
    );
  }

  return null;
}
