import { useMemo } from "react";
import type { Leg, Snapshot } from "../engine/types";
import { legGain, pct, rate } from "../lib/format";
import { useNarrow } from "../lib/useNarrow";
import "./RingDisplay.css";

const W = 1000;
const H = 800;
const CX = 500;
const CY = 405;
const R = 300;

export interface Ghost {
  from: number;
  to: number;
  /** 0 = relaxed in the round shown, 1+ = earlier rounds (drawn fainter). */
  age: number;
}

interface Props {
  snapshot: Snapshot | null;
  /** The loop to draw, as currency indexes (first = last). */
  cycle?: number[] | null;
  legs?: Leg[] | null;
  selectedLeg?: number | null;
  onSelectLeg?: (legNo: number | null) => void;
  ghosts?: Ghost[];
  /** Show Bellman-Ford's virtual source joined to every station (replay round 0). */
  spokes?: boolean;
  /** Distance per station (replay). */
  dist?: number[] | null;
  /** Stations to outline (replay). */
  marked?: number[];
  /** Changes when a new loop lands, to replay the draw-on. */
  eventKey?: string | number;
  /** Game: clicking a station calls this with its index. */
  onStationClick?: (index: number) => void;
  /** Game: the loop the player is drawing, as station indexes. */
  userPath?: number[];
  title?: string;
}

/** Even-odd ray test: is the point inside the polygon? */
function inside(x: number, y: number, poly: [number, number][]): boolean {
  let hit = false;
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const [xi, yi] = poly[i];
    const [xj, yj] = poly[j];
    if (yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi || 1e-9) + xi) hit = !hit;
  }
  return hit;
}

function station(i: number, n: number): [number, number] {
  const a = -Math.PI / 2 + (i * 2 * Math.PI) / n;
  return [CX + R * Math.cos(a), CY + R * Math.sin(a)];
}

/**
 * The currency network drawn like a metro map: currencies are stations, every exchange pair is a
 * thin grey line, and the arbitrage loop is one bold blue line that leaves a station and returns to it.
 */
