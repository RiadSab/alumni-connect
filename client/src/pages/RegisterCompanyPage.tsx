// Company sign-up: registers the owner account and the company together.
// Required fields only; the rest of the company profile is filled in later.

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Building2, Mail, User } from "lucide-react";
import { useRegisterCompany } from "@/features/auth/hooks";
import { AuthScreen } from "@/features/auth/AuthScreen";
import { AuthBrand, AuthField, AuthPasswordField } from "@/features/auth/fields";
import { useT } from "@/features/i18n/lang-context";
import { registerCompanySchema } from "@/types/auth";
import { companyUserPositionOptions, fieldsOptions } from "@/types/enums";
import { isApiError } from "@/lib/http";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const emptyForm = {
  firstName: "",
  lastName: "",
  email: "",
  password: "",
  position: "",
  companyName: "",
  companyEmail: "",
  companyField: "",
};

export function RegisterCompanyPage() {
  const { t } = useT();
  const register = useRegisterCompany();
  const navigate = useNavigate();

  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  function setField(name: keyof typeof emptyForm, value: string) {
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setFormError(null);

    const payload = {
      firstName: form.firstName,
      lastName: form.lastName,
      email: form.email,
      password: form.password,
      position: form.position,
      companyName: form.companyName,
      companyEmail: form.companyEmail,
      companyField: form.companyField,
    };

    const result = registerCompanySchema.safeParse(payload);
    if (!result.success) {
      const errors: Record<string, string> = {};
      for (const issue of result.error.issues) {
        const field = issue.path[0];
        if (typeof field === "string" && errors[field] === undefined) {
          errors[field] = issue.message;
        }
      }
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    register.mutate(result.data, {
      onSuccess: () => navigate("/login", { state: { justRegistered: true } }),
      onError: (error) => {
        setFormError(isApiError(error) ? error.message : t("auth.error.generic"));
      },
    });
  }

  return (
    <AuthScreen>
      <Card className="w-full max-w-lg bg-card/95 shadow-[var(--shadow-2)] backdrop-blur-sm">
        <CardContent className="flex flex-col gap-6 p-7">
          <AuthBrand title={t("auth.company.title")} subtitle={t("auth.company.subtitle")} />

          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <h2 className="text-sm font-semibold text-muted-foreground">
              {t("auth.company.yourAccount")}
            </h2>

            <AuthField
              id="firstName"
              label={t("auth.field.firstName")}
              icon={User}
              value={form.firstName}
              onChange={(event) => setField("firstName", event.target.value)}
              error={fieldErrors.firstName}
            />
            <AuthField
              id="lastName"
              label={t("auth.field.lastName")}
              icon={User}
              value={form.lastName}
              onChange={(event) => setField("lastName", event.target.value)}
              error={fieldErrors.lastName}
            />
            <AuthField
              id="email"
              label={t("auth.field.email")}
              icon={Mail}
              type="email"
              value={form.email}
              onChange={(event) => setField("email", event.target.value)}
              error={fieldErrors.email}
            />
            <AuthPasswordField
              id="password"
              label={t("auth.field.password")}
              value={form.password}
              onChange={(event) => setField("password", event.target.value)}
              error={fieldErrors.password}
            />

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium">{t("auth.field.position")}</label>
              <Select value={form.position} onValueChange={(value) => setField("position", value)}>
                <SelectTrigger>
                  <SelectValue placeholder={t("auth.placeholder.selectPosition")} />
                </SelectTrigger>
                <SelectContent>
                  {companyUserPositionOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fieldErrors.position && (
                <p className="text-sm text-destructive">{fieldErrors.position}</p>
              )}
            </div>

            <h2 className="mt-2 text-sm font-semibold text-muted-foreground">
              {t("auth.company.yourCompany")}
            </h2>

            <AuthField
              id="companyName"
              label={t("auth.field.companyName")}
              icon={Building2}
              value={form.companyName}
              onChange={(event) => setField("companyName", event.target.value)}
              error={fieldErrors.companyName}
            />
            <AuthField
              id="companyEmail"
              label={t("auth.field.companyEmail")}
              icon={Mail}
              type="email"
              value={form.companyEmail}
              onChange={(event) => setField("companyEmail", event.target.value)}
              error={fieldErrors.companyEmail}
            />

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium">{t("auth.field.companyField")}</label>
              <Select
                value={form.companyField}
                onValueChange={(value) => setField("companyField", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t("auth.placeholder.selectField")} />
                </SelectTrigger>
                <SelectContent>
                  {fieldsOptions.map((option) => (
                    <SelectItem key={option.value} value={option.value}>
                      {option.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {fieldErrors.companyField && (
                <p className="text-sm text-destructive">{fieldErrors.companyField}</p>
              )}
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <Button type="submit" className="h-10" disabled={register.isPending}>
              {register.isPending ? t("auth.company.creating") : t("auth.company.submit")}
            </Button>
          </form>

          <div className="space-y-1.5 border-t border-border pt-4 text-center text-sm text-muted-foreground">
            <p>
              {t("auth.company.joiningExisting")}{" "}
              <Link to="/register/company-member" className="font-medium text-primary underline-offset-4 hover:underline">
                {t("auth.company.joinInstead")}
              </Link>
            </p>
            <p>
              {t("auth.common.alreadyHaveAccount")}{" "}
              <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                {t("auth.common.login")}
              </Link>
            </p>
          </div>
        </CardContent>
      </Card>
    </AuthScreen>
  );
}
