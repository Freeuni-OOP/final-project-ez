import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";

import { type PublicComposition, getPublicComposition } from "@/lib/api";
import { AudioEngine } from "@/lib/audio-engine";
import { SoundVisualizer } from "@/components/sound-visualizer";
import { LikeButton } from "@/components/like-button";
import { useI18n } from "@/lib/i18n";

export const Route = createFileRoute("/c/$slug")({
  component: PublicComposition_,
});

function PublicComposition_() {
  const { slug } = Route.useParams();
  const { t } = useI18n();

  const [comp, setComp] = useState<PublicComposition | null>(null);
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
          <p className="text-sm text-muted-foreground">…</p>
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
              <div className="mt-3 flex items-center gap-3">
                <button
                  onClick={playing ? stop : play}
                  className="inline-flex items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
                >
                  {playing ? t("stop") : t("play")}
                </button>
                <LikeButton
                  key={comp.id}
                  compositionId={comp.id}
                  likeCount={comp.likeCount}
                  likedByMe={comp.likedByMe}
                />
              </div>
            </div>

            <pre className="mt-4 overflow-x-auto rounded-md border border-border bg-background px-3 py-2 font-mono text-xs leading-relaxed text-foreground">
              {comp.pattern}
            </pre>
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
