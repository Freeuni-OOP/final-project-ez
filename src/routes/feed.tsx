import { createFileRoute, Link } from "@tanstack/react-router";

import { SpotlightCard } from "@/components/ui/spotlight-card";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { useFollowingFeed } from "@/lib/queries";

export const Route = createFileRoute("/feed")({
  head: () => ({
    meta: [
      { title: "AlgoRythm — Following" },
      {
        name: "description",
        content: "Public compositions from the people you follow.",
      },
    ],
  }),
  component: Feed,
});

function Feed() {
  const { t } = useI18n();
  const { user, loading } = useAuth();
  const { data, isPending } = useFollowingFeed(user?.username);
  const items = data ?? [];

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-6xl px-6 py-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{t("feed_title")}</h1>
        <p className="mt-2 text-sm text-muted-foreground sm:text-base">{t("feed_sub")}</p>

        {loading ? (
          <p className="mt-8 text-sm text-muted-foreground">…</p>
        ) : !user ? (
          <p className="mt-8 text-sm text-muted-foreground">{t("login_to_feed")}</p>
        ) : !isPending && items.length === 0 ? (
          <p className="mt-8 text-sm text-muted-foreground">{t("empty_feed")}</p>
        ) : (
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((c) => (
              <Link key={c.slug} to="/c/$slug" params={{ slug: c.slug }} className="block">
                <SpotlightCard className="flex h-full flex-col p-4">
                  <p className="truncate text-sm font-semibold text-foreground">{c.title}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("by")} {c.author} · {c.bpm} {t("bpm")}
                  </p>
                  <p className="mt-3 truncate font-mono text-[11px] text-muted-foreground">
                    {c.pattern.split("\n")[0]}
                  </p>
                </SpotlightCard>
              </Link>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
