"""
SHAP value extraction wrapper.
Provides a unified interface for computing SHAP explanations
across different model types (LightGBM, GNN).
"""
import numpy as np
import logging

logger = logging.getLogger(__name__)


class ShapExplainer:
    """Wraps SHAP TreeExplainer for LightGBM models."""

    def __init__(self, booster):
        import shap
        self.explainer = shap.TreeExplainer(booster)
        logger.info("SHAP TreeExplainer initialized")

    def explain(self, features: np.ndarray, feature_names: list) -> dict:
        """
        Compute SHAP values for a single prediction.
        Returns dict mapping feature_name -> SHAP contribution.
        """
        try:
            shap_values = self.explainer.shap_values(features)
            if isinstance(shap_values, list):
                vals = shap_values[0]  # binary classification: class 0
            else:
                vals = shap_values
            if vals.ndim > 1:
                vals = vals[0]
            return {name: float(v) for name, v in zip(feature_names, vals)}
        except Exception as e:
            logger.warning("SHAP explain failed: %s", e)
            return {}
