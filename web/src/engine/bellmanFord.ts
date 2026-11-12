import type { Edge, Snapshot } from "./types";

const EPS = 1e-12;

/** Same rule as GraphBuilder.java: every ordered pair becomes an edge with weight -log(rate x (1 - fee)). */
export function buildEdges(snapshot: Snapshot, fee: number): Edge[] {
  const n = snapshot.codes.length;
  const edges: Edge[] = [];
  for (let from = 0; from < n; from++) {
    for (let to = 0; to < n; to++) {
      if (from !== to) {
        edges.push({ from, to, weight: -Math.log(snapshot.rates[from][to] * (1 - fee)) });
      }
    }
  }
  return edges;
}

/** Same algorithm as BellmanFordDetector.java. Returns the cycle as indexes, first = last, or []. */
export function findCycle(edges: Edge[], n: number): number[] {
  return traceBellmanFord(edges, n, false).cycle;
}

export interface Relaxation {
  from: number;
  to: number;
  before: number;
  after: number;
}

export interface Round {
  round: number;
  relaxations: Relaxation[];
  dist: number[];
  parent: number[];
}

export interface Trace {
  n: number;
  rounds: Round[];
  /** True when round n still relaxed an edge: a negative cycle exists. */
  hasCycle: boolean;
  /** The node changed last in the check round; the walk back starts here. */
  lastUpdated: number;
  /** The n parent steps taken to land inside the cycle. */
  walkBack: number[];
  cycle: number[];
}

/**
 * Bellman-Ford from a virtual source (every distance starts at 0), recording every round
 * so the replay can show exactly which edges relaxed and how the cycle was extracted.
 */
export function traceBellmanFord(edges: Edge[], n: number, record = true): Trace {
  const dist = new Array<number>(n).fill(0);
  const parent = new Array<number>(n).fill(-1);
  const rounds: Round[] = [];
  let lastUpdated = -1;
  if (record) rounds.push({ round: 0, relaxations: [], dist: [...dist], parent: [...parent] });
  for (let round = 1; round <= n; round++) {
    lastUpdated = -1;
    const relaxations: Relaxation[] = [];
    for (const e of edges) {
      const candidate = dist[e.from] + e.weight;
      if (candidate < dist[e.to] - EPS) {
        if (record) relaxations.push({ from: e.from, to: e.to, before: dist[e.to], after: candidate });
        dist[e.to] = candidate;
        parent[e.to] = e.from;
        lastUpdated = e.to;
      }
    }
    if (record) rounds.push({ round, relaxations, dist: [...dist], parent: [...parent] });
    if (lastUpdated === -1) {
      return { n, rounds, hasCycle: false, lastUpdated: -1, walkBack: [], cycle: [] };
    }
  }
  const walkBack = [lastUpdated];
  let v = lastUpdated;
  for (let i = 0; i < n; i++) {
    v = parent[v];
    walkBack.push(v);
  }
  const cycle: number[] = [];
  let current = v;
  do {
    cycle.push(current);
    current = parent[current];
  } while (current !== v);
  cycle.push(v);
  cycle.reverse();
  return { n, rounds, hasCycle: true, lastUpdated, walkBack, cycle };
}

/** The slow, obvious cross-check from TriangleValidator.java: every 3-currency loop. */
export function profitableTriangles(snapshot: Snapshot, fee: number): number[][] {
  const n = snapshot.codes.length;
  const r = snapshot.rates;
  const keep = (1 - fee) ** 3;
  const found: number[][] = [];
  for (let a = 0; a < n; a++) {
    for (let b = a + 1; b < n; b++) {
      for (let c = a + 1; c < n; c++) {
        if (b === c) continue;
        if (r[a][b] * r[b][c] * r[c][a] * keep > 1 + EPS) found.push([a, b, c, a]);
      }
    }
  }
  return found;
}
