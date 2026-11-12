import { BASE_CODE, type Currency, type MarketSettings, type Snapshot } from "./types";

const MIN_ERROR = 0.006;
const MAX_ERROR = 0.012;

/** Box-Muller: a normally distributed random number from two uniform ones. */
function gaussian(random: () => number): number {
  const u = 1 - random();
  const v = random();
  return Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v);
}

/**
 * A pretend forex market, the same rules as SimulatedRateProvider.java: prices random-walk,
 * and with the inject chance one pair is made 0.6 to 1.2 % too good so a loop appears.
 */
export class Simulator {
  private codes: string[] = [];
  private prices: number[] = [];

  constructor(currencies: Currency[], private readonly random: () => number = Math.random) {
    this.setCurrencies(currencies);
  }

  setCurrencies(currencies: Currency[]): void {
    const active = currencies.filter((c) => c.active);
    const old = new Map(this.codes.map((code, i) => [code, this.prices[i]]));
    this.codes = active.map((c) => c.code);
    // keep the walked price of currencies that stay, so editing the list does not reset the market
    this.prices = active.map((c) => old.get(c.code) ?? c.usdPrice);
  }

  /** Forces the listed prices (used after a manual price edit). */
  resetPrices(currencies: Currency[]): void {
    this.codes = [];
    this.setCurrencies(currencies);
  }

  next(settings: MarketSettings): Snapshot & { injected: [number, number] | null } {
    this.move(settings.volatility);
    const rates = this.matrix(settings.spread);
    let injected: [number, number] | null = null;
    if (this.codes.length >= 2 && this.random() < settings.injectChance) {
      injected = this.inject(rates);
    }
    return { codes: [...this.codes], rates, timestamp: Date.now(), injected };
  }

  private move(volatility: number): void {
    for (let i = 0; i < this.prices.length; i++) {
      if (this.codes[i] !== BASE_CODE) {
        this.prices[i] *= 1 + gaussian(this.random) * volatility;
      }
    }
  }

  private matrix(spread: number): number[][] {
    const n = this.prices.length;
    const rates: number[][] = [];
    for (let i = 0; i < n; i++) {
      rates.push([]);
      for (let j = 0; j < n; j++) {
        rates[i].push(i === j ? 1 : (this.prices[i] / this.prices[j]) * (1 - spread));
      }
    }
    return rates;
  }

  private inject(rates: number[][]): [number, number] {
    const n = this.codes.length;
    const from = Math.floor(this.random() * n);
    let to = Math.floor(this.random() * (n - 1));
    if (to >= from) to++;
    const factor = 1 + MIN_ERROR + this.random() * (MAX_ERROR - MIN_ERROR);
    rates[from][to] *= factor;
    rates[to][from] /= factor;
    return [from, to];
  }
}

/** A seeded random number generator (mulberry32), so tests and the benchmark are repeatable. */
export function seeded(seed: number): () => number {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** A consistent market from prices (used by the lab and tests). */
export function snapshotFromPrices(codes: string[], prices: number[], spread: number): Snapshot {
  const rates = prices.map((pi, i) => prices.map((pj, j) => (i === j ? 1 : (pi / pj) * (1 - spread))));
  return { codes: [...codes], rates, timestamp: Date.now() };
}
