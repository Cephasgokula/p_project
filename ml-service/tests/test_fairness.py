"""Tests for fairness pipeline."""
import pandas as pd
import numpy as np
from fairness.aif360_pipeline import compute_parity_metrics, FairnessAdjuster


def test_compute_parity_metrics_equal():
    df = pd.DataFrame(
        {
            "approved": [True, True, False, False, True, True, False, False],
            "gender_group": [0, 0, 0, 0, 1, 1, 1, 1],
            "age_group": [20, 20, 30, 30, 20, 20, 30, 30],
            "geo_tier": [1, 1, 1, 1, 1, 1, 1, 1],
        }
    )
    metrics = compute_parity_metrics(df)
    assert "parity_diff_gender_group" in metrics
    # Equal groups should have ~0 parity diff
    assert metrics["parity_diff_gender_group"] < 0.01


def test_compute_parity_metrics_unequal():
    df = pd.DataFrame(
        {
            "approved": [True, True, True, True, False, False, False, False],
            "gender_group": [0, 0, 0, 0, 1, 1, 1, 1],
        }
    )
    metrics = compute_parity_metrics(df)
    assert metrics["parity_diff_gender_group"] == 1.0


def test_fairness_adjuster_no_fit():
    adjuster = FairnessAdjuster()
    # Without fitting, should return the same probability
    result = adjuster.adjust(0.3, gender=1, age_grp=30, geo=1)
    assert result == 0.3
