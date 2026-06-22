// Top navigation bar that shows on every page.
// Left: the app name, links back home. Right: links to the three pages, with the
// current one highlighted. Labels come from useI18n so nothing is hardcoded.

import { Link, useRouterState } from "@tanstack/react-router";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

const navItems = [
  { to: "/", label: "nav_composer" },
  { to: "/library", label: "nav_library" },
  { to: "/syntax", label: "nav_syntax" },
] as const;

export function AppHeader() {
  const { t } = useI18n();
  const pathname = useRouterState({ select: (s) => s.location.pathname });

  return (
    <header className="sticky top-0 z-30 border-b border-border bg-background/85 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-6 py-4">
        <Link to="/" className="text-lg font-bold tracking-tight text-foreground">
          {t("app_name")}
        </Link>

        <nav className="flex items-center gap-1">
          {navItems.map((item) => {
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
          })}
        </nav>
      </div>
    </header>
  );
}
