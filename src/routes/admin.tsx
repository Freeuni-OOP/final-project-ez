import { createFileRoute } from "@tanstack/react-router";

import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import {
  useAdminRemoveComment,
  useAdminRemoveComposition,
  useAdminReports,
  useDismissReport,
  useSiteStats,
} from "@/lib/queries";

export const Route = createFileRoute("/admin")({
  component: Admin,
});

function Admin() {
  const { t } = useI18n();
  const { user, loading } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const stats = useSiteStats(isAdmin);
  const reports = useAdminReports(isAdmin);
  const removeComposition = useAdminRemoveComposition();
  const removeComment = useAdminRemoveComment();
  const dismiss = useDismissReport();

  const busy = removeComposition.isPending || removeComment.isPending || dismiss.isPending;

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-4xl px-6 py-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{t("admin_title")}</h1>
        <p className="mt-2 text-sm text-muted-foreground sm:text-base">{t("admin_sub")}</p>

        {loading ? (
          <p className="mt-8 text-sm text-muted-foreground">…</p>
        ) : !isAdmin ? (
          <p className="mt-8 text-sm text-muted-foreground">{t("admin_forbidden")}</p>
        ) : (
          <>
            {stats.data && (
              <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-3">
                <StatCard label={t("admin_users")} value={stats.data.userCount} />
                <StatCard label={t("admin_compositions")} value={stats.data.compositionCount} />
                <StatCard label={t("admin_open_reports")} value={stats.data.openReportCount} />
              </div>
            )}

            <h2 className="mt-10 mb-4 text-lg font-semibold">{t("admin_reports")}</h2>

            {reports.isLoading ? (
              <p className="text-sm text-muted-foreground">…</p>
            ) : reports.isError ? (
              <p className="text-sm text-muted-foreground">{t("admin_forbidden")}</p>
            ) : (reports.data ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">{t("admin_no_reports")}</p>
            ) : (
              <ul className="space-y-3">
                {(reports.data ?? []).map((r) => (
                  <li
                    key={r.id}
                    className="rounded-md border border-border bg-foreground/5 px-4 py-3"
                  >
                    <div className="flex items-baseline justify-between gap-2">
                      <span className="text-xs font-medium uppercase tracking-wide text-muted-foreground">
                        {r.targetType}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {t("admin_reported_by")} {r.reporter}
                      </span>
                    </div>
                    <p className="mt-1 truncate text-sm font-medium text-foreground">
                      {r.targetLabel}
                    </p>
                    {r.reason && <p className="mt-1 text-sm text-muted-foreground">“{r.reason}”</p>}
                    <div className="mt-3 flex gap-2">
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() =>
                          r.targetType === "COMPOSITION"
                            ? removeComposition.mutate(r.targetId)
                            : removeComment.mutate(r.targetId)
                        }
                        className="rounded-md bg-destructive px-3 py-1.5 text-xs font-semibold text-destructive-foreground transition-colors hover:bg-destructive/90 disabled:opacity-60"
                      >
                        {t("admin_remove")}
                      </button>
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => dismiss.mutate(r.id)}
                        className="rounded-md border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground disabled:opacity-60"
                      >
                        {t("admin_dismiss")}
                      </button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </section>
    </main>
  );
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-xl border border-border bg-foreground/5 p-4">
      <p className="text-2xl font-bold text-foreground">{value}</p>
      <p className="mt-1 text-xs text-muted-foreground">{label}</p>
    </div>
  );
}
