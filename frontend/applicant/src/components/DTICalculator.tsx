import React, { useState } from "react";

export default function DTICalculator() {
  const [income, setIncome] = useState(0);
  const [debt, setDebt] = useState(0);
  const [amount, setAmount] = useState(0);
  const [term, setTerm] = useState(36);

  const dti = income > 0 ? (debt / income) * 100 : 0;
  const emi = term > 0 ? amount * 0.009 : 0;
  const dtiWithEMI = income > 0 ? ((debt + emi) / income) * 100 : 0;

  const dtiColor = (v: number) =>
    v < 30 ? "text-green-600" : v < 40 ? "text-amber-500" : "text-red-600";

  return (
    <div className="bg-slate-50 border border-slate-200 rounded-xl p-5">
      <h4 className="font-semibold text-slate-700 mb-4">
        Real-time DTI Calculator
      </h4>
      <div className="grid grid-cols-2 gap-4 mb-4">
        {[
          { label: "Monthly Income (₹)", val: income, set: setIncome },
          { label: "Existing Debt (₹)", val: debt, set: setDebt },
          { label: "Loan Amount (₹)", val: amount, set: setAmount },
          { label: "Tenure (months)", val: term, set: setTerm },
        ].map(({ label, val, set }) => (
          <div key={label}>
            <label className="text-xs text-slate-500 font-medium">{label}</label>
            <input
              type="number"
              value={val || ""}
              onChange={(e) => set(Number(e.target.value))}
              className="w-full mt-1 border border-slate-300 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
        ))}
      </div>
      <div className="grid grid-cols-2 gap-4 text-center">
        <div className="bg-white rounded-lg p-3 border border-slate-200">
          <p className="text-xs text-slate-500">Current DTI</p>
          <p className={`text-2xl font-bold ${dtiColor(dti)}`}>
            {dti.toFixed(1)}%
          </p>
        </div>
        <div className="bg-white rounded-lg p-3 border border-slate-200">
          <p className="text-xs text-slate-500">DTI after loan</p>
          <p className={`text-2xl font-bold ${dtiColor(dtiWithEMI)}`}>
            {dtiWithEMI.toFixed(1)}%
          </p>
        </div>
      </div>
      <p className="text-xs text-slate-400 mt-3">
        Target: DTI after loan &lt; 40% for best approval odds.
      </p>
    </div>
  );
}
