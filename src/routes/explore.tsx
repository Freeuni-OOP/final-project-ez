import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";

import { type PublicComposition, listPublicCompositions } from "@/lib/api";
import { SpotlightCard } from "@/components/ui/spotlight-card";
import { useI18n } from "@/lib/i18n";

const PAGE_SIZE = 20;

export const Route = createFileRoute("/explore")({
  head: () => ({
    meta: [
      { title: "AlgoRythm — Explore" },
      {
        name: "description",
        content: "Browse public compositions shared by the AlgoRythm community.",
      },
    ],
  }),
  component: Explore,
});

function Explore() {
  const { t } = useI18n();
  const [items, setItems] = useState<PublicComposition[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [page, setPage] = useState(0);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    listPublicCompositions(0, PAGE_SIZE)
      .then((batch) => {
        setItems(batch);
        setHasMore(batch.length === PAGE_SIZE);
      })
      .catch(() => {
        setItems([]);
        setHasMore(false);
      })
      .finally(() => setLoaded(true));
  }, []);

  const loadMore = () => {
    if (loadingMore || !hasMore) return;

    const nextPage = page + 1;
    setLoadingMore(true);

    listPublicCompositions(nextPage, PAGE_SIZE)
      .then((batch) => {
        setItems((prev) => [...prev, ...batch]);
        setPage(nextPage);
        setHasMore(batch.length === PAGE_SIZE);
      })
      .finally(() => setLoadingMore(false));
  };

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-6xl px-6 py-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
          {t("explore_title")}
        </h1>
        <p className="mt-2 text-sm text-muted-foreground sm:text-base">
          {t("explore_sub")}
        </p>

        {!loaded ? (
          <p className="mt-8 text-sm text-muted-foreground">Loading…</p>
        ) : items.length === 0 ? (
          <p className="mt-8 text-sm text-muted-foreground">{t("empty_explore")}</p>
        ) : (
          <>
            <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {items.map((c) => (
                <Link
                  key={c.slug}
                  to="/c/$slug"
                  params={{ slug: c.slug }}
                  className="block"
                >
                  <SpotlightCard className="flex h-full flex-col p-4">
                    <p className="truncate text-sm font-semibold text-foreground">
                      {c.title}
                    </p>
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

            {hasMore && (
              <button
                onClick={loadMore}
                disabled={loadingMore}
                className="mx-auto mt-8 block rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
              >
                {loadingMore ? "Loading…" : "Load more"}
              </button>
            )}
          </>
        )}
      </section>
    </main>
  );
}