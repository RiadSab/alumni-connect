// Protected page (behind RequireAuth). For now it just proves the guard and the
// auth context work by showing the logged-in user.

import { useAuth } from "@/features/auth/auth-context";

export function DashboardPage() {
  const { user } = useAuth();

  return (
    <div>
      <h1 className="text-2xl font-bold">Dashboard</h1>
      <p className="text-muted-foreground">
        Welcome, {user?.firstName}. Your role is {user?.userType}.
      </p>
    </div>
  );
}
