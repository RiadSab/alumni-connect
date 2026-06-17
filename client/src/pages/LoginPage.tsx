// Login form. Plain controlled inputs (no form library), validated with the
// loginSchema we already wrote. On success it saves the session via the auth
// context and goes to the dashboard.

import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { useLogin } from "@/features/auth/hooks";
import { useAuth } from "@/features/auth/auth-context";
import { loginSchema } from "@/types/auth";
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

export function LoginPage() {
  const auth = useAuth();
  const login = useLogin();
  const navigate = useNavigate();
  const location = useLocation();

  // Set by the register page after a successful sign-up.
  const justRegistered = (location.state as { justRegistered?: boolean } | null)?.justRegistered;
  // Where to return after login (set by RequireAuth or a "log in to…" link).
  const from = (location.state as { from?: string } | null)?.from ?? "/dashboard";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
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
        setFormError(
          isApiError(error) ? error.message : "Something went wrong. Please try again.",
        );
      },
    });
  }

  return (
    <div className="mx-auto max-w-sm">
      <Card>
        <CardHeader>
          <CardTitle>Log in</CardTitle>
          <CardDescription>Welcome back to Alumni Connect.</CardDescription>
        </CardHeader>
        <CardContent>
          {justRegistered && (
            <p className="mb-4 text-sm text-success">
              Account created. It needs admin approval before you can log in.
            </p>
          )}
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <div className="flex flex-col gap-2">
              <label htmlFor="email" className="text-sm font-medium">
                Email
              </label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                aria-invalid={fieldErrors.email !== undefined}
              />
              {fieldErrors.email && (
                <p className="text-sm text-destructive">{fieldErrors.email}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="password" className="text-sm font-medium">
                Password
              </label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                aria-invalid={fieldErrors.password !== undefined}
              />
              {fieldErrors.password && (
                <p className="text-sm text-destructive">{fieldErrors.password}</p>
              )}
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <Button type="submit" disabled={login.isPending}>
              {login.isPending ? "Logging in..." : "Log in"}
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              No account?{" "}
              <Link to="/register/candidate" className="underline">
                Create one
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
