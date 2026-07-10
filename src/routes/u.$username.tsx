import { createFileRoute, Link } from "@tanstack/react-router";
import { useState } from "react";

import { useAuth } from "@/lib/auth";
import { SpotlightCard } from "@/components/ui/spotlight-card";
import { useI18n } from "@/lib/i18n";
import { useToggleFollow, useUserFollowers, useUserFollowing, useUserProfile } from "@/lib/queries";
import { ErrorState, LoadingState } from "@/components/states";
import { UserListDialog } from "@/components/user-list-dialog";

export const Route = createFileRoute("/u/$username")({
  component: Profile,
});

function Profile() {
  const { username } = Route.useParams();
  const { t } = useI18n();
  const { user } = useAuth();

  const { data: profile, isError } = useUserProfile(username);
  const toggleFollow = useToggleFollow(username);

  const [openList, setOpenList] = useState<"followers" | "following" | null>(null);
  const followers = useUserFollowers(username, openList === "followers");
  const following = useUserFollowing(username, openList === "following");

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-6xl px-6 py-12">
        {isError ? (
          <ErrorState message={t("not_found")} />
        ) : !profile ? (
          <LoadingState message={t("loading")} />
        ) : (
          <>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{profile.username}</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {t("joined")} {new Date(profile.joinedAt).toLocaleDateString()}
            </p>

            <div className="mt-3 flex flex-wrap items-center gap-4">
              <p className="text-sm text-muted-foreground">
                <button
                  type="button"
                  onClick={() => setOpenList("followers")}
                  className="rounded-sm transition-colors hover:text-foreground"
                >
                  <span className="font-semibold text-foreground">{profile.followerCount}</span>{" "}
                  {t("followers")}
                </button>{" "}
                ·{" "}
                <button
                  type="button"
                  onClick={() => setOpenList("following")}
                  className="rounded-sm transition-colors hover:text-foreground"
                >
                  <span className="font-semibold text-foreground">{profile.followingCount}</span>{" "}
                  {t("following")}
                </button>
              </p>
              {user && user.username !== profile.username && (
                <button
                  type="button"
                  onClick={() => toggleFollow.mutate(!profile.isFollowing)}
                  disabled={toggleFollow.isPending}
                  aria-pressed={profile.isFollowing}
                  className={
                    profile.isFollowing
                      ? "rounded-md border border-border px-4 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:border-primary hover:text-foreground disabled:opacity-60"
                      : "rounded-md bg-primary px-4 py-1.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
                  }
                >
                  {profile.isFollowing ? t("unfollow") : t("follow")}
                </button>
              )}
            </div>

            <h2 className="mt-8 mb-4 text-lg font-semibold">{t("published_works")}</h2>

            {profile.compositions.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t("no_published")}</p>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {profile.compositions.map((c) => (
                  <Link key={c.slug} to="/c/$slug" params={{ slug: c.slug }} className="block">
                    <SpotlightCard className="flex h-full flex-col p-4">
                      <p className="truncate text-sm font-semibold text-foreground">{c.title}</p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {c.bpm} {t("bpm")}
                      </p>

                      {c.tags.length > 0 && (
                        <div className="mt-3 flex flex-wrap gap-2">
                          {c.tags.map((tag) => (
                            <span
                              key={tag}
                              className="rounded-full border border-border px-2 py-1 text-xs text-muted-foreground"
                            >
                              #{tag}
                            </span>
                          ))}
                        </div>
                      )}

                      <p className="mt-3 truncate font-mono text-[11px] text-muted-foreground">
                        {c.pattern.split("\n")[0]}
                      </p>
                    </SpotlightCard>
                  </Link>
                ))}
              </div>
            )}
          </>
        )}

        <div className="mt-8">
          <Link to="/explore" className="text-sm font-medium text-primary hover:underline">
            ← {t("explore_title")}
          </Link>
        </div>
      </section>

      {openList === "followers" && (
        <UserListDialog
          title={t("followers")}
          users={followers.data}
          isLoading={followers.isPending}
          emptyMessage={t("no_followers")}
          onClose={() => setOpenList(null)}
        />
      )}
      {openList === "following" && (
        <UserListDialog
          title={t("following")}
          users={following.data}
          isLoading={following.isPending}
          emptyMessage={t("no_following")}
          onClose={() => setOpenList(null)}
        />
      )}
    </main>
  );
}
