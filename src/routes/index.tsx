import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useRef, useState } from "react";

import { AudioEngine, Parser } from "@/lib/audio-engine";
import { SoundVisualizer } from "@/components/sound-visualizer";
import { SpotlightCard } from "@/components/ui/spotlight-card";
import { CompositionBar } from "@/components/composition-bar";
import { useI18n } from "@/lib/i18n";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/")({
  validateSearch: (search: Record<string, unknown>) => {
    const rawEditId = search.editId;
    const parsedEditId =
      typeof rawEditId === "number"
        ? rawEditId
        : typeof rawEditId === "string"
          ? Number(rawEditId)
          : undefined;

    return {
      editId:
        parsedEditId !== undefined && Number.isFinite(parsedEditId) ? parsedEditId : undefined,
    };
  },
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
// Ready-made patterns the user can load with one click. Each carries its own
// tempo so loading it also sets the BPM. They span styles + the full instrument
// set so a new user immediately hears what the app can do.
const EXAMPLES: { name: string; bpm: number; pattern: string }[] = [
  {
    name: "Four on the floor",
    bpm: 128,
    pattern: [
      "drum: kick - kick - kick - kick -",
      "drum: - hat - hat - hat - hat",
      "drum: - - - - clap - - -",
      "bass: C2 - C2 - G1 - G1 -",
    ].join("\n"),
  },
  {
    name: "Slow pad piece",
    bpm: 70,
    pattern: [
      "pad: C3 - - - F3 - - -",
      "pad: E3 - - - A3 - - -",
      "chime: - - G5 - - - C6 -",
      "drum: kick - - - - - - -",
    ].join("\n"),
  },
  {
    name: "Melodic walk",
    bpm: 110,
    pattern: [
      "pluck: C4 E4 G4 B4 C5 B4 G4 E4",
      "bass: C2 - - - G2 - - -",
      "drum: - hat - hat - hat - hat",
    ].join("\n"),
  },
  {
    name: "Fast breakbeat",
    bpm: 160,
    pattern: [
      "drum: kick - - kick - - kick -",
      "drum: - - snare - - snare - snare",
      "drum: hat hat ohat hat hat hat ohat hat",
      "drum: - - - - - - tomlo tomhi",
    ].join("\n"),
  },
  {
    name: "Full arrangement",
    bpm: 124,
    pattern: [
      "drum: kick - - - kick - - -",
      "drum: - - - - snare - - -",
      "drum: hat hat hat hat hat hat hat hat",
      "drum: - - cowbell - - - cowbell -",
      "bass: C2 - E2 - G2 - E2 -",
      "lead: C5 - E5 G5 - B5 C6 -",
      "pad: C3 - - - - - - -",
    ].join("\n"),
  },
];

type Mix = { volume: number; muted: boolean; solo: boolean };

/**
 * Full-viewport animated landing: flowing lines on a canvas, a glow that follows
 * the cursor, and a display title where "code" is pixel-styled and "music" is
 * script-styled. A cue scrolls down to the composer below.
 */
