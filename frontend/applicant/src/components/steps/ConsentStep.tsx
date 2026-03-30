import React, { useState } from "react";
import { ApplicantForm } from "../../types";

interface Props {
  form: Partial<ApplicantForm>;
  onNext: (data: Partial<ApplicantForm>) => void;
  loading?: boolean;
}

export default function ConsentStep({ form, onNext, loading }: Props) {
  const [consent, setConsent] = useState(false);
  const [terms, setTerms] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (consent && terms) {
      onNext({});
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="bg-slate-50 rounded-xl p-5 space-y-4">
        <h3 className="font-semibold text-slate-700">Application Summary</h3>
        <div className="grid grid-cols-2 gap-3 text-sm">
          <div>
            <span className="text-slate-500">Name:</span>{" "}
            <span className="font-medium">{form.full_name}</span>
          </div>
          <div>
            <span className="text-slate-500">Age:</span>{" "}
            <span className="font-medium">{form.age}</span>
          </div>
          <div>
            <span className="text-slate-500">Income:</span>{" "}
            <span className="font-medium">₹{form.income?.toLocaleString()}</span>
          </div>
          <div>
            <span className="text-slate-500">Amount:</span>{" "}
            <span className="font-medium">₹{form.amount?.toLocaleString()}</span>
          </div>
          <div>
            <span className="text-slate-500">Purpose:</span>{" "}
            <span className="font-medium capitalize">{form.purpose}</span>
          </div>
          <div>
            <span className="text-slate-500">Tenure:</span>{" "}
            <span className="font-medium">{form.term_months} months</span>
          </div>
        </div>
      </div>

      <label className="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          checked={consent}
          onChange={(e) => setConsent(e.target.checked)}
          className="mt-1 h-4 w-4 rounded border-slate-300 text-blue-600
                     focus:ring-blue-500"
        />
        <span className="text-sm text-slate-600">
          I consent to a credit bureau check as part of this application.
        </span>
      </label>

      <label className="flex items-start gap-3 cursor-pointer">
        <input
          type="checkbox"
          checked={terms}
          onChange={(e) => setTerms(e.target.checked)}
          className="mt-1 h-4 w-4 rounded border-slate-300 text-blue-600
                     focus:ring-blue-500"
        />
        <span className="text-sm text-slate-600">
          I accept the terms and conditions of LendIQ.
        </span>
      </label>

      <button
        type="submit"
        disabled={!consent || !terms || loading}
        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold 
                   py-2.5 rounded-xl transition-colors disabled:opacity-50"
      >
        {loading ? "Submitting…" : "Submit Application"}
      </button>
    </form>
  );
}
