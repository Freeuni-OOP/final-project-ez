// Login / sign-up modal. Tabbed between the two modes, with basic client-side
// validation and backend error messages surfaced inline. Closes on success.

import { useState } from "react";

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

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-sm rounded-xl border border-border bg-background p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
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
    </div>
  );
}
