// Reusable card with a soft "spotlight" glow that follows the cursor.
// The glow is a radial-gradient positioned at CSS vars (--x/--y) that we update
// on mouse move, tinted with the theme primary color. Pass onClick to make the
// whole card act as a button.

import { useRef, type CSSProperties, type ReactNode } from "react";
import { cn } from "@/lib/utils";

interface SpotlightCardProps {
  children: ReactNode;
  /** Classes for the inner content wrapper (padding, layout, etc). */
  className?: string;
  /** If set, the whole card is clickable (keyboard-accessible). */
  onClick?: () => void;
}

export function SpotlightCard({ children, className, onClick }: SpotlightCardProps) {
  const ref = useRef<HTMLDivElement | null>(null);
  const interactive = typeof onClick === "function";

  const handleMove = (e: React.MouseEvent<HTMLDivElement>) => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    el.style.setProperty("--x", `${e.clientX - r.left}px`);
    el.style.setProperty("--y", `${e.clientY - r.top}px`);
  };

  return (
    <div
      ref={ref}
      onMouseMove={handleMove}
      onClick={onClick}
      role={interactive ? "button" : undefined}
      tabIndex={interactive ? 0 : undefined}
      onKeyDown={
        interactive
          ? (e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onClick?.();
              }
            }
          : undefined
      }
      style={{ "--x": "50%", "--y": "50%" } as CSSProperties}
      className={cn(
        "group relative overflow-hidden rounded-xl border border-border bg-foreground/5 transition-colors duration-200 hover:border-primary/60",
        interactive &&
          "cursor-pointer text-left outline-none focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/40",
      )}
    >
      {/* spotlight glow */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 opacity-0 transition-opacity duration-300 group-hover:opacity-20"
        style={{
          background:
            "radial-gradient(240px circle at var(--x) var(--y), var(--primary), transparent 65%)",
        }}
      />
      <div className={cn("relative", className)}>{children}</div>
    </div>
  );
}
