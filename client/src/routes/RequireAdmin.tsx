// Route guard for administrator-only pages. These routes nest inside RequireAuth,
// so a logged-in user is guaranteed here — we only check the user type. Anyone who
// isn't an administrator is sent home.

import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

export function RequireAdmin() {
  const { user } = useAuth();

  if (user?.userType !== "ADMINISTRATOR") {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
