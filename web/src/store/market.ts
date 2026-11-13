import { useSyncExternalStore } from "react";
import { detect, timeDetection } from "../engine/detect";
import { Simulator } from "../engine/simulator";
import {
  DEFAULT_CURRENCIES,
  DEFAULT_SETTINGS,
  type ArbEvent,
  type Currency,
  type EngineSource,
  type MarketSettings,
  type Snapshot,
} from "../engine/types";
import { alerts } from "./alerts";
import { load, remove, save } from "./storage";

const HISTORY_LIMIT = 500;
const RECENT_LIMIT = 40;
const TIMING_WINDOW = 60;
const TIMELINE_LIMIT = 180;

export type JavaStatus = "off" | "connecting" | "online" | "offline";

export interface JavaInfo {
  source: string;
  paused: boolean;
  database: boolean;
  fee: number;
  stats: { ticks: number; found: number; bestProfit: number; bestPath: string; avgTickMicros: number };
  recent: { id: number; path: string[]; profitPercent: number; source: string; detectedAt: string }[];
}

export interface MarketState {
  currencies: Currency[];
  settings: MarketSettings;
  paused: boolean;
  source: EngineSource;
  javaUrl: string;
  javaStatus: JavaStatus;
  javaError: string;
  java: JavaInfo | null;
  snapshot: Snapshot | null;
  injected: [number, number] | null;
  /** Session finds, newest first. */
  recent: ArbEvent[];
  /** Persisted finds, newest first, at most 500. */
  history: ArbEvent[];
  ticks: number;
  found: number;
  best: ArbEvent | null;
  avgMicros: number;
  lastMicros: number;
  startedAt: number;
  /** The last 180 ticks (about 3 minutes) for the Time Machine. */
  timeline: TimelinePoint[];
}

export interface TimelinePoint {
  ts: number;
  snapshot: Snapshot;
  eventId: number | null;
  profit: number;
}

interface JavaState extends JavaInfo {
  snapshot: Snapshot | null;
}

/**
 * The single source of truth for the web app. It runs the tick loop (browser simulator,
 * or polling the Java engine's /api/state), runs Bellman-Ford on each snapshot, and keeps
 * the finds. React components subscribe with useMarket().
 */
class Market {
  private state: MarketState;
  private listeners = new Set<() => void>();
  private simulator: Simulator;
  private timer: number | undefined;
  private timings: number[] = [];
  private nextId: number;
  private lastJavaTimestamp = 0;

