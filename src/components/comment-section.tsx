import { useState, type FormEvent } from "react";

import { reportComment } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { useComments, useDeleteComment, usePostComment } from "@/lib/queries";
import { ReportButton } from "@/components/report-button";

interface CommentSectionProps {
  slug: string;
  compositionId: number;
}

export function CommentSection({ slug, compositionId }: CommentSectionProps) {
  const { user } = useAuth();
  const { t } = useI18n();
  const { data: comments, isPending, isError } = useComments(slug);
  const postComment = usePostComment(slug);
  const deleteComment = useDeleteComment(slug);
  const [body, setBody] = useState("");

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    const text = body.trim();
    if (!text || postComment.isPending) return;
    try {
      await postComment.mutateAsync({ compositionId, body: text });
      setBody("");
    } catch {
      // keep the text so the user can retry
    }
  };

  const list = comments ?? [];

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
              disabled={postComment.isPending || body.trim().length === 0}
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
        {isError ? (
          <p role="alert" className="text-sm text-muted-foreground">
            {t("comments_error")}
          </p>
        ) : isPending ? (
          <p role="status" className="text-sm text-muted-foreground">
            …
          </p>
        ) : list.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t("no_comments")}</p>
        ) : (
          list.map((c) => (
            <div key={c.id} className="rounded-md border border-border bg-foreground/5 px-3 py-2">
              <div className="flex items-baseline justify-between gap-2">
                <span className="text-sm font-medium text-foreground">{c.author}</span>
                <span className="text-xs text-muted-foreground">
                  {new Date(c.createdAt).toLocaleDateString()}
                </span>
              </div>
              <p className="mt-1 whitespace-pre-wrap break-words text-sm text-foreground">
                {c.body}
              </p>
              <div className="mt-1 flex items-center justify-end gap-3">
                {user && <ReportButton onReport={() => reportComment(c.id)} />}
                {user?.username === c.author && (
                  <button
                    type="button"
                    onClick={() => deleteComment.mutate(c.id)}
                    className="text-xs text-muted-foreground transition-colors hover:text-destructive"
                  >
                    {t("delete")}
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
}
