// The reason prompt shared by every admin lifecycle action (approve / reject /
// suspend / reactivate). It renders its own trigger button; clicking it opens a
// dialog with a required "reason" field, then fires the passed-in mutation. The
// backend records that reason as the account's statusChangeReason.
//
// Each row creates its own mutation instances and hands them in, so pending/error
// state stays isolated per row. The mutation already invalidates the admin lists on
// success, which is what makes the approved/rejected row drop out of the list.

import { useState } from "react";
import type { UseMutationResult } from "@tanstack/react-query";
import { useT } from "@/features/i18n/lang-context";
import { isApiError } from "@/lib/http";
import { statusChangeSchema } from "@/types/admin";
import type { StatusChangeDTO } from "@/types/admin";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogTrigger,
  DialogClose,
  DialogContent,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";

// Every approve/reject/suspend/reactivate hook returns exactly this shape.
export type StatusChangeAction = UseMutationResult<
  string,
  Error,
  { id: number; body: StatusChangeDTO },
  unknown
>;

type Variant = "default" | "outline" | "destructive";

export function ReasonDialog({
  id,
  action,
  triggerLabel,
  triggerVariant = "default",
  title,
  confirmLabel,
  confirmVariant = "default",
}: {
  id: number;
  action: StatusChangeAction;
  triggerLabel: string;
  triggerVariant?: Variant;
  title: string;
  confirmLabel: string;
  confirmVariant?: Variant;
}) {
  const { t } = useT();
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);

  function handleOpenChange(next: boolean) {
    setOpen(next);
    if (!next) {
      // Reset on close so reopening starts clean (no stale text or prior error).
      setReason("");
      setFieldError(null);
      action.reset();
    }
  }

  function handleConfirm() {
    const parsed = statusChangeSchema.safeParse({ reason });
    if (!parsed.success) {
      setFieldError(t("admin.reason.required"));
      return;
    }
    setFieldError(null);
    action.mutate(
      { id, body: parsed.data },
      { onSuccess: () => handleOpenChange(false) },
    );
  }

  const serverError = action.error
    ? isApiError(action.error)
      ? action.error.message
      : t("admin.actionError")
    : null;

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button variant={triggerVariant} size="sm">
          {triggerLabel}
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogTitle>{title}</DialogTitle>
        <DialogDescription>{t("admin.reason.prompt")}</DialogDescription>

        <textarea
          rows={3}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          placeholder={t("admin.reason.placeholder")}
          className="mt-4 w-full rounded-lg border border-input bg-transparent px-2.5 py-1.5 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
        {fieldError && <p className="mt-1.5 text-sm text-destructive">{fieldError}</p>}
        {serverError && <p className="mt-1.5 text-sm text-destructive">{serverError}</p>}

        <DialogFooter>
          <DialogClose asChild>
            <Button variant="ghost" size="sm">
              {t("admin.cancel")}
            </Button>
          </DialogClose>
          <Button
            variant={confirmVariant}
            size="sm"
            onClick={handleConfirm}
            disabled={action.isPending}
          >
            {action.isPending ? t("admin.saving") : confirmLabel}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
