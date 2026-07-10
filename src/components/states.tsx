// Shared loading / empty / error blocks so every route renders these states the
// same way, with the right accessibility semantics: loading and empty announce
// politely to screen readers, errors announce assertively as alerts. Callers pass
// their own spacing via className.

import { cn } from "@/lib/utils";

interface StateProps {
  message: string;
  className?: string;
}

export function LoadingState({ message, className }: StateProps) {
  return (
    <p role="status" aria-live="polite" className={cn("text-sm text-muted-foreground", className)}>
      {message}
    </p>
  );
}

export function EmptyState({ message, className }: StateProps) {
  return (
    <p role="status" className={cn("text-sm text-muted-foreground", className)}>
      {message}
    </p>
  );
}

export function ErrorState({ message, className }: StateProps) {
  return (
    <p role="alert" className={cn("text-sm text-muted-foreground", className)}>
      {message}
    </p>
  );
}
