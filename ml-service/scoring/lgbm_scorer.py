import onnxruntime as ort
import numpy as np
import shap
import boto3
import os
import logging
from pathlib import Path
from scoring.feature_engineering import build_features, FEATURE_NAMES

logger = logging.getLogger(__name__)


class LGBMScorer:
    """
    Loads the champion LightGBM model from S3 (ONNX format) on startup.
    Uses ONNX Runtime for in-process inference — no model server, no extra hop.
    Target: p99 inference < 8ms.
    """

    def __init__(self):
        model_path = self._download_model()
        opts = ort.SessionOptions()
        opts.intra_op_num_threads = 2
        opts.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL
        self.session = ort.InferenceSession(model_path, opts)
        self.input_name = self.session.get_inputs()[0].name
        logger.info("ONNX model loaded: %s", model_path)

    def _download_model(self) -> str:
        local = os.environ.get("MODEL_PATH")
        if local and Path(local).exists():
            return local

        bucket = os.environ.get("MODEL_BUCKET", "lendiq-models")
        key = os.environ.get("CHAMPION_PATH", "champion/lgbm.onnx")
        local = f"/tmp/{Path(key).name}"
        if not Path(local).exists():
            boto3.client("s3").download_file(bucket, key, local)
        return local

    def score(self, request) -> dict:
        features = build_features(request)
        outputs = self.session.run(None, {self.input_name: features})
        default_prob = float(outputs[1][0][1])
        lgbm_score = round((1 - default_prob) * 1000, 2)

        shap_vals = self._compute_shap(features)
        return {
            "lgbm_score": lgbm_score,
            "default_prob": default_prob,
            "shap_values": shap_vals,
        }

    def _compute_shap(self, features: np.ndarray) -> dict:
        """TreeExplainer for LightGBM SHAP values."""
        try:
            import lightgbm as lgb
            booster_path = os.environ.get("LGBM_BOOSTER_PATH", "/tmp/lgbm.txt")
            if not Path(booster_path).exists():
                return {}
            booster = lgb.Booster(model_file=booster_path)
            explainer = shap.TreeExplainer(booster)
            vals = explainer.shap_values(features)[0]
            return {name: float(v) for name, v in zip(FEATURE_NAMES, vals)}
        except Exception as e:
            logger.warning("SHAP computation failed: %s", e)
            return {}
