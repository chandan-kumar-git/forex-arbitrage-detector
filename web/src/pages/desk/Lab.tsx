import { useEffect, useMemo, useState } from "react";
import { LegTable } from "../../components/LegTable";
import { RateLattice } from "../../components/RateLattice";
import { RingDisplay } from "../../components/RingDisplay";
import { buildEdges, profitableTriangles, traceBellmanFord } from "../../engine/bellmanFord";
import { detect } from "../../engine/detect";
import { snapshotFromPrices } from "../../engine/simulator";
import type { ArbEvent, Snapshot } from "../../engine/types";
import { pathText, pct, percentOf } from "../../lib/format";
import { market, useMarket } from "../../store/market";

function clone(s: Snapshot): Snapshot {
  return { codes: [...s.codes], rates: s.rates.map((r) => [...r]), timestamp: Date.now() };
}

/** A frozen market you can edit by hand: change any rate and the detector answers at once. */
export function Lab() {
  const live = useMarket((s) => s.snapshot);
  const currencies = useMarket((s) => s.currencies);
  const settings = useMarket((s) => s.settings);
  const [snap, setSnap] = useState<Snapshot | null>(() => (market.getState().snapshot ? clone(market.getState().snapshot!) : null));
  const [fee, setFee] = useState(settings.fee);
  const [from, setFrom] = useState(1);
  const [to, setTo] = useState(2);
  const [boost, setBoost] = useState("0.8");
  const [selectedLeg, setSelectedLeg] = useState<number | null>(null);

  // the desk may open before the first tick: take the first live market that arrives
  useEffect(() => {
    if (!snap && live) setSnap(clone(live));
  }, [live, snap]);

  const fair = () => {
    const active = currencies.filter((c) => c.active);
    setSnap(snapshotFromPrices(active.map((c) => c.code), active.map((c) => c.usdPrice), settings.spread));
  };

  const result = useMemo(() => (snap ? detect(snap, fee) : null), [snap, fee]);
  const trace = useMemo(() => (snap ? traceBellmanFord(buildEdges(snap, fee), snap.codes.length) : null), [snap, fee]);
  const triangles = useMemo(() => (snap ? profitableTriangles(snap, fee).length : 0), [snap, fee]);

  const asEvent: ArbEvent | null =
    result && snap
      ? { id: 0, path: result.path, cycle: result.cycle, profitPercent: result.profitPercent, fee, source: "browser", detectedAt: snap.timestamp, legs: result.legs, snapshot: snap }
      : null;

  const n = snap?.codes.length ?? 0;
  const boostNum = Number(boost);
  const boostInvalid = !Number.isFinite(boostNum) || boostNum <= -50 || boostNum >= 50;

  const inject = () => {
    if (!snap || from === to || boostInvalid) return;
    const next = clone(snap);
    const factor = 1 + boostNum / 100;
    next.rates[from][to] *= factor;
    next.rates[to][from] /= factor;
    setSnap(next);
  };

  return (
    <>
      <div className="page-head">
        <h1>Lab</h1>
        <div className="actions">
          <button type="button" className="btn btn-sm" onClick={() => live && setSnap(clone(live))} disabled={!live}>
            Freeze the live market
          </button>
          <button type="button" className="btn btn-sm" onClick={fair}>
            Start from a fair market
          </button>
        </div>
        <p>
          Edit any rate in the lattice, or push one pair out of line, and watch Bellman-Ford answer instantly. Nothing
          here touches the live market or the stored events.
        </p>
      </div>

      {!snap ? (
        <div className="panel">
          <div className="empty">
            <h3>No market to work on yet</h3>
            <p>Wait for the first live tick, or start from a fair market built from your currencies.</p>
            <button type="button" className="btn btn-primary" onClick={fair}>
              Start from a fair market
            </button>
          </div>
        </div>
      ) : (
        <div className="two-col">
          <div className="stack">
            <section className="panel" aria-labelledby="lab-lattice">
              <div className="panel-head">
                <h2 id="lab-lattice">Editable rates</h2>
                <span className="spacer" />
                <span className="label">Enter to apply</span>
              </div>
              <div className="panel-body">
                <RateLattice
                  snapshot={snap}
                  cycle={result?.cycle}
                  selectedLeg={selectedLeg}
                  editable
                  onEdit={(i, j, v) => {
                    const next = clone(snap);
                    next.rates[i][j] = v;
                    setSnap(next);
                  }}
                />
              </div>
            </section>

            <section className="panel" aria-labelledby="lab-tools">
              <div className="panel-head">
                <h2 id="lab-tools">Tools</h2>
              </div>
              <div className="panel-body">
                <div className="form-grid">
                  <label className="field">
                    <span>Make this pair better</span>
                    <select className="select" value={from} onChange={(e) => setFrom(Number(e.target.value))}>
                      {snap.codes.map((c, i) => (
                        <option key={c} value={i}>
                          {c}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="field">
                    <span>Into</span>
                    <select className="select" value={to} onChange={(e) => setTo(Number(e.target.value))}>
                      {snap.codes.map((c, i) => (
                        <option key={c} value={i}>
                          {c}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="field">
                    <span>By %</span>
                    <input className="input" value={boost} inputMode="decimal" aria-invalid={boostInvalid} onChange={(e) => setBoost(e.target.value)} />
                    {boostInvalid ? <span className="field-error">Between −50 and 50</span> : <span className="field-help">The reverse rate gets worse by the same factor</span>}
                  </label>
                </div>
                <div className="form-actions">
                  <button type="button" className="btn btn-primary btn-sm" onClick={inject} disabled={from === to || boostInvalid || n < 2}>
                    Apply
                  </button>
                </div>
                <div className="slider-row" style={{ marginTop: 20 }}>
                  <label htmlFor="lab-fee" className="label">
                    Fee per leg (lab only)
                  </label>
                  <output htmlFor="lab-fee">{percentOf(fee, 2)}</output>
                  <input id="lab-fee" className="range" type="range" min={0} max={0.02} step={0.0001} value={fee} onChange={(e) => setFee(Number(e.target.value))} />
                  <span className="field-help">Raise the fee until the loop disappears: that is the break-even fee.</span>
                </div>
              </div>
            </section>
          </div>

          <div className="stack">
            <section className="panel" aria-labelledby="lab-result" aria-live="polite">
              <div className="panel-head">
                <h2 id="lab-result">Detector answer</h2>
              </div>
              {asEvent ? (
                <>
                  <div className="result-big">
                    <span className="profit gain">{pct(asEvent.profitPercent)}</span>
                    <span className="num">{pathText(asEvent.path)}</span>
                  </div>
                  <LegTable event={asEvent} selectedLeg={selectedLeg} onSelectLeg={setSelectedLeg} />
                </>
              ) : (
                <div className="result-big">
                  <span className="profit" style={{ color: "var(--ink-3)" }}>
                    No loop
                  </span>
                  <span className="field-help">No cycle pays after the fee in this market.</span>
                </div>
              )}
              <div className="panel-body" style={{ borderTop: "1px solid var(--line)" }}>
                <p className="num field-help">
                  Bellman-Ford rounds: {trace ? trace.rounds.length - 1 : 0} of {n} · edges relaxed:{" "}
                  {trace ? trace.rounds.reduce((a, r) => a + r.relaxations.length, 0) : 0} · profitable triangles by brute force: {triangles}
                </p>
              </div>
            </section>
            <section className="panel display-panel" aria-label="Lab market map">
              <RingDisplay
                snapshot={snap}
                cycle={result?.cycle}
                legs={result?.legs}
                selectedLeg={selectedLeg}
                onSelectLeg={setSelectedLeg}
                eventKey={`${snap.timestamp}`}
                title="Lab market"
              />
            </section>
          </div>
        </div>
      )}
    </>
  );
}
