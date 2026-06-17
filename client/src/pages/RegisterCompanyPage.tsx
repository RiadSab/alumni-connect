// Company sign-up: registers the owner account and the company together. Required
// fields only; the rest of the company profile (description, website, address,
// size, logo) is filled in later from the company settings screen.

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useRegisterCompany } from "@/features/auth/hooks";
import { registerCompanySchema } from "@/types/auth";
import { companyUserPositionOptions, fieldsOptions } from "@/types/enums";
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
        setFormError(
          isApiError(error) ? error.message : "Something went wrong. Please try again.",
        );
      },
    });
  }

  return (
    <div className="mx-auto max-w-lg">
      <Card>
        <CardHeader>
          <CardTitle>Register your company</CardTitle>
          <CardDescription>
            This creates your owner account and the company. Both await admin approval.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
            <h2 className="text-sm font-semibold text-muted-foreground">Your account</h2>

            <div className="flex flex-col gap-2">
              <label htmlFor="firstName" className="text-sm font-medium">
                First name
              </label>
              <Input
                id="firstName"
                value={form.firstName}
                onChange={(event) => setField("firstName", event.target.value)}
              />
              {fieldErrors.firstName && (
                <p className="text-sm text-destructive">{fieldErrors.firstName}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="lastName" className="text-sm font-medium">
                Last name
              </label>
              <Input
                id="lastName"
                value={form.lastName}
                onChange={(event) => setField("lastName", event.target.value)}
              />
              {fieldErrors.lastName && (
                <p className="text-sm text-destructive">{fieldErrors.lastName}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="email" className="text-sm font-medium">
                Email
              </label>
              <Input
                id="email"
                type="email"
                value={form.email}
                onChange={(event) => setField("email", event.target.value)}
              />
              {fieldErrors.email && <p className="text-sm text-destructive">{fieldErrors.email}</p>}
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="password" className="text-sm font-medium">
                Password
              </label>
              <Input
                id="password"
                type="password"
                value={form.password}
                onChange={(event) => setField("password", event.target.value)}
              />
              {fieldErrors.password && (
                <p className="text-sm text-destructive">{fieldErrors.password}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Your position</label>
              <Select
                value={form.position}
                onValueChange={(value) => setField("position", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select your position" />
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

            <h2 className="mt-2 text-sm font-semibold text-muted-foreground">Your company</h2>

            <div className="flex flex-col gap-2">
              <label htmlFor="companyName" className="text-sm font-medium">
                Company name
              </label>
              <Input
                id="companyName"
                value={form.companyName}
                onChange={(event) => setField("companyName", event.target.value)}
              />
              {fieldErrors.companyName && (
                <p className="text-sm text-destructive">{fieldErrors.companyName}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="companyEmail" className="text-sm font-medium">
                Company email
              </label>
              <Input
                id="companyEmail"
                type="email"
                value={form.companyEmail}
                onChange={(event) => setField("companyEmail", event.target.value)}
              />
              {fieldErrors.companyEmail && (
                <p className="text-sm text-destructive">{fieldErrors.companyEmail}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Company field</label>
              <Select
                value={form.companyField}
                onValueChange={(value) => setField("companyField", value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a field" />
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

            <Button type="submit" disabled={register.isPending}>
              {register.isPending ? "Creating..." : "Register company"}
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link to="/login" className="underline">
                Log in
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
