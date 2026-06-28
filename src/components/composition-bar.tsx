// Save / load panel for the composer. Only shows when signed in. Saves the
// current pattern + BPM under a title, lists the user's saved compositions, and
// loads or deletes them. All calls go through the API client (token attached).

import { useEffect, useState } from "react";

import {
  type Composition,
  createComposition,
  deleteComposition,
  listCompositions,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";

export function CompositionBar({
  source,
  bpm,
  onLoad,
}: {
  source: string;
  bpm: number;
  onLoad: (pattern: string, bpm: number) => void;
}) {
  const { user } = useAuth();
  const { t } = useI18n();

  const [items, setItems] = useState<Composition[]>([]);
  const [title, setTitle] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const refresh = () => {
    listCompositions()
      .then(setItems)
      .catch(() => setItems([]));
  };

  // Load the list when the user signs in; clear it when they sign out.
  useEffect(() => {
    if (user) refresh();
    else setItems([]);
  }, [user]);

  if (!user) {
    return (
      <div className="rounded-xl border border-border bg-foreground/5 p-4 text-sm text-muted-foreground">
        {t("login_to_save")}
      </div>
    );
  }

  const save = async () => {
    setError(null);
    if (!title.trim()) {
      setError(t("title_required"));
      return;
    }
    setBusy(true);
    try {
      await createComposition({ title: title.trim(), pattern: source, bpm });
      setTitle("");
      refresh();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Save failed.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number) => {
    try {
      await deleteComposition(id);
      refresh();
    } catch {
      // ignore — the list refreshes on the next action
    }
  };

  return (
    <div className="rounded-xl border border-border bg-foreground/5 p-4">
      <p className="mb-3 text-sm font-medium">{t("my_compositions")}</p>

      <div className="flex gap-2">
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={t("composition_title")}
          className="flex-1 rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
        />
        <button
          onClick={save}
          disabled={busy}
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
        >
          {t("save")}
        </button>
      </div>

      {error && <p className="mt-2 text-xs text-destructive">{error}</p>}

      {items.length > 0 && (
        <ul className="mt-3 space-y-1">
          {items.map((c) => (
            <li
              key={c.id}
              className="flex items-center justify-between gap-2 rounded-md border border-border px-3 py-2"
            >
              <span className="truncate text-sm text-foreground">
                {c.title}
                <span className="ml-1 text-xs text-muted-foreground">
                  · {c.bpm} {t("bpm")}
                </span>
              </span>
              <span className="flex shrink-0 gap-3">
                <button
                  onClick={() => onLoad(c.pattern, c.bpm)}
                  className="text-xs font-medium text-primary hover:underline"
                >
                  {t("load")}
                </button>
                <button
                  onClick={() => remove(c.id)}
                  className="text-xs text-muted-foreground transition-colors hover:text-destructive"
                >
                  {t("delete")}
                </button>
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
