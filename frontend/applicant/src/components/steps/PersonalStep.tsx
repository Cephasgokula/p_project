import React, { useState } from "react";
import { ApplicantForm } from "../../types";

interface Props {
  form: Partial<ApplicantForm>;
  onNext: (data: Partial<ApplicantForm>) => void;
}

export default function PersonalStep({ form, onNext }: Props) {
  const [name, setName] = useState(form.full_name || "");
  const [age, setAge] = useState(form.age || 0);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onNext({ full_name: name, age });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-slate-600 mb-1">
          Full Name
        </label>
        <input
          type="text"
          required
          value={name}
          onChange={(e) => setName(e.target.value)}
          className="w-full border border-slate-300 rounded-lg px-4 py-2.5 
                     focus:ring-2 focus:ring-blue-500 focus:outline-none"
          placeholder="Enter your full legal name"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-600 mb-1">
          Age
        </label>
        <input
          type="number"
          required
          min={18}
          max={70}
          value={age || ""}
          onChange={(e) => setAge(Number(e.target.value))}
          className="w-full border border-slate-300 rounded-lg px-4 py-2.5
                     focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
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
