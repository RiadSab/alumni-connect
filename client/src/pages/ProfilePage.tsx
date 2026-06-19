// Route component for /profile. The actual view depends on who is logged in:
// candidates get CandidateProfilePage. The company-user branch lands in the next
// commit (CompanyProfilePage); for now this just renders the candidate view.

import { CandidateProfilePage } from "@/pages/CandidateProfilePage";

export function ProfilePage() {
  return <CandidateProfilePage />;
}
