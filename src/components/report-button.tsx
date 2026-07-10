// A small "Report" control. Only rendered for signed-in users; on success it
// switches to a "Reported" confirmation and won't fire again. Safe inside a link
// (it stops the click from bubbling).

import { useState, type MouseEvent } from "react";
import { Flag } from "lucide-react";

import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

interface ReportButtonProps {
  onReport: () => Promise<unknown>;
  className?: string;
}

export function ReportButton({ onReport, className }: ReportButtonProps) {
  const { user } = useAuth();
  const { t } = useI18n();
  const [reported, setReported] = useState(false);
  const [busy, setBusy] = useState(false);

  if (!user) return null;

  const click = async (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (busy || reported) return;
    setBusy(true);
    try {
      await onReport();
      setReported(true);
    } catch {
      // leave it clickable so the user can retry
    } finally {
      setBusy(false);
    }
  };

  return (
    <button
      type="button"
      onClick={click}
      disabled={busy || reported}
      className={cn(
        "inline-flex items-center gap-1.5 text-xs text-muted-foreground transition-colors hover:text-destructive disabled:opacity-60",
        reported && "text-destructive",
        className,
      )}
    >
      <Flag className="h-3.5 w-3.5" />
      {reported ? t("reported") : t("report")}
    </button>
  );
}
