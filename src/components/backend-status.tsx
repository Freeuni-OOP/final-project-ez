import { useBackendHealth } from "@/lib/queries";

/**
 * Small, unobtrusive badge showing whether the backend is reachable.
 * Pings /api/health; renders nothing until the first check settles.
 */
export function BackendStatus() {
  const { data } = useBackendHealth();

  if (data === undefined) return null;
  const ok = data?.status === "ok";

  return (
    <div className="fixed bottom-3 right-3 z-50 flex items-center gap-2 rounded-full border border-border bg-background/80 px-3 py-1 text-xs text-muted-foreground backdrop-blur">
      <span className={`h-2 w-2 rounded-full ${ok ? "bg-green-500" : "bg-red-500"}`} />
      {ok ? "backend connected" : "backend offline"}
    </div>
  );
}
