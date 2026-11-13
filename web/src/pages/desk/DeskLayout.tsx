import { useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { CommandPalette } from "../../components/CommandPalette";
import { Icon, type IconName } from "../../components/Icon";
import { Toaster } from "../../components/Toaster";
import { Tour } from "../../components/Tour";
import { load, save } from "../../store/storage";
import { Wordmark } from "../../components/Wordmark";
import { market, useMarket } from "../../store/market";
import "./desk.css";

const NAV: { to: string; label: string; icon: IconName; end?: boolean }[] = [
  { to: "/desk", label: "Live", icon: "overview", end: true },
  { to: "/desk/send", label: "Send money", icon: "send" },
  { to: "/desk/game", label: "Beat the algorithm", icon: "game" },
  { to: "/desk/events", label: "Events", icon: "events" },
  { to: "/desk/replay", label: "Replay", icon: "replay" },
  { to: "/desk/detector", label: "Currencies", icon: "detector" },
  { to: "/desk/lab", label: "Lab", icon: "lab" },
  { to: "/desk/analysis", label: "Analysis", icon: "analysis" },
  { to: "/desk/settings", label: "Settings", icon: "settings" },
];

/** The desk shell: sidebar and top strip stay put; only the main area changes. */
export function DeskLayout() {
  const s = useMarket((st) => st);
  const location = useLocation();
  const [touring, setTouring] = useState(false);
  const [toured, setToured] = useState(() => load<boolean>("toured", false));
  const startTour = () => setTouring(true);
  const endTour = () => {
    setTouring(false);
    setToured(true);
    save("toured", true);
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement;
      if (e.code === "Space" && !["INPUT", "TEXTAREA", "SELECT", "BUTTON", "A"].includes(target.tagName)) {
        e.preventDefault();
        void market.togglePause();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const javaDown = s.source === "java" && s.javaStatus !== "online";
  const status = javaDown ? "off" : s.paused ? "paused" : "live";

  return (
    <div className="desk">
      <aside className="rail">
        <div className="rail-brand">
          <Wordmark />
        </div>
        <nav aria-label="Desk">
          <ul>
            {NAV.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.end} className="rail-link" title={item.label}>
                  <Icon name={item.icon} size={20} />
                  <span>{item.label}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <div className="rail-foot">
          <Link to="/">
            <Icon name="back" size={16} />
            Back to the overview
          </Link>
        </div>
      </aside>

      <header className="strip">
        <span className="strip-home">
          <Wordmark />
        </span>
        <span className={`chip chip-${status}`}>
          <span className="chip-dot" aria-hidden="true" />
          {status === "off" ? (s.javaStatus === "connecting" ? "Connecting" : "Java offline") : status === "paused" ? "Paused" : "Live"}
        </span>
        <span className="chip chip-sim">{s.source === "java" ? "Java engine" : "Simulated market"}</span>
        <span className="strip-count">
          <strong>{s.found}</strong> {s.found === 1 ? "loop" : "loops"} found since you opened the page
        </span>
        <button
          type="button"
          className="btn btn-sm btn-quiet hide-sm"
          onClick={() => window.dispatchEvent(new Event("open-palette"))}
          title="Command palette (Ctrl+K)"
        >
          Search <kbd>Ctrl K</kbd>
        </button>
        <button type="button" className={`btn btn-sm ${toured ? "btn-quiet" : "tour-invite"}`} onClick={startTour}>
          Tour
        </button>
        <button
          type="button"
          className="btn btn-sm"
          onClick={() => void market.togglePause()}
          disabled={javaDown}
          aria-keyshortcuts="Space"
          title="Pause or resume (Space)"
        >
          <Icon name={s.paused ? "play" : "pause"} size={14} />
          {s.paused ? "Resume" : "Pause"}
        </button>
      </header>

      <main className="desk-main">
        {/* the key makes each screen play its entrance when you switch to it */}
        <div className="page-enter" key={location.pathname}>
          <Outlet />
        </div>
      </main>
      <Toaster />
      <CommandPalette onTour={startTour} />
      <Tour open={touring} onClose={endTour} />
    </div>
  );
}
