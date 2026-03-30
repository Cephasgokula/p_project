import React, { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import axios from "axios";

const API = process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1";

interface FraudFlag {
  id: string;
  applicationId: string;
  applicantName: string;
  fraudProbability: number;
  gnnScore: number;
  velocityCount: number;
  flaggedAt: string;
  status: "PENDING" | "CONFIRMED" | "CLEARED";
  details: Record<string, unknown>;
}

export default function FraudQueue() {
  const qc = useQueryClient();
  const [expanded, setExpanded] = useState<string | null>(null);

  const { data: flags = [] } = useQuery<FraudFlag[]>({
    queryKey: ["fraud-flags"],
    queryFn: () => axios.get(`${API}/fraud/flags`).then((r) => r.data),
    refetchInterval: 15000,
  });

  const resolve = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      axios.patch(`${API}/fraud/flags/${encodeURIComponent(id)}`, { status }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["fraud-flags"] }),
  });

  const probColor = (p: number) =>
    p >= 0.75 ? "text-red-600 bg-red-100" : p >= 0.5 ? "text-amber-600 bg-amber-100" : "text-green-600 bg-green-100";

  return (
    <div>
      <h2 className="text-2xl font-bold text-slate-800 mb-6">
        Fraud Review Queue <span className="text-base font-normal text-slate-400 ml-2">({flags.length})</span>
      </h2>
      <div className="space-y-3">
        {flags.map((f) => (
          <div key={f.id} className="bg-white border border-slate-200 rounded-xl overflow-hidden">
            <div
              className="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-50"
              onClick={() => setExpanded(expanded === f.id ? null : f.id)}
            >
              <div className="flex items-center gap-4">
                <span className={`px-2.5 py-1 rounded-full text-xs font-bold ${probColor(f.fraudProbability)}`}>
                  {(f.fraudProbability * 100).toFixed(0)}%
                </span>
                <span className="font-medium text-slate-700">{f.applicantName}</span>
                <span className="text-xs text-slate-400">App #{f.applicationId.slice(0, 8)}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className={`text-xs px-2 py-0.5 rounded ${
                  f.status === "PENDING" ? "bg-yellow-100 text-yellow-700" :
                  f.status === "CONFIRMED" ? "bg-red-100 text-red-700" :
                  "bg-green-100 text-green-700"
                }`}>
                  {f.status}
                </span>
                <span className="text-slate-300">{expanded === f.id ? "▲" : "▼"}</span>
              </div>
            </div>

            {expanded === f.id && (
              <div className="border-t border-slate-100 p-4 bg-slate-50">
                <div className="grid grid-cols-3 gap-4 mb-4 text-sm">
                  <div>
                    <p className="text-slate-500">GNN Score</p>
                    <p className="font-semibold">{f.gnnScore.toFixed(3)}</p>
                  </div>
                  <div>
                    <p className="text-slate-500">Velocity (24h)</p>
                    <p className="font-semibold">{f.velocityCount} apps</p>
                  </div>
                  <div>
                    <p className="text-slate-500">Flagged</p>
                    <p className="font-semibold">{new Date(f.flaggedAt).toLocaleString()}</p>
                  </div>
                </div>
                <pre className="text-xs bg-white border border-slate-200 rounded p-3 mb-4 overflow-auto max-h-40">
                  {JSON.stringify(f.details, null, 2)}
                </pre>
                {f.status === "PENDING" && (
                  <div className="flex gap-3">
                    <button
                      onClick={() => resolve.mutate({ id: f.id, status: "CONFIRMED" })}
                      className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium"
                    >
                      Confirm Fraud
                    </button>
                    <button
                      onClick={() => resolve.mutate({ id: f.id, status: "CLEARED" })}
                      className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg text-sm font-medium"
                    >
                      Clear
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
        {flags.length === 0 && (
          <p className="text-center py-16 text-slate-400">No pending fraud flags</p>
        )}
      </div>
    </div>
  );
}
