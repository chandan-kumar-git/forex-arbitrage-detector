import { useMemo, useState } from "react";
import { buildEdges, findCycle, profitableTriangles } from "../../engine/bellmanFord";
import { Simulator, seeded } from "../../engine/simulator";
import { DEFAULT_SETTINGS, type Currency } from "../../engine/types";
import { micros } from "../../lib/format";
import { useNarrow } from "../../lib/useNarrow";
import { useMarket } from "../../store/market";

interface Point {
  n: number;
  us: number;
}

const LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

function syntheticCurrencies(n: number, random: () => number): Currency[] {
  const list: Currency[] = [{ code: "USD", name: "US Dollar", usdPrice: 1, active: true }];
  for (let i = 1; i < n; i++) {
    const code = `X${LETTERS[Math.floor(i / 26) % 26]}${LETTERS[i % 26]}`;
    list.push({ code, name: code, usdPrice: Math.exp((random() - 0.5) * 6), active: true });
  }
  return list;
}

const tick = () => new Promise((r) => window.setTimeout(r, 0));

/** Measures Bellman-Ford in this browser and checks it against brute force. Every number here is measured live. */
export function Analysis() {
  const history = useMarket((s) => s.history);
  const fee = useMarket((s) => s.settings.fee);
  const [points, setPoints] = useState<Point[]>([]);
  const [running, setRunning] = useState(false);
  const [agree, setAgree] = useState<{ markets: number; withLoop: number; disagree: number } | null>(null);

  const runBenchmark = async () => {
    setRunning(true);
    setPoints([]);
    const measure = (n: number): number => {
      const sim = new Simulator(syntheticCurrencies(n, seeded(n)), seeded(100 + n));
      const snaps = Array.from({ length: 50 }, () => sim.next({ ...DEFAULT_SETTINGS, injectChance: 0.5 }));
      const batches: number[] = [];
      findCycle(buildEdges(snaps[0], fee), n); // warm-up
      for (let b = 0; b < 9; b++) {
        const start = performance.now();
        for (let k = 0; k < 4; k++) for (const s of snaps) findCycle(buildEdges(s, fee), n);
        batches.push(((performance.now() - start) * 1000) / (snaps.length * 4));
      }
      batches.sort((a, b) => a - b);
      return batches[4]; // median of 9 batches
    };
    const out: Point[] = [];
    for (let n = 3; n <= 20; n++) {
      out.push({ n, us: measure(n) });
      setPoints([...out]);
      await tick();
    }
    // a point far above both neighbours is usually the browser pausing (garbage collection):
    // measure it again, up to twice, and keep the lowest median
    for (let i = 1; i < out.length - 1; i++) {
      for (let retry = 0; retry < 2 && out[i].us > 1.5 * Math.max(out[i - 1].us, out[i + 1].us); retry++) {
        await tick();
        out[i] = { n: out[i].n, us: Math.min(out[i].us, measure(out[i].n)) };
      }
    }
    setPoints([...out]);
    const sim = new Simulator(syntheticCurrencies(6, seeded(9)), seeded(42));
    let withLoop = 0;
    let disagree = 0;
    const markets = 5000;
    for (let i = 0; i < markets; i++) {
      const s = sim.next({ ...DEFAULT_SETTINGS, injectChance: 0.5 });
      const cycle = findCycle(buildEdges(s, fee), s.codes.length);
      const tri = profitableTriangles(s, fee).length;
      if (cycle.length) withLoop++;
      // brute force only checks triangles, so it may miss longer loops but must never find one Bellman-Ford missed
      else if (tri > 0) disagree++;
      if (i % 1000 === 0) await tick();
    }
    setAgree({ markets, withLoop, disagree });
    setRunning(false);
  };

  return (
    <>
      <div className="page-head">
        <h1>Analysis</h1>
        <div className="actions">
          <button type="button" className="btn btn-primary btn-sm" onClick={() => void runBenchmark()} disabled={running}>
            {running ? "Measuring…" : points.length ? "Measure again" : "Run the measurement"}
          </button>
        </div>
        <p>
          How Bellman-Ford scales with the number of currencies, timed in this browser on simulated markets, plus a
          correctness check against the brute-force triangle search.
        </p>
      </div>

      <div className="two-col">
        <section className="panel" aria-labelledby="bench-title">
          <div className="panel-head">
            <h2 id="bench-title">Detection time by currencies</h2>
          </div>
          <div className="panel-body">
            {points.length ? (
              <ScalingChart points={points} />
            ) : (
              <div className="empty" style={{ padding: "24px 0" }}>
                <h3>Not measured yet</h3>
                <p>
                  Runs 1,000 detections for each size from 3 to 20 currencies and plots the median time per detection.
                  Takes a few seconds.
                </p>
              </div>
            )}
            <p className="field-help" style={{ marginTop: 12 }}>
              n currencies give n(n − 1) edges and up to n rounds, so the work grows like n³. The dashed line is n³
              fitted to all the measurements (least squares); a point far above both neighbours is measured again.
            </p>
          </div>
        </section>

        <div className="stack">
          <section className="panel" aria-labelledby="agree-title">
            <div className="panel-head">
              <h2 id="agree-title">Cross-check</h2>
            </div>
            <div className="panel-body java-panel">
              {agree ? (
                <dl>
                  <div>
                    <dt>Markets tested</dt>
                    <dd>{agree.markets.toLocaleString("en-US")}</dd>
                  </div>
                  <div>
                    <dt>Loops found</dt>
                    <dd>{agree.withLoop.toLocaleString("en-US")}</dd>
                  </div>
                  <div>
                    <dt>Disagreements</dt>
                    <dd className={agree.disagree === 0 ? "gain" : ""}>{agree.disagree}</dd>
                  </div>
                </dl>
              ) : (
                <p className="field-help">
                  Runs with the measurement: 5,000 random 6-currency markets. A disagreement would be a profitable
                  triangle that Bellman-Ford missed.
                </p>
              )}
            </div>
          </section>
          <section className="panel" aria-labelledby="dist-title">
            <div className="panel-head">
              <h2 id="dist-title">Profit of stored events</h2>
              <span className="spacer" />
              <span className="label">{history.length} events</span>
            </div>
            <div className="panel-body">
              <ProfitHistogram values={history.map((e) => e.profitPercent)} />
            </div>
          </section>
        </div>
      </div>
    </>
  );
}

