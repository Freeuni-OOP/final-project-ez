// Audio engine: turns the text patterns into sound with the Web Audio API.
// Pure logic + audio, no UI. The composer and library pages drive it through
// the AudioEngine class at the bottom.
//
// Pattern format: one line per track, "<kind>: tok tok tok".
//   drum:  kick hat snare hat
//   synth: C4 E4 G4 C5
// Every line shares one step grid (token 1 = step 1, etc). "-" is a rest.
//
// OOP shape: an abstract Instrument with drum/synth subclasses, a Parser that
// builds Tracks of Notes, a per-track Mixer for volume/mute, and a Sequencer
// (wrapped by AudioEngine) that schedules the steps in time.

// --- Instruments --------------------------------------------------

/** Base class for anything that can make a sound at a scheduled time. */
export abstract class Instrument {
  constructor(public readonly name: string) {}

  /**
   * Schedule one hit.
   * @param ctx   audio context
   * @param out   node to connect into (the track's gain)
   * @param when  context time in seconds to start at
   * @param freq  pitch in Hz (ignored by drums)
   * @param dur   note length in seconds
   */
  abstract play(
    ctx: AudioContext,
    out: AudioNode,
    when: number,
    freq: number,
    dur: number,
  ): void;
}

class Kick extends Instrument {
  constructor() {
    super("kick");
  }
  play(ctx: AudioContext, out: AudioNode, when: number, _f: number, dur: number) {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = "sine";
    osc.frequency.setValueAtTime(140, when);
    osc.frequency.exponentialRampToValueAtTime(40, when + 0.15);
    gain.gain.setValueAtTime(1, when);
    gain.gain.exponentialRampToValueAtTime(0.001, when + dur);
    osc.connect(gain).connect(out);
    osc.start(when);
    osc.stop(when + dur + 0.02);
  }
}

class Snare extends Instrument {
  constructor() {
    super("snare");
  }
  play(ctx: AudioContext, out: AudioNode, when: number, _f: number, dur: number) {
    // noise burst
    const size = Math.floor(ctx.sampleRate * dur);
    const buf = ctx.createBuffer(1, size, ctx.sampleRate);
    const data = buf.getChannelData(0);
    for (let i = 0; i < size; i++) data[i] = Math.random() * 2 - 1;
    const noise = ctx.createBufferSource();
    noise.buffer = buf;
    const hp = ctx.createBiquadFilter();
    hp.type = "highpass";
    hp.frequency.value = 1500;
    const ng = ctx.createGain();
    ng.gain.setValueAtTime(0.7, when);
    ng.gain.exponentialRampToValueAtTime(0.001, when + dur);
    noise.connect(hp).connect(ng).connect(out);
    noise.start(when);
    noise.stop(when + dur);

    // tonal body
    const osc = ctx.createOscillator();
    const og = ctx.createGain();
    osc.frequency.value = 200;
    og.gain.setValueAtTime(0.4, when);
    og.gain.exponentialRampToValueAtTime(0.001, when + dur * 0.5);
    osc.connect(og).connect(out);
    osc.start(when);
    osc.stop(when + dur);
  }
}

class HiHat extends Instrument {
  constructor() {
    super("hat");
  }
  play(ctx: AudioContext, out: AudioNode, when: number, _f: number, dur: number) {
    const size = Math.floor(ctx.sampleRate * dur);
    const buf = ctx.createBuffer(1, size, ctx.sampleRate);
    const data = buf.getChannelData(0);
    for (let i = 0; i < size; i++) data[i] = Math.random() * 2 - 1;
    const noise = ctx.createBufferSource();
    noise.buffer = buf;
    const hp = ctx.createBiquadFilter();
    hp.type = "highpass";
    hp.frequency.value = 7000;
    const g = ctx.createGain();
    g.gain.setValueAtTime(0.3, when);
    g.gain.exponentialRampToValueAtTime(0.001, when + dur);
    noise.connect(hp).connect(g).connect(out);
    noise.start(when);
    noise.stop(when + dur);
  }
}

