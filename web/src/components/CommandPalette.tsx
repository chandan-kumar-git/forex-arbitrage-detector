import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { alerts } from "../store/alerts";
import { market } from "../store/market";
import { Icon, type IconName } from "./Icon";
import "./CommandPalette.css";

interface Command {
  id: string;
  label: string;
  hint: string;
  icon: IconName;
  run: () => void;
}

/** Ctrl+K (or ⌘K, or /) anywhere in the desk: jump to any screen or run an action from the keyboard. */
export function CommandPalette({ onTour }: { onTour: () => void }) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [active, setActive] = useState(0);
  const input = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const typing = ["INPUT", "TEXTAREA", "SELECT"].includes((e.target as HTMLElement).tagName);
      if ((e.key.toLowerCase() === "k" && (e.ctrlKey || e.metaKey)) || (e.key === "/" && !typing)) {
        e.preventDefault();
        setOpen((o) => !o);
      }
      if (e.key === "Escape") setOpen(false);
    };
    const onOpen = () => setOpen(true);
    window.addEventListener("keydown", onKey);
    window.addEventListener("open-palette", onOpen);
    return () => {
      window.removeEventListener("keydown", onKey);
      window.removeEventListener("open-palette", onOpen);
    };
  }, []);

  useEffect(() => {
    if (open) {
      setQuery("");
      setActive(0);
      window.setTimeout(() => input.current?.focus(), 0);
    }
  }, [open]);

  const commands = useMemo<Command[]>(() => {
    const go = (to: string) => () => navigate(to);
    const s = market.getState();
    const list: Command[] = [
      { id: "live", label: "Go to Live", hint: "the map and the latest loop", icon: "overview", run: go("/desk") },
      { id: "send", label: "Send money", hint: "cheapest way to change money", icon: "send", run: go("/desk/send") },
      { id: "game", label: "Play: Beat the algorithm", hint: "find the loop before the timer", icon: "bolt", run: go("/desk/game") },
      { id: "events", label: "Go to Events", hint: "every loop found", icon: "events", run: go("/desk/events") },
      { id: "replay", label: "Replay the latest loop", hint: "Bellman-Ford step by step", icon: "replay", run: go(s.recent[0] ? `/desk/replay/${s.recent[0].id}` : "/desk/replay") },
      { id: "cur", label: "Go to Currencies", hint: "add or switch off stations", icon: "detector", run: go("/desk/detector") },
      { id: "lab", label: "Go to Lab", hint: "edit rates by hand", icon: "lab", run: go("/desk/lab") },
      { id: "analysis", label: "Go to Analysis", hint: "speed and cross-check", icon: "analysis", run: go("/desk/analysis") },
      { id: "settings", label: "Go to Settings", hint: "market, alerts, data source", icon: "settings", run: go("/desk/settings") },
      { id: "pause", label: s.paused ? "Resume the market" : "Pause the market", hint: "Space", icon: s.paused ? "play" : "pause", run: () => void market.togglePause() },
      { id: "alerts", label: alerts.prefs.enabled ? "Turn profit alerts off" : "Turn profit alerts on", hint: `at ${alerts.prefs.minProfit.toFixed(2)}%`, icon: "bolt", run: () => alerts.setPrefs({ enabled: !alerts.prefs.enabled }) },
      { id: "tour", label: "Take the guided tour", hint: "what every part of the desk does", icon: "arrow", run: onTour },
      { id: "home", label: "Open the landing page", hint: "the overview", icon: "back", run: go("/") },
    ];
    const code = query.trim().toUpperCase();
    if (/^[A-Z]{3}$/.test(code)) {
      list.unshift({ id: "code", label: `Send money from ${code}`, hint: "plan a transfer", icon: "send", run: go("/desk/send") });
    }
    return list;
  }, [navigate, onTour, query, open]);

  const shown = commands.filter((c) => `${c.label} ${c.hint}`.toLowerCase().includes(query.toLowerCase().trim()) || c.id === "code");

  if (!open) return null;
  const run = (c: Command | undefined) => {
    if (!c) return;
    setOpen(false);
    c.run();
  };

  return (
    <div className="palette-backdrop" onMouseDown={() => setOpen(false)}>
      <div className="palette" role="dialog" aria-modal="true" aria-label="Command palette" onMouseDown={(e) => e.stopPropagation()}>
        <input
          ref={input}
          className="palette-input"
          placeholder="Type a command, or a currency code like INR…"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setActive(0);
          }}
          onKeyDown={(e) => {
            if (e.key === "ArrowDown") {
              e.preventDefault();
              setActive((a) => Math.min(shown.length - 1, a + 1));
            }
            if (e.key === "ArrowUp") {
              e.preventDefault();
              setActive((a) => Math.max(0, a - 1));
            }
            if (e.key === "Enter") run(shown[active]);
          }}
          aria-activedescendant={shown[active] ? `cmd-${shown[active].id}` : undefined}
          aria-controls="palette-list"
        />
        <ul className="palette-list" id="palette-list" role="listbox">
          {shown.map((c, i) => (
            <li
              key={c.id}
              id={`cmd-${c.id}`}
              role="option"
              aria-selected={i === active}
              className={i === active ? "is-active" : ""}
              onMouseEnter={() => setActive(i)}
              onClick={() => run(c)}
              style={{ animationDelay: `${i * 18}ms` }}
            >
              <Icon name={c.icon} size={18} />
              <span className="palette-label">{c.label}</span>
              <span className="palette-hint">{c.hint}</span>
            </li>
          ))}
          {!shown.length ? <li className="palette-empty">No command matches “{query}”.</li> : null}
        </ul>
        <div className="palette-foot">
          <span><kbd>↑</kbd> <kbd>↓</kbd> choose</span>
          <span><kbd>Enter</kbd> run</span>
          <span><kbd>Esc</kbd> close</span>
        </div>
      </div>
    </div>
  );
}
