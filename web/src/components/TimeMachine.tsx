import { useEffect, useRef, useState } from "react";
import type { TimelinePoint } from "../store/market";
import { clock, pct } from "../lib/format";
import { Icon } from "./Icon";
import "./TimeMachine.css";

interface Props {
  timeline: TimelinePoint[];
  /** Index into timeline being viewed, or null for live. */
  index: number | null;
  onChange: (index: number | null) => void;
}

/**
 * Rewind the market: every bar is one tick of the last ~3 minutes, blue bars are ticks where a loop was found
 * (taller = more profit). Drag across it to see the market at that moment, or play the last minute back fast.
 */
export function TimeMachine({ timeline, index, onChange }: Props) {
  const svg = useRef<SVGSVGElement>(null);
  const [playing, setPlaying] = useState(false);
  const n = timeline.length;
  const maxProfit = Math.max(0.3, ...timeline.map((t) => t.profit));
  const loops = timeline.filter((t) => t.eventId !== null).length;

  // playback: step one tick every 250 ms (4x speed); reaching "now" returns to live
  useEffect(() => {
    if (!playing || index === null) return;
    const id = window.setTimeout(() => {
      if (index + 1 >= n) {
        setPlaying(false);
        onChange(null);
      } else onChange(index + 1);
    }, 250);
    return () => window.clearTimeout(id);
  }, [playing, index, n, onChange]);

  const togglePlay = () => {
    if (playing) {
      setPlaying(false);
      return;
    }
    onChange(index ?? Math.max(0, n - 60));
    setPlaying(true);
  };

  const pick = (clientX: number) => {
    const box = svg.current?.getBoundingClientRect();
    if (!box || n === 0) return;
    const i = Math.round(((clientX - box.left) / box.width) * (n - 1));
    onChange(Math.max(0, Math.min(n - 1, i)));
  };

  const viewed = index !== null ? timeline[index] : null;
  const ago = viewed ? Math.round((Date.now() - viewed.ts) / 1000) : 0;

  return (
    <section className="panel tm" aria-labelledby="tm-title">
      <div className="panel-head">
        <h2 id="tm-title">Time machine</h2>
        <span className={`chip ${viewed ? "chip-paused" : "chip-live"}`}>
          <span className="chip-dot" aria-hidden="true" />
          {viewed ? `Rewound · ${clock(viewed.ts)} (${ago}s ago)` : "Live"}
        </span>
        <span className="spacer" />
        <button type="button" className="btn btn-sm" onClick={togglePlay} disabled={n < 5}>
          <Icon name={playing ? "pause" : "play"} size={14} />
          {playing ? "Stop" : "Replay last minute"}
        </button>
        {viewed ? (
          <button type="button" className="btn btn-sm btn-primary" onClick={() => { setPlaying(false); onChange(null); }}>
            Back to live
          </button>
        ) : null}
      </div>
      <div className="tm-body">
        <svg
          ref={svg}
          className="tm-bars"
          viewBox={`0 0 ${Math.max(n, 1) * 6} 64`}
          preserveAspectRatio="none"
          role="slider"
          aria-label="Market timeline"
          aria-valuemin={0}
          aria-valuemax={Math.max(0, n - 1)}
          aria-valuenow={index ?? n - 1}
          aria-valuetext={viewed ? clock(viewed.ts) : "live"}
          tabIndex={0}
          onPointerDown={(e) => {
            setPlaying(false);
            (e.target as Element).setPointerCapture?.(e.pointerId);
            pick(e.clientX);
          }}
          onPointerMove={(e) => {
            if (e.buttons) pick(e.clientX);
          }}
          onKeyDown={(e) => {
            const cur = index ?? n - 1;
            if (e.key === "ArrowLeft") onChange(Math.max(0, cur - 1));
            if (e.key === "ArrowRight") onChange(cur + 1 >= n ? null : cur + 1);
            if (e.key === "End") onChange(null);
          }}
        >
          {timeline.map((t, i) => {
            const h = t.eventId !== null ? 14 + (t.profit / maxProfit) * 46 : 6;
            return <rect key={t.ts} x={i * 6 + 1} y={64 - h} width={4} height={h} rx={2} className={t.eventId !== null ? "tm-loop" : "tm-tick"} />;
          })}
          {index !== null ? <rect x={index * 6 - 1} y={0} width={8} height={64} rx={3} className="tm-cursor" /> : null}
        </svg>
        <div className="tm-foot">
          <span>{n ? clock(timeline[0].ts) : "—"}</span>
          <span>
            <strong>{loops}</strong> loops in the last {Math.round(n / 60) || "<1"} min
            {viewed && viewed.eventId !== null ? <> · this tick: <span className="gain">{pct(viewed.profit, 3)}</span></> : null}
          </span>
          <span>now</span>
        </div>
      </div>
    </section>
  );
}
