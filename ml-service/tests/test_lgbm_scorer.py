"""Tests for LightGBM scorer feature engineering."""
import numpy as np
from unittest.mock import MagicMock
from scoring.feature_engineering import build_features, FEATURE_NAMES, PURPOSE_RISK


def _make_request(**kwargs):
    defaults = {
        "income": 100000,
        "existing_debt": 30000,
        "employment_months": 36,
        "age": 30,
        "credit_bureau_score": 750,
        "existing_loans": 1,
        "requested_amount": 500000,
        "term_months": 24,
        "loan_purpose": "home",
        "velocity_count": 0,
        "device_fp": "test_device",
    }
    defaults.update(kwargs)
    req = MagicMock()
    for k, v in defaults.items():
        setattr(req, k, v)
    return req


def test_build_features_shape():
    req = _make_request()
    features = build_features(req)
    assert features.shape == (1, 12)
    assert features.dtype == np.float32


def test_dti_calculation():
    req = _make_request(existing_debt=50000, income=100000)
    features = build_features(req)
    dti = features[0][0]
    assert abs(dti - 0.5) < 1e-5


def test_dti_zero_income():
    req = _make_request(income=0)
    features = build_features(req)
    dti = features[0][0]
    assert dti == 1.0


def test_device_trust_score():
    req = _make_request(velocity_count=0)
    features = build_features(req)
    device_trust = features[0][5]
    assert device_trust == 1.0

    req2 = _make_request(velocity_count=5)
    features2 = build_features(req2)
    device_trust2 = features2[0][5]
    assert device_trust2 == 0.0


def test_purpose_risk_mapping():
    for purpose, expected_risk in PURPOSE_RISK.items():
        req = _make_request(loan_purpose=purpose)
        features = build_features(req)
        assert features[0][4] == expected_risk


def test_feature_names_count():
    assert len(FEATURE_NAMES) == 12
