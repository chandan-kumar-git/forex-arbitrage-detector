import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Icon } from "../components/Icon";
import { RateTicker } from "../components/RateTicker";
import { RingDisplay } from "../components/RingDisplay";
import { planRoute } from "../engine/route";
import { Reveal, useCountUp } from "../lib/motion";
import { Wordmark } from "../components/Wordmark";
import { pathText, pct, rate } from "../lib/format";
import { buildPhases } from "../lib/replay";
import { market, useMarket } from "../store/market";
import "./Landing.css";

export const REPO_URL = "https://github.com/Chandankumar775/arbitary-using-bellman-ford-";

export function Landing() {
  const state = useMarket((s) => s);
  const latest = state.recent[0] ?? null;
  const [selectedLeg, setSelectedLeg] = useState<number | null>(null);
  useEffect(() => setSelectedLeg(null), [latest?.id]);
  const javaDown = state.source === "java" && state.javaStatus !== "online";

  return (
    <div className="landing">
      <header className="topbar">
        <Wordmark />
        <nav className="topnav" aria-label="Sections">
          <a href="#real-life">Real life</a>
          <a href="#how">How it works</a>
          <a href="#replay">Replay</a>
          <a href="#built">How it's built</a>
        </nav>
        <a className="btn btn-quiet btn-sm topbar-git" href={REPO_URL} target="_blank" rel="noreferrer">
          <Icon name="github" size={18} />
          <span className="hide-sm">GitHub</span>
        </a>
      </header>
      <RateTicker snapshot={state.snapshot} />

      <main>
        <section className="hero" aria-labelledby="hero-title">
          <div className="hero-copy">
            <h1 id="hero-title">Find the currency loop that pays.</h1>
            <p className="hero-sub">
              Bellman-Ford watches a simulated forex market for a chain of trades that ends with more money than it
              started with, and shows you exactly how it found it.
            </p>
            <div className="hero-actions">
              <Link className="btn btn-primary" to="/desk">
                Open the live desk
                <Icon name="arrow" size={18} />
              </Link>
              <a className="btn btn-quiet" href="#how">
                How it works
              </a>
            </div>
            <p className="hero-live" aria-live="polite">
              <span className="live-dot" aria-hidden="true" />
              {javaDown ? (
                <>
                  The Java engine is not answering.{" "}
                  <button type="button" className="link-btn" onClick={() => market.useSource("browser")}>
                    Use the browser engine
                  </button>
                </>
              ) : latest ? (
                <>
                  Latest loop <span className="gain">{pct(latest.profitPercent)}</span> <strong>{pathText(latest.path)}</strong>
                </>
              ) : (
                "Watching the market for the first loop…"
              )}
            </p>
          </div>

          <figure className="hero-map">
            <RingDisplay
              snapshot={latest?.snapshot ?? state.snapshot}
              cycle={latest?.cycle}
              legs={latest?.legs}
              selectedLeg={selectedLeg}
              onSelectLeg={setSelectedLeg}
              eventKey={latest?.id}
              title="Live currency network"
            />
            <figcaption>Tap a line to see that trade. Simulated market, real algorithm.</figcaption>
          </figure>
        </section>

        <RealLife />
        <Reveal>
          <Features />
        </Reveal>
        <Reveal>
          <HowItWorks />
        </Reveal>
        <Reveal>
          <ReplayTeaser />
        </Reveal>
        <Reveal>
          <Built />
        </Reveal>

        <section className="closing" aria-labelledby="closing-title">
          <h2 id="closing-title">Watch it find one.</h2>
          <p>It runs entirely in your browser. Nothing to install.</p>
          <div className="hero-actions">
            <Link className="btn btn-primary" to="/desk">
              Open the live desk
              <Icon name="arrow" size={18} />
            </Link>
            <a className="btn" href={REPO_URL} target="_blank" rel="noreferrer">
              <Icon name="github" size={18} />
              View the source
            </a>
          </div>
        </section>
      </main>

      <footer className="footer">
        <Wordmark />
        <p>Built by Chandan Kumar for the Advanced Java course. Rates are simulated; this is not financial advice.</p>
      </footer>
    </div>
  );
}

