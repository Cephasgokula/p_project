import numpy as np
from dataclasses import dataclass

@dataclass
class ApplicationFeatures:
    dti: float
    income_stability: float
    credit_utilisation: float
    delinquency_recency: int
    loan_purpose_risk: int
    device_trust_score: float
    employment_months: int
    age: int
    credit_bureau_score: float
    existing_loans: int
    requested_amount: float
    term_months: int

PURPOSE_RISK = {"home": 1, "vehicle": 2, "personal": 3, "business": 4}

def build_features(req) -> np.ndarray:
    """
    Convert a ScoringRequest proto into a flat feature vector.
    Feature order must match the ONNX model's input schema.
    """
    dti = req.existing_debt / req.income if req.income > 0 else 1.0
    device_trust = max(0.0, 1.0 - (req.velocity_count / 5.0))
    purpose_risk = PURPOSE_RISK.get(req.loan_purpose, 3)
    income_stability = 0.1
    credit_util = 0.4
    delinquency_recency = 12

    return np.array([[
        dti,
        income_stability,
        credit_util,
        delinquency_recency,
        purpose_risk,
        device_trust,
        req.employment_months,
        req.age,
        req.credit_bureau_score,
        req.existing_loans,
        req.requested_amount,
        req.term_months,
    ]], dtype=np.float32)

FEATURE_NAMES = [
    "dti", "income_stability", "credit_utilisation", "delinquency_recency",
    "loan_purpose_risk", "device_trust_score", "employment_months", "age",
    "credit_bureau_score", "existing_loans", "requested_amount", "term_months",
]
