// Route component for /profile/edit. The actual form depends on who is logged in:
// company users get their company edit form, everyone else (candidates) get the
// candidate edit form.

import { useAuth } from "@/features/auth/auth-context";
import { CandidateProfileEditPage } from "@/pages/CandidateProfileEditPage";
import { CompanyProfileEditPage } from "@/pages/CompanyProfileEditPage";

export function ProfileEditPage() {
  const { user } = useAuth();
  if (user?.userType === "COMPANY_USER") {
    return <CompanyProfileEditPage />;
  }
  return <CandidateProfileEditPage />;
}
