import { useMemo } from "react";
import { crossDeviations } from "../engine/profit";
import { BASE_CODE, type Snapshot } from "../engine/types";
import { rate } from "../lib/format";
import "./RateLattice.css";

interface Props {
  snapshot: Snapshot | null;
  cycle?: number[] | null;
  selectedLeg?: number | null;
  editable?: boolean;
  onEdit?: (i: number, j: number, value: number) => void;
}

/**
 * The n x n rate matrix as a lattice of square cells. Tint shows how far a rate sits from the
 * cross rate through USD: the pair made too good shows blue, and its reverse red.
 */
export function RateLattice({ snapshot, cycle, selectedLeg = null, editable = false, onEdit }: Props) {
  const codes = snapshot?.codes ?? [];
  const n = codes.length;
  const base = codes.indexOf(BASE_CODE);
  const dev = useMemo(() => (snapshot ? crossDeviations(snapshot, base) : []), [snapshot, base]);

  const legOfCell = useMemo(() => {
    const map = new Map<string, number>();
    if (cycle) cycle.slice(0, -1).forEach((from, k) => map.set(`${from}-${cycle[k + 1]}`, k + 1));
    return map;
  }, [cycle]);

  if (!snapshot) return <p className="lattice-empty">Waiting for the first snapshot…</p>;

  return (
    <div className="lattice-wrap">
      <table className="lattice" style={{ ["--n" as string]: n }}>
        <caption className="visually-hidden">
          Exchange rates: row currency to column currency. Cells on the current loop are outlined.
        </caption>
        <thead>
          <tr>
            <th scope="col" className="lattice-corner">
              <span aria-hidden="true">from \ to</span>
            </th>
            {codes.map((c) => (
              <th key={c} scope="col">
                {c}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {codes.map((from, i) => (
            <tr key={from}>
              <th scope="row">{from}</th>
              {codes.map((to, j) => {
                const value = snapshot.rates[i][j];
                const d = dev[i]?.[j] ?? 1;
                const shift = Math.max(-1, Math.min(1, (d - 1) / 0.012));
                const legNo = legOfCell.get(`${i}-${j}`);
                const cls = [
                  "cell",
                  i === j ? "cell-self" : "",
                  legNo ? "cell-leg" : "",
                  legNo && selectedLeg === legNo ? "cell-sel" : "",
                ].join(" ");
                const tint =
                  i === j
                    ? undefined
                    : shift > 0.04
                      ? `rgba(37, 70, 240, ${0.1 + shift * 0.3})`
                      : shift < -0.04
                        ? `rgba(217, 58, 58, ${0.06 + -shift * 0.2})`
                        : undefined;
                return (
                  <td key={to} className={cls} style={{ background: tint }} title={`${from} → ${to}: ${value}`}>
                    {editable && i !== j ? (
                      <input
                        className="cell-input num"
                        defaultValue={rate(value)}
                        key={`${snapshot.timestamp}-${value}`}
                        aria-label={`Rate ${from} to ${to}`}
                        inputMode="decimal"
                        onBlur={(e) => {
                          const v = Number(e.currentTarget.value);
                          if (Number.isFinite(v) && v > 0) onEdit?.(i, j, v);
                          else e.currentTarget.value = rate(value);
                        }}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") e.currentTarget.blur();
                        }}
                      />
                    ) : (
                      <span className="num">{i === j ? "1" : rate(value)}</span>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
