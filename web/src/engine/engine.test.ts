import { describe, expect, it } from "vitest";
import { buildEdges, findCycle, profitableTriangles, traceBellmanFord } from "./bellmanFord";
import { legsOf, profitPercent } from "./profit";
import { planRoute } from "./route";
import { Simulator, seeded, snapshotFromPrices } from "./simulator";
import { DEFAULT_CURRENCIES, DEFAULT_SETTINGS, type Snapshot } from "./types";

// Same data as TestSnapshots.java: USD = 0, EUR = 1, GBP = 2, INR = 3.
const FEE = 0.0005;
const SPREAD = 0.001;
const CODES = ["USD", "EUR", "GBP", "INR"];
const PRICES = [1.0, 1.08, 1.27, 0.012];

function reference(): Snapshot {
  const s = snapshotFromPrices(CODES, PRICES, SPREAD);
  s.rates[1][2] *= 1.008;
  s.rates[2][1] /= 1.008;
  return s;
}

describe("profit", () => {
  it("gives the 0.3472 % reference case of the Java tests", () => {
    expect(profitPercent([3, 1, 2, 3], reference(), FEE)).toBeCloseTo(0.3472, 3);
  });

  it("loses the fees in a fair market", () => {
    expect(profitPercent([3, 1, 2, 3], snapshotFromPrices(CODES, PRICES, 0), FEE)).toBeLessThan(0);
  });

  it("lists legs whose log-returns add up to the profit", () => {
    const legs = legsOf([3, 1, 2, 3], reference(), FEE);
    expect(legs.map((l) => `${l.from}>${l.to}`)).toEqual(["INR>EUR", "EUR>GBP", "GBP>INR"]);
    const total = legs.reduce((sum, l) => sum + l.edge, 0);
    expect((Math.exp(total) - 1) * 100).toBeCloseTo(0.3472, 3);
  });

  it("puts the whole edge on the mispriced leg and only spread + fee on the fair ones", () => {
    const [toEur, eurGbp, gbpInr] = legsOf([3, 1, 2, 3], reference(), FEE);
    const fair = Math.log((1 - SPREAD) * (1 - FEE));
    expect(toEur.edge).toBeCloseTo(fair, 9);
    expect(gbpInr.edge).toBeCloseTo(fair, 9);
    expect(eurGbp.edge).toBeCloseTo(Math.log(1.008) + fair, 9);
  });

  it("charges legs through USD the same spread and fee as any other fair leg", () => {
    const fairMarket = snapshotFromPrices(CODES, PRICES, SPREAD);
    const legs = legsOf([0, 1, 3, 0], fairMarket, FEE);
    for (const leg of legs) expect(leg.edge).toBeCloseTo(Math.log((1 - SPREAD) * (1 - FEE)), 9);
  });
});

describe("bellman-ford", () => {
  it("finds a profitable cycle in the reference market", () => {
    const s = reference();
    const cycle = findCycle(buildEdges(s, FEE), 4);
    expect(cycle.length).toBeGreaterThanOrEqual(4);
    expect(cycle[0]).toBe(cycle[cycle.length - 1]);
    expect(profitPercent(cycle, s, FEE)).toBeGreaterThan(0);
  });

  it("finds nothing in a fair market", () => {
    expect(findCycle(buildEdges(snapshotFromPrices(CODES, PRICES, 0), FEE), 4)).toEqual([]);
  });

  it("builds one edge for every ordered pair", () => {
    expect(buildEdges(reference(), FEE)).toHaveLength(12);
  });

  it("records rounds, a walk back of n steps, and stops early when nothing relaxes", () => {
    const withCycle = traceBellmanFord(buildEdges(reference(), FEE), 4);
    expect(withCycle.hasCycle).toBe(true);
    expect(withCycle.rounds).toHaveLength(5); // round 0 + n rounds
    expect(withCycle.walkBack).toHaveLength(5);
    const fair = traceBellmanFord(buildEdges(snapshotFromPrices(CODES, PRICES, 0.001), FEE), 4);
    expect(fair.hasCycle).toBe(false);
    expect(fair.rounds.length).toBeLessThan(5);
  });

  it("agrees with the brute-force triangle check on 2000 simulated markets", () => {
    const sim = new Simulator(DEFAULT_CURRENCIES, seeded(7));
    for (let i = 0; i < 2000; i++) {
      const s = sim.next({ ...DEFAULT_SETTINGS, injectChance: 0.5 });
      const cycle = findCycle(buildEdges(s, FEE), s.codes.length);
      if (cycle.length === 0) expect(profitableTriangles(s, FEE)).toEqual([]);
    }
  });
});

describe("money route planner", () => {
  it("changes INR to JPY through USD because the direct cross is expensive", () => {
    const s = snapshotFromPrices(["USD", "INR", "JPY", "EUR"], [1, 0.012, 0.0067, 1.08], SPREAD);
    const plan = planRoute(s, "INR", "JPY", 50000)!;
    expect(plan.path).toEqual(["INR", "USD", "JPY"]);
    expect(plan.saving).toBeGreaterThan(0);
    expect(plan.received).toBeCloseTo(plan.steps[plan.steps.length - 1].after, 6);
  });

  it("goes direct when one side is already USD", () => {
    const s = snapshotFromPrices(["USD", "INR", "JPY"], [1, 0.012, 0.0067], SPREAD);
    expect(planRoute(s, "USD", "JPY", 100)!.path).toEqual(["USD", "JPY"]);
  });
});

describe("simulator", () => {
  it("keeps a rate of 1 from a currency to itself and never wins on a round trip", () => {
    const sim = new Simulator(DEFAULT_CURRENCIES, seeded(1));
    for (let k = 0; k < 200; k++) {
      const s = sim.next(DEFAULT_SETTINGS);
      for (let i = 0; i < s.codes.length; i++) {
        expect(s.rates[i][i]).toBe(1);
        for (let j = 0; j < s.codes.length; j++) {
          if (i !== j) expect(s.rates[i][j] * s.rates[j][i]).toBeLessThan(1);
        }
      }
    }
  });

  it("only uses active currencies", () => {
    const list = DEFAULT_CURRENCIES.map((c) => (c.code === "JPY" ? { ...c, active: false } : c));
    expect(new Simulator(list, seeded(2)).next(DEFAULT_SETTINGS).codes).not.toContain("JPY");
  });
});
