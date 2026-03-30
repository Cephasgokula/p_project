import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import "./index.css";
import ReferralFeed from "./components/ReferralFeed";
import RulesManager from "./components/RulesManager";
import Analytics from "./components/Analytics";

const queryClient = new QueryClient();
const LENDER_ID = "demo-lender-id";

const root = ReactDOM.createRoot(document.getElementById("root") as HTMLElement);
root.render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <div className="min-h-screen bg-slate-50">
          <nav className="bg-brand text-white px-6 py-4 flex items-center justify-between">
            <h1 className="text-xl font-bold">LendIQ — Lender Dashboard</h1>
            <div className="flex gap-4 text-sm">
              <a href="/" className="hover:text-blue-200">Referrals</a>
              <a href="/rules" className="hover:text-blue-200">Rules</a>
              <a href="/analytics" className="hover:text-blue-200">Analytics</a>
            </div>
          </nav>
          <div className="max-w-5xl mx-auto p-6">
            <Routes>
              <Route path="/" element={<ReferralFeed lenderId={LENDER_ID} authToken="" />} />
              <Route path="/rules" element={<RulesManager lenderId={LENDER_ID} />} />
              <Route path="/analytics" element={<Analytics lenderId={LENDER_ID} />} />
            </Routes>
          </div>
        </div>
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
);
