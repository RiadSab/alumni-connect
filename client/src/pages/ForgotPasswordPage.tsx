// Forgot-password flow, public at /forgot-password. Two steps in one page:
// request a code by email, then enter the code + a new password.

import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { KeyRound, Mail } from "lucide-react";
import { useForgotPassword, useResetPassword } from "@/features/auth/hooks";
import { useAuth } from "@/features/auth/auth-context";
import { AuthScreen } from "@/features/auth/AuthScreen";
import { AuthBrand, AuthField, AuthPasswordField } from "@/features/auth/fields";
import { useT } from "@/features/i18n/lang-context";
import { forgotPasswordSchema, resetPasswordSchema } from "@/types/auth";
import { isApiError } from "@/lib/http";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

const cardClass = "w-full max-w-sm bg-card/95 shadow-[var(--shadow-2)] backdrop-blur-sm";

export function ForgotPasswordPage() {
  const auth = useAuth();
  const { t } = useT();
  const [step, setStep] = useState<"request" | "reset">("request");
  const [email, setEmail] = useState("");
  const [done, setDone] = useState(false);

  if (auth.isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  if (done) {
    return (
      <AuthScreen>
        <Card className={cardClass}>
          <CardContent className="flex flex-col gap-6 p-7">
            <AuthBrand title={t("auth.forgot.doneTitle")} subtitle={t("auth.forgot.doneSubtitle")} />
            <Button asChild className="h-10 w-full">
              <Link to="/login">{t("auth.forgot.backToLogin")}</Link>
            </Button>
          </CardContent>
        </Card>
      </AuthScreen>
    );
  }

  return (
    <AuthScreen>
      <Card className={cardClass}>
        <CardContent className="flex flex-col gap-6 p-7">
          {step === "request" ? (
            <RequestStep email={email} setEmail={setEmail} onSent={() => setStep("reset")} />
          ) : (
            <ResetStep email={email} onDone={() => setDone(true)} />
          )}
        </CardContent>
      </Card>
    </AuthScreen>
  );
}

// Step 1 — ask for the email. The response is the same whether or not the email is
// registered (no enumeration), so we always advance to the code step.
function RequestStep({
  email,
  setEmail,
  onSent,
}: {
  email: string;
  setEmail: (value: string) => void;
  onSent: () => void;
}) {
  const { t } = useT();
  const forgot = useForgotPassword();
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    const result = forgotPasswordSchema.safeParse({ email });
    if (!result.success) {
      setError(t("auth.forgot.invalidEmail"));
      return;
    }
    forgot.mutate(result.data, {
      onSuccess: onSent,
      onError: (err) => setError(isApiError(err) ? err.message : t("auth.error.generic")),
    });
  }

  return (
    <>
      <AuthBrand title={t("auth.forgot.requestTitle")} subtitle={t("auth.forgot.requestSubtitle")} />
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        <AuthField
          id="email"
          label={t("auth.field.email")}
          icon={Mail}
          type="email"
          value={email}
          autoComplete="email"
          onChange={(event) => setEmail(event.target.value)}
          error={error ?? undefined}
        />

        <Button type="submit" className="h-10" disabled={forgot.isPending}>
          {forgot.isPending ? t("auth.forgot.sending") : t("auth.forgot.sendCode")}
        </Button>

        <p className="text-center text-sm text-muted-foreground">
          {t("auth.forgot.rememberedIt")}{" "}
          <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
            {t("auth.forgot.backToLogin")}
          </Link>
        </p>
      </form>
    </>
  );
}

// Step 2 — enter the emailed code and a new password.
function ResetStep({ email, onDone }: { email: string; onDone: () => void }) {
  const { t } = useT();
  const reset = useResetPassword();
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{ code?: string; newPassword?: string; confirmPassword?: string }>({});
  const [formError, setFormError] = useState<string | null>(null);

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    const result = resetPasswordSchema.safeParse({ email, code, newPassword });
    const errors: typeof fieldErrors = {};
    if (!result.success) {
      for (const issue of result.error.issues) {
        const field = issue.path[0];
        if (field === "code" && !errors.code) errors.code = issue.message;
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

    reset.mutate(
      { email, code, newPassword },
      {
        onSuccess: onDone,
        onError: (err) => setFormError(isApiError(err) ? err.message : t("auth.error.generic")),
      },
    );
  }

  return (
    <>
      <AuthBrand
        title={t("auth.forgot.resetTitle")}
        subtitle={t("auth.forgot.resetSubtitle", { email })}
      />
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        <AuthField
          id="code"
          label={t("auth.forgot.resetCode")}
          icon={KeyRound}
          inputMode="numeric"
          value={code}
          autoComplete="one-time-code"
          onChange={(event) => setCode(event.target.value)}
          error={fieldErrors.code}
        />
        <AuthPasswordField
          id="newPassword"
          label={t("auth.forgot.newPassword")}
          value={newPassword}
          autoComplete="new-password"
          onChange={(event) => setNewPassword(event.target.value)}
          error={fieldErrors.newPassword}
        />
        <AuthPasswordField
          id="confirmPassword"
          label={t("auth.forgot.confirmNewPassword")}
          value={confirmPassword}
          autoComplete="new-password"
          onChange={(event) => setConfirmPassword(event.target.value)}
          error={fieldErrors.confirmPassword}
        />

        {formError && <p className="text-sm text-destructive">{formError}</p>}

        <Button type="submit" className="h-10" disabled={reset.isPending}>
          {reset.isPending ? t("auth.forgot.resetting") : t("auth.forgot.resetPassword")}
        </Button>
      </form>
    </>
  );
}
