// App shell: a header that shows different links depending on whether someone is
// logged in, and an <Outlet /> where the current page renders.

import { Link, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";
import { useT } from "@/features/i18n/lang-context";
import { Button } from "@/components/ui/button";

export function RootLayout() {
  const { user, isAuthenticated, logout } = useAuth();
  const { lang, setLang, t } = useT();
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
          <Link to="/">{t("nav.jobs")}</Link>

          {isAuthenticated ? (
            <>
              <Link to="/dashboard">{t("nav.dashboard")}</Link>
              {user?.userType === "CANDIDATE" && (
                <>
                  <Link to="/applications">{t("nav.myApplications")}</Link>
                  <Link to="/profile">{t("nav.profile")}</Link>
                </>
              )}
              <Link to="/settings/password">{t("nav.settings")}</Link>
              <span className="text-muted-foreground">
                {user?.firstName} {user?.lastName}
              </span>
              <Button variant="outline" size="sm" onClick={handleLogout}>
                {t("nav.logout")}
              </Button>
            </>
          ) : (
            <Link to="/login">{t("nav.login")}</Link>
          )}

          <div className="flex overflow-hidden rounded-md border text-xs font-medium">
            {(["en", "fr"] as const).map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => setLang(option)}
                className={
                  option === lang
                    ? "bg-primary px-2.5 py-1 text-primary-foreground"
                    : "px-2.5 py-1 text-muted-foreground"
                }
              >
                {option.toUpperCase()}
              </button>
            ))}
          </div>
        </nav>
      </header>

      <main className="px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
