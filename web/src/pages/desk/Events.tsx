import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Icon } from "../../components/Icon";
import type { ArbEvent } from "../../engine/types";
import { dateTime, eventId, pathText, pct } from "../../lib/format";
import { market, useMarket } from "../../store/market";

type Period = "hour" | "today" | "week" | "all";
type SortKey = "time" | "profit" | "legs";

const PERIODS: { value: Period; label: string }[] = [
  { value: "hour", label: "Last hour" },
  { value: "today", label: "Today" },
  { value: "week", label: "Last 7 days" },
  { value: "all", label: "All" },
];

function since(period: Period): number {
  const now = Date.now();
  if (period === "hour") return now - 3600_000;
  if (period === "week") return now - 7 * 86400_000;
  if (period === "today") {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    return d.getTime();
  }
  return 0;
}

function toCsv(events: ArbEvent[]): string {
  const rows = [["id", "detected_at", "path", "legs", "profit_percent", "fee", "engine"]];
  for (const e of events) {
    rows.push([
      eventId(e.id),
      new Date(e.detectedAt).toISOString(),
      e.path.join(">"),
      String(e.legs.length),
      e.profitPercent.toFixed(6),
      e.fee.toFixed(6),
      e.source,
    ]);
  }
  return rows.map((r) => r.join(",")).join("\n");
}

