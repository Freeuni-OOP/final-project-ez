// Modal listing a user's followers or following. Same portal + backdrop pattern
// as the auth dialog (rendered to document.body, closes on the X, Escape, or a
// backdrop click) so it floats above the header's backdrop-blur.

import { useEffect } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";

import type { UserSummary } from "@/lib/api";
import { useI18n } from "@/lib/i18n";
import { UserList } from "@/components/user-list";
import { LoadingState } from "@/components/states";

export function UserListDialog({
  title,
  users,
  isLoading,
  emptyMessage,
  onClose,
}: {
  title: string;
  users: UserSummary[] | undefined;
  isLoading: boolean;
  emptyMessage: string;
  onClose: () => void;
}) {
  const { t } = useI18n();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [onClose]);

  if (typeof document === "undefined") return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={title}
    >
      <div
        className="relative flex max-h-[80vh] w-full max-w-sm flex-col rounded-xl border border-border bg-background shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b border-border px-5 py-4">
          <h2 className="text-sm font-semibold text-foreground">{title}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label={t("close")}
            className="rounded-md p-1 text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="min-h-24 overflow-y-auto px-5 py-2">
          {isLoading || !users ? (
            <LoadingState message={t("loading")} className="py-3" />
          ) : (
            <UserList users={users} emptyMessage={emptyMessage} onNavigate={onClose} />
          )}
        </div>
      </div>
    </div>,
    document.body,
  );
}