function Hero() {
  const { t } = useI18n();
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const glowRef = useRef<HTMLDivElement | null>(null);
  const colorRef = useRef<HTMLSpanElement | null>(null);

  // Flowing-lines background.
  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!canvas || !ctx) return;

    let raf = 0;
    let time = 0;

    const resize = () => {
      const dpr = window.devicePixelRatio || 1;
      canvas.width = Math.max(1, Math.floor(canvas.clientWidth * dpr));
      canvas.height = Math.max(1, Math.floor(canvas.clientHeight * dpr));
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    window.addEventListener("resize", resize);

    const themeColor = () =>
      (colorRef.current && getComputedStyle(colorRef.current).color) || "#c084fc";

    const LINES = 6;
    const draw = () => {
      const w = canvas.clientWidth;
      const h = canvas.clientHeight;
      const color = themeColor();
      ctx.clearRect(0, 0, w, h);
      ctx.strokeStyle = color;
      ctx.lineWidth = 1.5;

      for (let l = 0; l < LINES; l++) {
        const amp = 26 + l * 16;
        const yBase = h * 0.5 + (l - LINES / 2) * 44;
        ctx.globalAlpha = 0.08 + l * 0.025;
        ctx.beginPath();
        for (let x = 0; x <= w; x += 6) {
          const y =
            yBase +
            Math.sin(x * 0.008 + time + l) * amp +
            Math.sin(x * 0.003 - time * 0.7) * amp * 0.4;
          if (x === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        }
        ctx.stroke();
      }
      ctx.globalAlpha = 1;
      time += 0.01;
      raf = requestAnimationFrame(draw);
    };

    raf = requestAnimationFrame(draw);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", resize);
    };
  }, []);

  const onMouseMove = (e: React.MouseEvent<HTMLElement>) => {
    const el = glowRef.current;
    if (!el) return;
    const rect = e.currentTarget.getBoundingClientRect();
    el.style.transform = `translate(${e.clientX - rect.left - 200}px, ${e.clientY - rect.top - 200}px)`;
  };

  const scrollToComposer = () => {
    document.getElementById("composer")?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <section
      onMouseMove={onMouseMove}
      className="relative h-screen w-full overflow-hidden bg-background"
    >
      <canvas ref={canvasRef} className="absolute inset-0 h-full w-full" />

      {/* glow that follows the cursor */}
      <div
        ref={glowRef}
        className="pointer-events-none absolute left-0 top-0 h-[400px] w-[400px] rounded-full bg-primary/25 blur-[120px]"
      />

      {/* hidden helper to read the theme color for the canvas */}
      <span ref={colorRef} className="hidden text-primary" aria-hidden />

      <div className="relative z-10 flex h-full flex-col items-center justify-center px-6 text-center">
        <h1 className="flex flex-wrap items-center justify-center gap-x-5 gap-y-2">
          <span className="hero-code text-5xl text-foreground sm:text-7xl">code</span>
          <span className="text-2xl font-light text-muted-foreground sm:text-4xl">becomes</span>
          <span className="hero-music text-6xl text-primary sm:text-8xl">music</span>
        </h1>

        <p className="mt-8 max-w-xl text-sm text-muted-foreground sm:text-base">{t("tagline")}</p>

        <button
          onClick={scrollToComposer}
          className="group mt-12 flex flex-col items-center gap-2 text-xs uppercase tracking-widest text-muted-foreground transition-colors hover:text-foreground"
        >
          {t("scroll_cue")}
          <span className="animate-bounce text-lg">↓</span>
        </button>
      </div>

      <style>{`
        .hero-code {
          font-family: 'Press Start 2P', monospace;
          animation: heroGlitch 5s infinite;
        }
        .hero-music {
          font-family: 'Pacifico', cursive;
          display: inline-block;
          animation: heroWave 4s ease-in-out infinite;
        }
        @keyframes heroGlitch {
          0%, 88%, 100% { transform: translate(0, 0); text-shadow: none; opacity: 1; }
          90% { transform: translate(-2px, 1px); text-shadow: 2px 0 var(--primary), -2px 0 #44d; opacity: .85; }
          92% { transform: translate(2px, -1px); text-shadow: -2px 0 var(--primary), 2px 0 #44d; }
          94% { transform: translate(-1px, 0); text-shadow: none; opacity: 1; }
        }
        @keyframes heroWave {
          0%, 100% { transform: translateY(0) rotate(-2deg); }
          50% { transform: translateY(-10px) rotate(2deg); }
        }
        @media (prefers-reduced-motion: reduce) {
          .hero-code, .hero-music { animation: none; }
        }
      `}</style>
    </section>
  );
}

