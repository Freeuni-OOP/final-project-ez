import { useEffect, useState } from "react";
import { useNavigate } from "@tanstack/react-router";

import { type Composition } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import {
  useCreateComposition,
  useDeleteComposition,
  useMyCompositions,
  useTogglePublish,
  useUpdateComposition,
} from "@/lib/queries";

export function CompositionBar({
  source,
  bpm,
  onLoad,
  editId,
}: {
  source: string;
  bpm: number;
  onLoad: (pattern: string, bpm: number) => void;
  editId?: number;
}) {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { t } = useI18n();

  const { data: items = [] } = useMyCompositions(user?.username);
  const createMutation = useCreateComposition();
  const updateMutation = useUpdateComposition();
  const deleteMutation = useDeleteComposition();
  const toggleMutation = useTogglePublish();

  const [title, setTitle] = useState("");
  const [tagsText, setTagsText] = useState("");
  const [editing, setEditing] = useState<Composition | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [copiedId, setCopiedId] = useState<number | null>(null);
  useEffect(() => {
    if (!user || !editId || items.length === 0) return;

    const composition = items.find((item) => item.id === editId);
    if (!composition || editing?.id === composition.id) return;

    setError(null);
    setEditing(composition);
    setTitle(composition.title);
    setTagsText(composition.tags.join(", "));
    onLoad(composition.pattern, composition.bpm);

    void navigate({
      to: "/",
      search: {},
      hash: "composer",
      replace: true,
    });
  }, [editId, editing?.id, items, navigate, onLoad, user]);
  if (!user) {
    return (
      <div className="rounded-xl border border-border bg-foreground/5 p-4 text-sm text-muted-foreground">
        {t("login_to_save")}
      </div>
    );
  }

  const parseTags = () =>
    tagsText
      .split(",")
      .map((tag) => tag.trim())
      .filter(Boolean);

  const resetForm = () => {
    setTitle("");
    setTagsText("");
    setEditing(null);
  };

  const save = async () => {
    setError(null);

    if (!title.trim()) {
      setError(t("title_required"));
      return;
    }

    try {
      const body = {
        title: title.trim(),
        pattern: source,
        bpm,
        tags: parseTags(),
      };

      if (editing) {
        await updateMutation.mutateAsync({ id: editing.id, body });
      } else {
        await createMutation.mutateAsync(body);
      }

      resetForm();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Save failed.");
    }
  };

  const startEdit = (c: Composition) => {
    setError(null);
    setEditing(c);
    setTitle(c.title);
    setTagsText(c.tags.join(", "));
    onLoad(c.pattern, c.bpm);
  };

  const remove = (id: number) => {
    deleteMutation.mutate(id);
  };

  const togglePublish = (c: Composition) => {
    toggleMutation.mutate(c, {
      onError: (e) => setError(e instanceof Error ? e.message : "Could not update."),
    });
  };

  const copyLink = async (c: Composition) => {
    if (!c.slug) return;

    try {
      await navigator.clipboard.writeText(`${window.location.origin}/c/${c.slug}`);
      setCopiedId(c.id);
      window.setTimeout(() => setCopiedId((id) => (id === c.id ? null : id)), 1500);
    } catch {
      // clipboard unavailable — ignore
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
          disabled={createMutation.isPending || updateMutation.isPending}
          className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
        >
          {editing ? "Update" : t("save")}
        </button>
      </div>

      <input
        value={tagsText}
        onChange={(e) => setTagsText(e.target.value)}
        placeholder="Tags, separated by commas"
        className="mt-2 w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
      />

      {editing && (
        <button
          type="button"
          onClick={resetForm}
          className="mt-2 text-xs font-medium text-muted-foreground hover:text-foreground"
        >
          Cancel edit
        </button>
      )}

      {error && <p className="mt-2 text-xs text-destructive">{error}</p>}

      {items.length > 0 && (
        <ul className="mt-3 space-y-2">
          {items.map((c) => (
            <li key={c.id} className="rounded-md border border-border px-3 py-2">
              <div className="flex items-center justify-between gap-2">
                <span className="truncate text-sm text-foreground">
                  {c.title}
                  <span className="ml-1 text-xs text-muted-foreground">
                    · {c.bpm} {t("bpm")}
                  </span>
                </span>

                <span className="flex shrink-0 gap-3 text-xs">
                  <button
                    onClick={() => onLoad(c.pattern, c.bpm)}
                    className="font-medium text-primary hover:underline"
                  >
                    {t("load")}
                  </button>
                  <button
                    onClick={() => startEdit(c)}
                    className="font-medium text-primary hover:underline"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => togglePublish(c)}
                    className="text-muted-foreground transition-colors hover:text-foreground"
                  >
                    {c.isPublic ? t("unpublish") : t("publish")}
                  </button>
                  <button
                    onClick={() => remove(c.id)}
                    className="text-muted-foreground transition-colors hover:text-destructive"
                  >
                    {t("delete")}
                  </button>
                </span>
              </div>

              {c.tags.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-2">
                  {c.tags.map((tag) => (
                    <span
                      key={tag}
                      className="rounded-full border border-border px-2 py-1 text-xs text-muted-foreground"
                    >
                      #{tag}
                    </span>
                  ))}
                </div>
              )}

              {c.isPublic && c.slug && (
                <div className="mt-1 flex items-center gap-2 text-xs text-muted-foreground">
                  <code className="truncate rounded bg-foreground/10 px-1.5 py-0.5">
                    /c/{c.slug}
                  </code>
                  <button onClick={() => copyLink(c)} className="text-primary hover:underline">
                    {copiedId === c.id ? t("link_copied") : t("copy_link")}
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