class Clap extends Instrument {
  constructor() {
    super("clap");
  }
  play(ctx: AudioContext, out: AudioNode, when: number, _f: number, dur: number) {
    const offsets = [0, 0.012, 0.024, 0.04];
    offsets.forEach((o, i) => {
      const size = Math.floor(ctx.sampleRate * 0.08);
      const buf = ctx.createBuffer(1, size, ctx.sampleRate);
      const data = buf.getChannelData(0);
      for (let j = 0; j < size; j++) data[j] = Math.random() * 2 - 1;
      const s = ctx.createBufferSource();
      s.buffer = buf;
      const bp = ctx.createBiquadFilter();
      bp.type = "bandpass";
      bp.frequency.value = 1200;
      bp.Q.value = 0.7;
      const g = ctx.createGain();
      const amp = i === offsets.length - 1 ? 0.5 : 0.35;
      g.gain.setValueAtTime(amp, when + o);
      g.gain.exponentialRampToValueAtTime(0.001, when + o + dur);
      s.connect(bp).connect(g).connect(out);
      s.start(when + o);
      s.stop(when + o + dur);
    });
  }
}

/** Pitched oscillator voice. The waveform makes synth/saw/square/sine. */
class Synth extends Instrument {
  constructor(
    name: string,
    private wave: OscillatorType,
  ) {
    super(name);
  }
  play(ctx: AudioContext, out: AudioNode, when: number, freq: number, dur: number) {
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = this.wave;
    osc.frequency.value = freq;
    const attack = 0.01;
    const release = 0.08;
    gain.gain.setValueAtTime(0, when);
    gain.gain.linearRampToValueAtTime(0.25, when + attack);
    gain.gain.setValueAtTime(0.25, when + Math.max(attack, dur - release));
    gain.gain.exponentialRampToValueAtTime(0.001, when + dur);
    osc.connect(gain).connect(out);
    osc.start(when);
    osc.stop(when + dur + 0.02);
  }
}

/** Fat low voice for bass lines. */
class Bass extends Instrument {
  constructor() {
    super("bass");
  }
  play(ctx: AudioContext, out: AudioNode, when: number, freq: number, dur: number) {
    const osc = ctx.createOscillator();
    const sub = ctx.createOscillator();
    const lp = ctx.createBiquadFilter();
    lp.type = "lowpass";
    lp.frequency.value = 800;
    const g = ctx.createGain();
    osc.type = "sawtooth";
    osc.frequency.value = freq * 0.5;
    sub.type = "sine";
    sub.frequency.value = freq * 0.25;
    g.gain.setValueAtTime(0, when);
    g.gain.linearRampToValueAtTime(0.4, when + 0.01);
    g.gain.exponentialRampToValueAtTime(0.001, when + dur);
    osc.connect(lp);
    sub.connect(lp);
    lp.connect(g).connect(out);
    osc.start(when);
    sub.start(when);
    osc.stop(when + dur + 0.02);
    sub.stop(when + dur + 0.02);
  }
}

/** Drum tokens used inside a "drum:" line. */
const DRUMS: Record<string, Instrument> = {
  kick: new Kick(),
  snare: new Snare(),
  hat: new HiHat(),
  clap: new Clap(),
};

/** Line kinds that take pitched tokens like C4 / F#5. */
const PITCHED: Record<string, Instrument> = {
  synth: new Synth("synth", "triangle"),
  saw: new Synth("saw", "sawtooth"),
  square: new Synth("square", "square"),
  sine: new Synth("sine", "sine"),
  bass: new Bass(),
};

/** Names a "drum:" line can use, for the UI / library page. */
export const DRUM_NAMES = Object.keys(DRUMS);
/** Pitched line kinds, for the UI / library page. */
export const PITCHED_KINDS = Object.keys(PITCHED);

// --- Pitch maths --------------------------------------------------

