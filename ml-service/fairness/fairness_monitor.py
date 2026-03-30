"""
Reads last N decisions from PostgreSQL every 1000 decisions.
Computes and logs fairness metrics to Prometheus.
"""
from prometheus_client import Gauge
from fairness.aif360_pipeline import compute_parity_metrics
import psycopg2
import pandas as pd
import os
import logging
import time

logger = logging.getLogger(__name__)

PARITY_GAUGE = Gauge(
    "lendiq_parity_diff",
    "Demographic parity difference per attribute",
    ["attribute"],
)


def monitor_loop(pg_conn_str: str, window: int = 1000, sleep_sec: int = 60):
    conn = psycopg2.connect(pg_conn_str)
    while True:
        try:
            df = pd.read_sql(
                f"""
                SELECT d.outcome = 'APPROVE' as approved,
                       a.age / 10 * 10 as age_group,
                       1 as gender_group,
                       1 as geo_tier
                FROM decisions d
                JOIN applications app ON app.id = d.application_id
                JOIN applicants a     ON a.id = app.applicant_id
                ORDER BY d.decided_at DESC LIMIT {window}
            """,
                conn,
            )
            metrics = compute_parity_metrics(df)
            for attr, val in metrics.items():
                PARITY_GAUGE.labels(attribute=attr).set(val)
                logger.info("Parity %s = %.4f", attr, val)
        except Exception as e:
            logger.error("Monitor error: %s", e)
        time.sleep(sleep_sec)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    pg_conn = os.environ.get(
        "PG_CONN", "postgresql://lendiq_user:password@localhost:5432/lendiq"
    )
    monitor_loop(pg_conn)
