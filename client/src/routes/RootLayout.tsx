// App shell: a header that shows different links depending on whether someone is
// logged in, and an <Outlet /> where the current page renders.

import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";
import { Button } from "@/components/ui/button";

export function RootLayout() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <div className="min-h-screen">
      <header className="flex items-center justify-between border-b px-6 py-4">
        <Link to="/" className="text-lg font-semibold">
          Alumni Connect
        </Link>

        <nav className="flex items-center gap-4 text-sm">
          <Link to="/">Jobs</Link>

          {isAuthenticated ? (
            <>
              <Link to="/dashboard">Dashboard</Link>
              <span className="text-muted-foreground">
                {user?.firstName} {user?.lastName}
              </span>
              <Button variant="outline" size="sm" onClick={handleLogout}>
                Log out
              </Button>
            </>
          ) : (
            <Link to="/login">Login</Link>
          )}
        </nav>
      </header>

      <main className="px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