const SEMITONES: Record<string, number> = {
  C: 0,
  D: 2,
  E: 4,
  F: 5,
  G: 7,
  A: 9,
  B: 11,
};

/** Turn a note token like "C4" or "F#5" into a frequency in Hz. */
export function noteToFrequency(token: string): number {
  const m = token.match(/^([A-Ga-g])([#b]?)(-?\d+)$/);
  if (!m) throw new Error(`Invalid note: ${token}`);
  let semi = SEMITONES[m[1].toUpperCase()];
  if (m[2] === "#") semi++;
  else if (m[2] === "b") semi--;
  const octave = parseInt(m[3], 10);
  const midi = (octave + 1) * 12 + semi;
  return 440 * Math.pow(2, (midi - 69) / 12);
}

// --- Parser -------------------------------------------------------

/** One scheduled hit on the grid. */
export interface Note {
  instrument: Instrument;
  frequency: number; // 0 for drums
  step: number;
  token: string;
}

/** One parsed line: a kind plus its notes, with mixer state. */
export interface Track {
  kind: string;
  notes: Note[];
  steps: number;
  raw: string;
  volume: number; // 0..1
  muted: boolean;
}

/** Result of parsing the whole text. */
export interface ParseResult {
  tracks: Track[];
  errors: string[];
}

/** Parses pattern text into tracks. */
export class Parser {
  parse(source: string): ParseResult {
    const tracks: Track[] = [];
    const errors: string[] = [];
    source.split(/\r?\n/).forEach((line, idx) => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith("//")) return;
      try {
        tracks.push(this.parseLine(trimmed));
      } catch (e) {
        errors.push(`Line ${idx + 1}: ${(e as Error).message}`);
      }
    });
    return { tracks, errors };
  }

  private parseLine(line: string): Track {
    const colon = line.indexOf(":");
    if (colon === -1) throw new Error(`Missing ':' in "${line}"`);
    const kind = line.slice(0, colon).trim().toLowerCase();
    const tokens = line.slice(colon + 1).trim().split(/\s+/);

    const isDrum = kind === "drum";
    const pitched = PITCHED[kind];
    if (!isDrum && !pitched) throw new Error(`Unknown track kind: ${kind}`);

    const notes: Note[] = [];
    tokens.forEach((tok, step) => {
      if (!tok || tok === "-") return; // rest
      if (isDrum) {
        const inst = DRUMS[tok.toLowerCase()];
        if (!inst) throw new Error(`Unknown drum: ${tok}`);
        notes.push({ instrument: inst, frequency: 0, step, token: tok });
      } else {
        notes.push({
          instrument: pitched,
          frequency: noteToFrequency(tok),
          step,
          token: tok,
        });
      }
    });

    return { kind, notes, steps: tokens.length, raw: line, volume: 0.8, muted: false };
  }
}

// --- Engine -------------------------------------------------------

/** Read-only view of a track's mixer settings, for the UI. */
export interface TrackMixerState {
  index: number;
  kind: string;
  volume: number;
  muted: boolean;
}

/**
 * Main entry point. Load a pattern, then play/stop and tweak BPM or the mixer.
 *
 *   const engine = new AudioEngine();
 *   engine.load("drum: kick hat snare hat\nsynth: C4 E4 G4 C5");
 *   engine.play();
 *   engine.setBpm(140);
 *   engine.setTrackMuted(0, true);
 *   engine.stop();
 */
export class AudioEngine {
  private parser = new Parser();
  private tracks: Track[] = [];

  private ctx: AudioContext | null = null;
  private master: GainNode | null = null;
  private trackGains: GainNode[] = [];

  private timer: number | null = null;
  private nextStepTime = 0;
  private currentStep = 0;
  private totalSteps = 0;
  private playing = false;

  private readonly stepsPerBeat = 4; // each token is a 16th note
  private readonly scheduleAhead = 0.1; // seconds scheduled in advance
  private readonly lookaheadMs = 25; // how often the scheduler wakes up

  constructor(
    private bpm = 120,
    private masterVolume = 0.85,
  ) {}

  /** Parse pattern text and load it. Returns any parse errors. Keeps mixer
   *  settings for tracks that stayed at the same index/kind. */
  load(source: string): string[] {
    const { tracks, errors } = this.parser.parse(source);
    tracks.forEach((t, i) => {
      const prev = this.tracks[i];
      if (prev && prev.kind === t.kind) {
        t.volume = prev.volume;
        t.muted = prev.muted;
      }
    });
    this.tracks = tracks;
    this.totalSteps = tracks.reduce((max, t) => Math.max(max, t.steps), 0);
    if (this.playing) this.applyMixer();
    return errors;
  }

  /** Seconds per step at the current BPM. */
  private stepSeconds(): number {
    return 60 / this.bpm / this.stepsPerBeat;
  }

  isPlaying(): boolean {
    return this.playing;
  }

  getBpm(): number {
    return this.bpm;
  }

  /** Change tempo. Takes effect immediately, even while playing. */
  setBpm(bpm: number): void {
    this.bpm = Math.max(20, Math.min(300, bpm));
  }

  /** Current mixer state for every track. */
  getTracks(): TrackMixerState[] {
    return this.tracks.map((t, index) => ({
      index,
      kind: t.kind,
      volume: t.volume,
      muted: t.muted,
    }));
  }

  setTrackVolume(index: number, volume: number): void {
    const t = this.tracks[index];
    if (!t) return;
    t.volume = Math.max(0, Math.min(1, volume));
    this.applyMixer();
  }

  setTrackMuted(index: number, muted: boolean): void {
    const t = this.tracks[index];
    if (!t) return;
    t.muted = muted;
    this.applyMixer();
  }

  /** Push the current volume/mute values onto the live gain nodes. */
  private applyMixer(): void {
    if (!this.ctx) return;
    this.tracks.forEach((t, i) => {
      const g = this.trackGains[i];
      if (g) g.gain.value = t.muted ? 0 : t.volume;
    });
  }

  /** Start playback from the top. No-op if already playing or nothing loaded. */
  play(): void {
    if (this.playing || this.totalSteps === 0) return;

    const Ctx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext })
        .webkitAudioContext;
    this.ctx = new Ctx();

    this.master = this.ctx.createGain();
    this.master.gain.value = this.masterVolume;
    this.master.connect(this.ctx.destination);

    // one gain node per track = the per-track mixer
    this.trackGains = this.tracks.map((t) => {
      const g = this.ctx!.createGain();
      g.gain.value = t.muted ? 0 : t.volume;
      g.connect(this.master!);
      return g;
    });

    this.currentStep = 0;
    this.nextStepTime = this.ctx.currentTime + 0.05;
    this.playing = true;
    this.scheduler();
  }

  /** Stop playback and release the audio context. */
  stop(): void {
    this.playing = false;
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    if (this.ctx) {
      void this.ctx.close();
      this.ctx = null;
      this.master = null;
      this.trackGains = [];
    }
  }

  /** Schedules every step that falls inside the lookahead window, then sleeps. */
  private scheduler = (): void => {
    if (!this.playing || !this.ctx) return;

    while (this.nextStepTime < this.ctx.currentTime + this.scheduleAhead) {
      const step = this.currentStep % this.totalSteps;
      const dur = this.stepSeconds();
      this.tracks.forEach((track, i) => {
        const out = this.trackGains[i] ?? this.master!;
        for (const note of track.notes) {
          if (note.step === step) {
            const len = note.frequency === 0 ? dur * 0.9 : dur * 0.95;
            note.instrument.play(this.ctx!, out, this.nextStepTime, note.frequency, len);
          }
        }
      });
      this.nextStepTime += dur;
      this.currentStep++;
    }

    this.timer = window.setTimeout(this.scheduler, this.lookaheadMs);
  };
}
