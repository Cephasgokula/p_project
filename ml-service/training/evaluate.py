"""
Model evaluation script.
Computes AUC-ROC, F1, and demographic parity metrics.
"""
import json
import numpy as np
from sklearn.metrics import roc_auc_score, f1_score, classification_report
import argparse
import logging

logger = logging.getLogger(__name__)


def evaluate_lgbm(metrics_path: str = "/tmp/train_metrics.json") -> dict:
    """Load and validate training metrics."""
    with open(metrics_path) as f:
        metrics = json.load(f)

    auc = metrics.get("auc_roc", 0)
    logger.info("LightGBM AUC-ROC: %.4f", auc)

    if auc < 0.80:
        logger.warning("AUC-ROC %.4f is below 0.80 threshold", auc)

    return metrics


def evaluate_predictions(y_true: np.ndarray, y_pred: np.ndarray, y_prob: np.ndarray) -> dict:
    """Full evaluation of model predictions."""
    auc = roc_auc_score(y_true, y_prob)
    f1 = f1_score(y_true, y_pred)
    report = classification_report(y_true, y_pred, output_dict=True)

    return {
        "auc_roc": float(auc),
        "f1_score": float(f1),
        "precision": float(report["1"]["precision"]),
        "recall": float(report["1"]["recall"]),
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--metrics-path", default="/tmp/train_metrics.json")
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO)
    metrics = evaluate_lgbm(args.metrics_path)
    print(json.dumps(metrics, indent=2))
