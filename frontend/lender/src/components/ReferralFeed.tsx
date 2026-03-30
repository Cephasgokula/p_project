import React, { useEffect, useState } from "react";

interface Referral {
  application_id: string;
  final_score: number;
  amount: number;
  purpose: string;
  dti: number;
  employment_months: number;
  received_at: string;
}

interface Props {
  lenderId: string;
  authToken: string;
}

const API = process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1";

export default function ReferralFeed({ lenderId }: Props) {
  const [referrals, setReferrals] = useState<Referral[]>([]);
  const [filter, setFilter] = useState({ minScore: 0, maxScore: 1000, purpose: "all" });

  useEffect(() => {
    const sse = new EventSource(
      `${API}/lenders/${encodeURIComponent(lenderId)}/referrals/stream`
    );
    sse.onmessage = (e) => {
      const ref: Referral = JSON.parse(e.data);
      setReferrals((prev) => [ref, ...prev].slice(0, 100));
    };
    sse.onerror = () => sse.close();
    return () => sse.close();
  }, [lenderId]);

  const filtered = referrals.filter(
    (r) =>
      r.final_score >= filter.minScore &&
      r.final_score <= filter.maxScore &&
      (filter.purpose === "all" || r.purpose === filter.purpose)
  );

  const scoreColor = (s: number) =>
    s >= 700
      ? "bg-green-100 text-green-700"
      : s >= 550
      ? "bg-amber-100 text-amber-700"
      : "bg-red-100 text-red-700";

  return (
    <div>
      <h2 className="text-2xl font-bold text-slate-800 mb-6">Live Referral Feed</h2>
      <div className="flex gap-4 mb-6 flex-wrap">
        {["home", "vehicle", "personal", "business", "all"].map((p) => (
          <button
            key={p}
            onClick={() => setFilter((f) => ({ ...f, purpose: p }))}
            className={`px-3 py-1.5 rounded-full text-sm font-medium capitalize transition-colors ${
              filter.purpose === p
                ? "bg-blue-600 text-white"
                : "bg-slate-100 text-slate-600"
            }`}
          >
            {p}
          </button>
        ))}
        <div className="flex items-center gap-2 ml-auto">
          <span className="text-sm text-slate-500">Score:</span>
          <input
            type="number"
            placeholder="Min"
            value={filter.minScore || ""}
            onChange={(e) => setFilter((f) => ({ ...f, minScore: Number(e.target.value) }))}
            className="w-16 border border-slate-300 rounded px-2 py-1 text-sm"
          />
          <span className="text-slate-400">–</span>
          <input
            type="number"
            placeholder="Max"
            value={filter.maxScore || ""}
            onChange={(e) => setFilter((f) => ({ ...f, maxScore: Number(e.target.value) }))}
            className="w-16 border border-slate-300 rounded px-2 py-1 text-sm"
          />
        </div>
      </div>

      <div className="space-y-3">
        {filtered.map((ref) => (
          <div
            key={ref.application_id}
            className="border border-slate-200 rounded-xl p-4 bg-white hover:border-blue-300 transition-colors"
          >
            <div className="flex items-center justify-between mb-3">
              <span className={`px-3 py-1 rounded-full text-sm font-bold ${scoreColor(ref.final_score)}`}>
                Score {ref.final_score.toFixed(0)}
              </span>
              <span className="text-xs text-slate-400">
                {new Date(ref.received_at).toLocaleTimeString()}
              </span>
            </div>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div>
                <p className="text-xs text-slate-500">Amount</p>
                <p className="font-semibold text-slate-800">₹{(ref.amount / 1000).toFixed(0)}K</p>
              </div>
              <div>
                <p className="text-xs text-slate-500">DTI</p>
                <p className="font-semibold">{(ref.dti * 100).toFixed(1)}%</p>
              </div>
              <div>
                <p className="text-xs text-slate-500">Employment</p>
                <p className="font-semibold">{ref.employment_months}mo</p>
              </div>
            </div>
          </div>
        ))}
        {filtered.length === 0 && (
          <div className="text-center py-16 text-slate-400">
            No referrals match current filters
          </div>
        )}
      </div>
    </div>
  );
}
