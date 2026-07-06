// Login / sign-up modal. Tabbed between the two modes, with basic client-side
// validation and backend error messages surfaced inline. Closes on success.
//
// Rendered through a portal to document.body so it floats above the whole page
// and centers in the viewport — otherwise the header's backdrop-blur would trap
// its fixed positioning inside the header bar. Closes on the X button, the
// Escape key, or a click on the backdrop (in both modes).

import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";

import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

type Mode = "login" | "signup";

export function AuthDialog({
  initialMode,
  onClose,
}: {
  initialMode: Mode;
  onClose: () => void;
}) {
  const { login, register } = useAuth();
  const { t } = useI18n();

  const [mode, setMode] = useState<Mode>(initialMode);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  // Close on Escape.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // basic validation
    if (!username.trim() || !password || (mode === "signup" && !email.trim())) {
      setError("Please fill in all fields.");
      return;
    }
    if (mode === "signup" && password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }

    setBusy(true);
    try {
      if (mode === "login") await login(username.trim(), password);
      else await register(username.trim(), email.trim(), password);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      setBusy(false);
    }
  };

  const tabClass = (m: Mode) =>
    cn(
      "flex-1 rounded-md px-3 py-2 text-sm font-medium transition-colors",
      mode === m
        ? "bg-primary/10 text-primary"
        : "text-muted-foreground hover:text-foreground",
    );

  // Portal target only exists on the client; render nothing during SSR.
  if (typeof document === "undefined") return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="relative w-full max-w-sm rounded-xl border border-border bg-background p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          aria-label={t("close")}
          className="absolute right-3 top-3 rounded-md p-1 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
        >
          <X className="h-4 w-4" />
        </button>

        <div className="mb-4 flex gap-1 rounded-lg border border-border p-1">
          <button type="button" className={tabClass("login")} onClick={() => setMode("login")}>
            {t("login")}
          </button>
          <button type="button" className={tabClass("signup")} onClick={() => setMode("signup")}>
            {t("signup")}
          </button>
        </div>

        <form onSubmit={submit} className="space-y-3">
          <label className="block">
            <span className="mb-1 block text-xs font-medium text-muted-foreground">
              {mode === "login" ? t("login_id") : t("username")}
            </span>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
            />
          </label>

          {mode === "signup" && (
            <label className="block">
              <span className="mb-1 block text-xs font-medium text-muted-foreground">
                {t("email")}
              </span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
              />
            </label>
          )}

          <label className="block">
            <span className="mb-1 block text-xs font-medium text-muted-foreground">
              {t("password")}
            </span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={mode === "login" ? "current-password" : "new-password"}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
            />
          </label>

          {error && (
            <p className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={busy}
            className="w-full rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
          >
            {mode === "login" ? t("login") : t("signup")}
          </button>
        </form>
      </div>
    </div>,
    document.body,
  );
}
