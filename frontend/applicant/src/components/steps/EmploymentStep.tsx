import React, { useState } from "react";
import { ApplicantForm } from "../../types";

interface Props {
  form: Partial<ApplicantForm>;
  onNext: (data: Partial<ApplicantForm>) => void;
}

export default function EmploymentStep({ form, onNext }: Props) {
  const [income, setIncome] = useState(form.income || 0);
  const [months, setMonths] = useState(form.employment_months || 0);
  const [debt, setDebt] = useState(form.existing_debt || 0);
  const [score, setScore] = useState(form.credit_bureau_score || 0);

  const dti = income > 0 ? ((debt / income) * 100).toFixed(1) : "0.0";

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onNext({
      income,
      employment_months: months,
      existing_debt: debt,
      credit_bureau_score: score,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-600 mb-1">
            Monthly Income (₹)
          </label>
          <input
            type="number"
            required
            min={0}
            value={income || ""}
            onChange={(e) => setIncome(Number(e.target.value))}
            className="w-full border border-slate-300 rounded-lg px-4 py-2.5
                       focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-600 mb-1">
            Employment (months)
          </label>
          <input
            type="number"
            required
            min={0}
            value={months || ""}
            onChange={(e) => setMonths(Number(e.target.value))}
            className="w-full border border-slate-300 rounded-lg px-4 py-2.5
                       focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-slate-600 mb-1">
            Existing Monthly Debt (₹)
          </label>
          <input
            type="number"
            required
            min={0}
            value={debt || ""}
            onChange={(e) => setDebt(Number(e.target.value))}
            className="w-full border border-slate-300 rounded-lg px-4 py-2.5
                       focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-slate-600 mb-1">
            Credit Bureau Score
          </label>
          <input
            type="number"
            required
            min={300}
            max={900}
            value={score || ""}
            onChange={(e) => setScore(Number(e.target.value))}
            className="w-full border border-slate-300 rounded-lg px-4 py-2.5
                       focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>
      </div>
      <div className="bg-slate-50 rounded-lg p-3 text-center">
        <span className="text-sm text-slate-500">Current DTI: </span>
        <span
          className={`font-bold ${
            parseFloat(dti) < 30
              ? "text-green-600"
              : parseFloat(dti) < 40
              ? "text-amber-500"
              : "text-red-600"
          }`}
        >
          {dti}%
        </span>
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
