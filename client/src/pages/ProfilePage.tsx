// Route component for /profile. The actual view depends on who is logged in:
// company users get their company profile, everyone else (candidates) get the
// candidate profile.

import { useAuth } from "@/features/auth/auth-context";
import { CandidateProfilePage } from "@/pages/CandidateProfilePage";
import { CompanyProfilePage } from "@/pages/CompanyProfilePage";

export function ProfilePage() {
  const { user } = useAuth();
  if (user?.userType === "COMPANY_USER") {
    return <CompanyProfilePage />;
  }
  return <CandidateProfilePage />;
}
