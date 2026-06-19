// Route component for /dashboard. The view depends on who is logged in: company
// users get their company dashboard; administrators get the moderation dashboard;
// everyone else gets the simple placeholder for now (a real candidate dashboard
// comes later).

import { useAuth } from "@/features/auth/auth-context";
import { CompanyDashboardPage } from "@/pages/CompanyDashboardPage";
import { AdminDashboardPage } from "@/pages/AdminDashboardPage";

export function DashboardPage() {
  const { user } = useAuth();

  if (user?.userType === "COMPANY_USER") {
    return <CompanyDashboardPage />;
  }

  if (user?.userType === "ADMINISTRATOR") {
    return <AdminDashboardPage />;
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
