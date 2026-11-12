import { Link } from "react-router-dom";
import { useAlerts } from "../store/alerts";
import { Icon } from "./Icon";
import "./Toaster.css";

/** Alert toasts in the corner of the desk; each links to the replay of its loop. */
export function Toaster() {
  const a = useAlerts();
  return (
    <div className="toaster" aria-live="polite">
      {a.toasts.map((t) => (
        <div key={t.id} className="toast" role="status">
          <span className="toast-dot" aria-hidden="true" />
          <div className="toast-text">
            <strong>{t.title}</strong>
            <span>{t.body}</span>
          </div>
          <Link className="btn btn-sm" to={`/desk/replay/${t.eventId}`} onClick={() => a.dismiss(t.id)}>
            Replay
          </Link>
          <button type="button" className="toast-close" aria-label="Dismiss" onClick={() => a.dismiss(t.id)}>
            <Icon name="close" size={16} />
          </button>
        </div>
      ))}
    </div>
  );
}
