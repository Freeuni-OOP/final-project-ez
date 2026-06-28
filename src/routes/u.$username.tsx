import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useState } from "react";

import { type UserProfile, getUserProfile } from "@/lib/api";
import { SpotlightCard } from "@/components/ui/spotlight-card";
import { useI18n } from "@/lib/i18n";

export const Route = createFileRoute("/u/$username")({
  component: Profile,
});

function Profile() {
  const { username } = Route.useParams();
  const { t } = useI18n();

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let active = true;
    setProfile(null);
    setError(false);
    getUserProfile(username)
      .then((p) => {
        if (active) setProfile(p);
      })
      .catch(() => {
        if (active) setError(true);
      });
    return () => {
      active = false;
    };
  }, [username]);

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-6xl px-6 py-12">
        {error ? (
          <p className="text-sm text-muted-foreground">{t("not_found")}</p>
        ) : !profile ? (
          <p className="text-sm text-muted-foreground">…</p>
        ) : (
          <>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
              {profile.username}
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              {t("joined")} {new Date(profile.joinedAt).toLocaleDateString()}
            </p>

            <h2 className="mt-8 mb-4 text-lg font-semibold">
              {t("published_works")}
            </h2>

            {profile.compositions.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t("no_published")}</p>
            ) : (
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {profile.compositions.map((c) => (
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
                        {c.bpm} {t("bpm")}
                      </p>
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
          <Link
            to="/explore"
            className="text-sm font-medium text-primary hover:underline"
          >
            ← {t("explore_title")}
          </Link>
        </div>
      </section>
    </main>
  );
}
