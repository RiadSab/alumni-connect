// Route component for /profile/edit. The actual form depends on who is logged in:
// candidates get CandidateProfileEditPage. The company-user branch lands in the
// next commit (CompanyProfileEditPage); for now this just renders the candidate one.

import { CandidateProfileEditPage } from "@/pages/CandidateProfileEditPage";

export function ProfileEditPage() {
  return <CandidateProfileEditPage />;
}
