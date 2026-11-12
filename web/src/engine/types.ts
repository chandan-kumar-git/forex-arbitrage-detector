/** One currency: 3-letter code, name, price of one unit in US dollars, and whether the market uses it. */
export interface Currency {
  code: string;
  name: string;
  usdPrice: number;
  active: boolean;
}

/** All rates at one moment. rates[i][j] = units of codes[j] you get for one unit of codes[i]. */
export interface Snapshot {
  codes: string[];
  rates: number[][];
  timestamp: number;
}

/** A directed edge of the currency graph with weight -log(rate x (1 - fee)). */
export interface Edge {
  from: number;
  to: number;
  weight: number;
}

/** One conversion step of a loop. */
export interface Leg {
  legNo: number;
  from: string;
  to: string;
  rate: number;
  /**
   * This leg's edge over the fair cross rate through USD, as a log-return after the fee.
   * The fair parts cancel around a loop, so the legs' edges add up exactly to the loop's log-return.
   */
  edge: number;
}

export type EngineSource = "browser" | "java";

/** One detected arbitrage, stored with the snapshot that produced it so it can be replayed. */
export interface ArbEvent {
  id: number;
  path: string[];
  cycle: number[];
  profitPercent: number;
  fee: number;
  source: EngineSource;
  detectedAt: number;
  legs: Leg[];
  snapshot: Snapshot;
}

export interface MarketSettings {
  fee: number;
  spread: number;
  injectChance: number;
  volatility: number;
  tickMs: number;
}

export const DEFAULT_SETTINGS: MarketSettings = {
  fee: 0.0005,
  spread: 0.001,
  injectChance: 0.3,
  volatility: 0.0005,
  tickMs: 1000,
};

export const BASE_CODE = "USD";

export const DEFAULT_CURRENCIES: Currency[] = [
  { code: "USD", name: "US Dollar", usdPrice: 1.0, active: true },
  { code: "EUR", name: "Euro", usdPrice: 1.08, active: true },
  { code: "GBP", name: "British Pound", usdPrice: 1.27, active: true },
  { code: "INR", name: "Indian Rupee", usdPrice: 0.012, active: true },
  { code: "JPY", name: "Japanese Yen", usdPrice: 0.0067, active: true },
];
