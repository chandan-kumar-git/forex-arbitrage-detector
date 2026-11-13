import { useCallback, useEffect, useMemo, useState } from "react";
import { Icon } from "../../components/Icon";
import { RateLattice } from "../../components/RateLattice";
import { RingDisplay } from "../../components/RingDisplay";
import { detect, type Detection } from "../../engine/detect";
import { legsOf, profitPercent } from "../../engine/profit";
import { Simulator } from "../../engine/simulator";
import { DEFAULT_CURRENCIES, DEFAULT_SETTINGS, type Snapshot } from "../../engine/types";
import { pct } from "../../lib/format";
import { useCountUp } from "../../lib/motion";
import { load, save } from "../../store/storage";
import "./Game.css";

const ROUND_SECONDS = 20;
const FEE = DEFAULT_SETTINGS.fee;
const HINT_COST = 40;

interface Round {
  snapshot: Snapshot;
  answer: Detection;
}

/** A market that is guaranteed to hide one profitable loop. */
function newRound(): Round {
  const sim = new Simulator(DEFAULT_CURRENCIES);
  for (;;) {
    const s = sim.next({ ...DEFAULT_SETTINGS, injectChance: 1 });
    const { injected: _drop, ...snapshot } = s;
    void _drop;
    const answer = detect(snapshot, FEE);
    if (answer) return { snapshot, answer };
  }
}

type Phase = "play" | "won" | "lost" | "timeout";

/**
 * "Beat the algorithm": find the profitable loop yourself by tapping stations, before the timer runs out.
 * It teaches what Bellman-Ford does: Bellman-Ford checks every route in microseconds; a person needs seconds.
 */
