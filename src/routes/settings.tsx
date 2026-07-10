import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useState, type FormEvent } from "react";

import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { useChangeEmail, useChangePassword, useDeleteAccount } from "@/lib/queries";

export const Route = createFileRoute("/settings")({
  component: Settings,
});

type Feedback = { ok: boolean; text: string } | null;

const inputClass =
  "w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary";
const labelClass = "mb-1 block text-xs font-medium text-muted-foreground";
const primaryButton =
  "inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60";

function messageOf(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback;
}

function Shell({ children }: { children: React.ReactNode }) {
  const { t } = useI18n();
  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-lg px-6 py-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{t("settings_title")}</h1>
        <p className="mt-2 text-sm text-muted-foreground sm:text-base">{t("settings_sub")}</p>
        {children}
      </section>
    </main>
  );
}

function Settings() {
  const { t } = useI18n();
  const { user, loading, logout } = useAuth();
  const navigate = useNavigate();

  const changePassword = useChangePassword();
  const [pwCurrent, setPwCurrent] = useState("");
  const [pwNew, setPwNew] = useState("");
  const [pwFeedback, setPwFeedback] = useState<Feedback>(null);

  const changeEmail = useChangeEmail();
  const [emailCurrent, setEmailCurrent] = useState("");
  const [email, setEmail] = useState(user?.email ?? "");
  const [emailFeedback, setEmailFeedback] = useState<Feedback>(null);

  const deleteAccount = useDeleteAccount();
  const [confirming, setConfirming] = useState(false);
  const [deleteCurrent, setDeleteCurrent] = useState("");
  const [deleteError, setDeleteError] = useState<string | null>(null);

  if (loading) {
    return (
      <Shell>
        <p role="status" className="mt-8 text-sm text-muted-foreground">
          …
        </p>
      </Shell>
    );
  }
  if (!user) {
    return (
      <Shell>
        <p className="mt-8 text-sm text-muted-foreground">{t("login_to_settings")}</p>
      </Shell>
    );
  }

  const submitPassword = async (e: FormEvent) => {
    e.preventDefault();
    setPwFeedback(null);
    try {
      await changePassword.mutateAsync({ currentPassword: pwCurrent, newPassword: pwNew });
      setPwCurrent("");
      setPwNew("");
      setPwFeedback({ ok: true, text: t("password_updated") });
    } catch (error) {
      setPwFeedback({ ok: false, text: messageOf(error, t("something_wrong")) });
    }
  };

  const submitEmail = async (e: FormEvent) => {
    e.preventDefault();
    setEmailFeedback(null);
    try {
      const updated = await changeEmail.mutateAsync({ currentPassword: emailCurrent, email });
      setEmailCurrent("");
      setEmail(updated.email);
      setEmailFeedback({ ok: true, text: t("email_updated") });
    } catch (error) {
      setEmailFeedback({ ok: false, text: messageOf(error, t("something_wrong")) });
    }
  };

  const confirmDelete = async () => {
    setDeleteError(null);
    try {
      await deleteAccount.mutateAsync(deleteCurrent);
      logout();
      await navigate({ to: "/" });
    } catch (error) {
      setDeleteError(messageOf(error, t("something_wrong")));
    }
  };

  return (
    <Shell>
      <form onSubmit={submitPassword} className="mt-8 space-y-3">
        <h2 className="text-lg font-semibold">{t("change_password")}</h2>
        <label className="block">
          <span className={labelClass}>{t("current_password")}</span>
          <input
            type="password"
            autoComplete="current-password"
            value={pwCurrent}
            onChange={(e) => setPwCurrent(e.target.value)}
            className={inputClass}
          />
        </label>
        <label className="block">
          <span className={labelClass}>{t("new_password")}</span>
          <input
            type="password"
            autoComplete="new-password"
            value={pwNew}
            onChange={(e) => setPwNew(e.target.value)}
            className={inputClass}
          />
        </label>
        <button
          type="submit"
          disabled={changePassword.isPending || !pwCurrent || pwNew.length < 8}
          className={primaryButton}
        >
          {t("change_password")}
        </button>
        {pwFeedback && (
          <p className={pwFeedback.ok ? "text-xs text-primary" : "text-xs text-destructive"}>
            {pwFeedback.text}
          </p>
        )}
      </form>

      <form onSubmit={submitEmail} className="mt-10 space-y-3">
        <h2 className="text-lg font-semibold">{t("change_email")}</h2>
        <label className="block">
          <span className={labelClass}>{t("email")}</span>
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className={inputClass}
          />
        </label>
        <label className="block">
          <span className={labelClass}>{t("current_password")}</span>
          <input
            type="password"
            autoComplete="current-password"
            value={emailCurrent}
            onChange={(e) => setEmailCurrent(e.target.value)}
            className={inputClass}
          />
        </label>
        <button
          type="submit"
          disabled={changeEmail.isPending || !emailCurrent || !email.trim()}
          className={primaryButton}
        >
          {t("change_email")}
        </button>
        {emailFeedback && (
          <p className={emailFeedback.ok ? "text-xs text-primary" : "text-xs text-destructive"}>
            {emailFeedback.text}
          </p>
        )}
      </form>

      <div className="mt-10 rounded-xl border border-destructive/40 bg-destructive/5 p-4">
        <h2 className="text-lg font-semibold text-destructive">{t("delete_account")}</h2>
        <p className="mt-1 text-sm text-muted-foreground">{t("delete_account_warn")}</p>
        {!confirming ? (
          <button
            type="button"
            onClick={() => setConfirming(true)}
            className="mt-3 rounded-md border border-destructive px-4 py-2 text-sm font-semibold text-destructive transition-colors hover:bg-destructive hover:text-destructive-foreground"
          >
            {t("delete_account")}
          </button>
        ) : (
          <div className="mt-3 space-y-3">
            <label className="block">
              <span className={labelClass}>{t("current_password")}</span>
              <input
                type="password"
                autoComplete="current-password"
                value={deleteCurrent}
                onChange={(e) => setDeleteCurrent(e.target.value)}
                className={inputClass}
              />
            </label>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={confirmDelete}
                disabled={deleteAccount.isPending || !deleteCurrent}
                className="rounded-md bg-destructive px-4 py-2 text-sm font-semibold text-destructive-foreground transition-colors hover:bg-destructive/90 disabled:opacity-60"
              >
                {t("delete_account_confirm")}
              </button>
              <button
                type="button"
                onClick={() => setConfirming(false)}
                className="rounded-md border border-border px-4 py-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
              >
                {t("settings_cancel")}
              </button>
            </div>
            {deleteError && <p className="text-xs text-destructive">{deleteError}</p>}
          </div>
        )}
      </div>
    </Shell>
  );
}
