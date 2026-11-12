import { useState } from "react";
import { Icon } from "../../components/Icon";
import { BASE_CODE, DEFAULT_CURRENCIES, type Currency } from "../../engine/types";
import { validateCurrency, type FieldErrors } from "../../lib/validate";
import { market, useMarket } from "../../store/market";

const MAX_ACTIVE = 20;

/** Rough USD prices for quick adds; the ECB button replaces them with published reference rates. */
const PRESETS: Currency[] = [
  { code: "AUD", name: "Australian Dollar", usdPrice: 0.66, active: true },
  { code: "CAD", name: "Canadian Dollar", usdPrice: 0.73, active: true },
  { code: "CHF", name: "Swiss Franc", usdPrice: 1.13, active: true },
  { code: "CNY", name: "Chinese Yuan", usdPrice: 0.14, active: true },
  { code: "SGD", name: "Singapore Dollar", usdPrice: 0.75, active: true },
  { code: "SEK", name: "Swedish Krona", usdPrice: 0.095, active: true },
  { code: "ZAR", name: "South African Rand", usdPrice: 0.055, active: true },
];

const EMPTY = { code: "", name: "", price: "", active: true };

export function Detector() {
  const currencies = useMarket((s) => s.currencies);
  const [form, setForm] = useState(EMPTY);
  const [editing, setEditing] = useState<string | null>(null);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [message, setMessage] = useState<{ kind: "info" | "error"; text: string } | null>(null);
  const [loadingEcb, setLoadingEcb] = useState(false);

  const activeCount = currencies.filter((c) => c.active).length;

  const save = () => {
    const { currency, errors: errs } = validateCurrency(form, currencies, editing);
    setErrors(errs);
    if (!currency) return;
    const becomesActive = currency.active && !currencies.find((c) => c.code === currency.code)?.active;
    if (becomesActive && activeCount >= MAX_ACTIVE) {
      setErrors({ code: `At most ${MAX_ACTIVE} active currencies` });
      return;
    }
    const next = editing ? currencies.map((c) => (c.code === editing ? currency : c)) : [...currencies, currency];
    market.setCurrencies(next, editing !== null && currencies.find((c) => c.code === editing)?.usdPrice !== currency.usdPrice);
    setMessage({ kind: "info", text: editing ? `${currency.code} updated. The market uses it from the next tick.` : `${currency.code} added to the market.` });
    setForm(EMPTY);
    setEditing(null);
  };

  const edit = (c: Currency) => {
    setEditing(c.code);
    setForm({ code: c.code, name: c.name, price: String(c.usdPrice), active: c.active });
    setErrors({});
    setMessage(null);
  };

  const toggle = (c: Currency) => {
    if (c.code === BASE_CODE) return;
    if (!c.active && activeCount >= MAX_ACTIVE) {
      setMessage({ kind: "error", text: `At most ${MAX_ACTIVE} currencies can be active.` });
      return;
    }
    market.setCurrencies(currencies.map((x) => (x.code === c.code ? { ...x, active: !x.active } : x)));
  };

  const remove = (c: Currency) => {
    if (c.code === BASE_CODE) return;
    market.setCurrencies(currencies.filter((x) => x.code !== c.code));
    if (editing === c.code) {
      setEditing(null);
      setForm(EMPTY);
    }
    setMessage({ kind: "info", text: `${c.code} removed. Stored events that use it stay replayable.` });
  };

  const loadEcb = async () => {
    setLoadingEcb(true);
    setMessage(null);
    try {
      const codes = currencies.filter((c) => c.code !== BASE_CODE).map((c) => c.code);
      const response = await fetch(`https://api.frankfurter.app/latest?from=USD&to=${codes.join(",")}`);
      if (!response.ok) throw new Error(String(response.status));
      const data = (await response.json()) as { date: string; rates: Record<string, number> };
      const missing: string[] = [];
      const next = currencies.map((c) => {
        if (c.code === BASE_CODE) return c;
        const perUsd = data.rates[c.code];
        if (!perUsd) {
          missing.push(c.code);
          return c;
        }
        return { ...c, usdPrice: Number((1 / perUsd).toPrecision(6)) };
      });
      market.setCurrencies(next, true);
      setMessage({
        kind: "info",
        text: `Prices set from the European Central Bank reference rates of ${data.date}.${missing.length ? ` Not published for: ${missing.join(", ")}.` : ""} The simulator walks on from here.`,
      });
    } catch {
      setMessage({ kind: "error", text: "Could not reach the reference-rate service. Check the connection and try again; the current prices are unchanged." });
    } finally {
      setLoadingEcb(false);
    }
  };

  const available = PRESETS.filter((p) => !currencies.some((c) => c.code === p.code));

  return (
    <>
      <div className="page-head">
        <h1>Currencies</h1>
        <div className="actions">
          <button type="button" className="btn btn-sm" onClick={() => void loadEcb()} disabled={loadingEcb}>
            <Icon name="globe" size={14} />
            {loadingEcb ? "Loading…" : "Use ECB reference prices"}
          </button>
          <button
            type="button"
            className="btn btn-sm btn-quiet"
            onClick={() => {
              market.setCurrencies(DEFAULT_CURRENCIES, true);
              setMessage({ kind: "info", text: "Back to the five default currencies and prices." });
            }}
          >
            Reset to defaults
          </button>
        </div>
        <p>
          The stations on the map. Prices are in US dollars; the simulator derives every cross rate from them. USD is
          the base and cannot be removed. {activeCount} of at most {MAX_ACTIVE} active.
        </p>
      </div>

      {message ? (
        <p className={`notice ${message.kind === "info" ? "notice-info" : ""}`} role="status" style={{ marginBottom: 20 }}>
          {message.text}
        </p>
      ) : null}

      <div className="two-col">
        <section className="panel" aria-labelledby="list-title">
          <div className="panel-head">
            <h2 id="list-title">Market list</h2>
          </div>
          <div className="table-wrap">
            <table className="data">
              <thead>
                <tr>
                  <th scope="col">Code</th>
                  <th scope="col">Name</th>
                  <th scope="col" className="r">
                    USD price
                  </th>
                  <th scope="col">Active</th>
                  <th scope="col">
                    <span className="visually-hidden">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {currencies.map((c) => (
                  <tr key={c.code} style={{ opacity: c.active ? 1 : 0.55 }}>
                    <td className="num" style={{ fontWeight: 700 }}>
                      {c.code}
                    </td>
                    <td>{c.name}</td>
                    <td className="num r">{c.usdPrice}</td>
                    <td>
                      <input
                        type="checkbox"
                        checked={c.active}
                        disabled={c.code === BASE_CODE}
                        aria-label={`${c.code} active`}
                        onChange={() => toggle(c)}
                      />
                    </td>
                    <td className="r" style={{ whiteSpace: "nowrap" }}>
                      <button type="button" className="btn btn-sm btn-quiet" onClick={() => edit(c)}>
                        Edit
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-quiet btn-danger"
                        onClick={() => remove(c)}
                        disabled={c.code === BASE_CODE}
                        aria-label={`Remove ${c.code}`}
                      >
                        <Icon name="trash" size={14} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {activeCount < 3 ? (
            <p className="notice" style={{ margin: 16 }}>
              A loop needs at least 3 active currencies. Activate another one to resume detection.
            </p>
          ) : null}
        </section>

        <div className="stack">
          <section className="panel" aria-labelledby="form-title">
            <div className="panel-head">
              <h2 id="form-title">{editing ? `Edit ${editing}` : "Add a currency"}</h2>
            </div>
            <form
              className="panel-body"
              onSubmit={(e) => {
                e.preventDefault();
                save();
              }}
              noValidate
            >
              <div className="form-grid">
                <label className="field">
                  <span>Code</span>
                  <input
                    className="input"
                    value={form.code}
                    maxLength={3}
                    readOnly={editing !== null}
                    aria-invalid={Boolean(errors.code)}
                    onChange={(e) => setForm({ ...form, code: e.target.value })}
                    placeholder="AUD"
                  />
                  {errors.code ? <span className="field-error">{errors.code}</span> : <span className="field-help">3 uppercase letters</span>}
                </label>
                <label className="field">
                  <span>Name</span>
                  <input
                    className="input"
                    value={form.name}
                    aria-invalid={Boolean(errors.name)}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="Australian Dollar"
                  />
                  {errors.name ? <span className="field-error">{errors.name}</span> : <span className="field-help">2 to 40 characters</span>}
                </label>
                <label className="field">
                  <span>USD price</span>
                  <input
                    className="input"
                    value={form.price}
                    inputMode="decimal"
                    aria-invalid={Boolean(errors.price)}
                    onChange={(e) => setForm({ ...form, price: e.target.value })}
                    placeholder="0.66"
                  />
                  {errors.price ? <span className="field-error">{errors.price}</span> : <span className="field-help">Price of one unit in US dollars</span>}
                </label>
              </div>
              <label className="confirm-row" style={{ marginTop: 14 }}>
                <input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} />
                Active in the market
              </label>
              <div className="form-actions">
                <button type="submit" className="btn btn-primary btn-sm">
                  <Icon name={editing ? "detector" : "plus"} size={14} />
                  {editing ? "Update" : "Add"}
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-quiet"
                  onClick={() => {
                    setForm(EMPTY);
                    setEditing(null);
                    setErrors({});
                  }}
                >
                  Clear
                </button>
              </div>
            </form>
          </section>

          {available.length ? (
            <section className="panel" aria-labelledby="preset-title">
              <div className="panel-head">
                <h2 id="preset-title">Quick add</h2>
              </div>
              <div className="panel-body">
                <div className="presets">
                  {available.map((p) => (
                    <button
                      key={p.code}
                      type="button"
                      className="btn btn-sm"
                      disabled={activeCount >= MAX_ACTIVE}
                      onClick={() => {
                        market.setCurrencies([...currencies, p]);
                        setMessage({ kind: "info", text: `${p.code} added at an approximate price of ${p.usdPrice} USD.` });
                      }}
                    >
                      <Icon name="plus" size={12} />
                      {p.code}
                    </button>
                  ))}
                </div>
                <p className="field-help" style={{ marginTop: 12 }}>
                  Approximate prices. Use the ECB button above for published reference rates.
                </p>
              </div>
            </section>
          ) : null}
        </div>
      </div>
    </>
  );
}
