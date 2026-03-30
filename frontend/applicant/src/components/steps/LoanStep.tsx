import React, { useState } from "react";
import { ApplicantForm } from "../../types";

interface Props {
  form: Partial<ApplicantForm>;
  onNext: (data: Partial<ApplicantForm>) => void;
}

const PURPOSES = [
  { value: "home", label: "Home Loan" },
  { value: "vehicle", label: "Vehicle Loan" },
  { value: "personal", label: "Personal Loan" },
  { value: "business", label: "Business Loan" },
];

export default function LoanStep({ form, onNext }: Props) {
  const [amount, setAmount] = useState(form.amount || 0);
  const [term, setTerm] = useState(form.term_months || 36);
  const [purpose, setPurpose] = useState(form.purpose || "personal");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onNext({ amount, term_months: term, purpose });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-slate-600 mb-1">
          Loan Amount (₹)
        </label>
        <input
          type="number"
          required
          min={1000}
          max={10000000}
          value={amount || ""}
          onChange={(e) => setAmount(Number(e.target.value))}
          className="w-full border border-slate-300 rounded-lg px-4 py-2.5
                     focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-600 mb-1">
          Tenure (months)
        </label>
        <input
          type="number"
          required
          min={3}
          max={360}
          value={term}
          onChange={(e) => setTerm(Number(e.target.value))}
          className="w-full border border-slate-300 rounded-lg px-4 py-2.5
                     focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-600 mb-2">
          Loan Purpose
        </label>
        <div className="grid grid-cols-2 gap-3">
          {PURPOSES.map((p) => (
            <button
              key={p.value}
              type="button"
              onClick={() => setPurpose(p.value)}
              className={`p-3 rounded-xl border text-sm font-medium transition-all
                ${
                  purpose === p.value
                    ? "border-blue-500 bg-blue-50 text-blue-700"
                    : "border-slate-200 text-slate-600 hover:border-slate-300"
                }`}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>
      <button
        type="submit"
        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold 
                   py-2.5 rounded-xl transition-colors"
      >
        Continue
      </button>
    </form>
  );
}
