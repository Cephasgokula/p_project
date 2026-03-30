"""
gRPC server: exposes ScoringService backed by LightGBM (ONNX) + AIF360.
Target: p99 < 8ms per Score() call.
"""
import grpc
import concurrent.futures
import os
import sys
import logging
import time

# Add parent to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from scoring import scoring_pb2, scoring_pb2_grpc
from scoring.lgbm_scorer import LGBMScorer
from fairness.aif360_pipeline import FairnessAdjuster
from prometheus_client import start_http_server, Histogram, Counter

logger = logging.getLogger(__name__)

SCORE_LATENCY = Histogram(
    "lendiq_score_latency_seconds",
    "gRPC Score() latency",
    buckets=[0.001, 0.002, 0.005, 0.008, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5],
)
SCORE_COUNT = Counter("lendiq_score_total", "Total scoring requests")


class ScoringServicer(scoring_pb2_grpc.ScoringServiceServicer):
    def __init__(self):
        self.scorer = LGBMScorer()
        self.adjuster = FairnessAdjuster()
        logger.info("ScoringServicer ready")

    def Score(self, request, context):
        t0 = time.perf_counter()
        try:
            SCORE_COUNT.inc()
            result = self.scorer.score(request)
            lgbm_score = result["lgbm_score"]
            default_prob = result["default_prob"]
            shap_values = result["shap_values"]

            adj_prob = self.adjuster.adjust(
                default_prob=default_prob,
                gender=1,
                age_grp=30,
                geo=1,
            )
            fairness_score = round((1 - adj_prob) * 1000, 2)

            latency = time.perf_counter() - t0
            SCORE_LATENCY.observe(latency)

            return scoring_pb2.ScoringResponse(
                application_id=request.application_id,
                lgbm_score=lgbm_score,
                default_prob=default_prob,
                fairness_score=fairness_score,
                model_version=os.environ.get("MODEL_VERSION", "1.0.0"),
                shap_values=shap_values,
            )
        except Exception as e:
            logger.exception("Score() failed for %s", request.application_id)
            return scoring_pb2.ScoringResponse(
                application_id=request.application_id, error=str(e)
            )

    def ScoreBatch(self, request_iterator, context):
        """Streaming batch scoring."""
        for request in request_iterator:
            yield self.Score(request, context)

    def GetModelInfo(self, request, context):
        return scoring_pb2.ModelInfoResponse(
            lgbm_version=os.environ.get("MODEL_VERSION", "1.0.0"),
            gnn_version=os.environ.get("GNN_VERSION", "1.0.0"),
            last_retrain=os.environ.get("LAST_RETRAIN", "unknown"),
            current_auc_roc=float(os.environ.get("MODEL_AUC", "0.82")),
        )


def serve():
    start_http_server(9090)

    server = grpc.server(
        concurrent.futures.ThreadPoolExecutor(max_workers=8),
        options=[
            ("grpc.max_receive_message_length", 4 * 1024 * 1024),
            ("grpc.keepalive_time_ms", 10000),
        ],
    )
    scoring_pb2_grpc.add_ScoringServiceServicer_to_server(ScoringServicer(), server)
    port = os.environ.get("GRPC_PORT", "50051")
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    logger.info("gRPC ScoringService listening on :%s", port)
    server.wait_for_termination()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    serve()


if __name__ == "__main__":
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    serve()
