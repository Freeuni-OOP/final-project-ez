// A list of users (search results, followers, following) where each row links
// to the profile and carries its own follow/unfollow button. Rows track their
// follow state locally for instant feedback and roll back if the request fails;
// the shared mutation reconciles the surrounding caches on success.

import { useState } from "react";
import { Link } from "@tanstack/react-router";

import type { UserSummary } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { useToggleFollowUser } from "@/lib/queries";
import { EmptyState } from "@/components/states";

function UserRow({ person, onNavigate }: { person: UserSummary; onNavigate?: () => void }) {
  const { t } = useI18n();
  const { user } = useAuth();
  const toggle = useToggleFollowUser();

  const [following, setFollowing] = useState(person.isFollowing);
  const isSelf = user?.username === person.username;

  const onToggle = () => {
    const next = !following;
    setFollowing(next);
    toggle.mutate({ username: person.username, next }, { onError: () => setFollowing(!next) });
  };

  return (
    <li className="flex items-center justify-between gap-3 py-3">
      <Link
        to="/u/$username"
        params={{ username: person.username }}
        onClick={onNavigate}
        className="min-w-0 flex-1"
      >
        <p className="truncate text-sm font-semibold text-foreground hover:underline">
          {person.username}
        </p>
        <p className="text-xs text-muted-foreground">
          {t("joined")} {new Date(person.joinedAt).toLocaleDateString()}
        </p>
      </Link>

      {user && !isSelf && (
        <button
          type="button"
          onClick={onToggle}
          disabled={toggle.isPending}
          aria-pressed={following}
          className={
            following
              ? "shrink-0 rounded-md border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:border-primary hover:text-foreground disabled:opacity-60"
              : "shrink-0 rounded-md bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
          }
        >
          {following ? t("unfollow") : t("follow")}
        </button>
      )}
    </li>
  );
}

export function UserList({
  users,
  emptyMessage,
  onNavigate,
}: {
  users: UserSummary[];
  emptyMessage: string;
  onNavigate?: () => void;
}) {
  if (users.length === 0) {
    return <EmptyState message={emptyMessage} />;
  }

  return (
    <ul className="divide-y divide-border">
      {users.map((person) => (
        <UserRow key={person.username} person={person} onNavigate={onNavigate} />
      ))}
    </ul>
  );
}
