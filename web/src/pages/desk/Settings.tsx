import { useState } from "react";
import { DEFAULT_SETTINGS, type MarketSettings } from "../../engine/types";
import { percentOf } from "../../lib/format";
import { useAlerts } from "../../store/alerts";
import { market, useMarket } from "../../store/market";

/** Profit alerts: a toast in the desk, and a browser notification when the tab is in the background. */
function AlertsPanel() {
  const a = useAlerts();
  const p = a.prefs;
  const supported = typeof window !== "undefined" && "Notification" in window;
  return (
    <section className="panel" aria-labelledby="alerts-title">
      <div className="panel-head">
        <h2 id="alerts-title">Profit alerts</h2>
        <span className="spacer" />
        <span className="label">{a.fired} fired</span>
      </div>
      <div className="panel-body stack">
        <label className="confirm-row">
          <input type="checkbox" checked={p.enabled} onChange={(e) => a.setPrefs({ enabled: e.target.checked })} />
          Show an alert when a loop pays at least
        </label>
        <div className="slider-row">
          <label htmlFor="alert-min" className="label">
            Minimum profit
          </label>
          <output htmlFor="alert-min">{p.minProfit.toFixed(2)}%</output>
          <input id="alert-min" className="range" type="range" min={0} max={1.5} step={0.05} value={p.minProfit} onChange={(e) => a.setPrefs({ minProfit: Number(e.target.value) })} />
        </div>
        {supported ? (
          <label className="confirm-row">
            <input
              type="checkbox"
              checked={p.browser}
              onChange={async (e) => {
                const on = e.target.checked;
                if (on && Notification.permission !== "granted") {
                  const answer = await Notification.requestPermission();
                  a.setPrefs({ browser: answer === "granted" });
                } else a.setPrefs({ browser: on });
              }}
            />
            Also notify me when this tab is in the background
          </label>
        ) : null}
        <p className="field-help">Like a trader's price alert: you do not have to watch the screen to catch a good loop.</p>
      </div>
    </section>
  );
}

interface Slider {
  key: keyof MarketSettings;
  label: string;
  min: number;
  max: number;
  step: number;
  show: (v: number) => string;
  help: string;
}

const SLIDERS: Slider[] = [
  { key: "fee", label: "Fee per leg", min: 0, max: 0.05, step: 0.0001, show: (v) => percentOf(v, 2), help: "Cost of every conversion. Try 1 %: loops stop paying." },
  { key: "spread", label: "Spread", min: 0, max: 0.02, step: 0.0001, show: (v) => percentOf(v, 2), help: "Gap between buy and sell rate, applied to every pair." },
  { key: "injectChance", label: "Inject chance", min: 0, max: 1, step: 0.01, show: (v) => percentOf(v, 0), help: "How often the simulator pushes one rate 0.6–1.2 % out of line." },
  { key: "volatility", label: "Volatility", min: 0, max: 0.005, step: 0.0001, show: (v) => percentOf(v, 2), help: "Size of the random price step on each tick." },
  { key: "tickMs", label: "Tick interval", min: 250, max: 5000, step: 250, show: (v) => `${v} ms`, help: "How often the browser engine reads the market." },
];

export function Settings() {
  const s = useMarket((st) => st);
  const [url, setUrl] = useState(s.javaUrl);
  const urlValid = /^https?:\/\/[^\s/]+(:\d+)?\/?$/.test(url.trim());

  return (
    <>
      <div className="page-head">
        <h1>Settings</h1>
        <p>Market rules for the browser engine, and where the rates come from. Changes apply from the next tick and are remembered in this browser.</p>
      </div>

      <div className="two-col">
        <section className="panel" aria-labelledby="market-title">
          <div className="panel-head">
            <h2 id="market-title">Market</h2>
            <span className="spacer" />
            <button type="button" className="btn btn-sm btn-quiet" onClick={market.resetSettings}>
              Reset defaults
            </button>
          </div>
          <div className="panel-body stack">
            {SLIDERS.map((sl) => (
              <div key={sl.key} className="slider-row">
                <label htmlFor={`set-${sl.key}`} className="label">
                  {sl.label}
                </label>
                <output htmlFor={`set-${sl.key}`}>{sl.show(s.settings[sl.key])}</output>
                <input
                  id={`set-${sl.key}`}
                  className="range"
                  type="range"
                  min={sl.min}
                  max={sl.max}
                  step={sl.step}
                  value={s.settings[sl.key]}
                  onChange={(e) => market.updateSettings({ [sl.key]: Number(e.target.value) })}
                />
                <span className="field-help">
                  {sl.help} Default {sl.show(DEFAULT_SETTINGS[sl.key])}.
                </span>
              </div>
            ))}
            {s.source === "java" ? (
              <p className="notice notice-info">
                The Java engine uses its own fee, spread and inject chance (set in the desktop app). The fee here is used
                by the browser's re-check of the Java snapshots.
              </p>
            ) : null}
          </div>
        </section>

        <div className="stack">
          <section className="panel" aria-labelledby="source-title">
            <div className="panel-head">
              <h2 id="source-title">Data source</h2>
            </div>
            <div className="panel-body stack">
              <div className="segmented" role="group" aria-label="Engine">
                <button type="button" aria-pressed={s.source === "browser"} onClick={() => market.useSource("browser")}>
                  Browser engine
                </button>
                <button type="button" aria-pressed={s.source === "java"} onClick={() => market.useSource("java")} disabled={!urlValid}>
                  Java engine
                </button>
              </div>
              <label className="field">
                <span>Java engine address</span>
                <input
                  className="input"
                  value={url}
                  aria-invalid={!urlValid}
                  onChange={(e) => setUrl(e.target.value)}
                  onBlur={() => urlValid && market.setJavaUrl(url.trim())}
                />
                {urlValid ? (
                  <span className="field-help">The desktop app serves GET /api/state here while it runs.</span>
                ) : (
                  <span className="field-error">Use an address like http://127.0.0.1:8080</span>
                )}
              </label>
              {s.source === "java" ? (
                s.javaStatus === "online" ? (
                  <p className="notice notice-info" role="status">
                    Connected. Rates come from Spring Boot; the browser re-runs Bellman-Ford on every Java snapshot.
                  </p>
                ) : (
                  <p className="notice" role="status">
                    {s.javaError || "Connecting…"}
                  </p>
                )
              ) : (
                <p className="field-help">
                  To use the Java engine: run <code>run-all.bat</code> on this computer, then pick Java engine. Works in
                  Chrome and Edge from the Vercel page or from <code>npm run dev</code>.
                </p>
              )}
            </div>
          </section>

          <AlertsPanel />

          <section className="panel" aria-labelledby="storage-title">
            <div className="panel-head">
              <h2 id="storage-title">Stored data</h2>
            </div>
            <div className="panel-body stack">
              <p className="field-help">
                {s.history.length} events, {s.currencies.length} currencies and these settings are kept in this browser
                only. Nothing is sent anywhere.
              </p>
              <dl className="keys">
                <div>
                  <dt>Pause / resume</dt>
                  <dd>
                    <kbd>Space</kbd>
                  </dd>
                </div>
                <div>
                  <dt>Replay steps</dt>
                  <dd>
                    <kbd>←</kbd> <kbd>→</kbd>
                  </dd>
                </div>
              </dl>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}
