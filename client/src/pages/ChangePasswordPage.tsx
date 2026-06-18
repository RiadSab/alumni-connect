// Change-password form, under /settings/password. Any logged-in user can change
// their own password (candidate, company, member). Plain controlled inputs —
// same pattern as LoginPage. The backend returns 400 if the current password is
// wrong; that message is surfaced as the form error.
//
// English labels for now (form i18n is a later pass, same as login/register).

import { useState } from "react";
import { useChangePassword } from "@/features/auth/hooks";
import { changePasswordSchema } from "@/types/auth";
import { isApiError } from "@/lib/http";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export function ChangePasswordPage() {
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

    // Validate old + new with the schema, then a client-only "confirm matches".
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
      errors.confirmPassword = "Passwords don't match.";
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
          setFormError(
            isApiError(error) ? error.message : "Something went wrong. Please try again.",
          );
        },
      },
    );
  }

  return (
    <div className="mx-auto max-w-sm">
      <Card>
        <CardHeader>
          <CardTitle>Change password</CardTitle>
          <CardDescription>Update the password you use to log in.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <PasswordField
              id="oldPassword"
              label="Current password"
              value={oldPassword}
              onChange={setOldPassword}
              autoComplete="current-password"
              error={fieldErrors.oldPassword}
            />
            <PasswordField
              id="newPassword"
              label="New password"
              value={newPassword}
              onChange={setNewPassword}
              autoComplete="new-password"
              error={fieldErrors.newPassword}
            />
            <PasswordField
              id="confirmPassword"
              label="Confirm new password"
              value={confirmPassword}
              onChange={setConfirmPassword}
              autoComplete="new-password"
              error={fieldErrors.confirmPassword}
            />

            {formError && <p className="text-sm text-destructive">{formError}</p>}
            {success && <p className="text-sm text-success">Password changed.</p>}

            <Button type="submit" disabled={change.isPending}>
              {change.isPending ? "Saving..." : "Change password"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

// label + password input + error trio.
function PasswordField({
  id,
  label,
  value,
  onChange,
  autoComplete,
  error,
}: {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete: string;
  error?: string;
}) {
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <Input
        id={id}
        type="password"
        value={value}
        autoComplete={autoComplete}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={error !== undefined}
      />
      {error && <p className="text-sm text-destructive">{error}</p>}
    </div>
  );
}
