// App shell: a sticky header (nav links, language toggle, user menu) over the routed page.

import { useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { ChevronDown, LogOut, Menu, Settings, User } from "lucide-react";
import { useAuth } from "@/features/auth/auth-context";
import { useMyPhotoUrl } from "@/features/candidates/hooks";
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
import {
  Sheet,
  SheetClose,
  SheetContent,
  SheetDescription,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";

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
  const [menuOpen, setMenuOpen] = useState(false);

  const isCandidate = user?.userType === "CANDIDATE";
  const isCompany = user?.userType === "COMPANY_USER";
  const isAdmin = user?.userType === "ADMINISTRATOR";
  function handleLogout() {
    logout();
    navigate("/");
  }

  // Primary nav links, shared by the desktop bar and the mobile sheet.
  const navItems = [
    { to: "/", label: t("nav.jobs"), end: true },
    ...(isAuthenticated ? [{ to: "/dashboard", label: t("nav.dashboard") }] : []),
    ...(isCandidate ? [{ to: "/applications", label: t("nav.myApplications") }] : []),
    ...(isCandidate ? [{ to: "/saved", label: t("nav.saved") }] : []),
    ...(isCompany ? [{ to: "/company/jobs", label: t("nav.companyJobs") }] : []),
    ...(isCompany ? [{ to: "/company/team", label: t("nav.companyTeam") }] : []),
  ];

  return (
    <div className="min-h-screen">
      {/* Soft light background image behind every page. */}
      <div
        aria-hidden
        className="fixed inset-0 -z-10 bg-cover bg-center bg-no-repeat"
        style={{ backgroundImage: "url(/light_background.png)" }}
      />

      <header className="sticky top-0 z-40 border-b border-border bg-card/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2">
              <img
                src="/logo_blue_background_cmp.png"
                alt="Alumni Connect"
                className="size-8 rounded-lg object-cover"
              />
              <span className="text-base font-semibold tracking-tight">Alumni Connect</span>
            </Link>

            <nav className="hidden items-center gap-1 md:flex">
              {navItems.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.end} className={navLinkClass}>
                  {item.label}
                </NavLink>
              ))}
              {isAdmin && <ModerateMenu />}
            </nav>
          </div>

          <div className="hidden items-center gap-3 md:flex">
            <LangToggle />
            {isAuthenticated && user ? (
              <UserMenu user={user} onLogout={handleLogout} />
            ) : (
              <Button asChild size="sm">
                <Link to="/login">{t("nav.login")}</Link>
              </Button>
            )}
          </div>

          <Sheet open={menuOpen} onOpenChange={setMenuOpen}>
            <SheetTrigger asChild>
              <button
                type="button"
                aria-label={t("nav.menu")}
                className="rounded-md p-2 text-muted-foreground transition-colors hover:bg-muted/60 hover:text-foreground md:hidden"
              >
                <Menu className="size-5" />
              </button>
            </SheetTrigger>
            <SheetContent side="right">
              <SheetTitle>Alumni Connect</SheetTitle>
              <SheetDescription className="sr-only">{t("nav.menu")}</SheetDescription>

              <nav className="flex flex-col gap-1">
                {navItems.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={navLinkClass}
                    onClick={() => setMenuOpen(false)}
                  >
                    {item.label}
                  </NavLink>
                ))}
                {isAdmin && (
                  <>
                    <p className="mt-3 px-3 text-xs font-medium uppercase tracking-wider text-[var(--color-stone)]">
                      {t("nav.moderate")}
                    </p>
                    <NavLink to="/admin/pending" className={navLinkClass} onClick={() => setMenuOpen(false)}>
                      {t("nav.moderate.pending")}
                    </NavLink>
                    <NavLink to="/admin/users" className={navLinkClass} onClick={() => setMenuOpen(false)}>
                      {t("nav.moderate.users")}
                    </NavLink>
                    <NavLink to="/admin/companies" className={navLinkClass} onClick={() => setMenuOpen(false)}>
                      {t("nav.moderate.companies")}
                    </NavLink>
                    <NavLink to="/admin/alumni" className={navLinkClass} onClick={() => setMenuOpen(false)}>
                      {t("nav.moderate.alumni")}
                    </NavLink>
                  </>
                )}
              </nav>

              <div className="mt-auto flex flex-col gap-3 border-t border-border pt-4">
                <div className="flex items-center justify-between gap-3">
                  <span className="text-sm font-medium text-foreground">{t("nav.language")}</span>
                  <LangToggle />
                </div>
                {isAuthenticated && user ? (
                  <>
                    {(isCandidate || isCompany) && (
                      <SheetClose asChild>
                        <Link to="/profile" className={navLinkClass({ isActive: false })}>
                          {t("nav.profile")}
                        </Link>
                      </SheetClose>
                    )}
                    <SheetClose asChild>
                      <Link to="/settings/password" className={navLinkClass({ isActive: false })}>
                        {t("nav.settings")}
                      </Link>
                    </SheetClose>
                    <Button variant="outline" onClick={() => { setMenuOpen(false); handleLogout(); }}>
                      <LogOut className="size-4" /> {t("nav.logout")}
                    </Button>
                  </>
                ) : (
                  <SheetClose asChild>
                    <Button asChild>
                      <Link to="/login">{t("nav.login")}</Link>
                    </Button>
                  </SheetClose>
                )}
              </div>
            </SheetContent>
          </Sheet>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        <Outlet />
      </main>
    </div>
  );
}

// The user's avatar: a candidate's own photo when set, otherwise their initials.
// enabled is candidate-only, so company/admin users never hit the photo endpoint.
function NavAvatar({ user }: { user: AuthUser }) {
  const photoUrl = useMyPhotoUrl(user.userType === "CANDIDATE");
  if (photoUrl) {
    return <img src={photoUrl} alt="" className="size-7 rounded-full object-cover" />;
  }
  return (
    <span className="grid size-7 place-items-center rounded-full bg-primary text-xs font-semibold text-primary-foreground">
      {user.firstName.charAt(0)}
      {user.lastName.charAt(0)}
    </span>
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
  // Both candidates and company users have a /profile (it dispatches by type).
  const showProfile = user.userType === "CANDIDATE" || user.userType === "COMPANY_USER";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex items-center gap-2 rounded-full border border-border bg-card py-1 pl-1 pr-2.5 text-sm transition-colors hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <NavAvatar user={user} />
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
        <DropdownMenuItem asChild>
          <Link to="/admin/alumni">{t("nav.moderate.alumni")}</Link>
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
