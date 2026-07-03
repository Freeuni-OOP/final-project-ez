// Comments for a public composition: the list, plus a post form when signed in
// and a delete action on your own comments. Handles loading and error states.
// Posting appends optimistically-from-response; deleting is optimistic with
// rollback if the request fails.

import { useEffect, useState, type FormEvent } from "react";

import {
  type Comment,
  deleteComment,
  listComments,
  postComment,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";

interface CommentSectionProps {
  slug: string;
  compositionId: number;
}

export function CommentSection({ slug, compositionId }: CommentSectionProps) {
  const { user } = useAuth();
  const { t } = useI18n();

  // null = still loading
  const [comments, setComments] = useState<Comment[] | null>(null);
  const [error, setError] = useState(false);
  const [body, setBody] = useState("");
  const [posting, setPosting] = useState(false);

  useEffect(() => {
    let active = true;
    setComments(null);
    setError(false);
    listComments(slug)
      .then((cs) => {
        if (active) setComments(cs);
      })
      .catch(() => {
        if (active) setError(true);
      });
    return () => {
      active = false;
    };
  }, [slug]);

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const text = body.trim();
    if (!text || posting) return;
    setPosting(true);
    try {
      const created = await postComment(compositionId, text);
      setComments((cs) => [...(cs ?? []), created]);
      setBody("");
    } catch {
      // leave the text in place so the user can retry
    } finally {
      setPosting(false);
    }
  };

  const remove = async (id: number) => {
    const prev = comments;
    setComments((cs) => (cs ?? []).filter((c) => c.id !== id));
    try {
      await deleteComment(id);
    } catch {
      setComments(prev); // rollback
    }
  };

  return (
    <section className="mt-10">
      <h2 className="text-lg font-semibold">{t("comments")}</h2>

      {user ? (
        <form onSubmit={submit} className="mt-4">
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder={t("comment_placeholder")}
            maxLength={2000}
            rows={3}
            className="w-full resize-y rounded-md border border-border bg-background px-3 py-2 text-sm text-foreground outline-none focus:border-primary"
          />
          <div className="mt-2 flex justify-end">
            <button
              type="submit"
              disabled={posting || body.trim().length === 0}
              className="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-60"
            >
              {t("post_comment")}
            </button>
          </div>
        </form>
      ) : (
        <p className="mt-4 text-sm text-muted-foreground">{t("login_to_comment")}</p>
      )}

      <div className="mt-6 space-y-4">
        {error ? (
          <p className="text-sm text-muted-foreground">{t("comments_error")}</p>
        ) : comments === null ? (
          <p className="text-sm text-muted-foreground">…</p>
        ) : comments.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("no_comments")}</p>
        ) : (
          comments.map((c) => (
            <div
              key={c.id}
              className="rounded-md border border-border bg-foreground/5 px-3 py-2"
            >
              <div className="flex items-baseline justify-between gap-2">
                <span className="text-sm font-medium text-foreground">
                  {c.author}
                </span>
                <span className="text-xs text-muted-foreground">
                  {new Date(c.createdAt).toLocaleDateString()}
                </span>
              </div>
              <p className="mt-1 whitespace-pre-wrap break-words text-sm text-foreground">
                {c.body}
              </p>
              {user?.username === c.author && (
                <div className="mt-1 flex justify-end">
                  <button
                    type="button"
                    onClick={() => remove(c.id)}
                    className="text-xs text-muted-foreground transition-colors hover:text-destructive"
                  >
                    {t("delete")}
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </section>
  );
}