function Composer() {
  const { editId } = Route.useSearch();
  const { t } = useI18n();

  const [source, setSource] = useState(EXAMPLES[0].pattern);
  const [bpm, setBpm] = useState(EXAMPLES[0].bpm);
  const [playing, setPlaying] = useState(false);
  const [analyser, setAnalyser] = useState<AnalyserNode | null>(null);
  const [master, setMaster] = useState(0.85);
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
      parsed.tracks.map(
        (tr, i) => prev[i] ?? { volume: tr.volume, muted: tr.muted, solo: tr.solo },
      ),
    );
  }, [parsed.tracks.length]);

  // Stop audio if the user leaves the page.
  useEffect(() => () => engineRef.current?.stop(), []);

  const play = () => {
    const engine = getEngine();
    engine.load(source);
    engine.setBpm(bpm);
    engine.setMasterVolume(master);
    mix.forEach((m, i) => {
      engine.setTrackVolume(i, m.volume);
      engine.setTrackMuted(i, m.muted);
      engine.setTrackSolo(i, m.solo);
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
    const v = Math.max(40, Math.min(220, Math.round(value || 0)));
    setBpm(v);
    engineRef.current?.setBpm(v);
  };

  const changeMaster = (value: number) => {
    setMaster(value);
    engineRef.current?.setMasterVolume(value);
  };

  // Load an example: set both its pattern and its tempo.
  const loadExample = (ex: (typeof EXAMPLES)[number]) => {
    setSource(ex.pattern);
    changeBpm(ex.bpm);
  };

  // Load a saved composition: its pattern + tempo.
  const loadComposition = (pattern: string, savedBpm: number) => {
    setSource(pattern);
    changeBpm(savedBpm);
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

  const toggleSolo = (i: number) => {
    const solo = !(mix[i]?.solo ?? false);
    setMix((prev) => prev.map((m, idx) => (idx === i ? { ...m, solo } : m)));
    engineRef.current?.setTrackSolo(i, solo);
  };

  const anySolo = mix.some((m) => m.solo);

  return (
    <main className="bg-background text-foreground">
      <Hero />

      {/* Composer */}
      <div id="composer" className="mx-auto grid max-w-6xl gap-6 px-6 py-12 lg:grid-cols-2">
        {/* Editor + controls */}
        <div className="flex flex-col gap-4">
          <div className="rounded-xl border border-border bg-foreground/5 p-4">
            <div className="mb-3 flex flex-wrap items-center gap-4">
              <button
                onClick={playing ? stop : play}
                className="inline-flex items-center justify-center rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
              >
                {playing ? t("stop") : t("play")}
              </button>

              {/* BPM stepper: minus / number / plus */}
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <span>{t("bpm")}</span>
                <div className="flex items-center overflow-hidden rounded-md border border-border">
                  <button
                    onClick={() => changeBpm(bpm - 1)}
                    aria-label="Decrease BPM"
                    className="px-2 py-1 text-foreground transition-colors hover:bg-accent"
                  >
                    −
                  </button>
                  <input
                    type="number"
                    min={40}
                    max={220}
                    value={bpm}
                    onChange={(e) => changeBpm(Number(e.target.value))}
                    className="w-14 border-x border-border bg-background py-1 text-center tabular-nums text-foreground outline-none"
                  />
                  <button
                    onClick={() => changeBpm(bpm + 1)}
                    aria-label="Increase BPM"
                    className="px-2 py-1 text-foreground transition-colors hover:bg-accent"
                  >
                    +
                  </button>
                </div>
              </div>

              {/* Master volume */}
              <label className="flex items-center gap-2 text-sm text-muted-foreground">
                {t("master")}
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.01}
                  value={master}
                  onChange={(e) => changeMaster(Number(e.target.value))}
                  className="accent-primary"
                />
                <span className="w-9 tabular-nums text-foreground">{Math.round(master * 100)}</span>
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
            <p className="mb-3 text-sm font-medium">{t("examples")}</p>
            <div className="grid gap-3 sm:grid-cols-3">
              {EXAMPLES.map((ex) => (
                <SpotlightCard key={ex.name} onClick={() => loadExample(ex)} className="p-3">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-medium text-foreground">{ex.name}</p>
                    <span className="shrink-0 rounded-full border border-border px-2 py-0.5 text-[10px] tabular-nums text-muted-foreground">
                      {ex.bpm} {t("bpm")}
                    </span>
                  </div>
                  <p className="mt-1 truncate font-mono text-[11px] text-muted-foreground">
                    {ex.pattern.split("\n")[0]}
                  </p>
                </SpotlightCard>
              ))}
            </div>
          </div>

          {/* Save / load (signed-in users) */}
          <CompositionBar source={source} bpm={bpm} onLoad={loadComposition} editId={editId} />
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
                  <div
                    key={ti}
                    className={cn(
                      "flex items-center gap-2 transition-opacity",
                      anySolo && !mix[ti]?.solo && "opacity-40",
                    )}
                  >
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
                  <div
                    key={i}
                    className={cn(
                      "flex items-center gap-3 transition-opacity",
                      anySolo && !mix[i]?.solo && "opacity-40",
                    )}
                  >
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
                      onClick={() => toggleSolo(i)}
                      className={cn(
                        "rounded-md border px-2 py-1 text-xs transition-colors",
                        mix[i]?.solo
                          ? "border-primary bg-primary/10 text-primary"
                          : "border-border text-muted-foreground hover:text-foreground",
                      )}
                    >
                      {t("solo")}
                    </button>
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
