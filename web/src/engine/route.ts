import type { Snapshot } from "./types";

/**
 * Real-life use: the cheapest way to change money, the same model as RouteService.java.
 * Banks quote rare pairs with a wide spread, so changing through USD is often cheaper.
 */
const MAJORS = new Set(["USD", "EUR", "GBP", "JPY", "CHF"]);
export const MAJOR_SPREAD = 0.0005;
export const MINOR_SPREAD = 0.004;
export const CROSS_PENALTY = 0.008;
export const TRANSFER_FEE = 0.001;

export function bankSpread(from: string, to: string): number {
  const s = (c: string) => (MAJORS.has(c) ? MAJOR_SPREAD : MINOR_SPREAD);
  return s(from) + s(to) + (from !== "USD" && to !== "USD" ? CROSS_PENALTY : 0);
}

export interface RoutePlan {
  from: string;
  to: string;
  amount: number;
  path: string[];
  steps: { from: string; to: string; rate: number; after: number }[];
  received: number;
  direct: number;
  saving: number;
  savingPercent: number;
}

/** What a bank pays for every pair, from each currency's mid value in USD. */
export function bankRates(snapshot: Snapshot): number[][] {
  const { codes, rates } = snapshot;
  const usd = codes.indexOf("USD");
  const value = codes.map((_, k) => (usd < 0 || k === usd ? 1 : Math.sqrt(rates[k][usd] / rates[usd][k])));
  return codes.map((a, i) =>
    codes.map((b, j) => (i === j ? 1 : (value[i] / value[j]) * (1 - bankSpread(a, b)) * (1 - TRANSFER_FEE))),
  );
}

/** Shortest path on -log(rate): the smallest sum of -log is the biggest product of rates. */
export function bestPath(rate: number[][], src: number, dst: number): number[] {
  const n = rate.length;
  const dist = new Array<number>(n).fill(Infinity);
  const parent = new Array<number>(n).fill(-1);
  dist[src] = 0;
  for (let round = 1; round < n; round++) {
    for (let i = 0; i < n; i++) {
      for (let j = 0; j < n; j++) {
        const w = -Math.log(rate[i][j]);
        if (i !== j && dist[i] + w < dist[j] - 1e-12) {
          dist[j] = dist[i] + w;
          parent[j] = i;
        }
      }
    }
  }
  const path: number[] = [];
  const seen = new Set<number>();
  for (let v = dst; v !== -1 && !seen.has(v); v = parent[v]) {
    seen.add(v);
    path.push(v);
    if (v === src) break;
  }
  if (path[path.length - 1] !== src) return [src, dst];
  return path.reverse();
}

export function planRoute(snapshot: Snapshot, from: string, to: string, amount: number): RoutePlan | null {
  const src = snapshot.codes.indexOf(from);
  const dst = snapshot.codes.indexOf(to);
  if (src < 0 || dst < 0 || src === dst || !(amount > 0)) return null;
  const bank = bankRates(snapshot);
  const idx = bestPath(bank, src, dst);
  let money = amount;
  const steps = [];
  for (let k = 1; k < idx.length; k++) {
    const r = bank[idx[k - 1]][idx[k]];
    money *= r;
    steps.push({ from: snapshot.codes[idx[k - 1]], to: snapshot.codes[idx[k]], rate: r, after: money });
  }
  const direct = amount * bank[src][dst];
  return {
    from,
    to,
    amount,
    path: idx.map((i) => snapshot.codes[i]),
    steps,
    received: money,
    direct,
    saving: money - direct,
    savingPercent: ((money - direct) / direct) * 100,
  };
}