  constructor() {
    const currencies = load<Currency[]>("currencies", DEFAULT_CURRENCIES);
    const history = load<ArbEvent[]>("history", []);
    this.nextId = load<number>("nextId", (history[0]?.id ?? 0) + 1);
    this.simulator = new Simulator(currencies);
    const source = load<EngineSource>("source", "browser");
    this.state = {
      currencies,
      settings: { ...DEFAULT_SETTINGS, ...load<Partial<MarketSettings>>("settings", {}) },
      paused: false,
      source,
      javaUrl: load<string>("javaUrl", "http://127.0.0.1:8080"),
      javaStatus: source === "java" ? "connecting" : "off",
      javaError: "",
      java: null,
      snapshot: null,
      injected: null,
      recent: [],
      history,
      ticks: 0,
      found: 0,
      best: null,
      avgMicros: 0,
      lastMicros: 0,
      startedAt: Date.now(),
      timeline: [],
    };
  }

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    if (this.listeners.size === 1) this.schedule(0);
    return () => {
      this.listeners.delete(listener);
    };
  };

  getState = () => this.state;

  private set(patch: Partial<MarketState>) {
    this.state = { ...this.state, ...patch };
    this.listeners.forEach((l) => l());
  }

  private schedule(delay: number) {
    window.clearTimeout(this.timer);
    this.timer = window.setTimeout(() => void this.tick(), delay);
  }

  private async tick() {
    const s = this.state;
    try {
      if (s.source === "java") {
        await this.pollJava();
      } else if (!s.paused) {
        const next = this.simulator.next(s.settings);
        const { injected, ...snapshot } = next;
        this.analyse(snapshot, "browser", injected);
      }
    } finally {
      this.schedule(this.state.source === "java" ? 1000 : this.state.settings.tickMs);
    }
  }

  private analyse(snapshot: Snapshot, source: EngineSource, injected: [number, number] | null) {
    const fee = this.state.settings.fee;
    const found = detect(snapshot, fee);
    const micros = timeDetection(snapshot, fee);
    this.timings.push(micros);
    if (this.timings.length > TIMING_WINDOW) this.timings.shift();
    // median, so a cold first tick or a busy moment does not skew the readout
    const sorted = [...this.timings].sort((a, b) => a - b);
    const avgMicros = sorted[Math.floor(sorted.length / 2)];
    const patch: Partial<MarketState> = {
      snapshot,
      injected,
      ticks: this.state.ticks + 1,
      avgMicros,
      lastMicros: micros,
    };
    if (found) {
      const event: ArbEvent = {
        id: this.nextId++,
        path: found.path,
        cycle: found.cycle,
        profitPercent: found.profitPercent,
        fee,
        source,
        detectedAt: snapshot.timestamp,
        legs: found.legs,
        snapshot,
      };
      const history = [event, ...this.state.history].slice(0, HISTORY_LIMIT);
      patch.recent = [event, ...this.state.recent].slice(0, RECENT_LIMIT);
      patch.history = history;
      patch.found = this.state.found + 1;
      if (!this.state.best || event.profitPercent > this.state.best.profitPercent) patch.best = event;
      save("history", history);
      save("nextId", this.nextId);
      alerts.check(event);
    }
    const newEvent = patch.recent ? patch.recent[0] : null;
    const point: TimelinePoint = { ts: snapshot.timestamp, snapshot, eventId: newEvent?.id ?? null, profit: newEvent?.profitPercent ?? 0 };
    patch.timeline = [...this.state.timeline, point].slice(-TIMELINE_LIMIT);
    this.set(patch);
  }

  private async pollJava() {
    const url = this.state.javaUrl.replace(/\/+$/, "");
    try {
      const controller = new AbortController();
      const timeout = window.setTimeout(() => controller.abort(), 2500);
      const response = await fetch(`${url}/api/state`, { signal: controller.signal, cache: "no-store" });
      window.clearTimeout(timeout);
      if (!response.ok) throw new Error(`The Java API answered ${response.status}`);
      const data = (await response.json()) as JavaState;
      const { snapshot, ...info } = data;
      this.set({ javaStatus: "online", javaError: "", java: info, paused: info.paused });
      if (snapshot && snapshot.timestamp !== this.lastJavaTimestamp) {
        this.lastJavaTimestamp = snapshot.timestamp;
        this.analyse(snapshot, "java", null);
      }
    } catch (error) {
      const message =
        error instanceof DOMException && error.name === "AbortError"
          ? "No answer within 2.5 s"
          : "Cannot reach the Java engine";
      this.set({ javaStatus: "offline", javaError: `${message} at ${url}. Start it with run-all.bat, then wait a few seconds.` });
    }
  }

  // ---- actions -------------------------------------------------------------

  togglePause = async () => {
    const paused = !this.state.paused;
    if (this.state.source === "java") {
      try {
        await fetch(`${this.state.javaUrl.replace(/\/+$/, "")}/api/pause?paused=${paused}`, { method: "POST" });
      } catch {
        this.set({ javaError: "Could not reach the Java engine to pause it." });
        return;
      }
    }
    this.set({ paused });
  };

  useSource = (source: EngineSource) => {
    if (source === this.state.source) return;
    save("source", source);
    this.lastJavaTimestamp = 0;
    this.set({ source, javaStatus: source === "java" ? "connecting" : "off", java: null, javaError: "", paused: false });
    this.schedule(0);
  };

  setJavaUrl = (javaUrl: string) => {
    save("javaUrl", javaUrl);
    this.set({ javaUrl });
  };

  updateSettings = (patch: Partial<MarketSettings>) => {
    const settings = { ...this.state.settings, ...patch };
    save("settings", settings);
    this.set({ settings });
  };

  resetSettings = () => {
    save("settings", DEFAULT_SETTINGS);
    this.set({ settings: { ...DEFAULT_SETTINGS } });
  };

  setCurrencies = (currencies: Currency[], resetPrices = false) => {
    save("currencies", currencies);
    if (resetPrices) this.simulator.resetPrices(currencies);
    else this.simulator.setCurrencies(currencies);
    this.set({ currencies });
  };

  deleteEvents = (ids: number[]) => {
    const drop = new Set(ids);
    const history = this.state.history.filter((e) => !drop.has(e.id));
    save("history", history);
    this.set({ history, recent: this.state.recent.filter((e) => !drop.has(e.id)) });
  };

  clearHistory = () => {
    remove("history");
    this.set({ history: [] });
  };

  findEvent = (id: number): ArbEvent | undefined =>
    this.state.recent.find((e) => e.id === id) ?? this.state.history.find((e) => e.id === id);
}

export const market = new Market();

export function useMarket<T>(select: (state: MarketState) => T): T {
  return useSyncExternalStore(market.subscribe, () => select(market.getState()));
}