const FEATURES = [
  { to: "/desk/send", title: "Send money", text: "The cheapest route between two currencies, and how much it saves." },
  { to: "/desk/game", title: "Beat the algorithm", text: "Find the hidden loop yourself before the 20-second timer runs out." },
  { to: "/desk", title: "Time machine", text: "Rewind the last 3 minutes of the market, or replay them at 4× speed." },
  { to: "/desk/settings", title: "Profit alerts", text: "A toast, a chime or a browser notification when a loop is worth it." },
  { to: "/desk/replay", title: "Step-by-step replay", text: "Watch Bellman-Ford find a loop, round by round." },
];

/** What you can do, drawn as one line with a stop per feature. */
function Features() {
  return (
    <section id="features" className="section" aria-labelledby="features-title">
      <div className="section-head">
        <h2 id="features-title">Five things to try</h2>
        <p>Each stop opens straight into the live desk. Press Ctrl+K there to jump anywhere.</p>
      </div>
      <ol className="feature-line">
        {FEATURES.map((f, i) => (
          <li key={f.title} style={{ animationDelay: `${i * 90}ms` }}>
            <Link to={f.to} className="feature-stop">
              <span className="feature-dot" aria-hidden="true" />
              <strong>{f.title}</strong>
              <span>{f.text}</span>
              <span className="feature-go">
                Open <Icon name="arrow" size={14} />
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </section>
  );
}

const SEND_TARGETS = ["JPY", "EUR", "GBP"];

/** The real-life use, interactive: move the slider and see how much the best route saves. */
function RealLife() {
  const snapshot = useMarket((s) => s.snapshot);
  const [amount, setAmount] = useState(100000);
  const [to, setTo] = useState("JPY");
  const plan = snapshot ? planRoute(snapshot, "INR", to, amount) : null;
  const saving = useCountUp(plan ? plan.saving : 0);
  const received = useCountUp(plan ? plan.received : 0);
  const fmt = (v: number) => v.toLocaleString("en-IN", { maximumFractionDigits: 0 });

  return (
    <section id="real-life" className="section real-life" aria-labelledby="real-title">
      <div className="real-grid">
        <Reveal>
          <div className="real-copy">
            <h2 id="real-title">Not just a puzzle. It saves real money.</h2>
            <p>
              The graph search that finds arbitrage loops also finds the cheapest way to change money. Banks quote
              rare pairs like INR → JPY with a wide spread, so paying through USD often gives you more.
            </p>
            <ul className="uses">
              <li>
                <strong>Paying fees abroad</strong>
                <span>Students and families sending rupees overseas can see the route that keeps the most money.</span>
              </li>
              <li>
                <strong>Catching mispriced quotes</strong>
                <span>Trading desks and exchanges use this check to spot prices that disagree, in real time.</span>
              </li>
              <li>
                <strong>Alerts instead of staring</strong>
                <span>Set a profit level once; the desk and the Java portal tell you when a loop reaches it.</span>
              </li>
            </ul>
            <Link className="btn btn-primary" to="/desk/send">
              Plan a transfer
              <Icon name="arrow" size={18} />
            </Link>
          </div>
        </Reveal>
        <Reveal delay={120}>
          <div className="send-card">
            <div className="send-card-row">
              <label htmlFor="send-amount" className="label">
                You send
              </label>
              <strong className="num">₹{fmt(amount)}</strong>
            </div>
            <input
              id="send-amount"
              className="range"
              type="range"
              min={10000}
              max={500000}
              step={5000}
              value={amount}
              onChange={(e) => setAmount(Number(e.target.value))}
            />
            <div className="segmented" role="group" aria-label="Currency you need">
              {SEND_TARGETS.map((c) => (
                <button key={c} type="button" aria-pressed={to === c} onClick={() => setTo(c)}>
                  INR → {c}
                </button>
              ))}
            </div>
            {plan ? (
              <>
                <div className="send-route" aria-label={plan.path.join(" to ")} key={plan.path.join("-")}>
                  {plan.path.map((c, i) => (
                    <span key={`${c}-${i}`} className="send-stop" style={{ animationDelay: `${i * 140}ms` }}>
                      <b />
                      {c}
                    </span>
                  ))}
                </div>
                <dl className="send-figures">
                  <div>
                    <dt>Best route gives</dt>
                    <dd className="num">{fmt(received)} {to}</dd>
                  </div>
                  <div>
                    <dt>Changing directly gives</dt>
                    <dd className="num">{fmt(plan.direct)} {to}</dd>
                  </div>
                  <div className="send-saving">
                    <dt>You keep extra</dt>
                    <dd className="num">
                      {plan.saving > 0.005 ? `${fmt(saving)} ${to}` : "0"}
                      {plan.saving > 0.005 ? <span>+{plan.savingPercent.toFixed(2)}%</span> : null}
                    </dd>
                  </div>
                </dl>
              </>
            ) : (
              <p className="replay-wait">Waiting for the first market tick…</p>
            )}
          </div>
        </Reveal>
      </div>
    </section>
  );
}

function HowItWorks() {
  const latest = useMarket((s) => s.recent[0] ?? null);
  const fee = useMarket((s) => s.settings.fee);
  const leg = latest?.legs[0];
  return (
    <section id="how" className="section" aria-labelledby="how-title">
      <div className="section-head">
        <h2 id="how-title">How it works</h2>
        <p>Three ideas, applied to the market every second.</p>
      </div>
      <ol className="line-steps">
        <li className="step">
          <span className="step-stop" aria-hidden="true" />
          <h3>Rates become a map</h3>
          <p>
            Each currency is a station. Each exchange rate is a line from one station to another, like a metro map.
          </p>
        </li>
        <li className="step">
          <span className="step-stop" aria-hidden="true" />
          <h3>Multiplying becomes adding</h3>
          <p>
            A loop pays when its rates multiply to more than 1. Taking −log of each rate (after the fee) turns that into
            a sum below zero, which Bellman-Ford is built to find.
          </p>
          {leg ? (
            <p className="step-example">
              {leg.from} → {leg.to}: −log({rate(leg.rate)} × {(1 - fee).toFixed(4)}) = {(-Math.log(leg.rate * (1 - fee))).toFixed(4)}
            </p>
          ) : null}
        </li>
        <li className="step step-end">
          <span className="step-stop" aria-hidden="true" />
          <h3>The loop is the profit</h3>
          <p>
            If distances still shrink after n rounds, a negative loop exists. Walking back the parent pointers pulls it
            out, and multiplying its rates gives the profit.
          </p>
          {latest ? (
            <p className="step-example">
              {pathText(latest.path)} = <span className="gain">{pct(latest.profitPercent)}</span>
            </p>
          ) : null}
        </li>
      </ol>
    </section>
  );
}

function ReplayTeaser() {
  const latest = useMarket((s) => s.recent[0] ?? s.history[0] ?? null);
  const [pinned, setPinned] = useState(latest);
  useEffect(() => {
    if (!pinned && latest) setPinned(latest);
  }, [latest, pinned]);
  const built = useMemo(() => (pinned ? buildPhases(pinned) : null), [pinned]);
  const phases = built?.phases ?? [];
  const [step, setStep] = useState(0);
  const [playing, setPlaying] = useState(true);

  useEffect(() => {
    if (!playing || phases.length === 0) return;
    const t = window.setTimeout(
      () => {
        if (step >= phases.length - 1) {
          setPinned(latest);
          setStep(0);
        } else setStep(step + 1);
      },
      step >= phases.length - 1 ? 3500 : 1800,
    );
    return () => window.clearTimeout(t);
  }, [playing, step, phases.length, latest]);

  const phase = phases[Math.min(step, phases.length - 1)];

  return (
    <section id="replay" className="section replay-band" aria-labelledby="replay-title">
      <div className="replay-grid">
        <div className="replay-copy">
          <h2 id="replay-title">Every find comes with its proof.</h2>
          <p>
            The detector keeps the market snapshot behind each loop, so it can run Bellman-Ford again and show you each
            round: which lines it relaxed, and how the loop was pulled out at the end.
          </p>
          {pinned ? (
            <Link className="btn btn-primary" to={`/desk/replay/${pinned.id}`}>
              Open the full replay
              <Icon name="arrow" size={18} />
            </Link>
          ) : null}
        </div>
        {pinned && phase ? (
          <div className="replay-card">
            <div className="replay-card-head">
              <span className="replay-step-name">{phase.name}</span>
              <span className="label">
                step {step + 1} of {phases.length}
              </span>
              <button
                type="button"
                className="btn btn-sm btn-quiet"
                onClick={() => setPlaying(!playing)}
                aria-label={playing ? "Pause the replay" : "Play the replay"}
              >
                <Icon name={playing ? "pause" : "play"} size={14} />
              </button>
            </div>
            <RingDisplay
              snapshot={pinned.snapshot}
              cycle={phase.showLoop ? built!.trace.cycle : null}
              legs={phase.showLoop ? pinned.legs : null}
              ghosts={phase.ghosts}
              spokes={phase.spokes}
              marked={phase.marked}
              eventKey={`${pinned.id}-${step}`}
              title="Replay"
            />
            <p className="replay-text" aria-live="polite">
              {phase.text}
            </p>
            <div className="replay-dots" role="group" aria-label="Replay steps">
              {phases.map((p, i) => (
                <button
                  key={p.name}
                  type="button"
                  aria-label={p.name}
                  aria-current={i === step ? "step" : undefined}
                  onClick={() => {
                    setStep(i);
                    setPlaying(false);
                  }}
                />
              ))}
            </div>
          </div>
        ) : (
          <p className="replay-wait">The first loop is on its way; its replay appears here as soon as it lands.</p>
        )}
      </div>
    </section>
  );
}

function Built() {
  return (
    <section id="built" className="section" aria-labelledby="built-title">
      <div className="section-head">
        <h2 id="built-title">Built twice, same algorithm</h2>
        <p>
          It started as a Java desktop app for an Advanced Java course. This page runs the same algorithm, ported to
          TypeScript, and can also read the Java app live when both run on one computer.
        </p>
      </div>
      <table className="compare">
        <caption className="visually-hidden">The Java desktop app compared with this web app</caption>
        <thead>
          <tr>
            <th scope="col">
              <span className="visually-hidden">Aspect</span>
            </th>
            <th scope="col">Java desktop app</th>
            <th scope="col">This web app</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th scope="row">Built with</th>
            <td>Java 17, Spring Boot</td>
            <td>React, TypeScript</td>
          </tr>
          <tr>
            <th scope="row">Interface</th>
            <td>Swing window with a live network graph</td>
            <td>This page and the live desk, in any browser</td>
          </tr>
          <tr>
            <th scope="row">Storage</th>
            <td>MySQL through JDBC, one transaction per loop</td>
            <td>Your browser, the newest 500 loops</td>
          </tr>
          <tr>
            <th scope="row">Rates</th>
            <td>Its own simulator, or a feed server over TCP sockets</td>
            <td>Its own simulator, or the Java app on your computer</td>
          </tr>
          <tr>
            <th scope="row">Checked by</th>
            <td>34 JUnit tests</td>
            <td>12 tests, including a brute-force cross-check</td>
          </tr>
        </tbody>
      </table>
    </section>
  );
}
