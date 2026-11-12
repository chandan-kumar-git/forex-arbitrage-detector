import { buildEdges, traceBellmanFord, type Trace } from "../engine/bellmanFord";
import type { ArbEvent } from "../engine/types";
import type { Ghost } from "../components/RingDisplay";

export type PhaseKind = "source" | "round" | "check" | "walk" | "loop" | "none";

export interface Phase {
  kind: PhaseKind;
  /** Short cue name, e.g. "Round 3". */
  name: string;
  /** What happened, in plain words. */
  text: string;
  dist: number[] | null;
  ghosts: Ghost[];
  spokes: boolean;
  marked: number[];
  showLoop: boolean;
  relaxed: number;
}

/** Re-runs Bellman-Ford on the event's own snapshot and turns the trace into named, scrubbable cues. */
export function buildPhases(event: ArbEvent): { trace: Trace; phases: Phase[] } {
  const { snapshot, fee } = event;
  const n = snapshot.codes.length;
  const code = (i: number) => snapshot.codes[i];
  const trace = traceBellmanFord(buildEdges(snapshot, fee), n);
  const phases: Phase[] = [];

  phases.push({
    kind: "source",
    name: "Virtual source",
    text: `Every distance starts at 0, as if one hidden node pointed at all ${n} currencies. That lets one run find a loop anywhere in the graph.`,
    dist: trace.rounds[0].dist,
    ghosts: [],
    spokes: true,
    marked: [],
    showLoop: false,
    relaxed: 0,
  });

  for (let r = 1; r < trace.rounds.length; r++) {
    const round = trace.rounds[r];
    const prev = trace.rounds[r - 1];
    const isCheck = r === n;
    const k = round.relaxations.length;
    const ghosts: Ghost[] = [
      ...prev.relaxations.map((x) => ({ from: x.from, to: x.to, age: 1 })),
      ...round.relaxations.map((x) => ({ from: x.from, to: x.to, age: 0 })),
    ];
    let text: string;
    if (k === 0) {
      text = `No edge gives a shorter distance any more. Nothing changed, so there is no negative cycle and the search stops early.`;
    } else if (isCheck) {
      text = `Check round: after ${n - 1} rounds every shortest path is final unless a negative cycle exists. ${k} edge${k === 1 ? "" : "s"} still relaxed, so a loop with total weight below 0 is proven.`;
    } else {
      const best = round.relaxations.reduce((a, b) => (b.after - b.before < a.after - a.before ? b : a));
      text = `${k} edge${k === 1 ? "" : "s"} relaxed: going through them gave a smaller total weight. Biggest drop: ${code(best.from)} → ${code(best.to)}, ${best.before.toFixed(5)} to ${best.after.toFixed(5)}.`;
    }
    phases.push({
      kind: isCheck && k > 0 ? "check" : "round",
      name: isCheck ? `Round ${r} · check` : `Round ${r}`,
      text,
      dist: round.dist,
      ghosts,
      spokes: false,
      marked: round.relaxations.map((x) => x.to),
      showLoop: false,
      relaxed: k,
    });
  }

  if (trace.hasCycle) {
    const walk = trace.walkBack.map(code).join(" ← ");
    phases.push({
      kind: "walk",
      name: "Walk back",
      text: `Start at ${code(trace.lastUpdated)}, the last currency that changed, and follow parent pointers ${n} times. After n steps you are certainly inside the loop: ${walk}.`,
      dist: trace.rounds[trace.rounds.length - 1].dist,
      ghosts: [],
      spokes: false,
      marked: trace.walkBack,
      showLoop: false,
      relaxed: 0,
    });
    phases.push({
      kind: "loop",
      name: "Loop extracted",
      text: `Follow parents once around and reverse: ${trace.cycle.map(code).join(" → ")}. Multiplying its rates after fees gives ${event.profitPercent >= 0 ? "+" : ""}${event.profitPercent.toFixed(3)} %.`,
      dist: null,
      ghosts: [],
      spokes: false,
      marked: [],
      showLoop: true,
      relaxed: 0,
    });
  } else {
    phases.push({
      kind: "none",
      name: "No cycle",
      text: "This snapshot has no negative cycle under the current fee.",
      dist: null,
      ghosts: [],
      spokes: false,
      marked: [],
      showLoop: false,
      relaxed: 0,
    });
  }
  return { trace, phases };
}
