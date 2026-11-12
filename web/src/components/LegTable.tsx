import type { ArbEvent } from "../engine/types";
import { legGain, rate } from "../lib/format";

interface Props {
  event: ArbEvent;
  selectedLeg: number | null;
  onSelectLeg: (legNo: number | null) => void;
}

/** The legs of one event; selecting a row highlights that line on the map. */
export function LegTable({ event, selectedLeg, onSelectLeg }: Props) {
  return (
    <div className="table-wrap">
    <table className="legs">
      <caption className="visually-hidden">Conversion steps of this loop</caption>
      <thead>
        <tr>
          <th scope="col">Leg</th>
          <th scope="col">Convert</th>
          <th scope="col" className="r">
            Rate
          </th>
          <th scope="col" className="r" title="This leg's gain or loss against the fair cross rate through USD, after the fee. The legs add up to the loop's profit.">
            Edge
          </th>
        </tr>
      </thead>
      <tbody>
        {event.legs.map((leg) => {
          const sel = selectedLeg === leg.legNo;
          return (
            <tr key={leg.legNo} className={sel ? "is-sel" : selectedLeg ? "is-dim" : ""}>
              <td className="num">{leg.legNo}</td>
              <td>
                <button
                  type="button"
                  className="leg-btn"
                  aria-pressed={sel}
                  onClick={() => onSelectLeg(sel ? null : leg.legNo)}
                >
                  <span className="leg-swatch" aria-hidden="true" />
                  <span className="num">
                    {leg.from} → {leg.to}
                  </span>
                </button>
              </td>
              <td className="num r">{rate(leg.rate)}</td>
              <td className={`num r ${leg.edge > 0 ? "gain" : "loss"}`}>{legGain(leg.edge)}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
    </div>
  );
}
