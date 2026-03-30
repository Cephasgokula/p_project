import React, { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { submitApplication, getApplication } from "../api/client";
import { ApplicationResponse, ApplicantForm } from "../types";
import PersonalStep from "./steps/PersonalStep";
import EmploymentStep from "./steps/EmploymentStep";
import LoanStep from "./steps/LoanStep";
import ConsentStep from "./steps/ConsentStep";
import DecisionView from "./DecisionView";

const STEPS = ["Personal Info", "Employment", "Loan Details", "Consent"];

export default function ApplicationWizard() {
  const [step, setStep] = useState(0);
  const [form, setForm] = useState<Partial<ApplicantForm>>({});
  const [appId, setAppId] = useState<string | null>(null);
  const [result, setResult] = useState<ApplicationResponse | null>(null);

  const mutation = useMutation({
    mutationFn: submitApplication,
    onSuccess: async (data: ApplicationResponse) => {
      setAppId(data.application_id);
      const poll = setInterval(async () => {
        try {
          const updated = await getApplication(data.application_id);
          if (updated.status !== "pending") {
            clearInterval(poll);
            setResult(updated);
          }
        } catch {
          clearInterval(poll);
        }
      }, 2000);
    },
  });

  const handleNext = (stepData: Partial<ApplicantForm>) => {
    const updated = { ...form, ...stepData };
    setForm(updated);
    if (step < STEPS.length - 1) {
      setStep((s) => s + 1);
    } else {
      mutation.mutate({
        applicant_id: "demo-uuid",
        amount: updated.amount,
        term_months: updated.term_months,
        purpose: updated.purpose,
        device_fingerprint: navigator.userAgent,
        consent_timestamp: new Date().toISOString(),
      });
    }
  };

  if (result) return <DecisionView response={result} />;

  return (
    <div className="max-w-2xl mx-auto p-6">
      <div className="flex gap-2 mb-8">
        {STEPS.map((_, i) => (
          <div
            key={i}
            className={`flex-1 h-1 rounded-full transition-colors ${
              i <= step ? "bg-blue-600" : "bg-gray-200"
            }`}
          />
        ))}
      </div>
      <h2 className="text-2xl font-bold text-slate-800 mb-6">{STEPS[step]}</h2>

      {step === 0 && <PersonalStep form={form} onNext={handleNext} />}
      {step === 1 && <EmploymentStep form={form} onNext={handleNext} />}
      {step === 2 && <LoanStep form={form} onNext={handleNext} />}
      {step === 3 && (
        <ConsentStep
          form={form}
          onNext={handleNext}
          loading={mutation.isPending}
        />
      )}

      {mutation.isError && (
        <p className="text-red-600 mt-4">Submission failed. Please try again.</p>
      )}
      {appId && !result && (
        <div className="flex items-center gap-3 mt-6 text-slate-600">
          <div className="animate-spin w-5 h-5 border-2 border-blue-600 border-t-transparent rounded-full" />
          Processing your application…
        </div>
      )}
    </div>
  );
}
