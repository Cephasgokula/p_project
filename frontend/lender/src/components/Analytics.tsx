import React from "react";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from "recharts";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1";

export default function Analytics({ lenderId }: { lenderId: string }) {
  const { data } = useQuery({
    queryKey: ["lender-stats", lenderId],
    queryFn: () =>
      axios.get(`${API}/lenders/${encodeURIComponent(lenderId)}/stats`).then((r) => r.data),
    refetchInterval: 60000,
  });

  const stats = data ?? {
    totalReferrals: 0,
    approvals: 0,
    acceptanceRate: 0,
    avgFinalScore: 0,
  };

  const chartData = [
    { name: "Referrals", value: stats.totalReferrals },
    { name: "Approvals", value: stats.approvals },
  ];

  return (
    <div>
      <h2 className="text-2xl font-bold text-slate-800 mb-6">Analytics (30-day)</h2>

      <div className="grid grid-cols-4 gap-4 mb-8">
        {[
          { label: "Total Referrals", value: stats.totalReferrals, color: "blue" },
          { label: "Approvals", value: stats.approvals, color: "green" },
          {
            label: "Acceptance Rate",
            value: `${(stats.acceptanceRate * 100).toFixed(1)}%`,
            color: "purple",
          },
          { label: "Avg Score", value: stats.avgFinalScore?.toFixed(0) ?? "—", color: "amber" },
        ].map(({ label, value, color }) => (
          <div key={label} className={`p-4 rounded-xl border bg-${color}-50 border-${color}-200`}>
            <p className="text-xs font-medium text-slate-500">{label}</p>
            <p className="text-2xl font-bold text-slate-800 mt-1">{value}</p>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h3 className="text-lg font-semibold text-slate-700 mb-4">Referrals vs Approvals</h3>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis />
            <Tooltip />
            <Bar dataKey="value" fill="#2563EB" radius={8} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
