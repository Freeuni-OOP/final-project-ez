// Canvas visual that reacts to the music while it plays.
// Drop-in component for the composer page: pass it the playing AnalyserNode and
// it draws frequency bars; with no analyser it shows a calm resting wave.
// Colors come from the app theme (--primary), so it matches the rest of the app.

import { useEffect, useRef } from "react";
import { cn } from "@/lib/utils";

interface SoundVisualizerProps {
  /** Analyser tapped off the playing audio. Pass null when nothing plays. */
  analyser?: AnalyserNode | null;
  /** Extra classes for sizing (defaults to a full-width strip). */
  className?: string;
}

const BARS = 48;
const GAP = 2;

export function SoundVisualizer({ analyser, className }: SoundVisualizerProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const colorRef = useRef<HTMLSpanElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");
    if (!canvas || !ctx) return;

    let raf = 0;
    let t = 0;
    const freq = analyser ? new Uint8Array(analyser.frequencyBinCount) : null;

    // Match the canvas backing store to the element size and pixel ratio so the
    // drawing stays crisp on high-DPI screens.
    const resize = () => {
      const dpr = window.devicePixelRatio || 1;
      canvas.width = Math.max(1, Math.floor(canvas.clientWidth * dpr));
      canvas.height = Math.max(1, Math.floor(canvas.clientHeight * dpr));
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resize();
    window.addEventListener("resize", resize);

    // Read the resolved theme primary color (defined as oklch in CSS vars).
    const themeColor = () =>
      (colorRef.current && getComputedStyle(colorRef.current).color) || "#c084fc";

    const draw = () => {
      const w = canvas.clientWidth;
      const h = canvas.clientHeight;
      const color = themeColor();
      ctx.clearRect(0, 0, w, h);
      ctx.fillStyle = color;

      const barWidth = (w - GAP * (BARS - 1)) / BARS;

      if (analyser && freq) {
        // live audio: frequency bars
        analyser.getByteFrequencyData(freq);
        for (let i = 0; i < BARS; i++) {
          const bin = Math.floor((i / BARS) * (freq.length * 0.7));
          const v = freq[bin] / 255; // 0..1
          const barH = Math.max(2, v * h);
          ctx.globalAlpha = 0.35 + v * 0.65;
          ctx.fillRect(i * (barWidth + GAP), h - barH, barWidth, barH);
        }
      } else {
        // resting state: gentle rolling wave
        for (let i = 0; i < BARS; i++) {
          const v = (Math.sin((i / BARS) * Math.PI * 4 + t) + 1) / 2; // 0..1
          const barH = 4 + v * h * 0.12;
          ctx.globalAlpha = 0.25 + v * 0.2;
          ctx.fillRect(i * (barWidth + GAP), h - barH, barWidth, barH);
        }
      }

      ctx.globalAlpha = 1;
      t += 0.05;
      raf = requestAnimationFrame(draw);
    };

    raf = requestAnimationFrame(draw);
    return () => {
      cancelAnimationFrame(raf);
      window.removeEventListener("resize", resize);
    };
  }, [analyser]);

  return (
    <div className={cn("relative h-32 w-full", className)}>
      {/* hidden helper just to read the theme color from CSS variables */}
      <span ref={colorRef} className="hidden text-primary" aria-hidden />
      <canvas ref={canvasRef} className="block h-full w-full" />
    </div>
  );
}
