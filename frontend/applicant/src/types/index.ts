export interface ApplicationRequest {
  applicant_id: string;
  amount: number;
  term_months: number;
  purpose: "home" | "vehicle" | "personal" | "business";
  device_fingerprint: string;
  consent_timestamp: string;
}

export interface ApplicationResponse {
  application_id: string;
  status: "pending" | "approved" | "declined" | "referred";
  final_score?: number;
  outcome?: "APPROVE" | "DECLINE" | "REFER";
  lender?: { id: string; name: string };
  decision_path?: string[];
  shap_values?: Record<string, number>;
  processing_ms?: number;
}

export interface ApplicantForm {
  full_name: string;
  age: number;
  income: number;
  employment_months: number;
  existing_debt: number;
  credit_bureau_score: number;
  amount: number;
  term_months: number;
  purpose: string;
}
