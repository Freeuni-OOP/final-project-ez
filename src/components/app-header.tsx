// Top navigation bar that shows on every page.
// Left: the app name, links back home. Middle: page nav. Right: auth state —
// the username + logout when signed in, or login/sign-up buttons when not.

import { useState } from "react";
import { Link, useRouterState } from "@tanstack/react-router";

import { AuthDialog } from "@/components/auth-dialog";
import { NotificationsMenu } from "@/components/notifications-menu";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

const navItems = [
  { to: "/", label: "nav_composer" },
  { to: "/library", label: "nav_library" },
  { to: "/syntax", label: "nav_syntax" },
  { to: "/explore", label: "nav_explore" },
] as const;

export function AppHeader() {
  const { t } = useI18n();
  const { user, logout } = useAuth();
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const [authMode, setAuthMode] = useState<"login" | "signup" | null>(null);

  return (
    <header className="sticky top-0 z-30 border-b border-border bg-background/85 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-6 py-4">
        <Link to="/" className="text-lg font-bold tracking-tight text-foreground">
          {t("app_name")}
        </Link>

        <nav className="flex items-center gap-1">
          {(user ? [...navItems, { to: "/feed", label: "nav_feed" } as const] : navItems).map(
            (item) => {
              const active = pathname === item.to;
              return (
                <Link
                  key={item.to}
                  to={item.to}
                  className={cn(
                    "rounded-md px-3 py-2 text-sm font-medium transition-colors",
                    active
                      ? "bg-primary/10 text-primary"
                      : "text-foreground/70 hover:bg-accent hover:text-foreground",
                  )}
                >
                  {t(item.label)}
                </Link>
              );
            },
          )}
        </nav>

        {/* Auth state */}
        <div className="flex items-center gap-2">
          {user ? (
            <>
              <NotificationsMenu username={user.username} />
              <Link
                to="/settings"
                className="text-sm font-medium text-foreground transition-colors hover:text-primary"
              >
                {user.username}
              </Link>
              <button
                onClick={logout}
                className="rounded-md border border-border px-3 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:border-primary hover:text-foreground"
              >
                {t("logout")}
              </button>
            </>
          ) : (
            <>
              <button
                onClick={() => setAuthMode("login")}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-foreground/70 transition-colors hover:text-foreground"
              >
                {t("login")}
              </button>
              <button
                onClick={() => setAuthMode("signup")}
                className="rounded-md bg-primary px-3 py-1.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
              >
                {t("signup")}
              </button>
            </>
          )}
        </div>
      </div>

      {authMode && <AuthDialog initialMode={authMode} onClose={() => setAuthMode(null)} />}
    </header>
  );
}
