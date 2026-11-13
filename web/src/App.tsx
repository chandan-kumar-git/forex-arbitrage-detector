import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Landing } from "./pages/Landing";
import { DeskLayout } from "./pages/desk/DeskLayout";
import { Overview } from "./pages/desk/Overview";
import { SendMoney } from "./pages/desk/SendMoney";
import { Game } from "./pages/desk/Game";
import { Events } from "./pages/desk/Events";
import { Replay } from "./pages/desk/Replay";
import { Detector } from "./pages/desk/Detector";
import { Lab } from "./pages/desk/Lab";
import { Analysis } from "./pages/desk/Analysis";
import { Settings } from "./pages/desk/Settings";

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/desk" element={<DeskLayout />}>
          <Route index element={<Overview />} />
          <Route path="send" element={<SendMoney />} />
          <Route path="game" element={<Game />} />
          <Route path="events" element={<Events />} />
          <Route path="replay" element={<Replay />} />
          <Route path="replay/:id" element={<Replay />} />
          <Route path="detector" element={<Detector />} />
          <Route path="lab" element={<Lab />} />
          <Route path="analysis" element={<Analysis />} />
          <Route path="settings" element={<Settings />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
