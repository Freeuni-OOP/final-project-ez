import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";

import {
  type Comment,
  type PublicComposition,
  getPublicComposition,
  listComments,
} from "@/lib/api";
import { AudioEngine } from "@/lib/audio-engine";
import { SoundVisualizer } from "@/components/sound-visualizer";
import { useI18n } from "@/lib/i18n";

const COMMENT_PAGE_SIZE = 20;

export const Route = createFileRoute("/c/$slug")({
  component: PublicComposition_,
});

function PublicComposition_() {
  const { slug } = Route.useParams();
  const { t } = useI18n();

  const [comp, setComp] = useState<PublicComposition | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [commentsLoaded, setCommentsLoaded] = useState(false);
  const [commentPage, setCommentPage] = useState(0);
  const [loadingMoreComments, setLoadingMoreComments] = useState(false);
  const [hasMoreComments, setHasMoreComments] = useState(true);

  const [error, setError] = useState(false);
  const [playing, setPlaying] = useState(false);
  const [analyser, setAnalyser] = useState<AnalyserNode | null>(null);
  const engineRef = useRef<AudioEngine | null>(null);

  useEffect(() => {
    let active = true;

    getPublicComposition(slug)
      .then((c) => {
        if (active) setComp(c);
      })
      .catch(() => {
        if (active) setError(true);
      });

    return () => {
      active = false;
      engineRef.current?.stop();
    };
  }, [slug]);

  useEffect(() => {
    let active = true;

    setCommentsLoaded(false);
    setCommentPage(0);
    setHasMoreComments(true);

    listComments(slug, 0, COMMENT_PAGE_SIZE)
      .then((batch) => {
        if (!active) return;
        setComments(batch);
        setHasMoreComments(batch.length === COMMENT_PAGE_SIZE);
      })
      .catch(() => {
        if (!active) return;
        setComments([]);
        setHasMoreComments(false);
      })
      .finally(() => {
        if (active) setCommentsLoaded(true);
      });

    return () => {
      active = false;
    };
  }, [slug]);

  const loadMoreComments = () => {
    if (loadingMoreComments || !hasMoreComments) return;

    const nextPage = commentPage + 1;
    setLoadingMoreComments(true);

    listComments(slug, nextPage, COMMENT_PAGE_SIZE)
      .then((batch) => {
        setComments((prev) => [...prev, ...batch]);
        setCommentPage(nextPage);
        setHasMoreComments(batch.length === COMMENT_PAGE_SIZE);
      })
      .finally(() => setLoadingMoreComments(false));
  };

  const play = () => {
    if (!comp) return;
    const engine = engineRef.current ?? (engineRef.current = new AudioEngine(comp.bpm));
    engine.load(comp.pattern);
    engine.setBpm(comp.bpm);
    engine.play();
    setPlaying(true);
    setAnalyser(engine.getAnalyser());
  };

  const stop = () => {
    engineRef.current?.stop();
    setPlaying(false);
    setAnalyser(null);
  };

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-3xl px-6 py-12">
        {error ? (
          <p className="text-sm text-muted-foreground">{t("not_found")}</p>
        ) : !comp ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : (
          <>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
              {comp.title}
            </h1>
            <p className="mt-2 text-sm text-muted-foreground">
              {t("by")}{" "}
              <Link
                to="/u/$username"
                params={{ username: comp.author }}
                className="text-primary hover:underline"
              >
                {comp.author}
              </Link>{" "}
              · {comp.bpm} {t("bpm")}
            </p>

            <div className="mt-6 rounded-xl border border-border bg-foreground/5 p-4">
              <SoundVisualizer analyser={analyser} className="h-24" />
              <button
                onClick={playing ? stop : play}
                className="mt-3 inline-flex items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
              >
                {playing ? t("stop") : t("play")}
              </button>
            </div>

            <pre className="mt-4 overflow-x-auto rounded-md border border-border bg-background px-3 py-2 font-mono text-xs leading-relaxed text-foreground">
              {comp.pattern}
            </pre>

            <section className="mt-8">
              <h2 className="text-xl font-semibold">Comments</h2>

              {!commentsLoaded ? (
                <p className="mt-4 text-sm text-muted-foreground">
                  Loading comments…
                </p>
              ) : comments.length === 0 ? (
                <p className="mt-4 text-sm text-muted-foreground">
                  No comments yet.
                </p>
              ) : (
                <div className="mt-4 space-y-3">
                  {comments.map((comment) => (
                    <div
                      key={comment.id}
                      className="rounded-md border border-border bg-foreground/5 p-3"
                    >
                      <p className="text-sm font-medium">{comment.author}</p>
                      <p className="mt-1 text-sm text-muted-foreground">
                        {comment.body}
                      </p>
                    </div>
                  ))}
                </div>
              )}

              {hasMoreComments && (
                <button
                  onClick={loadMoreComments}
                  disabled={loadingMoreComments}
                  className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
                >
                  {loadingMoreComments ? "Loading…" : "Load more comments"}
                </button>
              )}
            </section>
          </>
        )}

        <div className="mt-8">
          <Link
            to="/explore"
            className="text-sm font-medium text-primary hover:underline"
          >
            ← {t("explore_title")}
          </Link>
        </div>
      </section>
    </main>
  );
}