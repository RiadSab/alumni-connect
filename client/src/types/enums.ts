// Enum value sets mirrored from the backend API reference (§"Enums").
//
// tsconfig has `erasableSyntaxOnly`, so we can't use TS `enum`. Each enum is a
// readonly tuple (the source of truth, also reused by zod request schemas) plus
// a same-named union type via declaration merging. `toOptions` derives the
// {value, label} lists that dropdowns / chips / badges render.

export type EnumOption<T extends string> = { value: T; label: string };

function humanize(value: string): string {
  return value
    .toLowerCase()
    .split("_")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

/** Build UI options for an enum, overriding labels where humanize() is wrong. */
export function toOptions<T extends string>(
  values: readonly T[],
  overrides: Partial<Record<T, string>> = {},
): EnumOption<T>[] {
  return values.map((value) => ({ value, label: overrides[value] ?? humanize(value) }));
}

// --- Users -----------------------------------------------------------------

export const UserType = ["CANDIDATE", "COMPANY_USER", "ADMINISTRATOR"] as const;
export type UserType = (typeof UserType)[number];
export const userTypeOptions = toOptions(UserType);

export const UserStatus = [
  "ACTIVE",
  "INACTIVE",
  "SUSPENDED",
  "PENDING",
  "DELETED",
  "REJECTED",
] as const;
export type UserStatus = (typeof UserStatus)[number];
export const userStatusOptions = toOptions(UserStatus);

// --- Companies -------------------------------------------------------------

export const CompanyStatus = ["PENDING", "ACTIVE", "REJECTED", "SUSPENDED", "PARTNER"] as const;
export type CompanyStatus = (typeof CompanyStatus)[number];
export const companyStatusOptions = toOptions(CompanyStatus);

export const CompanySize = ["STARTUP", "SMALL", "MEDIUM", "LARGE", "MULTINATIONAL"] as const;
export type CompanySize = (typeof CompanySize)[number];
export const companySizeOptions = toOptions(CompanySize);

export const CompanyRole = ["OWNER", "RECRUITER", "MEMBER"] as const;
export type CompanyRole = (typeof CompanyRole)[number];
export const companyRoleOptions = toOptions(CompanyRole);

export const CompanyUserPosition = ["HR", "MANAGER", "EMPLOYEE", "INTERN", "OTHER"] as const;
export type CompanyUserPosition = (typeof CompanyUserPosition)[number];
export const companyUserPositionOptions = toOptions(CompanyUserPosition, { HR: "HR" });

// --- Shared (study field / company field) ----------------------------------

export const Fields = [
  "COMPUTER_SCIENCE",
  "INFORMATION_TECHNOLOGY",
  "SOFTWARE_ENGINEERING",
  "DATA_SCIENCE",
  "CYBER_SECURITY",
  "NETWORKING",
  "ARTIFICIAL_INTELLIGENCE",
  "MACHINE_LEARNING",
  "WEB_DEVELOPMENT",
  "MOBILE_DEVELOPMENT",
  "CLOUD_COMPUTING",
  "AEROSPACE_ENGINEERING",
  "MECHANICAL_ENGINEERING",
  "CIVIL_ENGINEERING",
  "ELECTRICAL_ENGINEERING",
  "CHEMICAL_ENGINEERING",
  "BIOTECHNOLOGY",
  "ENVIRONMENTAL_ENGINEERING",
  "INDUSTRIAL_ENGINEERING",
  "MATERIALS_SCIENCE",
  "PHYSICS",
  "MATHEMATICS",
  "STATISTICS",
  "CHEMISTRY",
  "BIOLOGY",
  "ECONOMICS",
  "BUSINESS_ADMINISTRATION",
] as const;
export type Fields = (typeof Fields)[number];
export const fieldsOptions = toOptions(Fields);

// --- Jobs ------------------------------------------------------------------

export const JobCity = [
  "CASABLANCA",
  "RABAT",
  "MARRAKECH",
  "TANGER",
  "AGADIR",
  "FES",
  "MEKNES",
  "OUJDA",
  "EL_JADIDA",
  "SAFI",
  "KHOURIBGA",
  "BERRECHID",
  "KENITRA",
  "SETTAT",
  "BENGUERIR",
  "TAZA",
  "NADOR",
  "AL_HOCEIMA",
  "TETOUAN",
  "LARACHE",
  "SIDI_KACEM",
  "SIDI_SLIMANE",
  "KHENIFRA",
  "BENI_MELLAL",
] as const;
export type JobCity = (typeof JobCity)[number];
export const jobCityOptions = toOptions(JobCity);

export const EmploymentType = [
  "FULL_TIME",
  "PART_TIME",
  "CONTRACT",
  "INTERN",
  "FREELANCE",
  "TEMPORARY",
] as const;
export type EmploymentType = (typeof EmploymentType)[number];
export const employmentTypeOptions = toOptions(EmploymentType);

export const JobStatus = ["OPEN", "CLOSED", "DRAFT", "EXPIRED"] as const;
export type JobStatus = (typeof JobStatus)[number];
export const jobStatusOptions = toOptions(JobStatus);

// --- Applications ----------------------------------------------------------

export const ApplicationStatus = [
  "APPLIED",
  "UNDER_REVIEW",
  "SCHEDULED_INTERVIEW",
  "INTERVIEWED",
  "REJECTED",
  "ACCEPTED",
  "WITHDRAWN",
] as const;
export type ApplicationStatus = (typeof ApplicationStatus)[number];
export const applicationStatusOptions = toOptions(ApplicationStatus);

export const Priority = ["LOW", "MEDIUM", "HIGH"] as const;
export type Priority = (typeof Priority)[number];
export const priorityOptions = toOptions(Priority);

export const EmploymentStatus = ["EMPLOYED", "STUDYING", "SEEKING"] as const;
export type EmploymentStatus = (typeof EmploymentStatus)[number];
export const employmentStatusOptions = toOptions(EmploymentStatus, { SEEKING: "Looking for a role" });

export const InterviewMode = ["ONLINE", "ONSITE"] as const;
export type InterviewMode = (typeof InterviewMode)[number];
export const interviewModeOptions = toOptions(InterviewMode, { ONSITE: "On site" });
