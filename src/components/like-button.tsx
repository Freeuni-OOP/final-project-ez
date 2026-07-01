// Like button + count for a public composition. When signed in it toggles the
// like (optimistic update, rolled back if the request fails). When signed out it
// shows the count read-only. Safe to place inside a <Link> — it stops the click
// from bubbling into navigation.

import { useState, type MouseEvent } from "react";
import { Heart } from "lucide-react";

import { likeComposition, unlikeComposition } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

interface LikeButtonProps {
  compositionId: number;
  likeCount: number;
  likedByMe: boolean;
  className?: string;
}

export function LikeButton({
  compositionId,
  likeCount,
  likedByMe,
  className,
}: LikeButtonProps) {
  const { user } = useAuth();
  const { t } = useI18n();
  const [liked, setLiked] = useState(likedByMe);
  const [count, setCount] = useState(likeCount);
  const [busy, setBusy] = useState(false);

  // Signed out: show the count, no interaction.
  if (!user) {
    return (
      <span
        className={cn(
          "inline-flex items-center gap-1.5 text-sm text-muted-foreground",
          className,
        )}
      >
        <Heart className="h-4 w-4" />
        {count}
      </span>
    );
  }

  const toggle = async (e: MouseEvent) => {
    // Don't trigger the surrounding card link.
    e.preventDefault();
    e.stopPropagation();
    if (busy) return;

    const next = !liked;
    setLiked(next);
    setCount((c) => c + (next ? 1 : -1));
    setBusy(true);
    try {
      if (next) await likeComposition(compositionId);
      else await unlikeComposition(compositionId);
    } catch {
      // Roll back on failure.
      setLiked(!next);
      setCount((c) => c + (next ? -1 : 1));
    } finally {
      setBusy(false);
    }
  };

  return (
    <button
      type="button"
      onClick={toggle}
      disabled={busy}
      aria-pressed={liked}
      aria-label={liked ? t("unlike") : t("like")}
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-sm transition-colors disabled:opacity-60",
        liked
          ? "text-primary"
          : "text-muted-foreground hover:text-foreground",
        className,
      )}
    >
      <Heart className={cn("h-4 w-4", liked && "fill-current")} />
      {count}
    </button>
  );
}
