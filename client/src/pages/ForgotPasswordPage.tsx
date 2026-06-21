// Forgot-password flow, public at /forgot-password. Two steps in one page: request a
// code by email, then enter the code + a new password. English labels, matching the
// other auth forms (form i18n is a later pass).

import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { useForgotPassword, useResetPassword } from "@/features/auth/hooks";
import { useAuth } from "@/features/auth/auth-context";
import { forgotPasswordSchema, resetPasswordSchema } from "@/types/auth";
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

export function ForgotPasswordPage() {
  const auth = useAuth();
  const [step, setStep] = useState<"request" | "reset">("request");
  const [email, setEmail] = useState("");
  const [done, setDone] = useState(false);

  if (auth.isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  if (done) {
    return (
      <div className="mx-auto max-w-sm">
        <Card>
          <CardHeader>
            <CardTitle>Password reset</CardTitle>
            <CardDescription>Your password has been changed.</CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild className="w-full">
              <Link to="/login">Back to login</Link>
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-sm">
      {step === "request" ? (
        <RequestStep
          email={email}
          setEmail={setEmail}
          onSent={() => setStep("reset")}
        />
      ) : (
        <ResetStep email={email} onDone={() => setDone(true)} />
      )}
    </div>
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
  const forgot = useForgotPassword();
  const [error, setError] = useState<string | null>(null);

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    const result = forgotPasswordSchema.safeParse({ email });
    if (!result.success) {
      setError("Enter a valid email address.");
      return;
    }
    forgot.mutate(result.data, {
      onSuccess: onSent,
      onError: (err) =>
        setError(isApiError(err) ? err.message : "Something went wrong. Please try again."),
    });
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Forgot password</CardTitle>
        <CardDescription>We'll email you a code to reset it.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
          <div className="flex flex-col gap-2">
            <label htmlFor="email" className="text-sm font-medium">
              Email
            </label>
            <Input
              id="email"
              type="email"
              value={email}
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={error !== null}
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <Button type="submit" disabled={forgot.isPending}>
            {forgot.isPending ? "Sending..." : "Send code"}
          </Button>

          <p className="text-center text-sm text-muted-foreground">
            Remembered it?{" "}
            <Link to="/login" className="underline">
              Back to login
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  );
}

// Step 2 — enter the emailed code and a new password.
function ResetStep({ email, onDone }: { email: string; onDone: () => void }) {
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
      errors.confirmPassword = "Passwords don't match.";
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
        onError: (err) =>
          setFormError(isApiError(err) ? err.message : "Something went wrong. Please try again."),
      },
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Enter your code</CardTitle>
        <CardDescription>
          If {email} is registered, we sent it a code. Enter it with your new password.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
          <div className="flex flex-col gap-2">
            <label htmlFor="code" className="text-sm font-medium">
              Reset code
            </label>
            <Input
              id="code"
              inputMode="numeric"
              value={code}
              autoComplete="one-time-code"
              onChange={(event) => setCode(event.target.value)}
              aria-invalid={fieldErrors.code !== undefined}
            />
            {fieldErrors.code && <p className="text-sm text-destructive">{fieldErrors.code}</p>}
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="newPassword" className="text-sm font-medium">
              New password
            </label>
            <Input
              id="newPassword"
              type="password"
              value={newPassword}
              autoComplete="new-password"
              onChange={(event) => setNewPassword(event.target.value)}
              aria-invalid={fieldErrors.newPassword !== undefined}
            />
            {fieldErrors.newPassword && (
              <p className="text-sm text-destructive">{fieldErrors.newPassword}</p>
            )}
          </div>

          <div className="flex flex-col gap-2">
            <label htmlFor="confirmPassword" className="text-sm font-medium">
              Confirm new password
            </label>
            <Input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              autoComplete="new-password"
              onChange={(event) => setConfirmPassword(event.target.value)}
              aria-invalid={fieldErrors.confirmPassword !== undefined}
            />
            {fieldErrors.confirmPassword && (
              <p className="text-sm text-destructive">{fieldErrors.confirmPassword}</p>
            )}
          </div>

          {formError && <p className="text-sm text-destructive">{formError}</p>}

          <Button type="submit" disabled={reset.isPending}>
            {reset.isPending ? "Resetting..." : "Reset password"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
