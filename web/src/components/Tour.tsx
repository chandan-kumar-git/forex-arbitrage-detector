import { useEffect, useLayoutEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "./Tour.css";

interface Step {
  route: string;
  selector: string;
  title: string;
  text: string;
}

const STEPS: Step[] = [
  { route: "/desk", selector: ".display-panel", title: "The live market", text: "Every currency is a station and every rate a line. When the detector finds a loop that makes money, it is drawn in blue." },
  { route: "/desk", selector: ".event-meta", title: "The latest loop", text: "How much the loop pays after fees, the route it takes, and when it was found." },
  { route: "/desk", selector: ".legs", title: "Each trade", text: "The trades of the loop. 'Edge' shows which trade beat the fair rate: that one mispriced rate is where the profit comes from." },
  { route: "/desk", selector: ".tm", title: "Time machine", text: "The last 3 minutes of the market. Blue bars are ticks with a loop. Drag across to rewind, or replay the last minute at 4× speed." },
  { route: "/desk", selector: 'a[href="/desk/send"]', title: "Real use: send money", text: "The same graph search finds the cheapest way to change money, for example INR through USD to JPY, and shows the saving." },
  { route: "/desk", selector: 'a[href="/desk/game"]', title: "Beat the algorithm", text: "A game: find the hidden loop yourself before the timer runs out, then see the detector's answer." },
  { route: "/desk", selector: ".strip .btn", title: "Pause, and Ctrl+K", text: "Pause the market to study a moment (Space). Press Ctrl+K anywhere for the command palette." },
];

/** A spotlight walkthrough of the desk, one part at a time. */
export function Tour({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [i, setI] = useState(0);
  const [rect, setRect] = useState<DOMRect | null>(null);
  const location = useLocation();
  const navigate = useNavigate();
  const step = STEPS[i];

  useEffect(() => {
    if (open) setI(0);
  }, [open]);

  useEffect(() => {
    if (open && location.pathname !== step.route) navigate(step.route);
  }, [open, step, location.pathname, navigate]);

  // find the element (it may appear a moment after navigation) and follow it on scroll / resize
  useLayoutEffect(() => {
    if (!open) return;
    let frame = 0;
    let tries = 0;
    const measure = () => {
      const el = document.querySelector(step.selector);
      if (el) {
        el.scrollIntoView({ block: "nearest", behavior: "smooth" });
        setRect(el.getBoundingClientRect());
      } else if (tries++ < 60) frame = requestAnimationFrame(measure);
    };
    measure();
    const update = () => {
      const el = document.querySelector(step.selector);
      if (el) setRect(el.getBoundingClientRect());
    };
    window.addEventListener("resize", update);
    window.addEventListener("scroll", update, true);
    const t = window.setInterval(update, 400);
    return () => {
      cancelAnimationFrame(frame);
      window.removeEventListener("resize", update);
      window.removeEventListener("scroll", update, true);
      window.clearInterval(t);
    };
  }, [open, step]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
      if (e.key === "ArrowRight") setI((x) => Math.min(STEPS.length - 1, x + 1));
      if (e.key === "ArrowLeft") setI((x) => Math.max(0, x - 1));
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open || !rect) return null;
  const pad = 8;
  const below = rect.bottom + 230 < window.innerHeight;
  const cardTop = below ? rect.bottom + pad + 14 : Math.max(16, rect.top - pad - 14 - 210);
  const cardLeft = Math.min(Math.max(16, rect.left), window.innerWidth - 376);
  const last = i === STEPS.length - 1;

  return (
    <div className="tour" role="dialog" aria-modal="true" aria-label={`Tour step ${i + 1} of ${STEPS.length}: ${step.title}`}>
      <div className="tour-click" onClick={onClose} />
      <div
        className="tour-spot"
        style={{ top: rect.top - pad, left: rect.left - pad, width: rect.width + pad * 2, height: rect.height + pad * 2 }}
      />
      <div className="tour-card" key={i} style={{ top: cardTop, left: cardLeft }}>
        <div className="tour-count">
          Step {i + 1} of {STEPS.length}
        </div>
        <h3>{step.title}</h3>
        <p>{step.text}</p>
        <div className="tour-dots" aria-hidden="true">
          {STEPS.map((_, k) => (
            <span key={k} className={k === i ? "on" : ""} />
          ))}
        </div>
        <div className="tour-actions">
          <button type="button" className="btn btn-sm btn-quiet" onClick={onClose}>
            Skip
          </button>
          <span className="spacer" />
          {i > 0 ? (
            <button type="button" className="btn btn-sm" onClick={() => setI(i - 1)}>
              Back
            </button>
          ) : null}
          <button type="button" className="btn btn-sm btn-primary" autoFocus onClick={() => (last ? onClose() : setI(i + 1))}>
            {last ? "Done" : "Next"}
          </button>
        </div>
      </div>
    </div>
  );
}
