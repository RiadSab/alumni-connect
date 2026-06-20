// Admin moderation queue: users and companies awaiting approval, each with
// Approve / Reject actions that open the reason dialog. Reached at /admin/pending.
// Approving or rejecting invalidates the admin lists, so the row drops out here.

import type { ReactNode } from "react";
import { CloudOff, RefreshCw } from "lucide-react";
import {
  usePendingUsers,
  usePendingCompanies,
  useApproveUser,
  useRejectUser,
  useApproveCompany,
  useRejectCompany,
} from "@/features/admin/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ReasonDialog } from "@/components/admin/ReasonDialog";
import { userTypeOptions, fieldsOptions, companySizeOptions } from "@/types/enums";
import type { AdminUserDTO, AdminCompanyDTO } from "@/types/admin";

export function AdminPendingPage() {
  const { t } = useT();

  return (
    <div className="mx-auto max-w-3xl space-y-10">
      <div>
        <h1 className="text-2xl font-semibold text-foreground">{t("admin.pending.title")}</h1>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("admin.pending.subtitle")}</p>
      </div>

      <PendingUsers />
      <PendingCompanies />
    </div>
  );
}

function PendingUsers() {
  const { t, tn } = useT();
  // ponytail: size 100 loads the whole queue in one shot; paginate if the pending
  // backlog ever grows past that.
  const query = usePendingUsers({ size: 100 });

  return (
    <Section
      title={t("admin.pending.users")}
      query={query}
      count={(n) => tn(n, "admin.pending.users.count.one", "admin.pending.users.count.other")}
      empty={t("admin.pending.users.empty")}
      renderRow={(user) => <PendingUserRow key={user.id} user={user} />}
    />
  );
}

function PendingCompanies() {
  const { t, tn } = useT();
  // ponytail: same size-100 cap as the users queue above.
  const query = usePendingCompanies({ size: 100 });

  return (
    <Section
      title={t("admin.pending.companies")}
      query={query}
      count={(n) => tn(n, "admin.pending.companies.count.one", "admin.pending.companies.count.other")}
      empty={t("admin.pending.companies.empty")}
      renderRow={(company) => <PendingCompanyRow key={company.id} company={company} />}
    />
  );
}

// Shared section shell: heading + loading / error / empty / list states. Kept
// generic over the row type so both queues reuse it.
function Section<T>({
  title,
  query,
  count,
  empty,
  renderRow,
}: {
  title: string;
  query: {
    isLoading: boolean;
    isError: boolean;
    data?: { content: T[]; totalElements: number };
    refetch: () => void;
  };
  count: (n: number) => string;
  empty: string;
  renderRow: (item: T) => ReactNode;
}) {
  const { t } = useT();

  return (
    <section>
      <h2 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[var(--color-stone)]">
        {title}
      </h2>

      {query.isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-lg" />
          <Skeleton className="h-20 w-full rounded-lg" />
        </div>
      ) : query.isError || !query.data ? (
        <div className="flex flex-col items-center rounded-lg border border-border bg-card px-8 py-12 text-center">
          <div className="mb-4 grid size-14 place-items-center rounded-xl bg-[color-mix(in_srgb,var(--color-error)_12%,#fff)] text-[var(--color-error)]">
            <CloudOff className="size-6" />
          </div>
          <h3 className="text-lg font-semibold text-foreground">{t("board.error.title")}</h3>
          <p className="mt-2 max-w-sm text-sm text-[var(--color-steel)]">{t("board.error.body")}</p>
          <Button className="mt-5" onClick={() => query.refetch()}>
            <RefreshCw className="size-4" /> {t("board.retry")}
          </Button>
        </div>
      ) : query.data.content.length === 0 ? (
        <p className="rounded-lg border border-border bg-card px-5 py-8 text-center text-sm text-[var(--color-slate)]">
          {empty}
        </p>
      ) : (
        <>
          <p className="mb-3 text-sm text-[var(--color-steel)]">{count(query.data.totalElements)}</p>
          <div className="space-y-3">{query.data.content.map(renderRow)}</div>
        </>
      )}
    </section>
  );
}

function PendingUserRow({ user }: { user: AdminUserDTO }) {
  const { t } = useT();
  const approve = useApproveUser();
  const reject = useRejectUser();
  const type = userTypeOptions.find((o) => o.value === user.userType)?.label ?? user.userType;

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-5">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">
            {user.firstName} {user.lastName}
          </h3>
          <Badge variant="outline">{type}</Badge>
        </div>
        <a className="mt-1.5 block text-sm text-[var(--color-slate)] hover:underline" href={`mailto:${user.email}`}>
          {user.email}
        </a>
      </div>

      <div className="flex shrink-0 gap-2">
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
      </div>
    </article>
  );
}

function PendingCompanyRow({ company }: { company: AdminCompanyDTO }) {
  const { t } = useT();
  const approve = useApproveCompany();
  const reject = useRejectCompany();
  const field = fieldsOptions.find((o) => o.value === company.field)?.label ?? company.field;
  const size = company.size
    ? companySizeOptions.find((o) => o.value === company.size)?.label ?? company.size
    : null;

  return (
    <article className="flex items-start justify-between gap-4 rounded-lg border border-border bg-card p-5">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold leading-tight text-foreground">{company.name}</h3>
          <Badge variant="outline">{field}</Badge>
        </div>
        <div className="mt-1.5 flex flex-wrap items-center gap-2.5 text-sm text-[var(--color-slate)]">
          <a className="hover:underline" href={`mailto:${company.email}`}>{company.email}</a>
          {size && <span>{size}</span>}
        </div>
      </div>

      <div className="flex shrink-0 gap-2">
        <ReasonDialog
          id={company.id}
          action={approve}
          triggerLabel={t("admin.approve")}
          title={t("admin.approveCompany")}
          confirmLabel={t("admin.approve")}
        />
        <ReasonDialog
          id={company.id}
          action={reject}
          triggerLabel={t("admin.reject")}
          triggerVariant="destructive"
          title={t("admin.rejectCompany")}
          confirmLabel={t("admin.reject")}
          confirmVariant="destructive"
        />
      </div>
    </article>
  );
}