export function RingDisplay({
  snapshot,
  cycle,
  legs,
  selectedLeg = null,
  onSelectLeg,
  ghosts,
  spokes = false,
  dist,
  marked,
  eventKey,
  title = "Currency network",
  onStationClick,
  userPath,
}: Props) {
  const narrow = useNarrow();
  const codes = snapshot?.codes ?? [];
  const n = codes.length;
  const pos = useMemo(() => codes.map((_, i) => station(i, n)), [codes, n]);
  const label = narrow ? 40 : 28;
  // badge text in drawing units; on phones the drawing is ~330px wide, so it needs larger units
  const small = narrow ? 34 : 20;
  const big = narrow ? 50 : 32;
  const pillH = narrow ? 120 : 88;
  const stationR = narrow ? 26 : 20;

  const loop = useMemo(() => {
    if (!cycle || cycle.length < 2) return [];
    return cycle.slice(0, -1).map((from, i) => ({ from, to: cycle[i + 1], legNo: i + 1 }));
  }, [cycle]);

  // the badge sits at the open point inside the loop farthest from every loop line,
  // so no line runs through it (found by sampling a grid inside the loop's bounds)
  const centroid = useMemo(() => {
    if (!loop.length || !pos.length) return null;
    const pts = loop.map((l) => pos[l.from]);
    const xs = pts.map((p) => p[0]);
    const ys = pts.map((p) => p[1]);
    const segDist = (px: number, py: number) =>
      Math.min(
        ...loop.map((l) => {
          const [ax, ay] = pos[l.from];
          const [bx, by] = pos[l.to];
          const t = Math.max(0, Math.min(1, ((px - ax) * (bx - ax) + (py - ay) * (by - ay)) / ((bx - ax) ** 2 + (by - ay) ** 2 || 1)));
          return Math.hypot(px - (ax + t * (bx - ax)), py - (ay + t * (by - ay)));
        }),
      );
    const mid: [number, number] = [xs.reduce((a, b) => a + b, 0) / xs.length, ys.reduce((a, b) => a + b, 0) / ys.length];
    const halfW = narrow ? 170 : 120;
    const need = pillH / 2 + 10;
    // clearance of a pill centred at (x, y): the nearest loop line or station to its middle or its two ends
    const clearance = (x: number, y: number) => {
      let c = Infinity;
      for (const dx of [-halfW * 0.85, 0, halfW * 0.85]) c = Math.min(c, segDist(x + dx, y));
      for (const [sx, sy] of pos) c = Math.min(c, Math.hypot(sx - x, (sy - y) * 1.6) - stationR - 24);
      return c;
    };
    let bestInside: [number, number] | null = null;
    let bestInsideScore = -Infinity;
    let bestOutside: [number, number] | null = null;
    let bestOutsideDist = Infinity;
    for (let gx = CX - R; gx <= CX + R; gx += 12) {
      for (let gy = CY - R; gy <= CY + R; gy += 12) {
        if (Math.hypot(gx - CX, gy - CY) > R * 0.78) continue;
        const c = clearance(gx, gy);
        if (inside(gx, gy, pts)) {
          if (c > bestInsideScore) {
            bestInsideScore = c;
            bestInside = [gx, gy];
          }
        } else if (c >= need) {
          // a thin loop has no room inside: take the clear spot closest to the loop
          const d = Math.hypot(gx - mid[0], gy - mid[1]);
          if (d < bestOutsideDist) {
            bestOutsideDist = d;
            bestOutside = [gx, gy];
          }
        }
      }
    }
    if (bestInside && bestInsideScore >= need) return bestInside;
    return bestOutside ?? bestInside ?? mid;
  }, [loop, pos, narrow, pillH, stationR]);

  const profit = legs && legs.length ? (Math.exp(legs.reduce((a, l) => a + l.edge, 0)) - 1) * 100 : null;
  const selected = selectedLeg !== null ? loop.find((l) => l.legNo === selectedLeg) : undefined;
  const selectedInfo = selected && legs ? legs[selected.legNo - 1] : undefined;
  const describe = cycle && cycle.length > 1 ? `Loop ${cycle.map((i) => codes[i]).join(" to ")}` : "No loop shown";

  return (
    <svg className="netmap" viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`${title}: ${n} currencies. ${describe}.`}>
      {/* every exchange pair */}
      {pos.map((a, i) =>
        pos.map((b, j) => (i < j ? <line key={`p-${i}-${j}`} x1={a[0]} y1={a[1]} x2={b[0]} y2={b[1]} className="nm-pair" /> : null)),
      )}

      {/* Bellman-Ford's virtual source, joined to every station with weight 0 */}
      {spokes ? (
        <g className="nm-source">
          {pos.map((p, i) => (
            <line key={`s-${i}`} x1={CX} y1={CY} x2={p[0]} y2={p[1]} />
          ))}
          <circle cx={CX} cy={CY} r="14" />
          <text x={CX} y={CY + 50} textAnchor="middle" style={{ fontSize: label * 0.8 }}>
            virtual source
          </text>
        </g>
      ) : null}

      {/* edges relaxed in the replay round */}
      {ghosts?.map((g, k) => (
        <line
          key={`g-${k}-${g.from}-${g.to}`}
          x1={pos[g.from]?.[0]}
          y1={pos[g.from]?.[1]}
          x2={pos[g.to]?.[0]}
          y2={pos[g.to]?.[1]}
          className="nm-ghost"
          style={{ opacity: g.age === 0 ? 0.9 : 0.3 }}
        />
      ))}

      {/* the player's own loop (game) */}
      {userPath && userPath.length > 1
        ? userPath.slice(0, -1).map((from, k) => {
            const to = userPath[k + 1];
            return <line key={`u-${k}`} x1={pos[from][0]} y1={pos[from][1]} x2={pos[to][0]} y2={pos[to][1]} className="nm-user" />;
          })
        : null}

      {/* the loop */}
      <g key={`loop-${eventKey ?? ""}`}>
        {loop.map((l) => {
          const [x1, y1] = pos[l.from];
          const [x2, y2] = pos[l.to];
          const sel = selectedLeg === l.legNo;
          const dim = selectedLeg !== null && !sel;
          const mx = (x1 + x2) / 2;
          const my = (y1 + y2) / 2;
          const ang = (Math.atan2(y2 - y1, x2 - x1) * 180) / Math.PI;
          return (
            <g key={l.legNo} className={`nm-leg ${dim ? "nm-dim" : ""}`}>
              <line
                x1={x1}
                y1={y1}
                x2={x2}
                y2={y2}
                pathLength={1}
                className="nm-loop"
                style={{ animationDelay: `${(l.legNo - 1) * 0.22}s` }}
              />
              <path
                d="M-7 -9 L5 0 L-7 9"
                transform={`translate(${mx} ${my}) rotate(${ang})`}
                className="nm-chevron"
                style={{ animationDelay: `${(l.legNo - 1) * 0.22 + 0.3}s` }}
              />
              {onSelectLeg ? (
                <line x1={x1} y1={y1} x2={x2} y2={y2} className="nm-hit" onClick={() => onSelectLeg(sel ? null : l.legNo)}>
                  <title>{`Leg ${l.legNo}: ${codes[l.from]} to ${codes[l.to]}`}</title>
                </line>
              ) : null}
            </g>
          );
        })}
      </g>

      {/* stations */}
      {pos.map(([x, y], i) => {
        const inLoop = cycle?.includes(i);
        const isMarked = marked?.includes(i);
        const out = narrow ? 64 : 50;
        const a = Math.atan2(y - CY, x - CX);
        const lx = x + Math.cos(a) * out;
        const ly = y + Math.sin(a) * out;
        const anchor = Math.abs(Math.cos(a)) < 0.3 ? "middle" : Math.cos(a) > 0 ? "start" : "end";
        return (
          <g
            key={codes[i]}
            className={`nm-station ${inLoop ? "is-loop" : ""} ${isMarked ? "is-marked" : ""} ${userPath?.includes(i) ? "is-user" : ""} ${onStationClick ? "is-clickable" : ""}`}
            onClick={onStationClick ? () => onStationClick(i) : undefined}
          >
            {inLoop ? <circle key={`rip-${eventKey ?? ""}`} cx={x} cy={y} r={stationR} className="nm-ripple" /> : null}
            {onStationClick ? <circle cx={x} cy={y} r={stationR + 34} className="nm-target" /> : null}
            <circle cx={x} cy={y} r={stationR} />
            <text x={lx} y={ly + label / 3} textAnchor={anchor} className="nm-code" style={{ fontSize: label }}>
              {codes[i]}
            </text>
            {dist && Number.isFinite(dist[i]) ? (
              <text x={lx} y={Math.sin(a) < -0.5 ? ly + label / 3 - label * 1.05 : ly + label / 3 + label * 1.1} textAnchor={anchor} className="nm-dist" style={{ fontSize: label * 0.72 }}>
                {dist[i] === 0 ? "d = 0" : `d = ${dist[i].toFixed(4)}`}
              </text>
            ) : null}
          </g>
        );
      })}

      {/* profit, or the selected leg */}
      {centroid && profit !== null ? (
        <g className="nm-badge" key={`badge-${eventKey ?? ""}-${selectedLeg ?? ""}`} transform={`translate(${centroid[0]} ${centroid[1]})`}>
          {selectedInfo ? (
            <>
              <rect x={narrow ? -230 : -150} y={-pillH / 2} width={narrow ? 460 : 300} height={pillH} rx={pillH / 2} />
              <text y={-pillH * 0.08} textAnchor="middle" className="nm-badge-small" style={{ fontSize: small }}>
                {selectedInfo.from} → {selectedInfo.to} at {rate(selectedInfo.rate)}
              </text>
              <text y={pillH * 0.34} textAnchor="middle" className={selectedInfo.edge > 0 ? "nm-badge-big" : "nm-badge-big nm-badge-loss"} style={{ fontSize: big }}>
                {legGain(selectedInfo.edge)}
              </text>
            </>
          ) : (
            <>
              <rect x={narrow ? -170 : -120} y={-pillH / 2} width={narrow ? 340 : 240} height={pillH} rx={pillH / 2} />
              <text y={-pillH * 0.08} textAnchor="middle" className="nm-badge-small" style={{ fontSize: small }}>
                profit after fees
              </text>
              <text y={pillH * 0.34} textAnchor="middle" className="nm-badge-big" style={{ fontSize: big }}>
                {pct(profit)}
              </text>
            </>
          )}
        </g>
      ) : null}

      {n < 3 ? (
        <text x={CX} y={CY} textAnchor="middle" className="nm-empty" style={{ fontSize: label }}>
          Needs at least 3 active currencies
        </text>
      ) : null}
    </svg>
  );
}
