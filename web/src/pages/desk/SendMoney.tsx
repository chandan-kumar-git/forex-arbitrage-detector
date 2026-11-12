import { useMemo, useState } from "react";
import { planRoute, type RoutePlan } from "../../engine/route";
import { useCountUp } from "../../lib/motion";
import { useMarket } from "../../store/market";
import "./SendMoney.css";

const PRESETS = [
  { label: "Student fees in Japan", from: "INR", to: "JPY", amount: 150000 },
  { label: "Family support from UK", from: "GBP", to: "INR", amount: 500 },
  { label: "Trip to Europe", from: "INR", to: "EUR", amount: 80000 },
];

function money(value: number, code: string) {
  return `${value.toLocaleString("en-IN", { maximumFractionDigits: 2, minimumFractionDigits: 2 })} ${code}`;
}

/** Real-life use: find the cheapest way to change money, and how much it saves. */
export function SendMoney() {
  const snapshot = useMarket((s) => s.snapshot);
  const codes = snapshot?.codes ?? [];
  const [from, setFrom] = useState("INR");
  const [to, setTo] = useState("JPY");
  const [amountText, setAmountText] = useState("50000");
  const amount = Number(amountText.replace(/,/g, ""));
  const invalid = !(amount > 0) || amount > 1e12;

  const plan = useMemo(() => (snapshot && !invalid ? planRoute(snapshot, from, to, amount) : null), [snapshot, from, to, amount, invalid]);
  const others = useMemo(() => {
    if (!snapshot || invalid) return [];
    return codes
      .filter((c) => c !== from)
      .map((c) => planRoute(snapshot, from, c, amount))
      .filter((p): p is RoutePlan => p !== null)
      .sort((a, b) => b.savingPercent - a.savingPercent);
  }, [snapshot, codes, from, amount, invalid]);

  return (
    <>
      <div className="page-head">
        <h1>Send money the cheapest way</h1>
        <p>
          Paying fees abroad or sending money home? Banks quote rare pairs (like INR → JPY) with a wide spread, so
          going through a common currency is often cheaper. The same graph search behind the detector finds the best
          route in the live market.
        </p>
      </div>

      <div className="presets" style={{ marginBottom: 20 }}>
        {PRESETS.map((p) => (
          <button
            key={p.label}
            type="button"
            className="btn btn-sm"
            onClick={() => {
              setFrom(p.from);
              setTo(p.to);
              setAmountText(String(p.amount));
            }}
          >
            {p.label}
          </button>
        ))}
      </div>

      <section className="panel send-form">
        <label className="field">
          <span>I have</span>
          <input className="input" inputMode="decimal" value={amountText} aria-invalid={invalid} onChange={(e) => setAmountText(e.target.value)} />
          {invalid ? <span className="field-error">Enter an amount above 0</span> : null}
        </label>
        <label className="field">
          <span>in</span>
          <select className="select" value={from} onChange={(e) => setFrom(e.target.value)}>
            {codes.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
        <button
          type="button"
          className="btn swap"
          aria-label="Swap currencies"
          onClick={() => {
            setFrom(to);
            setTo(from);
          }}
        >
          ⇄
        </button>
        <label className="field">
          <span>and need</span>
          <select className="select" value={to} onChange={(e) => setTo(e.target.value)}>
            {codes.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </label>
      </section>

      {plan ? <RouteResult plan={plan} /> : from === to ? <p className="notice" style={{ marginTop: 20 }}>Choose two different currencies.</p> : null}

      {others.length ? (
        <section className="panel" style={{ marginTop: 24 }}>
          <div className="panel-head">
            <h2>From {from} to every currency</h2>
          </div>
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th scope="col">To</th>
                  <th scope="col">Best route</th>
                  <th scope="col" className="r">You receive</th>
                  <th scope="col" className="r">Saving vs direct</th>
                </tr>
              </thead>
              <tbody>
                {others.map((p) => (
                  <tr key={p.to} onClick={() => setTo(p.to)} style={{ cursor: "pointer" }}>
                    <td style={{ fontWeight: 700 }}>{p.to}</td>
                    <td>{p.path.join(" → ")}</td>
                    <td className="r">{money(p.received, p.to)}</td>
                    <td className={`r ${p.saving > 1e-9 ? "gain" : "loss"}`}>{p.saving > 1e-9 ? `+${p.savingPercent.toFixed(2)}%` : "direct is best"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}
      <p className="field-help" style={{ marginTop: 16 }}>
        Model: 0.05% spread per major currency (USD, EUR, GBP, JPY, CHF), 0.40% per other currency, 0.80% extra when
        neither side is USD, and 0.10% fee per conversion. Rates are the live simulated market; the Java portal has
        the same planner at /portal/route.
      </p>
    </>
  );
}

function RouteResult({ plan }: { plan: RoutePlan }) {
  const received = useCountUp(plan.received);
  const saving = useCountUp(plan.saving);
  const better = plan.saving > plan.direct * 1e-9;
  const max = Math.max(plan.received, plan.direct);
  return (
    <div className="send-result" key={`${plan.from}-${plan.to}`}>
      <section className="panel result-main">
        <div className="panel-head">
          <h2>Best route</h2>
          <span className="spacer" />
          <span className={`chip ${better ? "chip-live" : "chip-sim"}`}>
            <span className="chip-dot" aria-hidden="true" />
            {better ? "Cheaper than direct" : "Direct is best"}
          </span>
        </div>
        <div className="panel-body">
          <div className="big-money num">{money(received, plan.to)}</div>
          <p className="field-help">you receive, after bank spreads and fees</p>
          <ol className="route-line" aria-label={plan.path.join(" to ")}>
            {plan.path.map((c, i) => (
              <li key={`${c}-${i}`} style={{ animationDelay: `${i * 160}ms` }}>
                <span className="route-stop" />
                <strong>{c}</strong>
                {i > 0 ? <span className="route-after num">{money(plan.steps[i - 1].after, c)}</span> : <span className="route-after num">{money(plan.amount, c)}</span>}
              </li>
            ))}
          </ol>
        </div>
      </section>
      <section className="panel">
        <div className="panel-head">
          <h2>Compared with changing directly</h2>
        </div>
        <div className="panel-body compare-bars">
          <div>
            <div className="bar-label">
              <span>Direct {plan.from} → {plan.to}</span>
              <span className="num">{money(plan.direct, plan.to)}</span>
            </div>
            <div className="bar"><span style={{ width: `${(plan.direct / max) * 100}%` }} className="bar-direct" /></div>
          </div>
          <div>
            <div className="bar-label">
              <span>Best route ({plan.steps.length} {plan.steps.length === 1 ? "step" : "steps"})</span>
              <span className="num">{money(plan.received, plan.to)}</span>
            </div>
            <div className="bar"><span style={{ width: `${(plan.received / max) * 100}%` }} className="bar-best" /></div>
          </div>
          <div className="saving">
            <span>You save</span>
            <strong className="num">{better ? money(saving, plan.to) : "nothing: direct is already best"}</strong>
            {better ? <span className="gain">+{plan.savingPercent.toFixed(2)}%</span> : null}
          </div>
        </div>
      </section>
    </div>
  );
}
