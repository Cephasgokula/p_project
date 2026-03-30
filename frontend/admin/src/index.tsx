import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route, NavLink } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import "./index.css";
import ModelMonitor from "./components/ModelMonitor";
import FraudQueue from "./components/FraudQueue";
import FairnessReport from "./components/FairnessReport";
import AuditTrail from "./components/AuditTrail";

const queryClient = new QueryClient();

const navLinks = [
  { to: "/", label: "Model Monitor" },
  { to: "/fraud", label: "Fraud Queue" },
  { to: "/fairness", label: "Fairness" },
  { to: "/audit", label: "Audit Trail" },
];

const root = ReactDOM.createRoot(document.getElementById("root") as HTMLElement);
root.render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="min-h-screen bg-slate-50">
          <nav className="bg-brand text-white px-6 py-4 flex items-center gap-8">
            <h1 className="text-xl font-bold mr-6">LendIQ — Admin</h1>
            {navLinks.map(({ to, label }) => (
              <NavLink
                key={to}
                to={to}
                end
                className={({ isActive }) =>
                  `text-sm ${isActive ? "font-bold text-blue-200" : "hover:text-blue-200"}`
                }
              >
                {label}
              </NavLink>
            ))}
          </nav>
          <div className="max-w-6xl mx-auto p-6">
            <Routes>
              <Route path="/" element={<ModelMonitor />} />
              <Route path="/fraud" element={<FraudQueue />} />
              <Route path="/fairness" element={<FairnessReport />} />
              <Route path="/audit" element={<AuditTrail />} />
            </Routes>
          </div>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
);
