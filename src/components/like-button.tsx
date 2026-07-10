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

export function LikeButton({ compositionId, likeCount, likedByMe, className }: LikeButtonProps) {
  const { user } = useAuth();
  const { t } = useI18n();
  const [liked, setLiked] = useState(likedByMe);
  const [count, setCount] = useState(likeCount);
  const [busy, setBusy] = useState(false);

  if (!user) {
    return (
      <span
        className={cn("inline-flex items-center gap-1.5 text-sm text-muted-foreground", className)}
      >
        <Heart className="h-4 w-4" />
        {count}
      </span>
    );
  }

  const toggle = async (e: MouseEvent) => {
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
        liked ? "text-primary" : "text-muted-foreground hover:text-foreground",
        className,
      )}
    >
      <Heart className={cn("h-4 w-4", liked && "fill-current")} />
      {count}
    </button>
  );
}
