// Route component for /profile. The actual view depends on who is logged in:
// company users get their company profile, candidates get the candidate profile.
// Administrators have no profile, so they're sent home.

import { Navigate } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";
import { CandidateProfilePage } from "@/pages/CandidateProfilePage";
import { CompanyProfilePage } from "@/pages/CompanyProfilePage";

export function ProfilePage() {
  const { user } = useAuth();
  if (user?.userType === "COMPANY_USER") {
    return <CompanyProfilePage />;
  }
  if (user?.userType === "CANDIDATE") {
    return <CandidateProfilePage />;
  }
  return <Navigate to="/" replace />;
}
