import React from "react";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from "recharts";
import { ApplicationResponse } from "../types";

interface Props {
  response: ApplicationResponse;
}

const SCORE_COLOR = (score: number) =>
  score >= 700 ? "#16A34A" : score >= 550 ? "#D97706" : "#DC2626";

export default function DecisionView({ response }: Props) {
  const score = response.final_score ?? 0;
  const outcome = response.outcome ?? "DECLINE";

  const shapData = Object.entries(response.shap_values ?? {})
    .sort((a, b) => Math.abs(b[1]) - Math.abs(a[1]))
    .slice(0, 6)
    .map(([name, value]) => ({
      name,
      value,
      fill: value > 0 ? "#16A34A" : "#DC2626",
    }));

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="text-center mb-8">
        <p className="text-sm font-medium text-slate-500 uppercase tracking-wide mb-2">
          Credit Score
        </p>
        <div className="text-7xl font-bold" style={{ color: SCORE_COLOR(score) }}>
          {score.toFixed(0)}
        </div>
        <div
          className={`inline-block mt-3 px-4 py-1.5 rounded-full text-sm font-semibold ${
            outcome === "APPROVE"
              ? "bg-green-100 text-green-800"
              : outcome === "REFER"
              ? "bg-amber-100 text-amber-800"
              : "bg-red-100 text-red-800"
          }`}
        >
          {outcome === "APPROVE"
            ? "✓ Approved"
            : outcome === "REFER"
            ? "⏳ Under Review"
            : "✗ Declined"}
        </div>
      </div>

      {shapData.length > 0 && (
        <div className="mb-8">
          <h3 className="text-lg font-semibold text-slate-700 mb-4">Key Factors</h3>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={shapData} layout="vertical" margin={{ left: 80, right: 20 }}>
              <XAxis type="number" tickFormatter={(v) => v.toFixed(1)} />
              <YAxis dataKey="name" type="category" width={80} tick={{ fontSize: 13 }} />
              <Tooltip formatter={(v: number) => v.toFixed(2)} />
              <Bar dataKey="value" radius={4}>
                {shapData.map((entry, i) => (
                  <Cell key={i} fill={entry.fill} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {response.decision_path && (
        <div>
          <h3 className="text-lg font-semibold text-slate-700 mb-4">Decision Path</h3>
          <div className="space-y-2">
            {response.decision_path.map((step, i) => (
              <div key={i} className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-full bg-blue-100 text-blue-700 text-xs font-bold flex items-center justify-center mt-0.5">
                  {i + 1}
                </div>
                <p className="text-sm text-slate-600 flex-1">{step}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {outcome === "DECLINE" && (
        <div className="mt-6 p-4 bg-amber-50 border border-amber-200 rounded-xl">
          <h4 className="font-semibold text-amber-800 mb-2">How to Improve</h4>
          <ul className="text-sm text-amber-700 space-y-1 list-disc list-inside">
            {shapData
              .filter((d) => d.value < 0)
              .map((d) => (
                <li key={d.name}>Improve your {d.name.replace(/_/g, " ")}</li>
              ))}
          </ul>
        </div>
      )}

      {outcome === "APPROVE" && response.lender && (
        <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-xl">
          <h4 className="font-semibold text-green-800">Referred to Lender</h4>
          <p className="text-green-700 mt-1">{response.lender.name}</p>
        </div>
      )}
    </div>
  );
}
