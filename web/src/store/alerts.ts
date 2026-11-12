import { useSyncExternalStore } from "react";
import type { ArbEvent } from "../engine/types";
import { load, save } from "./storage";

/** "Tell me when a loop pays at least X %": an in-app toast, and a browser notification if allowed. */
export interface AlertPrefs {
  enabled: boolean;
  minProfit: number;
  browser: boolean;
}

export interface Toast {
  id: number;
  title: string;
  body: string;
  eventId: number;
}

const DEFAULT: AlertPrefs = { enabled: true, minProfit: 0.6, browser: false };

class Alerts {
  prefs: AlertPrefs = { ...DEFAULT, ...load<Partial<AlertPrefs>>("alerts", {}) };
  toasts: Toast[] = [];
  fired = 0;
  private listeners = new Set<() => void>();
  private nextId = 1;
  private version = 0;

  subscribe = (l: () => void) => {
    this.listeners.add(l);
    return () => {
      this.listeners.delete(l);
    };
  };

  getVersion = () => this.version;

  private emit() {
    this.version++;
    this.listeners.forEach((l) => l());
  }

  setPrefs(patch: Partial<AlertPrefs>) {
    this.prefs = { ...this.prefs, ...patch };
    save("alerts", this.prefs);
    this.emit();
  }

  /** Called for every new event; shows a toast when it matches. */
  check(event: ArbEvent) {
    if (!this.prefs.enabled || event.profitPercent < this.prefs.minProfit) return;
    this.fired++;
    const toast: Toast = {
      id: this.nextId++,
      title: `+${event.profitPercent.toFixed(3)}% loop found`,
      body: event.path.join(" → "),
      eventId: event.id,
    };
    this.toasts = [toast, ...this.toasts].slice(0, 3);
    this.emit();
    window.setTimeout(() => this.dismiss(toast.id), 6000);
    if (this.prefs.browser && "Notification" in window && Notification.permission === "granted" && document.hidden) {
      new Notification(toast.title, { body: toast.body, tag: "arbitrage" });
    }
  }

  dismiss(id: number) {
    this.toasts = this.toasts.filter((t) => t.id !== id);
    this.emit();
  }
}

export const alerts = new Alerts();

export function useAlerts() {
  useSyncExternalStore(alerts.subscribe, alerts.getVersion);
  return alerts;
}
