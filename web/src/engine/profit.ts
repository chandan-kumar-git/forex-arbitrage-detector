import { BASE_CODE, type Leg, type Snapshot } from "./types";

/** (product of every rate after fee - 1) x 100, like ProfitCalculator.java. */
export function profitPercent(cycle: number[], snapshot: Snapshot, fee: number): number {
  let money = 1;
  for (let i = 0; i < cycle.length - 1; i++) {
    money *= snapshot.rates[cycle[i]][cycle[i + 1]] * (1 - fee);
  }
  return (money - 1) * 100;
}

export function legsOf(cycle: number[], snapshot: Snapshot, fee: number): Leg[] {
  // mid value of one unit of each currency in the base currency (USD, or the first currency):
  // the geometric mean of the sell and the inverted buy rate, so the spread drops out
  const found = snapshot.codes.indexOf(BASE_CODE);
  const base = found >= 0 ? found : 0;
  const value = (k: number) => (k === base ? 1 : Math.sqrt(snapshot.rates[k][base] / snapshot.rates[base][k]));
  const legs: Leg[] = [];
  for (let i = 0; i < cycle.length - 1; i++) {
    const from = cycle[i];
    const to = cycle[i + 1];
    const rate = snapshot.rates[from][to];
    legs.push({
      legNo: i + 1,
      from: snapshot.codes[from],
      to: snapshot.codes[to],
      rate,
      // ln(rate x (1 - fee)) + ln(value(to)) - ln(value(from)): the value terms cancel around the loop
      edge: Math.log(rate * (1 - fee)) + Math.log(value(to)) - Math.log(value(from)),
    });
  }
  return legs;
}

/**
 * How far each rate i->j sits from the cross rate through the base currency, normalised so the
 * typical pair reads 1. The injected error shows up as the pair (and its reverse) far from 1.
 */
export function crossDeviations(snapshot: Snapshot, base: number): number[][] {
  const n = snapshot.codes.length;
  const r = snapshot.rates;
  const raw: number[][] = [];
  const pool: number[] = [];
  for (let i = 0; i < n; i++) {
    raw.push([]);
    for (let j = 0; j < n; j++) {
      const direct = i === j || i === base || j === base || base < 0;
      const value = direct ? NaN : r[i][j] / (r[i][base] * r[base][j]);
      raw[i].push(value);
      if (!Number.isNaN(value)) pool.push(value);
    }
  }
  pool.sort((a, b) => a - b);
  const typical = pool.length ? pool[Math.floor(pool.length / 2)] : 1;
  return raw.map((row) => row.map((v) => (Number.isNaN(v) ? 1 : v / typical)));
}
