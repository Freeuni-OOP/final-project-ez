import { createFileRoute, Link } from "@tanstack/react-router";
import { useEffect, useRef, useState } from "react";

import { AudioEngine } from "@/lib/audio-engine";
import { SpotlightCard } from "@/components/ui/spotlight-card";
import { useI18n } from "@/lib/i18n";

export const Route = createFileRoute("/library")({
  head: () => ({
    meta: [
      { title: "AlgoRythm — Instrument Library" },
      {
        name: "description",
        content:
          "Every instrument and command you can use in an AlgoRythm pattern, with examples you can copy and previews you can hear.",
      },
    ],
  }),
  component: Library,
});

interface Entry {
  id: string;
  name: string;
  desc: string;
  example: string; // nice line to copy
  preview: string; // single hit + rests, to hear once
}

// Percussion: a "drum:" line with the sound name as the token.
const PERCUSSION: Entry[] = [
  {
    id: "kick",
    name: "Kick",
    desc: "Deep bass drum that anchors the downbeat.",
    example: "drum: kick - kick -",
    preview: "drum: kick - - - - - - -",
  },
  {
    id: "snare",
    name: "Snare",
    desc: "Sharp backbeat hit with a noisy body.",
    example: "drum: - snare - snare",
    preview: "drum: snare - - - - - - -",
  },
  {
    id: "hat",
    name: "Hi-hat",
    desc: "Tight closed hi-hat that keeps the pulse.",
    example: "drum: hat hat hat hat",
    preview: "drum: hat - - - - - - -",
  },
  {
    id: "clap",
    name: "Clap",
    desc: "Layered handclap, great on beats 2 and 4.",
    example: "drum: - clap - clap",
    preview: "drum: clap - - - - - - -",
  },
  {
    id: "ohat",
    name: "Open hi-hat",
    desc: "Open hi-hat with a longer, sizzling decay.",
    example: "drum: hat ohat hat ohat",
    preview: "drum: ohat - - - - - - -",
  },
  {
    id: "tom",
    name: "Tom (mid)",
    desc: "Mid tom with a short pitch drop.",
    example: "drum: tom - tom -",
    preview: "drum: tom - - - - - - -",
  },
  {
    id: "tomhi",
    name: "Tom (high)",
    desc: "Higher, snappier tom for fills.",
    example: "drum: tomhi tomhi - -",
    preview: "drum: tomhi - - - - - - -",
  },
  {
    id: "tomlo",
    name: "Tom (low)",
    desc: "Deep low tom for weighty fills.",
    example: "drum: tomlo - - tomlo",
    preview: "drum: tomlo - - - - - - -",
  },
  {
    id: "cowbell",
    name: "Cowbell",
    desc: "Metallic cowbell accent that cuts through.",
    example: "drum: - cowbell - cowbell",
    preview: "drum: cowbell - - - - - - -",
  },
];

// Pitched: the kind is the instrument, the tokens are notes like C4.
const PITCHED: Entry[] = [
  {
    id: "synth",
    name: "Synth",
    desc: "Triangle-wave lead — smooth and round.",
    example: "synth: C4 E4 G4 C5",
    preview: "synth: C4 - - - - - - -",
  },
  {
    id: "saw",
    name: "Saw",
    desc: "Bright sawtooth — rich and cutting.",
    example: "saw: C3 E3 G3 C4",
    preview: "saw: C3 - - - - - - -",
  },
  {
    id: "square",
    name: "Square",
    desc: "Hollow square wave with a retro, chiptune feel.",
    example: "square: C4 G4 C5 G4",
    preview: "square: C4 - - - - - - -",
  },
  {
    id: "sine",
    name: "Sine",
    desc: "Pure sine tone — soft and clean.",
    example: "sine: C4 E4 G4 C5",
    preview: "sine: C4 - - - - - - -",
  },
  {
    id: "bass",
    name: "Bass",
    desc: "Fat low bass with an added sub layer.",
    example: "bass: C2 - G2 -",
    preview: "bass: C2 - - - - - - -",
  },
  {
    id: "pad",
    name: "Pad",
    desc: "Warm, slow-attack pad for sustained chords.",
    example: "pad: C4 - - -",
    preview: "pad: C4 - - - - - - -",
  },
  {
    id: "pluck",
    name: "Pluck",
    desc: "Short plucked tone, bright then quick to fade.",
    example: "pluck: C4 E4 G4 C5",
    preview: "pluck: C4 - - - - - - -",
  },
  {
    id: "chime",
    name: "Chime",
    desc: "Bell-like chime with shimmering partials.",
    example: "chime: C5 G5 C6 -",
    preview: "chime: C5 - - - - - - -",
  },
  {
    id: "lead",
    name: "Lead",
    desc: "Bright, resonant lead that sits on top of a mix.",
    example: "lead: C4 E4 G4 B4",
    preview: "lead: C4 - - - - - - -",
  },
];

function Library() {
  const { t } = useI18n();
  const [copied, setCopied] = useState<string | null>(null);

  // One engine reused for previews; stopped before each new sound.
  const engineRef = useRef<AudioEngine | null>(null);
  const getEngine = () => {
    if (!engineRef.current) engineRef.current = new AudioEngine(120);
    return engineRef.current;
  };

  useEffect(() => () => engineRef.current?.stop(), []);

  const preview = (line: string) => {
    const engine = getEngine();
    engine.stop();
    engine.load(line);
    engine.play();
    window.setTimeout(() => engine.stop(), 700);
  };

  const copy = async (id: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(id);
      window.setTimeout(() => setCopied((c) => (c === id ? null : c)), 1500);
    } catch {
      // clipboard not available (e.g. insecure context) — ignore
    }
  };

  const renderCards = (entries: Entry[]) => (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {entries.map((e) => (
        <SpotlightCard key={e.id} className="flex h-full flex-col p-4">
          <h3 className="text-base font-semibold text-foreground">{e.name}</h3>
          <p className="mt-1 text-sm text-muted-foreground">{e.desc}</p>
          <code className="mt-3 block rounded-md border border-border bg-background px-3 py-2 font-mono text-xs text-foreground">
            {e.example}
          </code>
          <div className="mt-4 flex gap-2">
            <button
              onClick={() => copy(e.id, e.example)}
              className="rounded-md bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              {copied === e.id ? t("copied") : t("copy")}
            </button>
            <button
              onClick={() => preview(e.preview)}
              className="rounded-md border border-border px-3 py-1.5 text-xs font-medium text-muted-foreground transition-colors hover:border-primary hover:text-foreground"
            >
              {t("preview")}
            </button>
          </div>
        </SpotlightCard>
      ))}
    </div>
  );

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-6xl px-6 py-12">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{t("lib_title")}</h1>
            <p className="mt-2 max-w-2xl text-sm text-muted-foreground sm:text-base">
              {t("lib_sub")}
            </p>
          </div>
          <Link
            to="/"
            className="inline-flex items-center justify-center rounded-md border border-border px-4 py-2 text-sm font-medium text-foreground transition-colors hover:border-primary"
          >
            {t("open_composer")}
          </Link>
        </div>

        <h2 className="mt-10 mb-4 text-lg font-semibold">{t("category_drum")}</h2>
        {renderCards(PERCUSSION)}

        <h2 className="mt-10 mb-4 text-lg font-semibold">{t("category_pitched")}</h2>
        {renderCards(PITCHED)}
      </section>
    </main>
  );
}
