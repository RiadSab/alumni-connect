// Candidate sign-up form. Collects the required fields only; optional profile
// details (skills, links, bio) are added later from the profile screen. Plain
// controlled inputs validated with registerCandidateSchema.

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useRegisterCandidate } from "@/features/auth/hooks";
import { registerCandidateSchema } from "@/types/auth";
import { fieldsOptions } from "@/types/enums";
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

    // Build the payload, turning the text inputs into the types the schema wants.
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
          <CardTitle>Create a candidate account</CardTitle>
          <CardDescription>
            New accounts await admin approval before you can log in.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
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
              <label htmlFor="phoneNumber" className="text-sm font-medium">
                Phone number
              </label>
              <Input
                id="phoneNumber"
                value={form.phoneNumber}
                onChange={(event) => setField("phoneNumber", event.target.value)}
              />
              {fieldErrors.phoneNumber && (
                <p className="text-sm text-destructive">{fieldErrors.phoneNumber}</p>
              )}
            </div>

            <div className="flex items-center gap-3">
              <Switch
                id="isStudent"
                checked={form.isStudent}
                onCheckedChange={(checked) => setField("isStudent", checked)}
              />
              <label htmlFor="isStudent" className="text-sm font-medium">
                I am currently a student
              </label>
            </div>

            {form.isStudent && (
              <div className="flex flex-col gap-2">
                <label htmlFor="studentId" className="text-sm font-medium">
                  Student ID
                </label>
                <Input
                  id="studentId"
                  value={form.studentId}
                  onChange={(event) => setField("studentId", event.target.value)}
                />
                {fieldErrors.studentId && (
                  <p className="text-sm text-destructive">{fieldErrors.studentId}</p>
                )}
              </div>
            )}

            <div className="flex flex-col gap-2">
              <label className="text-sm font-medium">Field of study</label>
              <Select
                value={form.fieldOfStudy}
                onValueChange={(value) => setField("fieldOfStudy", value)}
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
              {fieldErrors.fieldOfStudy && (
                <p className="text-sm text-destructive">{fieldErrors.fieldOfStudy}</p>
              )}
            </div>

            <div className="flex flex-col gap-2">
              <label htmlFor="graduationYear" className="text-sm font-medium">
                Graduation year
              </label>
              <Input
                id="graduationYear"
                type="number"
                value={form.graduationYear}
                onChange={(event) => setField("graduationYear", event.target.value)}
              />
              {fieldErrors.graduationYear && (
                <p className="text-sm text-destructive">{fieldErrors.graduationYear}</p>
              )}
            </div>

            {formError && <p className="text-sm text-destructive">{formError}</p>}

            <Button type="submit" disabled={register.isPending}>
              {register.isPending ? "Creating account..." : "Create account"}
            </Button>

            <p className="text-center text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link to="/login" className="underline">
                Log in
              </Link>
            </p>
            <p className="text-center text-sm text-muted-foreground">
              Registering a company?{" "}
              <Link to="/register/company" className="underline">
                Register here
              </Link>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
