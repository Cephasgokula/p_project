import React from "react";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine,
} from "recharts";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1";

interface CohortStat {
  cohort: string;
  approvalRate: number;
  avgScore: number;
  count: number;
}

export default function FairnessReport() {
  const { data: cohorts = [] } = useQuery<CohortStat[]>({
    queryKey: ["fairness-cohorts"],
    queryFn: () => axios.get(`${API}/admin/fairness`).then((r) => r.data),
    refetchInterval: 60000,
  });

  const overallRate =
    cohorts.length > 0
      ? cohorts.reduce((s, c) => s + c.approvalRate * c.count, 0) /
        cohorts.reduce((s, c) => s + c.count, 0)
      : 0;

  const disparity = cohorts.length > 0
    ? Math.max(...cohorts.map((c) => c.approvalRate)) -
      Math.min(...cohorts.map((c) => c.approvalRate))
    : 0;

  return (
    <div>
      <h2 className="text-2xl font-bold text-slate-800 mb-6">Fairness Report</h2>

      <div className="grid grid-cols-3 gap-4 mb-8">
        <StatCard label="Overall Approval Rate" value={`${(overallRate * 100).toFixed(1)}%`} />
        <StatCard label="Max Disparity" value={`${(disparity * 100).toFixed(1)}pp`}
          warn={disparity > 0.05} />
        <StatCard label="Cohorts Monitored" value={String(cohorts.length)} />
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h3 className="text-lg font-semibold text-slate-700 mb-4">Approval Rate by Cohort</h3>
        <ResponsiveContainer width="100%" height={320}>
          <BarChart data={cohorts} layout="vertical">
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis type="number" domain={[0, 1]} tickFormatter={(v: number) => `${(v * 100).toFixed(0)}%`} />
            <YAxis type="category" dataKey="cohort" width={100} tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number) => `${(v * 100).toFixed(1)}%`} />
            <ReferenceLine x={overallRate} stroke="#6366F1" strokeDasharray="4 3" label="Overall" />
            <Bar dataKey="approvalRate" fill="#2563EB" radius={4} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-slate-500 border-b">
            <tr>
              <th className="px-4 py-3">Cohort</th>
              <th className="px-4 py-3">Count</th>
              <th className="px-4 py-3">Approval Rate</th>
              <th className="px-4 py-3">Avg Score</th>
              <th className="px-4 py-3">Gap vs Overall</th>
            </tr>
          </thead>
          <tbody>
            {cohorts.map((c) => {
              const gap = c.approvalRate - overallRate;
              return (
                <tr key={c.cohort} className="border-b border-slate-100 hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-700">{c.cohort}</td>
                  <td className="px-4 py-3">{c.count}</td>
                  <td className="px-4 py-3">{(c.approvalRate * 100).toFixed(1)}%</td>
                  <td className="px-4 py-3">{c.avgScore.toFixed(0)}</td>
                  <td className={`px-4 py-3 font-medium ${gap > 0 ? "text-green-600" : "text-red-600"}`}>
                    {gap > 0 ? "+" : ""}{(gap * 100).toFixed(1)}pp
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function StatCard({ label, value, warn }: { label: string; value: string; warn?: boolean }) {
  return (
    <div className={`p-4 rounded-xl border ${warn ? "border-red-300 bg-red-50" : "border-slate-200 bg-white"}`}>
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <p className={`text-lg font-bold mt-1 ${warn ? "text-red-600" : "text-slate-800"}`}>{value}</p>
    </div>
  );
}
