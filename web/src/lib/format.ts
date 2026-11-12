export const pct = (value: number, digits = 3) => `${value >= 0 ? "+" : "−"}${Math.abs(value).toFixed(digits)}%`;

export function rate(value: number): string {
  if (value >= 1000) return value.toFixed(2);
  if (value >= 1) return value.toFixed(5);
  return value.toPrecision(6);
}

export function micros(us: number): string {
  if (!Number.isFinite(us) || us <= 0) return "—";
  if (us >= 1000) return `${(us / 1000).toFixed(2)} ms`;
  if (us >= 100) return `${us.toFixed(0)} µs`;
  return `${us.toFixed(1)} µs`;
}

export const clock = (ts: number) =>
  new Date(ts).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit", second: "2-digit" });

export const dateTime = (ts: number) =>
  new Date(ts).toLocaleString("en-GB", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit", second: "2-digit" });

export const eventId = (id: number) => `ARB-${String(id).padStart(6, "0")}`;

export const pathText = (path: string[]) => path.join(" → ");

export const percentOf = (fraction: number, digits = 2) => `${(fraction * 100).toFixed(digits)}%`;

/** A leg's edge (a log-return) as the percent it adds to or takes from the loop. */
export const legGain = (edge: number) => pct((Math.exp(edge) - 1) * 100, 3);
