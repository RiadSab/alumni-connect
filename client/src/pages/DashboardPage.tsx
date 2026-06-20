// Route component for /dashboard. The view depends on who is logged in: company
// users get their company dashboard, administrators get the moderation dashboard,
// candidates get their job-search dashboard.

import { useAuth } from "@/features/auth/auth-context";
import { CompanyDashboardPage } from "@/pages/CompanyDashboardPage";
import { AdminDashboardPage } from "@/pages/AdminDashboardPage";
import { CandidateDashboardPage } from "@/pages/CandidateDashboardPage";

export function DashboardPage() {
  const { user } = useAuth();

  if (user?.userType === "COMPANY_USER") {
    return <CompanyDashboardPage />;
  }

  if (user?.userType === "ADMINISTRATOR") {
    return <AdminDashboardPage />;
  }

  return <CandidateDashboardPage />;
}
