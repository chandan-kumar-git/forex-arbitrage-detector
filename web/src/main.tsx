import "@fontsource-variable/hanken-grotesk";
import "@fontsource-variable/inter";
import "./styles/base.css";
import "./styles/controls.css";
import "./styles/layout.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
