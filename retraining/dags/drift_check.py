"""
Daily lightweight drift check — runs every night at 01:00 UTC.
Does NOT retrain — only fires alerts and pushes PSI metrics to Prometheus.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator
from datetime import datetime, timedelta

with DAG(
    dag_id="lendiq_daily_drift_check",
    schedule_interval="0 1 * * *",
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["ml", "monitoring"],
    default_args={
        "owner": "lendiq-ml",
        "retries": 1,
        "retry_delay": timedelta(minutes=5),
    },
) as dag:

    def daily_psi(**ctx):
        import sys

        sys.path.insert(0, "/opt/ml-service")
        from scoring.psi_check import check_all_features

        psi_scores = check_all_features("{{ var.value.pg_conn_string }}")
        for feat, val in psi_scores.items():
            print(f"PSI {feat}: {val:.4f}")
            if val > 0.2:
                print(f"ALERT: PSI breach on {feat} = {val:.4f}")

    PythonOperator(task_id="compute_psi", python_callable=daily_psi)
