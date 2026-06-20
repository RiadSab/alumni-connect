// Route component for /profile/edit. The actual form depends on who is logged in:
// company users get their company edit form, candidates get the candidate edit
// form. Administrators have no profile, so they're sent home.

import { Navigate } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";
import { CandidateProfileEditPage } from "@/pages/CandidateProfileEditPage";
import { CompanyProfileEditPage } from "@/pages/CompanyProfileEditPage";

export function ProfileEditPage() {
  const { user } = useAuth();
  if (user?.userType === "COMPANY_USER") {
    return <CompanyProfileEditPage />;
  }
  if (user?.userType === "CANDIDATE") {
    return <CandidateProfileEditPage />;
  }
  return <Navigate to="/" replace />;
}
