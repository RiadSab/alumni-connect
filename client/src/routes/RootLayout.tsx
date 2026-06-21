// App shell: a sticky header with the primary nav links, the language toggle,
// and a user dropdown (profile / settings / log out). An <Outlet /> renders the
// current page below.

import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ChevronDown, LogOut, Settings, User } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useT, type Lang } from "@/features/i18n/lang-context";
import type { AuthUser } from "@/lib/auth";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

function navLinkClass({ isActive }: { isActive: boolean }) {
  return cn(
    "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
    isActive
      ? "bg-muted text-foreground"
      : "text-muted-foreground hover:bg-muted/60 hover:text-foreground",
  );
}

export function RootLayout() {
  const { user, isAuthenticated, logout } = useAuth();
  const { t } = useT();
  const navigate = useNavigate();

  const isCandidate = user?.userType === "CANDIDATE";
  const isCompany = user?.userType === "COMPANY_USER";
  const isAdmin = user?.userType === "ADMINISTRATOR";
  function handleLogout() {
    logout();
    navigate("/");
  }

  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-border bg-card/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-6">
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2">
              <span className="grid size-8 place-items-center rounded-lg bg-primary text-sm font-bold text-primary-foreground">
                AC
              </span>
              <span className="text-base font-semibold tracking-tight">Alumni Connect</span>
            </Link>

            <nav className="flex items-center gap-1">
              <NavLink to="/" end className={navLinkClass}>
                {t("nav.jobs")}
              </NavLink>
              {isAuthenticated && (
                <NavLink to="/dashboard" className={navLinkClass}>
                  {t("nav.dashboard")}
                </NavLink>
              )}
              {isCandidate && (
                <NavLink to="/applications" className={navLinkClass}>
                  {t("nav.myApplications")}
                </NavLink>
              )}
              {isCandidate && (
                <NavLink to="/saved" className={navLinkClass}>
                  {t("nav.saved")}
                </NavLink>
              )}
              {isCompany && (
                <NavLink to="/company/jobs" className={navLinkClass}>
                  {t("nav.companyJobs")}
                </NavLink>
              )}
              {isCompany && (
                <NavLink to="/company/team" className={navLinkClass}>
                  {t("nav.companyTeam")}
                </NavLink>
              )}
              {isAdmin && <ModerateMenu />}
            </nav>
          </div>

          <div className="flex items-center gap-3">
            <LangToggle />
            {isAuthenticated && user ? (
              <UserMenu user={user} onLogout={handleLogout} />
            ) : (
              <Button asChild size="sm">
                <Link to="/login">{t("nav.login")}</Link>
              </Button>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}

function UserMenu({
  user,
  onLogout,
}: {
  user: AuthUser;
  onLogout: () => void;
}) {
  const { t } = useT();
  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`;
  // Both candidates and company users have a /profile (it dispatches by type).
  const showProfile = user.userType === "CANDIDATE" || user.userType === "COMPANY_USER";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex items-center gap-2 rounded-full border border-border bg-card py-1 pl-1 pr-2.5 text-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <span className="grid size-7 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
            {initials}
          </span>
          <span className="hidden font-medium sm:inline">{user.firstName}</span>
          <ChevronDown className="size-4 text-muted-foreground" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent>
        <DropdownMenuLabel>
          <div className="text-sm font-medium text-foreground">
            {user.firstName} {user.lastName}
          </div>
          <div className="text-xs text-muted-foreground">{user.email}</div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        {showProfile && (
          <DropdownMenuItem asChild>
            <Link to="/profile">
              <User /> {t("nav.profile")}
            </Link>
          </DropdownMenuItem>
        )}
        <DropdownMenuItem asChild>
          <Link to="/settings/password">
            <Settings /> {t("nav.settings")}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={onLogout}>
          <LogOut /> {t("nav.logout")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

// The admin nav entry: a "Moderate" dropdown linking to the three moderation
// screens (the approvals inbox, the users list, the companies list). The trigger
// shows the active styling whenever the current route is under /admin.
function ModerateMenu() {
  const { t } = useT();
  const { pathname } = useLocation();
  const onAdminRoute = pathname.startsWith("/admin");

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className={cn(navLinkClass({ isActive: onAdminRoute }), "flex items-center gap-1")}
        >
          {t("nav.moderate")}
          <ChevronDown className="size-4" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        <DropdownMenuItem asChild>
          <Link to="/admin/pending">{t("nav.moderate.pending")}</Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link to="/admin/users">{t("nav.moderate.users")}</Link>
        </DropdownMenuItem>
        <DropdownMenuItem asChild>
          <Link to="/admin/companies">{t("nav.moderate.companies")}</Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function LangToggle() {
  const { lang, setLang } = useT();
  return (
    <div className="flex h-8 overflow-hidden rounded-md border border-border text-xs font-medium">
      {(["en", "fr"] as const satisfies readonly Lang[]).map((option) => (
        <button
          key={option}
          type="button"
          onClick={() => setLang(option)}
          className={
            option === lang
              ? "bg-primary px-2.5 text-primary-foreground"
              : "px-2.5 text-muted-foreground transition-colors hover:text-foreground"
          }
        >
          {option.toUpperCase()}
        </button>
      ))}
    </div>
  );
}