/** A round step (1, 2 or 5 times a power of ten) giving about four gridlines. */
function niceStep(max: number): number {
  const raw = max / 4;
  const p = 10 ** Math.floor(Math.log10(raw));
  return [1, 2, 5, 10].map((m) => m * p).find((s) => s >= raw) ?? raw;
}

function ScalingChart({ points }: { points: Point[] }) {
  const narrow = useNarrow();
  const W = narrow ? 380 : 640;
  const H = narrow ? 300 : 340;
  const pad = { l: 64, r: 20, t: 24, b: 44 };
  const step = niceStep(Math.max(...points.map((p) => p.us)));
  const maxUs = Math.ceil((Math.max(...points.map((p) => p.us)) * 1.08) / step) * step;
  const x = (n: number) => pad.l + ((n - 3) / 17) * (W - pad.l - pad.r);
  const y = (us: number) => H - pad.b - (us / maxUs) * (H - pad.t - pad.b);
  const last = points[points.length - 1];
  // least-squares fit of us = k * n^3 over every point, so one outlier cannot bend the guide
  const k = points.reduce((a, p) => a + p.us * p.n ** 3, 0) / points.reduce((a, p) => a + p.n ** 6, 0);
  const ref = Array.from({ length: 18 }, (_, i) => i + 3)
    .map((n, i) => `${i ? "L" : "M"}${x(n).toFixed(1)} ${y(k * n ** 3).toFixed(1)}`)
    .join(" ");
  const line = points.map((p, i) => `${i ? "L" : "M"}${x(p.n).toFixed(1)} ${y(p.us).toFixed(1)}`).join(" ");
  const ticks = Array.from({ length: Math.round(maxUs / step) + 1 }, (_, i) => i * step);
  return (
    <svg className="chart" viewBox={`0 0 ${W} ${H}`} role="img" aria-label={`Detection time rises from ${micros(points[0].us)} at 3 currencies to ${micros(last.us)} at ${last.n}`}>
      {ticks.map((t) => (
        <g key={t}>
          <line className="grid" x1={pad.l} x2={W - pad.r} y1={y(t)} y2={y(t)} />
          <text x={pad.l - 10} y={y(t) + 4} textAnchor="end">
            {micros(t) === "—" ? "0" : micros(t)}
          </text>
        </g>
      ))}
      {(narrow ? [3, 8, 12, 16, 20] : [3, 5, 8, 10, 12, 15, 18, 20]).map((n) => (
        <text key={n} x={x(n)} y={H - pad.b + 20} textAnchor="middle">
          {n}
        </text>
      ))}
      <text x={(W + pad.l) / 2} y={H - 6} textAnchor="middle">
        currencies (n)
      </text>
      <line className="axis" x1={pad.l} x2={W - pad.r} y1={H - pad.b} y2={H - pad.b} />
      <path d={ref} fill="none" stroke="var(--ink-3)" strokeWidth="1.5" strokeDasharray="5 5" />
      <path d={line} fill="none" stroke="var(--blue)" strokeWidth="3" />
      {points.map((p) => (
        <circle key={p.n} cx={x(p.n)} cy={y(p.us)} r="3.5" fill="#fff" stroke="var(--blue)" strokeWidth="2">
          <title>{`${p.n} currencies: ${micros(p.us)}`}</title>
        </circle>
      ))}
      <text x={x(last.n) - 14} y={y(last.us) - 14} textAnchor="end" style={{ fill: "var(--blue)", fontWeight: 600 }}>
        {micros(last.us)}
      </text>
    </svg>
  );
}

