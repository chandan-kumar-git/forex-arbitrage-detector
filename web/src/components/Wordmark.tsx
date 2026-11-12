import { Link } from "react-router-dom";

/** A three-station loop glyph and the product name. */
export function Wordmark({ to = "/" }: { to?: string }) {
  return (
    <Link to={to} className="wordmark" aria-label="Arbitrage Detector, home">
      <svg width="30" height="30" viewBox="0 0 32 32" aria-hidden="true">
        <rect width="32" height="32" rx="9" fill="var(--blue)" />
        <path d="M16 8 L24.5 22.5 L7.5 22.5 Z" fill="none" stroke="#fff" strokeWidth="2.6" strokeLinejoin="round" />
        <circle cx="16" cy="8" r="3.2" fill="var(--blue)" stroke="#fff" strokeWidth="2.2" />
        <circle cx="24.5" cy="22.5" r="3.2" fill="var(--blue)" stroke="#fff" strokeWidth="2.2" />
        <circle cx="7.5" cy="22.5" r="3.2" fill="var(--blue)" stroke="#fff" strokeWidth="2.2" />
      </svg>
      <span className="wordmark-name">Arbitrage Detector</span>
    </Link>
  );
}
