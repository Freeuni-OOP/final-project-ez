import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState } from "react";

import { AudioEngine, Parser } from "@/lib/audio-engine";
import { SoundVisualizer } from "@/components/sound-visualizer";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "AlgoRythm — Hear your code" },
      {
        name: "description",
        content:
          "Write simple text patterns and hear them play. AlgoRythm turns code into music in the browser.",
      },
    ],
  }),
  component: Composer,
});

// A few ready-made patterns the user can load with one click.
const EXAMPLES: { name: string; pattern: string }[] = [
  {
    name: "Four on the floor",
    pattern: "drum: kick kick kick kick\ndrum: - hat - hat\nsynth: C4 - E4 -",
  },
  {
    name: "Backbeat",
    pattern: "drum: kick - snare -\ndrum: hat hat hat hat\nbass: C2 - G2 -",
  },
  {
    name: "Arp",
    pattern: "synth: C4 E4 G4 C5\nsaw: C3 - G3 -\ndrum: kick - kick -",
  },
];

type Mix = { volume: number; muted: boolean };

function Composer() {
  const { t } = useI18n();

  const [source, setSource] = useState(EXAMPLES[0].pattern);
  const [bpm, setBpm] = useState(120);
  const [playing, setPlaying] = useState(false);
  const [analyser, setAnalyser] = useState<AnalyserNode | null>(null);
  const [mix, setMix] = useState<Mix[]>([]);

  const engineRef = useRef<AudioEngine | null>(null);
  const getEngine = () => {
    if (!engineRef.current) engineRef.current = new AudioEngine(bpm);
    return engineRef.current;
  };

  // Parse the text for the grid + error list (read-only, no audio).
  const parser = useMemo(() => new Parser(), []);
  const parsed = useMemo(() => parser.parse(source), [parser, source]);
  const totalSteps = parsed.tracks.reduce((m, tr) => Math.max(m, tr.steps), 0);

  // Keep one mixer row per track; reuse settings when the track count changes.
  useEffect(() => {
    setMix((prev) =>
      parsed.tracks.map((tr, i) => prev[i] ?? { volume: tr.volume, muted: tr.muted }),
    );
  }, [parsed.tracks.length]);

  // Stop audio if the user leaves the page.
  useEffect(() => () => engineRef.current?.stop(), []);

  const play = () => {
    const engine = getEngine();
    engine.load(source);
    engine.setBpm(bpm);
    mix.forEach((m, i) => {
      engine.setTrackVolume(i, m.volume);
      engine.setTrackMuted(i, m.muted);
    });
    engine.play();
    setPlaying(true);
    setAnalyser(engine.getAnalyser());
  };

  const stop = () => {
    engineRef.current?.stop();
    setPlaying(false);
    setAnalyser(null);
  };

  const changeBpm = (value: number) => {
    setBpm(value);
    engineRef.current?.setBpm(value);
  };

  const changeVolume = (i: number, value: number) => {
    setMix((prev) => prev.map((m, idx) => (idx === i ? { ...m, volume: value } : m)));
    engineRef.current?.setTrackVolume(i, value);
  };

  const toggleMute = (i: number) => {
    const muted = !(mix[i]?.muted ?? false);
    setMix((prev) => prev.map((m, idx) => (idx === i ? { ...m, muted } : m)));
    engineRef.current?.setTrackMuted(i, muted);
  };

  return (
    <main className="min-h-screen bg-background text-foreground">
      {/* Hero */}
      <section className="relative overflow-hidden border-b border-border">
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-primary/20 via-transparent to-transparent" />
        <div className="relative mx-auto max-w-5xl px-6 py-16 text-center">
          <h1 className="whitespace-pre-line text-4xl font-bold tracking-tight sm:text-6xl">
            {t("brand")}
          </h1>
          <p className="mx-auto mt-4 max-w-2xl text-base text-muted-foreground sm:text-lg">
            {t("tagline")}
          </p>
        </div>
      </section>

      <div className="mx-auto grid max-w-6xl gap-6 px-6 py-10 lg:grid-cols-2">
        {/* Editor + controls */}
        <div className="flex flex-col gap-4">
          <div className="rounded-xl border border-border bg-foreground/5 p-4">
            <div className="mb-3 flex flex-wrap items-center gap-3">
              <button
                onClick={playing ? stop : play}
                className="inline-flex items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
              >
                {playing ? t("stop") : t("play")}
              </button>

              <label className="flex items-center gap-2 text-sm text-muted-foreground">
                {t("bpm")}
                <input
                  type="range"
                  min={40}
                  max={220}
                  value={bpm}
                  onChange={(e) => changeBpm(Number(e.target.value))}
                  className="accent-primary"
                />
                <span className="w-10 tabular-nums text-foreground">{bpm}</span>
              </label>
            </div>

            <label className="mb-1 block text-sm font-medium">{t("source")}</label>
            <textarea
              value={source}
              onChange={(e) => setSource(e.target.value)}
              spellCheck={false}
              rows={8}
              className="w-full resize-y rounded-md border border-border bg-background p-3 font-mono text-sm text-foreground outline-none focus:border-primary"
            />
            <p className="mt-1 text-xs text-muted-foreground">{t("rest_hint")}</p>

            {parsed.errors.length > 0 && (
              <div className="mt-3 rounded-md border border-destructive/40 bg-destructive/10 p-3 text-xs text-destructive">
                <p className="mb-1 font-semibold">{t("errors")}</p>
                <ul className="list-inside list-disc space-y-0.5">
                  {parsed.errors.map((err, i) => (
                    <li key={i}>{err}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>

          {/* Examples */}
          <div className="rounded-xl border border-border bg-foreground/5 p-4">
            <p className="mb-2 text-sm font-medium">{t("examples")}</p>
            <div className="flex flex-wrap gap-2">
              {EXAMPLES.map((ex) => (
                <button
                  key={ex.name}
                  onClick={() => setSource(ex.pattern)}
                  className="rounded-full border border-border px-3 py-1 text-xs text-muted-foreground transition-colors hover:border-primary hover:text-foreground"
                >
                  {ex.name}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Visualizer + grid + mixer */}
        <div className="flex flex-col gap-4">
          <div className="rounded-xl border border-border bg-foreground/5 p-4">
            <p className="mb-2 text-sm font-medium">{t("visualizer")}</p>
            <SoundVisualizer analyser={analyser} className="h-28" />
          </div>

          {/* Sequencer grid */}
          <div className="rounded-xl border border-border bg-foreground/5 p-4">
            <p className="mb-3 text-sm font-medium">{t("grid")}</p>
            {parsed.tracks.length === 0 ? (
              <p className="text-xs text-muted-foreground">—</p>
            ) : (
              <div className="space-y-2 overflow-x-auto">
                {parsed.tracks.map((track, ti) => (
                  <div key={ti} className="flex items-center gap-2">
                    <span className="w-14 shrink-0 truncate text-xs text-muted-foreground">
                      {track.kind}
                    </span>
                    <div className="flex gap-1">
                      {Array.from({ length: totalSteps }).map((_, s) => {
                        const note = track.notes.find((n) => n.step === s);
                        return (
                          <div
                            key={s}
                            title={note?.token}
                            className={cn(
                              "flex h-6 w-6 items-center justify-center rounded text-[9px]",
                              note
                                ? "bg-primary font-medium text-primary-foreground"
                                : "border border-border bg-background",
                            )}
                          >
                            {note ? note.token.slice(0, 3) : ""}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Mixer */}
          <div className="rounded-xl border border-border bg-foreground/5 p-4">
            <p className="mb-3 text-sm font-medium">
              {t("mixer")}
              <span className="ml-2 text-xs text-muted-foreground">
                {parsed.tracks.length} {t("tracks")}
              </span>
            </p>
            {parsed.tracks.length === 0 ? (
              <p className="text-xs text-muted-foreground">—</p>
            ) : (
              <div className="space-y-2">
                {parsed.tracks.map((track, i) => (
                  <div key={i} className="flex items-center gap-3">
                    <span className="w-14 shrink-0 truncate text-xs text-muted-foreground">
                      {track.kind}
                    </span>
                    <input
                      type="range"
                      min={0}
                      max={1}
                      step={0.01}
                      value={mix[i]?.volume ?? 0.8}
                      onChange={(e) => changeVolume(i, Number(e.target.value))}
                      className="flex-1 accent-primary"
                    />
                    <button
                      onClick={() => toggleMute(i)}
                      className={cn(
                        "rounded-md border px-2 py-1 text-xs transition-colors",
                        mix[i]?.muted
                          ? "border-primary bg-primary/10 text-primary"
                          : "border-border text-muted-foreground hover:text-foreground",
                      )}
                    >
                      {t("mute")}
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* How it works */}
      <section className="mx-auto max-w-3xl px-6 pb-16">
        <h2 className="mb-2 text-lg font-semibold">{t("how_title")}</h2>
        <p className="text-sm leading-relaxed text-muted-foreground">{t("how_body")}</p>
      </section>
    </main>
  );
}
