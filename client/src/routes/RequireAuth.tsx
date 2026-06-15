// Route guard for logged-in-only pages. If there's no logged-in user, redirect
// to /login. Otherwise render the nested routes via <Outlet />.

import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

export function RequireAuth() {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
