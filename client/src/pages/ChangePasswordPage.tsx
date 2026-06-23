// Change-password form, under /settings/password. Any logged-in user can change
// their own password. The backend returns 400 if the current password is wrong;
// that message is surfaced as the form error.

import { useState } from "react";
import { useChangePassword } from "@/features/auth/hooks";
import { AuthPasswordField } from "@/features/auth/fields";
import { useT } from "@/features/i18n/lang-context";
import { changePasswordSchema } from "@/types/auth";
import { isApiError } from "@/lib/http";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export function ChangePasswordPage() {
  const { t } = useT();
  const change = useChangePassword();

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{
    oldPassword?: string;
    newPassword?: string;
    confirmPassword?: string;
  }>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);
    setSuccess(false);

    const result = changePasswordSchema.safeParse({ oldPassword, newPassword });
    const errors: typeof fieldErrors = {};
    if (!result.success) {
      for (const issue of result.error.issues) {
        const field = issue.path[0];
        if (field === "oldPassword" && !errors.oldPassword) errors.oldPassword = issue.message;
        if (field === "newPassword" && !errors.newPassword) errors.newPassword = issue.message;
      }
    }
    if (newPassword !== confirmPassword) {
      errors.confirmPassword = t("auth.common.passwordsDontMatch");
    }
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    change.mutate(
      { oldPassword, newPassword },
      {
        onSuccess: () => {
          setSuccess(true);
          setOldPassword("");
          setNewPassword("");
          setConfirmPassword("");
        },
        onError: (error) => {
          setFormError(isApiError(error) ? error.message : t("auth.error.generic"));
        },
      },
    );
  }

  return (
    <div className="mx-auto max-w-sm">
      <Card>
        <CardHeader>
          <CardTitle>{t("auth.change.title")}</CardTitle>
          <CardDescription>{t("auth.change.subtitle")}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <AuthPasswordField
              id="oldPassword"
              label={t("auth.change.currentPassword")}
              value={oldPassword}
              autoComplete="current-password"
              onChange={(event) => setOldPassword(event.target.value)}
              error={fieldErrors.oldPassword}
            />
            <AuthPasswordField
              id="newPassword"
              label={t("auth.change.newPassword")}
              value={newPassword}
              autoComplete="new-password"
              onChange={(event) => setNewPassword(event.target.value)}
              error={fieldErrors.newPassword}
            />
            <AuthPasswordField
              id="confirmPassword"
              label={t("auth.change.confirmNewPassword")}
              value={confirmPassword}
              autoComplete="new-password"
              onChange={(event) => setConfirmPassword(event.target.value)}
              error={fieldErrors.confirmPassword}
            />

            {formError && <p className="text-sm text-destructive">{formError}</p>}
            {success && <p className="text-sm text-success">{t("auth.change.changed")}</p>}

            <Button type="submit" disabled={change.isPending}>
              {change.isPending ? t("auth.change.saving") : t("auth.change.submit")}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