function ProfitHistogram({ values }: { values: number[] }) {
  const bins = useMemo(() => {
    const width = 0.1;
    const counts = new Map<number, number>();
    for (const v of values) {
      const b = Math.min(15, Math.floor(v / width));
      counts.set(b, (counts.get(b) ?? 0) + 1);
    }
    const maxBin = Math.max(5, ...counts.keys());
    return Array.from({ length: maxBin + 1 }, (_, b) => ({ from: b * width, count: counts.get(b) ?? 0 }));
  }, [values]);
  const narrow = useNarrow();
  if (!values.length) return <p className="field-help">No stored events yet.</p>;
  const W = narrow ? 360 : 520;
  const H = 220;
  const pad = { l: 40, r: 8, t: 12, b: 38 };
  const max = Math.max(...bins.map((b) => b.count));
  const bw = (W - pad.l - pad.r) / bins.length;
  return (
    <svg className="chart" viewBox={`0 0 ${W} ${H}`} role="img" aria-label="Number of stored events by profit, in steps of 0.1 percent">
      {bins.map((b, i) => {
        const h = (b.count / max) * (H - pad.t - pad.b);
        return (
          <g key={i}>
            <rect x={pad.l + i * bw + 2} y={H - pad.b - h} width={bw - 4} height={h} fill="var(--blue)" opacity="0.85" rx="3">
              <title>{`${b.from.toFixed(1)}–${(b.from + 0.1).toFixed(1)} %: ${b.count}`}</title>
            </rect>
            {i % 2 === 0 ? (
              <text x={pad.l + i * bw + bw / 2} y={H - pad.b + 18} textAnchor="middle">
                {b.from.toFixed(1)}
              </text>
            ) : null}
          </g>
        );
      })}
      <line className="axis" x1={pad.l} x2={W - pad.r} y1={H - pad.b} y2={H - pad.b} />
      <text x={pad.l - 8} y={pad.t + 10} textAnchor="end">
        {max}
      </text>
      <text x={pad.l - 8} y={H - pad.b + 4} textAnchor="end">
        0
      </text>
      <text x={(W + pad.l) / 2} y={H - 4} textAnchor="middle">
        profit after fees (%)
      </text>
    </svg>
  );
}
