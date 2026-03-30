"""
AIF360 Fairness Pipeline.
At TRAINING time  -> reweight samples so minority groups aren't under-represented.
At INFERENCE time -> post-process predicted score to enforce demographic parity.
"""
from aif360.datasets import BinaryLabelDataset
from aif360.algorithms.preprocessing import Reweighing
from aif360.algorithms.postprocessing import RejectOptionClassifier
import pandas as pd
import numpy as np
import logging

logger = logging.getLogger(__name__)

PROTECTED_ATTRIBUTES = ["gender_group", "age_group", "geo_tier"]
PRIVILEGED_GROUPS = [{"gender_group": 1}]
UNPRIVILEGED_GROUPS = [{"gender_group": 0}]


def compute_sample_weights(df: pd.DataFrame) -> np.ndarray:
    """
    Computes sample weights using AIF360 Reweighing.
    Pass these weights to LightGBM training (sample_weight param).
    """
    dataset = BinaryLabelDataset(
        df=df,
        label_names=["label"],
        protected_attribute_names=PROTECTED_ATTRIBUTES,
        favorable_label=0,
        unfavorable_label=1,
    )
    rw = Reweighing(
        unprivileged_groups=UNPRIVILEGED_GROUPS,
        privileged_groups=PRIVILEGED_GROUPS,
    )
    rw.fit(dataset)
    reweighted = rw.transform(dataset)
    return reweighted.instance_weights


class FairnessAdjuster:
    """
    Wraps a trained RejectOptionClassifier.
    At inference time: takes LightGBM default_prob -> adjusted_prob ->
    fairness_score (0-1000).
    """

    def __init__(self):
        self.roc = None

    def fit(self, val_df: pd.DataFrame, val_probs: np.ndarray):
        """Fit the ROC post-processor on a validation set."""
        dataset_pred = self._to_aif_dataset(val_df, val_probs)
        dataset_true = self._to_aif_dataset(val_df, val_df["label"].values)
        self.roc = RejectOptionClassifier(
            unprivileged_groups=UNPRIVILEGED_GROUPS,
            privileged_groups=PRIVILEGED_GROUPS,
            low_class_thresh=0.4,
            high_class_thresh=0.6,
            metric_name="Statistical parity difference",
            metric_ub=0.10,
            metric_lb=-0.10,
        )
        self.roc.fit(dataset_true, dataset_pred)
        logger.info("FairnessAdjuster fitted on %d samples", len(val_df))

    def adjust(
        self, default_prob: float, gender: int, age_grp: int, geo: int
    ) -> float:
        """Adjust a single LightGBM default_prob for fairness."""
        if self.roc is None:
            return default_prob

        row_df = pd.DataFrame(
            [
                {
                    "default_prob": default_prob,
                    "label": int(default_prob > 0.5),
                    "gender_group": gender,
                    "age_group": age_grp,
                    "geo_tier": geo,
                }
            ]
        )
        dataset = self._to_aif_dataset(row_df, row_df["default_prob"].values)
        adjusted = self.roc.predict(dataset)
        adj_prob = float(adjusted.scores[0][0])
        return adj_prob

    def _to_aif_dataset(self, df, scores) -> BinaryLabelDataset:
        tmp = df.copy()
        tmp["score"] = scores
        tmp["label"] = (scores > 0.5).astype(int)
        return BinaryLabelDataset(
            df=tmp,
            label_names=["label"],
            protected_attribute_names=PROTECTED_ATTRIBUTES,
            scores_names=["score"],
            favorable_label=0,
            unfavorable_label=1,
        )


def compute_parity_metrics(decisions_df: pd.DataFrame) -> dict:
    """
    Called every 1000 decisions by fairness_monitor.py.
    Returns demographic parity difference per protected attribute pair.
    Alert fires if any value > 0.15.
    """
    metrics = {}
    for attr in PROTECTED_ATTRIBUTES:
        if attr not in decisions_df.columns:
            continue
        groups = decisions_df.groupby(attr)["approved"].mean()
        if len(groups) >= 2:
            parity_diff = float(groups.max() - groups.min())
            metrics[f"parity_diff_{attr}"] = parity_diff
            if parity_diff > 0.15:
                logger.warning(
                    "FAIRNESS ALERT: %s parity diff = %.3f > 0.15", attr, parity_diff
                )
    return metrics
