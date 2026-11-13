import type { ArbEvent } from "../engine/types";

/**
 * Draws a loop as a shareable PNG card (metro line, profit, time) and downloads it.
 * Plain canvas drawing, no library.
 */
export function downloadShareCard(event: ArbEvent) {
  const W = 1200;
  const H = 630;
  const canvas = document.createElement("canvas");
  canvas.width = W;
  canvas.height = H;
  const g = canvas.getContext("2d");
  if (!g) return;
  const font = (w: number, size: number) => `${w} ${size}px "Hanken Grotesk Variable", "Segoe UI", sans-serif`;

  g.fillStyle = "#f6f7f9";
  g.fillRect(0, 0, W, H);
  g.fillStyle = "#ffffff";
  roundRect(g, 48, 48, W - 96, H - 96, 32);
  g.fill();

  g.fillStyle = "#2546f0";
  roundRect(g, 96, 96, 56, 56, 16);
  g.fill();
  g.strokeStyle = "#fff";
  g.lineWidth = 5;
  g.beginPath();
  g.moveTo(124, 110);
  g.lineTo(140, 138);
  g.lineTo(108, 138);
  g.closePath();
  g.stroke();
  g.fillStyle = "#0e1116";
  g.font = font(700, 30);
  g.fillText("Arbitrage Detector", 172, 134);

  g.fillStyle = "#087a54";
  g.font = font(800, 120);
  g.fillText(`+${event.profitPercent.toFixed(3)}%`, 96, 300);
  g.fillStyle = "#6b7380";
  g.font = font(500, 28);
  g.fillText(`profit after fees · found ${new Date(event.detectedAt).toLocaleString("en-GB")}`, 100, 350);

  // the loop as a metro line
  const stops = event.path;
  const x0 = 130;
  const x1 = W - 130;
  const y = 460;
  g.strokeStyle = "#2546f0";
  g.lineWidth = 14;
  g.lineCap = "round";
  g.beginPath();
  g.moveTo(x0, y);
  g.lineTo(x1, y);
  g.stroke();
  stops.forEach((code, i) => {
    const x = x0 + ((x1 - x0) * i) / (stops.length - 1);
    g.fillStyle = "#fff";
    g.strokeStyle = "#2546f0";
    g.lineWidth = 12;
    g.beginPath();
    g.arc(x, y, 22, 0, Math.PI * 2);
    g.fill();
    g.stroke();
    g.fillStyle = "#0e1116";
    g.font = font(700, 30);
    g.textAlign = "center";
    g.fillText(code, x, y + 66);
    g.textAlign = "left";
  });

  g.fillStyle = "#6b7380";
  g.font = font(500, 22);
  g.fillText("Found by Bellman-Ford on a simulated market · not financial advice", 96, H - 80);

  const a = document.createElement("a");
  a.href = canvas.toDataURL("image/png");
  a.download = `loop-${event.path.join("-")}.png`;
  a.click();
}

function roundRect(g: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
  g.beginPath();
  g.moveTo(x + r, y);
  g.arcTo(x + w, y, x + w, y + h, r);
  g.arcTo(x + w, y + h, x, y + h, r);
  g.arcTo(x, y + h, x, y, r);
  g.arcTo(x, y, x + w, y, r);
  g.closePath();
}