export function Game() {
  const [round, setRound] = useState<Round>(newRound);
  const [path, setPath] = useState<number[]>([]);
  const [phase, setPhase] = useState<Phase>("play");
  const [left, setLeft] = useState(ROUND_SECONDS);
  const [hint, setHint] = useState(false);
  const [score, setScore] = useState(0);
  const [roundNo, setRoundNo] = useState(1);
  const [streak, setStreak] = useState(0);
  const [best, setBest] = useState(() => load<number>("gameBest", 0));
  const [gained, setGained] = useState(0);
  const shownScore = useCountUp(score, 900);

  const codes = round.snapshot.codes;
  const closed = path.length >= 3 && path[0] === path[path.length - 1];
  const yourProfit = closed ? profitPercent(path, round.snapshot, FEE) : null;

  const finish = useCallback(
    (result: Phase, finalPath: number[]) => {
      setPhase(result);
      if (result === "won") {
        const p = profitPercent(finalPath, round.snapshot, FEE);
        const sameAsAlgorithm = sameLoop(finalPath, round.answer.cycle);
        const points = Math.round(100 + p * 150 + left * 6 + (sameAsAlgorithm ? 50 : 0) - (hint ? HINT_COST : 0) + streak * 20);
        setGained(points);
        setScore((s) => {
          const total = s + points;
          if (total > best) {
            setBest(total);
            save("gameBest", total);
          }
          return total;
        });
        setStreak((k) => k + 1);
      } else {
        setGained(0);
        setStreak(0);
      }
    },
    [round, left, hint, streak, best],
  );

  // the countdown
  useEffect(() => {
    if (phase !== "play") return;
    if (left <= 0) {
      finish("timeout", path);
      return;
    }
    const t = window.setTimeout(() => setLeft((x) => Math.max(0, +(x - 0.1).toFixed(1))), 100);
    return () => window.clearTimeout(t);
  }, [phase, left, finish, path]);

  const tap = (i: number) => {
    if (phase !== "play") return;
    if (path.length === 0) return setPath([i]);
    if (i === path[path.length - 1]) return;
    if (i === path[0] && path.length >= 3) {
      const loop = [...path, i];
      setPath(loop);
      finish(profitPercent(loop, round.snapshot, FEE) > 0 ? "won" : "lost", loop);
      return;
    }
    if (path.includes(i)) return;                 // a simple loop visits each station once
    setPath([...path, i]);
  };

  const next = () => {
    setRound(newRound());
    setPath([]);
    setPhase("play");
    setLeft(ROUND_SECONDS);
    setHint(false);
    setRoundNo((r) => r + 1);
  };

  const reveal = phase !== "play";
  const answerLegs = useMemo(() => legsOf(round.answer.cycle, round.snapshot, FEE), [round]);

  return (
    <>
      <div className="page-head">
        <h1>Beat the algorithm</h1>
        <p>
          One rate in this market is out of line, so a profitable loop is hiding in it. Tap stations to draw a loop and
          tap the first station again to close it. You have {ROUND_SECONDS} seconds. The detector finds it in microseconds.
        </p>
      </div>

      <div className="game">
        <section className={`panel display-panel game-map ${phase}`} aria-label="Game map">
          <RingDisplay
            snapshot={round.snapshot}
            cycle={reveal ? round.answer.cycle : null}
            legs={reveal ? answerLegs : null}
            userPath={path}
            onStationClick={phase === "play" ? tap : undefined}
            eventKey={`${roundNo}-${reveal}`}
            title="Game market"
          />
          <p className="display-hint">
            {phase === "play"
              ? path.length === 0
                ? "Tap any station to start your loop."
                : `Your route: ${path.map((i) => codes[i]).join(" → ")}. ${path.length >= 3 ? `Tap ${codes[path[0]]} to close it.` : "Keep going."}`
              : "Blue: the detector's loop. Dotted: yours."}
          </p>
        </section>

        <div className="stack">
          <section className="panel game-hud">
            <Countdown left={left} total={ROUND_SECONDS} phase={phase} />
            <dl className="game-stats">
              <div>
                <dt>Score</dt>
                <dd className="num">{Math.round(shownScore)}</dd>
              </div>
              <div>
                <dt>Round</dt>
                <dd className="num">{roundNo}</dd>
              </div>
              <div>
                <dt>Streak</dt>
                <dd className="num">{streak}</dd>
              </div>
              <div>
                <dt>Best</dt>
                <dd className="num">{best}</dd>
              </div>
            </dl>
          </section>

          {phase === "play" ? (
            <section className="panel">
              <div className="panel-body stack">
                <div className="confirm-row">
                  <button type="button" className="btn btn-sm" onClick={() => setPath(path.slice(0, -1))} disabled={!path.length}>
                    Undo
                  </button>
                  <button type="button" className="btn btn-sm" onClick={() => setPath([])} disabled={!path.length}>
                    Clear
                  </button>
                  <button type="button" className="btn btn-sm" onClick={() => setHint(true)} disabled={hint}>
                    <Icon name="bolt" size={14} />
                    Hint (−{HINT_COST})
                  </button>
                  <button type="button" className="btn btn-sm btn-quiet" onClick={() => finish("timeout", path)}>
                    Give up
                  </button>
                </div>
                {hint ? (
                  <div className="game-hint">
                    <p className="field-help">Tinted cells sit away from the fair rate. Blue = too good: a good loop uses that trade.</p>
                    <RateLattice snapshot={round.snapshot} />
                  </div>
                ) : (
                  <p className="field-help">Tip: a loop needs at least 3 currencies. Look for the trade that pays more than it should.</p>
                )}
              </div>
            </section>
          ) : (
            <section className={`panel game-result ${phase}`} aria-live="polite">
              <div className="panel-body stack">
                <h2>{phase === "won" ? "You found a profitable loop!" : phase === "lost" ? "That loop loses money" : "Time's up"}</h2>
                {phase === "won" ? <p className="game-points num">+{gained} points</p> : null}
                <table className="data">
                  <tbody>
                    <tr>
                      <td>Your loop</td>
                      <td>{closed ? path.map((i) => codes[i]).join(" → ") : "not closed"}</td>
                      <td className={`r ${yourProfit !== null && yourProfit > 0 ? "gain" : "loss"}`}>{yourProfit !== null ? pct(yourProfit) : "—"}</td>
                    </tr>
                    <tr>
                      <td>Detector</td>
                      <td>{round.answer.path.join(" → ")}</td>
                      <td className="r gain">{pct(round.answer.profitPercent)}</td>
                    </tr>
                  </tbody>
                </table>
                <p className="field-help">
                  {phase === "won"
                    ? sameLoop(path, round.answer.cycle)
                      ? "Same loop as the detector: +50 bonus. You thought like Bellman-Ford."
                      : "A different profitable loop. The detector reports the first negative cycle it proves."
                    : "Every trade costs a 0.05% fee and the spread, so a loop only pays when one rate is far enough out of line."}
                </p>
                <button type="button" className="btn btn-primary" onClick={next} autoFocus>
                  Next round
                  <Icon name="arrow" size={16} />
                </button>
              </div>
            </section>
          )}
        </div>
      </div>
    </>
  );
}

/** The same loop, starting at any station (A→B→C→A equals B→C→A→B). */
function sameLoop(a: number[], b: number[]): boolean {
  if (a.length !== b.length || a.length < 2) return false;
  const x = a.slice(0, -1);
  const y = b.slice(0, -1);
  const start = y.indexOf(x[0]);
  return start >= 0 && x.every((v, k) => v === y[(start + k) % y.length]);
}

function Countdown({ left, total, phase }: { left: number; total: number; phase: Phase }) {
  const r = 34;
  const c = 2 * Math.PI * r;
  const frac = left / total;
  const urgent = phase === "play" && left <= 5;
  return (
    <div className={`countdown ${urgent ? "urgent" : ""}`}>
      <svg viewBox="0 0 80 80" aria-hidden="true">
        <circle cx="40" cy="40" r={r} className="cd-track" />
        <circle cx="40" cy="40" r={r} className="cd-fill" strokeDasharray={c} strokeDashoffset={c * (1 - frac)} />
      </svg>
      <span className="num" role="timer" aria-label={`${Math.ceil(left)} seconds left`}>
        {Math.ceil(left)}
      </span>
    </div>
  );
}
