import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Icon } from "../../components/Icon";
import { LegTable } from "../../components/LegTable";
import { RingDisplay } from "../../components/RingDisplay";
import type { ArbEvent } from "../../engine/types";
import { clock, eventId, micros, pathText, pct, percentOf } from "../../lib/format";
import { market, useMarket } from "../../store/market";

/** The live view: the network map, the loop in focus, and the session's log. */
export function Overview() {
  const s = useMarket((st) => st);
  const [pinnedId, setPinnedId] = useState<number | null>(null);
  const [selectedLeg, setSelectedLeg] = useState<number | null>(null);

  const latest = s.recent[0] ?? null;
  const pinned = pinnedId !== null ? (market.findEvent(pinnedId) ?? null) : null;
  const focus: ArbEvent | null = pinned ?? latest;

  useEffect(() => setSelectedLeg(null), [focus?.id]);

  return (
    <div className="overview">
      <section className="panel display-panel" aria-labelledby="display-title">
        <div className="panel-head">
          <h2 id="display-title">{pinned ? `Loop ${eventId(pinned.id)}` : "Live market"}</h2>
          <span className="spacer" />
          {pinned ? (
            <button type="button" className="btn btn-sm" onClick={() => setPinnedId(null)}>
              Back to live
            </button>
          ) : null}
        </div>
        <RingDisplay
          snapshot={focus?.snapshot ?? s.snapshot}
          cycle={focus?.cycle}
          legs={focus?.legs}
          selectedLeg={selectedLeg}
          onSelectLeg={setSelectedLeg}
          eventKey={focus?.id}
        />
        <p className="display-hint">The blue line is the loop. Tap a line or a row to see that trade.</p>
      </section>

      <div className="side-stack">
        <section className="panel" aria-labelledby="focus-title">
          <div className="panel-head">
            <h2 id="focus-title">{pinned ? "Selected loop" : "Latest loop"}</h2>
          </div>
          {focus ? (
            <>
              <div className="event-meta">
                <span className="event-profit">{pct(focus.profitPercent)}</span>
                <span className="event-path">{pathText(focus.path)}</span>
                <span className="event-sub">
                  {eventId(focus.id)} · {clock(focus.detectedAt)} · fee {percentOf(focus.fee)} per trade
                </span>
              </div>
              <LegTable event={focus} selectedLeg={selectedLeg} onSelectLeg={setSelectedLeg} />
              <div className="event-actions">
                <Link className="btn btn-primary btn-sm" to={`/desk/replay/${focus.id}`}>
                  <Icon name="replay" size={16} />
                  Replay how it was found
                </Link>
              </div>
            </>
          ) : (
            <div className="empty">
              <h3>Watching for the first loop</h3>
              <p>
                {s.source === "java"
                  ? "Waiting for snapshots from the Java engine."
                  : "The simulator nudges one rate out of line about every third second, so a loop usually appears quickly."}
              </p>
            </div>
          )}
        </section>

        <section className="panel" aria-labelledby="log-title">
          <div className="panel-head">
            <h2 id="log-title">Since you opened the page</h2>
            <span className="spacer" />
            <Link className="btn btn-sm btn-quiet" to="/desk/events">
              All events
              <Icon name="arrow" size={14} />
            </Link>
          </div>
          {s.recent.length ? (
            <ol className="log">
              {s.recent.slice(0, 8).map((e) => (
                <li key={e.id}>
                  <button type="button" aria-current={focus?.id === e.id} onClick={() => setPinnedId(e.id)}>
                    <span>{clock(e.detectedAt)}</span>
                    <span className="log-path">{pathText(e.path)}</span>
                    <span className="gain">{pct(e.profitPercent, 2)}</span>
                  </button>
                </li>
              ))}
            </ol>
          ) : (
            <p className="display-hint" style={{ paddingTop: 18 }}>
              Loops found this session appear here.
            </p>
          )}
        </section>

        {s.source === "java" ? <JavaPanel /> : null}
      </div>
    </div>
  );
}

function JavaPanel() {
  const s = useMarket((st) => st);
  const j = s.java;
  return (
    <section className="panel java-panel" aria-labelledby="java-title">
      <div className="panel-head">
        <h2 id="java-title">Java engine</h2>
        <span className="spacer" />
        <span className={`chip ${s.javaStatus === "online" ? "chip-live" : "chip-off"}`}>
          <span className="chip-dot" aria-hidden="true" />
          {s.javaStatus === "online" ? "Connected" : s.javaStatus === "connecting" ? "Connecting" : "Offline"}
        </span>
      </div>
      <div className="panel-body">
        {s.javaError ? <p className="notice">{s.javaError}</p> : null}
        {j ? (
          <>
            <dl>
              <div>
                <dt>Ticks</dt>
                <dd>{j.stats.ticks.toLocaleString("en-US")}</dd>
              </div>
              <div>
                <dt>Loops found</dt>
                <dd>{j.stats.found}</dd>
              </div>
              <div>
                <dt>Best</dt>
                <dd className="gain">{pct(j.stats.bestProfit, 2)}</dd>
              </div>
              <div>
                <dt>Tick time</dt>
                <dd>{micros(j.stats.avgTickMicros)}</dd>
              </div>
              <div>
                <dt>MySQL</dt>
                <dd>{j.database ? "Connected" : "Offline"}</dd>
              </div>
            </dl>
            <p className="field-help" style={{ marginTop: 14 }}>
              The Java app's own numbers. The map re-runs the browser engine on the same rates, so you can compare
              them.
            </p>
          </>
        ) : (
          <p className="field-help">Waiting for the first answer from {s.javaUrl}…</p>
        )}
      </div>
    </section>
  );
}
