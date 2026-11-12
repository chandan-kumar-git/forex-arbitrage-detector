import { buildEdges, findCycle } from "./bellmanFord";
import { legsOf, profitPercent } from "./profit";
import type { Leg, Snapshot } from "./types";

export interface Detection {
  cycle: number[];
  path: string[];
  profitPercent: number;
  legs: Leg[];
}

/** One detection pass, the same steps as ArbitrageService.detect in Java. */
export function detect(snapshot: Snapshot, fee: number): Detection | null {
  if (snapshot.codes.length < 3) return null;
  const cycle = findCycle(buildEdges(snapshot, fee), snapshot.codes.length);
  if (cycle.length === 0) return null;
  const profit = profitPercent(cycle, snapshot, fee);
  if (profit <= 0) return null;
  return {
    cycle,
    path: cycle.map((i) => snapshot.codes[i]),
    profitPercent: profit,
    legs: legsOf(cycle, snapshot, fee),
  };
}

/**
 * Time of one detection (graph build + Bellman-Ford) in microseconds. Browsers round
 * performance.now() to 5-100 µs, far coarser than one run, so the detection is repeated
 * until at least half a millisecond has passed and the total is divided by the runs.
 */
export function timeDetection(snapshot: Snapshot, fee: number): number {
  const n = snapshot.codes.length;
  findCycle(buildEdges(snapshot, fee), n); // one untimed warm-up run
  let runs = 0;
  const start = performance.now();
  let elapsed = 0;
  do {
    findCycle(buildEdges(snapshot, fee), n);
    runs++;
    elapsed = performance.now() - start;
  } while (elapsed < 0.5 && runs < 5000);
  return (elapsed * 1000) / runs;
}
