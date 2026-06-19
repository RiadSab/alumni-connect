// Route component for /dashboard. The view depends on who is logged in: company
// users get their company dashboard; everyone else gets the simple placeholder for
// now (a real candidate/admin dashboard comes later).

import { useAuth } from "@/features/auth/auth-context";
import { CompanyDashboardPage } from "@/pages/CompanyDashboardPage";

export function DashboardPage() {
  const { user } = useAuth();

  if (user?.userType === "COMPANY_USER") {
    return <CompanyDashboardPage />;
  }

  return (
    <div>
      <h1 className="text-2xl font-bold">Dashboard</h1>
      <p className="text-muted-foreground">
        Welcome, {user?.firstName}. Your role is {user?.userType}.
      </p>
    </div>
  );
}
