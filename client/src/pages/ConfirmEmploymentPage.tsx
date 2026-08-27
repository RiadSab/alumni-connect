// Landing page for the yearly nudge (/employment/confirm/:token). Confirming is a button, not the
// link itself, so a mail scanner opening the link can't answer on the alumnus's behalf.

import { CheckCircle2 } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import {
  useConfirmEmployment,
  useEmploymentConfirmDetails,
} from "@/features/employment/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { employmentStatusOptions } from "@/types/enums";

export function ConfirmEmploymentPage() {
  const { t, lang } = useT();
  const params = useParams<{ token: string }>();
  const token = params.token ?? "";
  const { data, isLoading, isError } = useEmploymentConfirmDetails(token);
  const confirm = useConfirmEmployment();

  if (isLoading) {
    return (
      <div className="mx-auto max-w-md">
        <Skeleton className="h-40 w-full rounded-lg" />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="mx-auto max-w-md rounded-lg border border-border bg-card p-6 text-center">
        <p className="text-sm text-destructive">{t("confirmEmployment.invalid")}</p>
        <Button className="mt-4" asChild>
          <Link to="/login">{t("confirmEmployment.signIn")}</Link>
        </Button>
      </div>
    );
  }

  if (confirm.isSuccess) {
    return (
      <div className="mx-auto max-w-md rounded-lg border border-border bg-card p-6 text-center">
        <CheckCircle2 className="mx-auto size-8 text-[var(--color-brand-green)]" />
        <p className="mt-3 text-sm font-medium text-foreground">{t("confirmEmployment.done")}</p>
      </div>
    );
  }

  const status = employmentStatusOptions.find((o) => o.value === data.status)?.label ?? data.status;
  const summary =
    data.status === "EMPLOYED" ? `${data.jobTitle} · ${data.employer}` : status;
  const since = t("confirmEmployment.since", {
    date: new Date(data.startedAt).toLocaleDateString(lang, { month: "long", year: "numeric" }),
  });

  return (
    <div className="mx-auto max-w-md rounded-lg border border-border bg-card p-6">
      <h1 className="text-xl font-semibold text-foreground">
        {t("confirmEmployment.greeting", { name: data.firstName })}
      </h1>
      <p className="mt-1 text-sm text-[var(--color-slate)]">{t("confirmEmployment.title")}</p>

      <div className="mt-4 rounded-md border border-border bg-[var(--color-surface)] p-4">
        <p className="text-base font-medium text-foreground">{summary}</p>
        <p className="mt-0.5 text-sm text-[var(--color-steel)]">{since}</p>
      </div>

      {confirm.isError && (
        <p className="mt-3 text-sm text-destructive">{t("confirmEmployment.invalid")}</p>
      )}

      <div className="mt-5 flex flex-wrap gap-3">
        <Button onClick={() => confirm.mutate(token)} disabled={confirm.isPending}>
          {t("confirmEmployment.confirm")}
        </Button>
        <Button variant="outline" asChild>
          <Link to="/profile/employment">{t("confirmEmployment.update")}</Link>
        </Button>
      </div>
    </div>
  );
}
