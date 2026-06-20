// Route guard for candidate-only pages. These routes nest inside RequireAuth, so a
// logged-in user is guaranteed here — we only check the user type. Anyone who isn't
// a candidate is sent home.

import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

export function RequireCandidate() {
  const { user } = useAuth();

  if (user?.userType !== "CANDIDATE") {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
