// Candidate sign-up form. Collects the required fields only; optional profile
// details are added later from the profile screen. Validated with
// registerCandidateSchema.

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { Calendar, Hash, Mail, Phone, User } from "lucide-react";
import { useRegisterCandidate } from "@/features/auth/hooks";
import { AuthScreen } from "@/features/auth/AuthScreen";
import { AuthBrand, AuthField, AuthPasswordField } from "@/features/auth/fields";
import { useT } from "@/features/i18n/lang-context";
import { registerCandidateSchema } from "@/types/auth";
import { fieldsOptions } from "@/types/enums";
import { isApiError } from "@/lib/http";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
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
  phoneNumber: "",
  isStudent: false,
  studentId: "",
  fieldOfStudy: "",
  graduationYear: "",
};

export function RegisterCandidatePage() {
  const { t } = useT();
  const register = useRegisterCandidate();
  const navigate = useNavigate();

  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);

  function setField(name: keyof typeof emptyForm, value: string | boolean) {
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
      phoneNumber: form.phoneNumber,
      isStudent: form.isStudent,
      studentId: form.isStudent ? form.studentId : undefined,
      fieldOfStudy: form.fieldOfStudy,
      graduationYear: form.graduationYear === "" ? undefined : Number(form.graduationYear),
    };

    const result = registerCandidateSchema.safeParse(payload);
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
          <AuthBrand title={t("auth.candidate.title")} subtitle={t("auth.candidate.subtitle")} />

          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
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
            <AuthField
              id="phoneNumber"
              label={t("auth.field.phone")}
              icon={Phone}
              value={form.phoneNumber}
              onChange={(event) => setField("phoneNumber", event.target.value)}
              error={fieldErrors.phoneNumber}
            />

            <div className="flex items-center gap-3">
              <Switch
                id="isStudent"
                checked={form.isStudent}
                onCheckedChange={(checked) => setField("isStudent", checked)}
              />
              <label htmlFor="isStudent" className="text-sm font-medium">
                {t("auth.candidate.isStudent")}
              </label>
            </div>

            {form.isStudent && (
              <AuthField
                id="studentId"
                label={t("auth.field.studentId")}
                icon={Hash}
                value={form.studentId}
                onChange={(event) => setField("studentId", event.target.value)}
                error={fieldErrors.studentId}
              />
            )}

            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium">{t("auth.field.fieldOfStudy")}</label>
              <Select
                value={form.fieldOfStudy}
                onValueChange={(value) => setField("fieldOfStudy", value)}
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
              {fieldErrors.fieldOfStudy && (
                <p className="text-sm text-destructive">{fieldErrors.fieldOfStudy}</p>
              )}
            </div>

            <AuthField
              id="graduationYear"
              label={t("auth.field.graduationYear")}
              icon={Calendar}
              type="number"
              value={form.graduationYear}
              onChange={(event) => setField("graduationYear", event.target.value)}
              error={fieldErrors.graduationYear}
            />

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <Button type="submit" className="h-10" disabled={register.isPending}>
              {register.isPending ? t("auth.candidate.creating") : t("auth.candidate.submit")}
            </Button>
          </form>

          <div className="space-y-1.5 border-t border-border pt-4 text-center text-sm text-muted-foreground">
            <p>
              {t("auth.common.alreadyHaveAccount")}{" "}
              <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                {t("auth.common.login")}
              </Link>
            </p>
            <p>
              {t("auth.candidate.registeringCompany")}{" "}
              <Link to="/register/company" className="font-medium text-primary underline-offset-4 hover:underline">
                {t("auth.candidate.registerHere")}
              </Link>
            </p>
          </div>
        </CardContent>
      </Card>
    </AuthScreen>
  );
}
