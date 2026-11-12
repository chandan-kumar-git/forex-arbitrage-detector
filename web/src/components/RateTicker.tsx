import { useEffect, useRef, useState } from "react";
import type { Snapshot } from "../engine/types";
import { rate } from "../lib/format";
import "./RateTicker.css";

interface Quote {
  pair: string;
  value: number;
  move: number;
}

/** A slowly scrolling strip of live rates, each marked up or down against the previous tick. */
export function RateTicker({ snapshot }: { snapshot: Snapshot | null }) {
  const previous = useRef<Map<string, number>>(new Map());
  const [quotes, setQuotes] = useState<Quote[]>([]);

  useEffect(() => {
    if (!snapshot) return;
    const usd = snapshot.codes.indexOf("USD");
    const next: Quote[] = [];
    snapshot.codes.forEach((code, i) => {
      snapshot.codes.forEach((other, j) => {
        // show each currency against USD and EUR, the way exchange boards do
        if (i === j || !(other === "USD" || (other === "EUR" && code !== "USD"))) return;
        if (usd < 0 && other === "USD") return;
        const pair = `${code}/${other}`;
        const value = snapshot.rates[i][j];
        const before = previous.current.get(pair);
        next.push({ pair, value, move: before === undefined ? 0 : value - before });
        previous.current.set(pair, value);
      });
    });
    setQuotes(next);
  }, [snapshot]);

  if (!quotes.length) return null;
  const row = quotes.map((q) => (
    <span key={q.pair} className="tick">
      <span className="tick-pair">{q.pair}</span>
      <span className="tick-value num">{rate(q.value)}</span>
      <span className={`tick-move ${q.move > 0 ? "up" : q.move < 0 ? "down" : ""}`} aria-hidden="true">
        {q.move > 0 ? "▲" : q.move < 0 ? "▼" : "•"}
      </span>
    </span>
  ));
  return (
    <div className="ticker" aria-label="Live simulated exchange rates">
      <div className="ticker-track">
        <div className="ticker-row">{row}</div>
        <div className="ticker-row" aria-hidden="true">
          {row}
        </div>
      </div>
    </div>
  );
}