/** Every stored find, filterable and sortable, the browser twin of the Java History screen. */
export function Events() {
  const history = useMarket((s) => s.history);
  const [period, setPeriod] = useState<Period>("all");
  const [minProfit, setMinProfit] = useState("0");
  const [code, setCode] = useState("");
  const [sort, setSort] = useState<{ key: SortKey; desc: boolean }>({ key: "time", desc: true });
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [confirmText, setConfirmText] = useState("");
  const [confirming, setConfirming] = useState(false);

  const min = Number(minProfit);
  const minInvalid = minProfit !== "" && (!Number.isFinite(min) || min < 0);

  const rows = useMemo(() => {
    const from = since(period);
    const c = code.trim().toUpperCase();
    const filtered = history.filter(
      (e) =>
        e.detectedAt >= from &&
        e.profitPercent >= (minInvalid ? 0 : min || 0) &&
        (c === "" || e.path.some((p) => p.includes(c))),
    );
    const dir = sort.desc ? -1 : 1;
    return filtered.sort((a, b) => {
      if (sort.key === "profit") return (a.profitPercent - b.profitPercent) * dir;
      if (sort.key === "legs") return (a.legs.length - b.legs.length) * dir || (a.detectedAt - b.detectedAt) * -1;
      return (a.detectedAt - b.detectedAt) * dir;
    });
  }, [history, period, min, minInvalid, code, sort]);

  const avg = rows.length ? rows.reduce((a, e) => a + e.profitPercent, 0) / rows.length : 0;
  const best = rows.reduce((a, e) => Math.max(a, e.profitPercent), 0);

  const sortBy = (key: SortKey) => setSort((s) => ({ key, desc: s.key === key ? !s.desc : true }));
  const arrow = (key: SortKey) => (sort.key === key ? (sort.desc ? "↓" : "↑") : "");

  const exportCsv = () => {
    const blob = new Blob([toCsv(rows)], { type: "text/csv" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `arbitrage-events-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  };

  const allSelected = rows.length > 0 && rows.every((r) => selected.has(r.id));

  return (
    <>
      <div className="page-head">
        <h1>Events</h1>
        <div className="actions">
          <button type="button" className="btn btn-sm" onClick={exportCsv} disabled={!rows.length}>
            <Icon name="download" size={14} />
            Export CSV
          </button>
        </div>
        <p>Every loop this browser has seen, newest 500 kept. Click an event to replay how it was found.</p>
      </div>

      <section className="panel">
        <div className="filters" role="search">
          <label className="field">
            <span>Period</span>
            <select className="select" value={period} onChange={(e) => setPeriod(e.target.value as Period)}>
              {PERIODS.map((p) => (
                <option key={p.value} value={p.value}>
                  {p.label}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Min profit %</span>
            <input
              className="input"
              inputMode="decimal"
              value={minProfit}
              aria-invalid={minInvalid}
              onChange={(e) => setMinProfit(e.target.value)}
            />
            {minInvalid ? <span className="field-error">Enter 0 or a positive number</span> : null}
          </label>
          <label className="field">
            <span>Currency</span>
            <input
              className="input"
              placeholder="e.g. INR"
              maxLength={3}
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          </label>
          <button
            type="button"
            className="btn btn-sm btn-quiet"
            onClick={() => {
              setPeriod("all");
              setMinProfit("0");
              setCode("");
            }}
          >
            Reset filters
          </button>
        </div>

        <div className="summary" aria-live="polite">
          <span>
            <strong>{rows.length}</strong> events
          </span>
          <span>
            avg <strong>{rows.length ? pct(avg) : "—"}</strong>
          </span>
          <span>
            best <strong>{rows.length ? pct(best) : "—"}</strong>
          </span>
          <span style={{ marginLeft: "auto" }} className="confirm-row">
            <button
              type="button"
              className="btn btn-sm btn-danger"
              disabled={!selected.size}
              onClick={() => {
                market.deleteEvents([...selected]);
                setSelected(new Set());
              }}
            >
              <Icon name="trash" size={14} />
              Delete selected{selected.size ? ` (${selected.size})` : ""}
            </button>
            {confirming ? (
              <>
                <label className="visually-hidden" htmlFor="confirm-clear">
                  Type DELETE to clear all events
                </label>
                <input
                  id="confirm-clear"
                  className="input"
                  placeholder="Type DELETE"
                  value={confirmText}
                  onChange={(e) => setConfirmText(e.target.value)}
                  autoFocus
                />
                <button
                  type="button"
                  className="btn btn-sm btn-danger"
                  disabled={confirmText !== "DELETE"}
                  onClick={() => {
                    market.clearHistory();
                    setConfirming(false);
                    setConfirmText("");
                    setSelected(new Set());
                  }}
                >
                  Clear all
                </button>
                <button type="button" className="btn btn-sm btn-quiet" onClick={() => setConfirming(false)}>
                  Cancel
                </button>
              </>
            ) : (
              <button type="button" className="btn btn-sm btn-quiet" disabled={!history.length} onClick={() => setConfirming(true)}>
                Clear all…
              </button>
            )}
          </span>
        </div>

        {rows.length ? (
          <div className="table-wrap" style={{ maxHeight: "calc(100dvh - 330px)", minHeight: 240 }}>
            <table className="data">
              <thead>
                <tr>
                  <th scope="col">
                    <input
                      type="checkbox"
                      aria-label="Select all shown"
                      checked={allSelected}
                      onChange={() => setSelected(allSelected ? new Set() : new Set(rows.map((r) => r.id)))}
                    />
                  </th>
                  <th scope="col">Event</th>
                  <th scope="col" className="col-opt" aria-sort={sort.key === "time" ? (sort.desc ? "descending" : "ascending") : "none"}>
                    <button type="button" onClick={() => sortBy("time")}>Detected {arrow("time")}</button>
                  </th>
                  <th scope="col" className="r" aria-sort={sort.key === "profit" ? (sort.desc ? "descending" : "ascending") : "none"}>
                    <button type="button" onClick={() => sortBy("profit")}>Profit {arrow("profit")}</button>
                  </th>
                  <th scope="col">Path</th>
                  <th scope="col" className="r col-opt" aria-sort={sort.key === "legs" ? (sort.desc ? "descending" : "ascending") : "none"}>
                    <button type="button" onClick={() => sortBy("legs")}>Legs {arrow("legs")}</button>
                  </th>
                  <th scope="col" className="col-opt">Engine</th>
                  <th scope="col">
                    <span className="visually-hidden">Replay</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {rows.map((e) => (
                  <tr key={e.id}>
                    <td>
                      <input
                        type="checkbox"
                        aria-label={`Select ${eventId(e.id)}`}
                        checked={selected.has(e.id)}
                        onChange={() => {
                          const next = new Set(selected);
                          if (next.has(e.id)) next.delete(e.id);
                          else next.add(e.id);
                          setSelected(next);
                        }}
                      />
                    </td>
                    <td className="num">
                      <Link to={`/desk/replay/${e.id}`}>{eventId(e.id)}</Link>
                    </td>
                    <td className="num col-opt">{dateTime(e.detectedAt)}</td>
                    <td className="num r gain">{pct(e.profitPercent)}</td>
                    <td className="num">{pathText(e.path)}</td>
                    <td className="num r col-opt">{e.legs.length}</td>
                    <td className="col-opt">{e.source === "java" ? "Java" : "Browser"}</td>
                    <td className="r">
                      <Link className="btn btn-sm btn-quiet" to={`/desk/replay/${e.id}`} aria-label={`Replay ${eventId(e.id)}`}>
                        <Icon name="replay" size={16} />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty">
            <h3>{history.length ? "No events match these filters" : "No events stored yet"}</h3>
            <p>
              {history.length
                ? "Widen the period or lower the minimum profit."
                : "Leave the event display running for a few seconds; every loop it finds is kept here."}
            </p>
          </div>
        )}
      </section>
    </>
  );
}
