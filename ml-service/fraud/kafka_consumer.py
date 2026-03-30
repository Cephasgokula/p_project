"""
Async Kafka consumer for GNN fraud detection.
Consumes from 'loan-apps' topic, runs GNN, publishes to 'fraud-results'.
Score Aggregator waits up to 50ms for this result.
"""
from kafka import KafkaConsumer, KafkaProducer
from fraud.gnn_detector import GNNFraudDetector
import json
import os
import logging
import time

logger = logging.getLogger(__name__)

BOOTSTRAP = os.environ.get("KAFKA_BOOTSTRAP", "localhost:9092")
IN_TOPIC = "loan-apps"
OUT_TOPIC = os.environ.get("FRAUD_TOPIC", "fraud-results")
GROUP_ID = "gnn-fraud-detector"


def run():
    detector = GNNFraudDetector()
    consumer = KafkaConsumer(
        IN_TOPIC,
        bootstrap_servers=BOOTSTRAP,
        group_id=GROUP_ID,
        value_deserializer=lambda b: json.loads(b.decode()),
        auto_offset_reset="latest",
        enable_auto_commit=True,
    )
    producer = KafkaProducer(
        bootstrap_servers=BOOTSTRAP,
        value_serializer=lambda v: json.dumps(v).encode(),
        acks="all",
    )

    logger.info("GNN Kafka consumer started on %s", IN_TOPIC)
    buffer, batch_timeout = [], 0.02
    last_flush = time.time()

    for msg in consumer:
        app = msg.value
        buffer.append(
            {
                "id": app.get("applicantId", app.get("applicant_id", "unknown")),
                "income": app.get("monthlyIncome", app.get("income", 50000)),
                "dti": app.get("dtiRatio", app.get("dti", 0.3)),
                "employment_months": app.get("employmentMonths", app.get("employment_months", 24)),
                "age": app.get("ageYears", app.get("age", 30)),
                "credit_bureau_score": app.get("creditBureauScore", app.get("credit_bureau_score", 650)),
                "device_fp": app.get("deviceFingerprint", app.get("device_fingerprint", "unknown")),
                "ip_hash": app.get("ipHash", app.get("ip_hash", "unknown")),
                "postal_code": app.get("postal_code", "000000"),
            }
        )

        if time.time() - last_flush >= batch_timeout or len(buffer) >= 50:
            results = detector.detect(buffer)
            for applicant_id, fraud_prob in results.items():
                producer.send(
                    OUT_TOPIC,
                    key=str(applicant_id).encode(),
                    value={
                        "applicant_id": str(applicant_id),
                        "fraud_prob": fraud_prob,
                        "detected_at": time.time(),
                    },
                )
            buffer.clear()
            last_flush = time.time()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    run()
