import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Icon } from "../../components/Icon";
import { LegTable } from "../../components/LegTable";
import { RingDisplay } from "../../components/RingDisplay";
import { TimeMachine } from "../../components/TimeMachine";
import { useCountUp } from "../../lib/motion";
import { downloadShareCard } from "../../lib/shareCard";
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

  // Time Machine: remember the viewed tick by its time, so it stays put while new ticks arrive
  const [rewindTs, setRewindTs] = useState<number | null>(null);
  const rewindIndex = rewindTs === null ? -1 : s.timeline.findIndex((t) => t.ts === rewindTs);
  const point = rewindIndex >= 0 ? s.timeline[rewindIndex] : null;
  const rewound = point ? (point.eventId !== null ? (market.findEvent(point.eventId) ?? null) : null) : undefined;
  const mapEvent = rewound === undefined ? focus : rewound;
  const mapSnapshot = point ? point.snapshot : (focus?.snapshot ?? s.snapshot);
  const profitShown = useCountUp(focus?.profitPercent ?? 0);

  useEffect(() => setSelectedLeg(null), [focus?.id]);

  return (
    <div className="overview">
      <div className="stack">
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
          snapshot={mapSnapshot}
          cycle={mapEvent?.cycle}
          legs={mapEvent?.legs}
          selectedLeg={selectedLeg}
          onSelectLeg={setSelectedLeg}
          eventKey={mapEvent?.id ?? point?.ts}
        />
        <p className="display-hint">
          {point ? (rewound ? "A loop was found at this moment." : "No loop at this moment: the market was fair.") : "The blue line is the loop. Tap a line or a row to see that trade."}
        </p>
      </section>
      <TimeMachine
        timeline={s.timeline}
        index={rewindIndex >= 0 ? rewindIndex : null}
        onChange={(i) => {
          setRewindTs(i === null ? null : s.timeline[i].ts);
          const id = i === null ? null : s.timeline[i].eventId;
          if (id !== null) setPinnedId(id);
        }}
      />
      </div>

      <div className="side-stack">
        <section className="panel" aria-labelledby="focus-title">
          <div className="panel-head">
            <h2 id="focus-title">{pinned ? "Selected loop" : "Latest loop"}</h2>
          </div>
          {focus ? (
            <>
              <div className="event-meta">
                <span className="event-profit">{pct(profitShown)}</span>
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
                <button type="button" className="btn btn-sm" onClick={() => downloadShareCard(focus)}>
                  <Icon name="download" size={16} />
                  Share card
                </button>
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
