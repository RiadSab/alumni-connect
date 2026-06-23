// Login form. Plain controlled inputs (no form library), validated with the
// loginSchema we already wrote. On success it saves the session via the auth
// context and goes to the dashboard. Sits on a full-bleed branded background.

import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { Eye, EyeOff, Lock, Mail } from "lucide-react";
import { useLogin } from "@/features/auth/hooks";
import { useAuth } from "@/features/auth/auth-context";
import { AuthScreen } from "@/features/auth/AuthScreen";
import { AuthBrand, AuthField } from "@/features/auth/fields";
import { useT } from "@/features/i18n/lang-context";
import { loginSchema } from "@/types/auth";
import { isApiError } from "@/lib/http";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export function LoginPage() {
  const auth = useAuth();
  const login = useLogin();
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useT();

  // Set by the register page after a successful sign-up.
  const justRegistered = (location.state as { justRegistered?: boolean } | null)?.justRegistered;
  // Where to return after login (set by RequireAuth or a "log in to…" link).
  const from = (location.state as { from?: string } | null)?.from ?? "/dashboard";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});
  const [formError, setFormError] = useState<string | null>(null);

  // Already logged in? Don't show the form.
  if (auth.isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    // Validate the inputs with the zod schema.
    const result = loginSchema.safeParse({ email, password });
    if (!result.success) {
      const errors: { email?: string; password?: string } = {};
      for (const issue of result.error.issues) {
        const field = issue.path[0];
        if (field === "email") errors.email = issue.message;
        if (field === "password") errors.password = issue.message;
      }
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    // Send to the server, then save the session and redirect.
    login.mutate(result.data, {
      onSuccess: (response) => {
        auth.login(response);
        navigate(from, { replace: true });
      },
      onError: (error) => {
        setFormError(isApiError(error) ? error.message : t("auth.error.generic"));
      },
    });
  }

  return (
    <AuthScreen>
        <Card className="w-full max-w-sm bg-card/95 shadow-[var(--shadow-2)] backdrop-blur-sm">
          <CardContent className="flex flex-col gap-6 p-7">
            <AuthBrand title={t("auth.login.title")} subtitle={t("auth.login.subtitle")} />

            {justRegistered && (
              <p className="rounded-lg bg-success/10 px-3 py-2 text-center text-sm text-success">
                {t("auth.login.justRegistered")}
              </p>
            )}

            <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
              <AuthField
                id="email"
                label={t("auth.login.email")}
                icon={Mail}
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                error={fieldErrors.email}
              />

              <div className="flex flex-col gap-1.5">
                <div className="flex items-center justify-between">
                  <label htmlFor="password" className="text-sm font-medium">
                    {t("auth.login.password")}
                  </label>
                  <Link
                    to="/forgot-password"
                    className="text-sm text-muted-foreground underline-offset-4 hover:text-foreground hover:underline"
                  >
                    {t("auth.login.forgot")}
                  </Link>
                </div>
                <div className="relative">
                  <Lock className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    className="h-10 pl-9 pr-9"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    aria-invalid={fieldErrors.password !== undefined}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((shown) => !shown)}
                    aria-label={
                      showPassword ? t("auth.login.hidePassword") : t("auth.login.showPassword")
                    }
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
                  >
                    {showPassword ? <EyeOff className="size-4" /> : <Eye className="size-4" />}
                  </button>
                </div>
                {fieldErrors.password && (
                  <p className="text-sm text-destructive">{fieldErrors.password}</p>
                )}
              </div>

              {formError && <p className="text-sm text-destructive">{formError}</p>}

              <Button type="submit" className="h-10 w-full" disabled={login.isPending}>
                {login.isPending ? t("auth.login.submitting") : t("auth.login.submit")}
              </Button>
            </form>

            <div className="border-t border-border pt-4 text-center text-sm text-muted-foreground">
              {t("auth.login.noAccount")}{" "}
              <Link to="/register/candidate" className="font-medium text-primary underline-offset-4 hover:underline">
                {t("auth.login.createOne")}
              </Link>
            </div>
          </CardContent>
        </Card>
    </AuthScreen>
  );
}
