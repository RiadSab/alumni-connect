// The 24h "are you still coming?" link (/mentoring/confirm/:token). Confirming is a button, not the
// link itself — a mail scanner opening the link would otherwise answer for the student.

import { CheckCircle2 } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { useConfirmSession, useSessionConfirmDetails } from "@/features/mentoring/hooks";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { mentoringTrackOptions } from "@/types/enums";

export function ConfirmSessionPage() {
  const { t, lang } = useT();
  const params = useParams<{ token: string }>();
  const token = params.token ?? "";
  const { data, isLoading, isError } = useSessionConfirmDetails(token);
  const confirm = useConfirmSession();

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
        <p className="text-sm text-destructive">{t("confirmSession.invalid")}</p>
        <Button className="mt-4" asChild>
          <Link to="/mentoring">{t("confirmSession.browse")}</Link>
        </Button>
      </div>
    );
  }

  if (confirm.isSuccess || data.state === "CONFIRMED") {
    return (
      <div className="mx-auto max-w-md rounded-lg border border-border bg-card p-6 text-center">
        <CheckCircle2 className="mx-auto size-8 text-[var(--color-brand-green)]" />
        <p className="mt-3 text-sm font-medium text-foreground">{t("confirmSession.done")}</p>
        <p className="mt-1 text-sm text-[var(--color-slate)]">{t("confirmSession.joinNote")}</p>
      </div>
    );
  }

  const track =
    mentoringTrackOptions.find((o) => o.value === data.slot.track)?.label ?? data.slot.track;

  return (
    <div className="mx-auto max-w-md rounded-lg border border-border bg-card p-6">
      <h1 className="text-xl font-semibold text-foreground">{t("confirmSession.title")}</h1>
      <p className="mt-1 text-sm text-[var(--color-slate)]">{t("confirmSession.subtitle")}</p>

      <div className="mt-4 rounded-md border border-border bg-[var(--color-surface)] p-4">
        <p className="text-base font-medium text-foreground">{track}</p>
        <p className="mt-0.5 text-sm text-[var(--color-steel)]">
          {new Date(data.slot.startsAt).toLocaleString(lang, {
            dateStyle: "full",
            timeStyle: "short",
          })}
        </p>
        <p className="mt-0.5 text-sm text-[var(--color-steel)]">{data.slot.mentorName}</p>
      </div>

      {confirm.isError && (
        <p className="mt-3 text-sm text-destructive">{t("confirmSession.invalid")}</p>
      )}

      <div className="mt-5 flex flex-wrap gap-3">
        <Button onClick={() => confirm.mutate(token)} disabled={confirm.isPending}>
          {t("confirmSession.confirm")}
        </Button>
        <Button variant="outline" asChild>
          <Link to="/mentoring">{t("confirmSession.manage")}</Link>
        </Button>
      </div>
    </div>
  );
}
