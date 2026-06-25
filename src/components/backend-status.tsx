import { useEffect, useState } from "react";
import { getHealth } from "@/lib/api";

/**
 * Small, unobtrusive badge showing whether the backend is reachable.
 * Pings /api/health once on mount; renders nothing until it knows.
 */
export function BackendStatus() {
  const [ok, setOk] = useState<boolean | null>(null);

  useEffect(() => {
    let active = true;
    getHealth().then((h) => {
      if (active) setOk(h?.status === "ok");
    });
    return () => {
      active = false;
    };
  }, []);

  if (ok === null) return null;

  return (
    <div className="fixed bottom-3 right-3 z-50 flex items-center gap-2 rounded-full border border-border bg-background/80 px-3 py-1 text-xs text-muted-foreground backdrop-blur">
      <span className={`h-2 w-2 rounded-full ${ok ? "bg-green-500" : "bg-red-500"}`} />
      {ok ? "backend connected" : "backend offline"}
    </div>
  );
}
