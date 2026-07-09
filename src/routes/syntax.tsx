import { createFileRoute, Link } from "@tanstack/react-router";

import { useI18n } from "@/lib/i18n";

export const Route = createFileRoute("/syntax")({
  head: () => ({
    meta: [
      { title: "AlgoRythm — Syntax Guide" },
      {
        name: "description",
        content:
          "Learn the small pattern language behind AlgoRythm: tracks, the timing grid, rests, and drum vs pitched tokens.",
      },
    ],
  }),
  component: Syntax,
});

function Example({ children }: { children: string }) {
  return (
    <pre className="mt-3 overflow-x-auto rounded-md border border-border bg-background px-3 py-2 font-mono text-xs leading-relaxed text-foreground">
      {children}
    </pre>
  );
}

function Syntax() {
  const { t } = useI18n();

  return (
    <main className="min-h-screen bg-background text-foreground">
      <section className="mx-auto max-w-3xl px-6 py-12">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{t("syntax_title")}</h1>
            <p className="mt-2 text-sm text-muted-foreground sm:text-base">{t("syntax_sub")}</p>
          </div>
          <Link
            to="/"
            className="inline-flex items-center justify-center rounded-md border border-border px-4 py-2 text-sm font-medium text-foreground transition-colors hover:border-primary"
          >
            {t("open_composer")}
          </Link>
        </div>

        {/* A line is a track */}
        <h2 className="mt-10 text-lg font-semibold">A line is a track</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          Each line of your pattern is one track. Write the <em>kind</em> of track, then a colon,
          then a series of tokens separated by spaces. The kind tells AlgoRythm what instrument to
          use; the tokens are what it plays.
        </p>
        <Example>{`drum: kick hat snare hat
synth: C4 E4 G4 C5`}</Example>

        {/* The timing grid */}
        <h2 className="mt-10 text-lg font-semibold">The timing grid</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          Every line shares one timing grid. The first token of each line plays on the first step,
          the second token on the second step, and so on, all the way across. So tokens that sit in
          the same column play together. The tempo is set by the BPM control on the composer.
        </p>
        <Example>{`drum:  kick  -     snare -
drum:   hat   hat   hat   hat
        ^step1 ^step2 ^step3 ^step4`}</Example>

        {/* Rests */}
        <h2 className="mt-10 text-lg font-semibold">Rests</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          A single hyphen <code className="rounded bg-foreground/10 px-1">-</code> is a rest: that
          step stays silent. Use rests to leave space and line tracks up against each other.
        </p>
        <Example>{`drum: kick - snare -`}</Example>

        {/* Drums vs pitched */}
        <h2 className="mt-10 text-lg font-semibold">Drum tokens vs pitched tokens</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          A <strong>drum</strong> line uses named percussion sounds as its tokens — for example{" "}
          <code className="rounded bg-foreground/10 px-1">kick</code>,{" "}
          <code className="rounded bg-foreground/10 px-1">snare</code>,{" "}
          <code className="rounded bg-foreground/10 px-1">hat</code>, or{" "}
          <code className="rounded bg-foreground/10 px-1">clap</code>.
        </p>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          A <strong>pitched</strong> line (like{" "}
          <code className="rounded bg-foreground/10 px-1">synth</code>,{" "}
          <code className="rounded bg-foreground/10 px-1">saw</code>,{" "}
          <code className="rounded bg-foreground/10 px-1">square</code>,{" "}
          <code className="rounded bg-foreground/10 px-1">sine</code> or{" "}
          <code className="rounded bg-foreground/10 px-1">bass</code>) uses note tokens. A note is a
          letter A–G, an optional <code className="rounded bg-foreground/10 px-1">#</code> (sharp)
          or <code className="rounded bg-foreground/10 px-1">b</code> (flat), and an octave number —
          for example <code className="rounded bg-foreground/10 px-1">C4</code> or{" "}
          <code className="rounded bg-foreground/10 px-1">F#5</code>. Higher octave numbers sound
          higher.
        </p>
        <Example>{`drum:  kick snare hat clap
synth: C4 E4 G4 C5
bass:  C2 - G2 -`}</Example>

        {/* Reference table */}
        <h2 className="mt-10 text-lg font-semibold">Track kinds at a glance</h2>
        <div className="mt-3 overflow-x-auto">
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-border text-muted-foreground">
                <th className="py-2 pr-4 font-medium">Kind</th>
                <th className="py-2 pr-4 font-medium">Type</th>
                <th className="py-2 font-medium">Example</th>
              </tr>
            </thead>
            <tbody className="font-mono text-xs">
              {[
                ["drum", "percussion", "drum: kick snare hat clap"],
                ["synth", "pitched (triangle)", "synth: C4 E4 G4"],
                ["saw", "pitched (sawtooth)", "saw: C3 E3 G3"],
                ["square", "pitched (square)", "square: C4 G4 C5"],
                ["sine", "pitched (sine)", "sine: C4 E4 G4"],
                ["bass", "pitched (sub bass)", "bass: C2 - G2 -"],
              ].map(([kind, type, ex]) => (
                <tr key={kind} className="border-b border-border/60">
                  <td className="py-2 pr-4 text-foreground">{kind}</td>
                  <td className="py-2 pr-4 font-sans text-muted-foreground">{type}</td>
                  <td className="py-2 text-foreground">{ex}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Putting it together */}
        <h2 className="mt-10 text-lg font-semibold">Putting it together</h2>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          Stack a few lines and they play as one loop. Paste this into the composer and hit play:
        </p>
        <Example>{`drum: kick - snare -
drum: hat hat hat hat
bass: C2 - G2 -
synth: C4 E4 G4 C5`}</Example>

        <div className="mt-10 flex gap-3">
          <Link
            to="/"
            className="inline-flex items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
          >
            {t("open_composer")}
          </Link>
          <Link
            to="/library"
            className="inline-flex items-center justify-center rounded-md border border-border px-4 py-2 text-sm font-medium text-foreground transition-colors hover:border-primary"
          >
            {t("nav_library")}
          </Link>
        </div>
      </section>
    </main>
  );
}
