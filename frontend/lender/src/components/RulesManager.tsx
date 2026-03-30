import React, { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import axios from "axios";

interface LenderRules {
  income_min: number;
  income_max: number;
  age_min: number;
  age_max: number;
  score_threshold: number;
  max_loan_amount: number;
}

const API = process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1";

export default function RulesManager({ lenderId }: { lenderId: string }) {
  const { data: current } = useQuery<LenderRules>({
    queryKey: ["lender-rules", lenderId],
    queryFn: () => axios.get(`${API}/lenders/${encodeURIComponent(lenderId)}`).then((r) => r.data),
  });

  const [rules, setRules] = useState<LenderRules>({
    income_min: current?.income_min ?? 30000,
    income_max: current?.income_max ?? 200000,
    age_min: current?.age_min ?? 21,
    age_max: current?.age_max ?? 60,
    score_threshold: current?.score_threshold ?? 650,
    max_loan_amount: current?.max_loan_amount ?? 5000000,
  });

  const mutation = useMutation({
    mutationFn: (r: LenderRules) =>
      axios.put(`${API}/lenders/${encodeURIComponent(lenderId)}/rules`, r),
    onSuccess: () => alert("Rules updated — Interval Tree rebuild triggered"),
  });

  const fields: Array<[keyof LenderRules, string]> = [
    ["income_min", "Min Income (₹/mo)"],
    ["income_max", "Max Income (₹/mo)"],
    ["age_min", "Min Age"],
    ["age_max", "Max Age"],
    ["score_threshold", "Min Credit Score"],
    ["max_loan_amount", "Max Loan Amount (₹)"],
  ];

  return (
    <div className="max-w-xl">
      <h3 className="text-xl font-bold text-slate-800 mb-6">Eligibility Rules</h3>
      <div className="grid grid-cols-2 gap-4 mb-6">
        {fields.map(([key, label]) => (
          <div key={key}>
            <label className="block text-xs font-medium text-slate-500 mb-1">{label}</label>
            <input
              type="number"
              value={rules[key]}
              onChange={(e) => setRules((r) => ({ ...r, [key]: Number(e.target.value) }))}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
        ))}
      </div>
      <div className="bg-blue-50 border border-blue-200 rounded-xl p-4 mb-4 text-sm text-blue-700">
        After saving, the Interval Tree will be rebuilt within 5 seconds.
      </div>
      <button
        onClick={() => mutation.mutate(rules)}
        disabled={mutation.isPending}
        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 rounded-xl transition-colors disabled:opacity-50"
      >
        {mutation.isPending ? "Saving…" : "Save Rules"}
      </button>
    </div>
  );
}
