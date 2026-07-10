import { createFileRoute, Link, useNavigate, useSearch } from "@tanstack/react-router";
import { useState } from "react";

import { SpotlightCard } from "@/components/ui/spotlight-card";
import { LikeButton } from "@/components/like-button";
import { useI18n } from "@/lib/i18n";
import { useInfinitePublicCompositions } from "@/lib/queries";
import { EmptyState, LoadingState } from "@/components/states";

export const Route = createFileRoute("/explore")({
  validateSearch: (search: Record<string, unknown>) => ({
    tag: typeof search.tag === "string" ? search.tag : undefined,
    q: typeof search.q === "string" ? search.q : "",
  }),
  component: Explore,
});

function Explore() {
  const { t } = useI18n();
  const navigate = useNavigate({ from: "/explore" });
  const searchParams = useSearch({ from: "/explore" });

  const [searchText, setSearchText] = useState(searchParams.q ?? "");
  const selectedTag = searchParams.tag;
  const query = searchParams.q ?? "";

  const { data, isPending, isFetchingNextPage, hasNextPage, fetchNextPage } =
    useInfinitePublicCompositions(selectedTag, query);
  const items = data?.pages.flat() ?? [];

  function applySearch() {
    void navigate({
      search: {
        q: searchText.trim() || undefined,
        tag: selectedTag,
      },
    });
  }

  function selectTag(tag: string) {
    void navigate({
      search: {
        tag,
        q: undefined,
      },
    });
  }

  function clearFilters() {
    setSearchText("");
    void navigate({ search: {} });
  }

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-6xl px-6 py-12">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{t("explore_title")}</h1>
        <p className="mt-2 text-sm text-muted-foreground sm:text-base">{t("explore_sub")}</p>

        <div className="mt-6 flex flex-col gap-3 sm:flex-row">
          <input
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") applySearch();
            }}
            aria-label={t("explore_search_placeholder")}
            placeholder={t("explore_search_placeholder")}
            className="min-h-10 flex-1 rounded-md border border-border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
          <button
            type="button"
            onClick={applySearch}
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
          >
            {t("explore_search")}
          </button>
          {(selectedTag || query) && (
            <button
              type="button"
              onClick={clearFilters}
              className="rounded-md border border-border px-4 py-2 text-sm"
            >
              {t("explore_clear")}
            </button>
          )}
        </div>

        {selectedTag && (
          <p className="mt-4 text-sm text-muted-foreground">
            {t("explore_filtering_by")}{" "}
            <span className="font-medium text-foreground">#{selectedTag}</span>
          </p>
        )}

        {isPending ? (
          <LoadingState message={t("explore_loading")} className="mt-8" />
        ) : items.length === 0 ? (
          <EmptyState message={t("explore_none")} className="mt-8" />
        ) : (
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {items.map((c) => (
              <Link key={c.slug} to="/c/$slug" params={{ slug: c.slug }} className="block">
                <SpotlightCard className="flex h-full flex-col p-4">
                  <p className="truncate text-sm font-semibold text-foreground">{c.title}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("by")} {c.author} · {c.bpm} {t("bpm")}
                  </p>

                  {c.tags.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-2">
                      {c.tags.map((tag) => (
                        <button
                          key={tag}
                          type="button"
                          onClick={(e) => {
                            e.preventDefault();
                            selectTag(tag);
                          }}
                          className="rounded-full border border-border px-2 py-1 text-xs text-muted-foreground hover:text-foreground"
                        >
                          #{tag}
                        </button>
                      ))}
                    </div>
                  )}

                  <p className="mt-3 truncate font-mono text-[11px] text-muted-foreground">
                    {c.pattern.split("\n")[0]}
                  </p>
                  <div className="mt-3 flex justify-end">
                    <LikeButton
                      compositionId={c.id}
                      likeCount={c.likeCount}
                      likedByMe={c.likedByMe}
                    />
                  </div>
                </SpotlightCard>
              </Link>
            ))}
          </div>
        )}

        {hasNextPage && (
          <div className="mt-8 flex justify-center">
            <button
              type="button"
              onClick={() => fetchNextPage()}
              disabled={isFetchingNextPage}
              className="rounded-md border border-border px-5 py-2 text-sm font-medium text-foreground transition-colors hover:bg-accent disabled:opacity-60"
            >
              {isFetchingNextPage ? t("loading_more") : t("load_more")}
            </button>
          </div>
        )}
      </section>
    </main>
  );
}
