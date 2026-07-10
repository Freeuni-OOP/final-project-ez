import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";

import { AudioEngine } from "@/lib/audio-engine";
import { SoundVisualizer } from "@/components/sound-visualizer";
import { useAuth } from "@/lib/auth";
import { useI18n } from "@/lib/i18n";
import { usePublicComposition, useRemixComposition, useReportComposition } from "@/lib/queries";
import { ReportButton } from "@/components/report-button";

export const Route = createFileRoute("/c/$slug")({
  component: PublicComposition_,
});

function PublicComposition_() {
  const { slug } = Route.useParams();
  const navigate = useNavigate();
  const { t } = useI18n();
  const { user } = useAuth();

  const { data: comp, isError } = usePublicComposition(slug);
  const remixMutation = useRemixComposition();
  const reportMutation = useReportComposition();
  const [playing, setPlaying] = useState(false);
  const [analyser, setAnalyser] = useState<AnalyserNode | null>(null);
  const [remixError, setRemixError] = useState<string | null>(null);
  const engineRef = useRef<AudioEngine | null>(null);

  // Stop any playback from the previous composition when navigating to a
  // different slug (or away from this page entirely).
  useEffect(() => {
    return () => {
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
  const remix = async () => {
    setRemixError(null);

    try {
      const copy = await remixMutation.mutateAsync(slug);

      await navigate({
        to: "/",
        search: { editId: copy.id },
        hash: "composer",
      });
    } catch (error) {
      setRemixError(error instanceof Error ? error.message : "Could not remix composition.");
    }
  };

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-3xl px-6 py-12">
        {isError ? (
          <p className="text-sm text-muted-foreground">{t("not_found")}</p>
        ) : !comp ? (
          <p className="text-sm text-muted-foreground">…</p>
        ) : (
          <>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{comp.title}</h1>
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
            {comp.tags.length > 0 && (
              <div className="mt-3 flex flex-wrap gap-2">
                {comp.tags.map((tag) => (
                  <Link
                    key={tag}
                    to="/explore"
                    search={{ tag }}
                    className="rounded-full border border-border px-2 py-1 text-xs text-muted-foreground hover:text-foreground"
                  >
                    #{tag}
                  </Link>
                ))}
              </div>
            )}

            <div className="mt-6 rounded-xl border border-border bg-foreground/5 p-4">
              <SoundVisualizer analyser={analyser} className="h-24" />
              <button
                onClick={playing ? stop : play}
                className="mt-3 inline-flex items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
              >
                {playing ? t("stop") : t("play")}
              </button>
            </div>
            {user && (
              <div className="mt-4 rounded-xl border border-border bg-foreground/5 p-4">
                <button
                  type="button"
                  onClick={remix}
                  disabled={remixMutation.isPending}
                  className="inline-flex items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {remixMutation.isPending ? "Creating your copy..." : "Remix"}
                </button>

                <p className="mt-2 text-xs text-muted-foreground">
                  Creates your own private copy. The original composition will not be changed.
                </p>

                {remixError && <p className="mt-2 text-xs text-destructive">{remixError}</p>}
              </div>
            )}
            <pre className="mt-4 overflow-x-auto rounded-md border border-border bg-background px-3 py-2 font-mono text-xs leading-relaxed text-foreground">
              {comp.pattern}
            </pre>

            <div className="mt-4">
              <ReportButton onReport={() => reportMutation.mutateAsync({ id: comp.id })} />
            </div>
          </>
        )}

        <div className="mt-8">
          <Link to="/explore" className="text-sm font-medium text-primary hover:underline">
            ← {t("explore_title")}
          </Link>
        </div>
      </section>
    </main>
  );
}
