import React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine,
} from "recharts";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1";

interface DriftPoint {
  date: string;
  psi: number;
}

interface ModelInfo {
  version: string;
  deployedAt: string;
  shadowVersion: string | null;
  driftHistory: DriftPoint[];
}

export default function ModelMonitor() {
  const qc = useQueryClient();

  const { data: model, isLoading } = useQuery<ModelInfo>({
    queryKey: ["model-info"],
    queryFn: () => axios.get(`${API}/admin/model`).then((r) => r.data),
    refetchInterval: 30000,
  });

  const promote = useMutation({
    mutationFn: () => axios.post(`${API}/admin/model/promote`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["model-info"] });
    },
  });

  if (isLoading || !model) {
    return <p className="text-slate-400 py-12 text-center">Loading model info…</p>;
  }

  const PSI_THRESHOLD = 0.2;

  return (
    <div>
      <h2 className="text-2xl font-bold text-slate-800 mb-6">Model Monitor</h2>

      <div className="grid grid-cols-3 gap-4 mb-8">
        <Stat label="Production Version" value={model.version} />
        <Stat label="Deployed" value={new Date(model.deployedAt).toLocaleDateString()} />
        <Stat label="Shadow Candidate" value={model.shadowVersion ?? "—"} />
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h3 className="text-lg font-semibold text-slate-700 mb-4">PSI Drift (30-day)</h3>
        <ResponsiveContainer width="100%" height={280}>
          <LineChart data={model.driftHistory}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" tick={{ fontSize: 12 }} />
            <YAxis domain={[0, 0.5]} />
            <Tooltip />
            <ReferenceLine y={PSI_THRESHOLD} stroke="#EF4444" strokeDasharray="6 3" label="Threshold" />
            <Line type="monotone" dataKey="psi" stroke="#6366F1" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {model.shadowVersion && (
        <button
          onClick={() => promote.mutate()}
          disabled={promote.isPending}
          className="bg-green-600 hover:bg-green-700 text-white font-semibold px-6 py-2.5 rounded-xl transition-colors disabled:opacity-50"
        >
          {promote.isPending ? "Promoting…" : `Promote ${model.shadowVersion} → Production`}
        </button>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="p-4 rounded-xl border border-slate-200 bg-white">
      <p className="text-xs font-medium text-slate-500">{label}</p>
      <p className="text-lg font-bold text-slate-800 mt-1">{value}</p>
    </div>
  );
}
