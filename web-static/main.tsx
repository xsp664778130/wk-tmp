import React from "react";
import ReactDOM from "react-dom/client";
import { SkillWorkspace } from "../app/skill-workspace";
import "../app/globals.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <SkillWorkspace initialUser={null} />
  </React.StrictMode>,
);
