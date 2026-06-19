// Route guard for company-user-only pages. These routes nest inside RequireAuth,
// so a logged-in user is guaranteed here — we only check the user type. Anyone who
// isn't a company user is sent home.

import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

export function RequireCompany() {
  const { user } = useAuth();

  if (user?.userType !== "COMPANY_USER") {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
