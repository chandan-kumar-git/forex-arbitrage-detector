import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { Icon } from "../../components/Icon";
import { RingDisplay } from "../../components/RingDisplay";
import { clock, eventId, pathText, pct, percentOf } from "../../lib/format";
import { buildPhases } from "../../lib/replay";
import { market, useMarket } from "../../store/market";

/** Bellman-Ford re-run on an event's own snapshot, shown as named cues you can scrub through. */
export function Replay() {
  const { id } = useParams();
  const [params, setParams] = useSearchParams();
  const latestId = useMarket((s) => s.recent[0]?.id ?? s.history[0]?.id ?? null);
  // without an id, replay the newest event and keep it fixed while you study it
  const [fallbackId] = useState(latestId);
  const eventIdNum = id ? Number(id) : (fallbackId ?? latestId);
  useMarket((s) => s.history.length); // re-render when history changes
  const event = eventIdNum !== null ? market.findEvent(eventIdNum) : undefined;

  const built = useMemo(() => (event ? buildPhases(event) : null), [event]);
  const phases = built?.phases ?? [];
  const step = Math.min(Math.max(0, Number(params.get("phase") ?? 0) || 0), Math.max(0, phases.length - 1));
  const [playing, setPlaying] = useState(false);

  const go = useCallback(
    (next: number) => {
      const clamped = Math.min(Math.max(0, next), Math.max(0, phases.length - 1));
      setParams((p) => {
        const q = new URLSearchParams(p);
        q.set("phase", String(clamped));
        return q;
      }, { replace: true });
    },
    [phases.length, setParams],
  );

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.target as HTMLElement).tagName === "INPUT") return;
      if (e.key === "ArrowRight") go(step + 1);
      if (e.key === "ArrowLeft") go(step - 1);
      if (e.key === "Home") go(0);
      if (e.key === "End") go(phases.length - 1);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [go, step, phases.length]);

  useEffect(() => {
    if (!playing) return;
    if (step >= phases.length - 1) {
      setPlaying(false);
      return;
    }
    const t = window.setTimeout(() => go(step + 1), 1300);
    return () => window.clearTimeout(t);
  }, [playing, step, phases.length, go]);

  if (!event || !built) {
    return (
      <div className="panel">
        <div className="empty">
          <h3>{id ? `${eventId(Number(id))} is not stored in this browser` : "No event to replay yet"}</h3>
          <p>Events are kept in this browser only. Open the event display and wait for a loop, then replay it.</p>
          <Link className="btn btn-primary" to="/desk">
            Back to the event display
          </Link>
        </div>
      </div>
    );
  }

  const phase = phases[step];
  const round = phase.kind === "round" || phase.kind === "check" ? built.trace.rounds[Math.min(step, built.trace.rounds.length - 1)] : null;
  const prevDist = step > 0 && round ? built.trace.rounds[step - 1]?.dist : null;
  const codes = event.snapshot.codes;

  return (
    <>
      <div className="page-head">
        <h1>Replay {eventId(event.id)}</h1>
        <div className="actions">
          <Link className="btn btn-sm" to="/desk/events">
            <Icon name="back" size={14} />
            Events
          </Link>
          <button
            type="button"
            className="btn btn-sm"
            onClick={() => void navigator.clipboard?.writeText(window.location.href)}
            title="Copy a link to this exact step"
          >
            <Icon name="link" size={14} />
            Copy link to this step
          </button>
        </div>
        <p>
          {pathText(event.path)} · <span className="gain">{pct(event.profitPercent)}</span> · fee {percentOf(event.fee)} per
          leg · {clock(event.detectedAt)}. The snapshot is replayed exactly as it was when the loop was found.
        </p>
      </div>

      <div className="replay">
        <section className="panel display-panel" aria-label="Network at this step">
          <RingDisplay
            snapshot={event.snapshot}
            cycle={phase.showLoop ? built.trace.cycle : null}
            legs={phase.showLoop ? event.legs : null}
            ghosts={phase.ghosts}
            spokes={phase.spokes}
            dist={phase.dist}
            marked={phase.marked}
            eventKey={`${event.id}-${step}`}
            title={`Replay of ${eventId(event.id)}, ${phase.name}`}
          />
          <p className="display-hint">
            Dashed blue lines were relaxed in this round (faint: the round before). d is each currency's distance from the
            virtual source; filled stations changed in this round.
          </p>
        </section>

        <div className="side-stack">
          <section className="panel" aria-labelledby="cue-title">
            <div className="scrubber">
              <div className="scrubber-controls">
                <button type="button" className="btn btn-sm" onClick={() => go(0)} aria-label="First step">
                  <Icon name="first" size={16} />
                </button>
                <button type="button" className="btn btn-sm" onClick={() => go(step - 1)} aria-label="Previous step">
                  <Icon name="prev" size={16} />
                </button>
                <button
                  type="button"
                  className={`btn btn-sm ${playing ? "" : "btn-primary"}`}
                  onClick={() => {
                    if (step >= phases.length - 1) go(0);
                    setPlaying(!playing);
                  }}
                >
                  <Icon name={playing ? "pause" : "play"} size={14} />
                  {playing ? "Pause" : "Play"}
                </button>
                <button type="button" className="btn btn-sm" onClick={() => go(step + 1)} aria-label="Next step">
                  <Icon name="next" size={16} />
                </button>
                <button type="button" className="btn btn-sm" onClick={() => go(phases.length - 1)} aria-label="Last step">
                  <Icon name="last" size={16} />
                </button>
                <span className="phase-count">
                  {step + 1} / {phases.length}
                </span>
              </div>
              <div className="phase-track" role="group" aria-label="Steps">
                {phases.map((p, i) => (
                  <button
                    key={p.name}
                    type="button"
                    className={`${i < step ? "done" : ""} k-${p.kind}`}
                    aria-current={i === step ? "step" : undefined}
                    aria-label={p.name}
                    title={p.name}
                    onClick={() => go(i)}
                  />
                ))}
              </div>
              <h2 id="cue-title" className="phase-title">
                {phase.name}
              </h2>
              <p className="phase-text" aria-live="polite">
                {phase.text}
              </p>
              <p className="field-help">
                Keys: <kbd>←</kbd> <kbd>→</kbd> step, <kbd>Home</kbd> <kbd>End</kbd> jump.
              </p>
            </div>
          </section>

          {phase.dist ? (
            <section className="panel" aria-labelledby="dist-title">
              <div className="panel-head">
                <h3 id="dist-title">Distances and parents</h3>
              </div>
              <div className="table-wrap">
                <table className="data dist-table">
                  <thead>
                    <tr>
                      <th scope="col">Currency</th>
                      <th scope="col" className="r">
                        Distance
                      </th>
                      <th scope="col">Parent</th>
                    </tr>
                  </thead>
                  <tbody>
                    {codes.map((c, i) => {
                      const parent = (round ?? built.trace.rounds[built.trace.rounds.length - 1]).parent[i];
                      const changed = prevDist ? prevDist[i] !== phase.dist![i] : false;
                      return (
                        <tr key={c}>
                          <td style={{ fontWeight: 700 }}>
                            {c}
                          </td>
                          <td className={`num r ${changed ? "changed" : ""}`}>{phase.dist![i].toFixed(6)}</td>
                          <td className="num">{parent >= 0 ? codes[parent] : "source"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </section>
          ) : null}

          {round && round.relaxations.length ? (
            <section className="panel" aria-labelledby="relax-title">
              <div className="panel-head">
                <h3 id="relax-title">Edges relaxed in {phase.name.toLowerCase()}</h3>
              </div>
              <ol className="relax-list" style={{ paddingTop: 12 }}>
                {round.relaxations.map((r, k) => (
                  <li key={k}>
                    {codes[r.from]} → {codes[r.to]}: {r.before.toFixed(6)} → {r.after.toFixed(6)}
                  </li>
                ))}
              </ol>
            </section>
          ) : null}
        </div>
      </div>
    </>
  );
}
