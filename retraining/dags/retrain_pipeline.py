"""
Weekly retraining DAG.
Schedule: every Sunday 02:00 UTC.
Stages: PSI check -> data prep -> parallel train -> evaluate -> shadow -> promote.
"""
from airflow import DAG
from airflow.operators.python import PythonOperator, BranchPythonOperator
from airflow.operators.bash import BashOperator
from airflow.operators.empty import EmptyOperator
from datetime import datetime, timedelta
import json
import os

DEFAULT_ARGS = {
    "owner": "lendiq-ml",
    "retries": 2,
    "retry_delay": timedelta(minutes=5),
    "email_on_failure": True,
    "email": ["ml-alerts@lendiq.internal"],
}

PG_CONN = "{{ var.value.pg_conn_string }}"
S3_BUCKET = "{{ var.value.ml_model_bucket }}"

with DAG(
    dag_id="lendiq_weekly_retrain",
    default_args=DEFAULT_ARGS,
    schedule_interval="0 2 * * 0",
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=["ml", "retraining"],
) as dag:

    def run_psi_check(**ctx):
        """Returns 'retrain_needed' or 'skip_retrain' branch."""
        import subprocess

        result = subprocess.run(
            [
                "python",
                "/opt/ml-service/scoring/psi_check.py",
                "--pg-conn",
                PG_CONN,
            ],
            capture_output=True,
            text=True,
        )
        psi_scores = json.loads(result.stdout)
        ctx["ti"].xcom_push("psi_scores", psi_scores)
        if any(v > 0.2 for v in psi_scores.values()):
            return "retrain_needed"
        return "skip_retrain"

    psi_check = BranchPythonOperator(
        task_id="psi_drift_check",
        python_callable=run_psi_check,
    )
    skip_retrain = EmptyOperator(task_id="skip_retrain")
    retrain_needed = EmptyOperator(task_id="retrain_needed")

    data_prep = BashOperator(
        task_id="data_preparation",
        bash_command="""
            python /opt/ml-service/scoring/feature_engineering.py \
              --pg-conn {{ var.value.pg_conn_string }} \
              --days 90 \
              --output-path /tmp/train_data.parquet
        """,
    )

    train_lgbm = BashOperator(
        task_id="train_lgbm",
        bash_command="""
            python /opt/ml-service/training/train_lgbm.py \
              --pg-conn {{ var.value.pg_conn_string }} \
              --bucket {{ var.value.ml_model_bucket }}
        """,
    )

    train_gnn = BashOperator(
        task_id="train_gnn",
        bash_command="""
            python /opt/ml-service/training/train_gnn.py \
              --pg-conn {{ var.value.pg_conn_string }} \
              --bucket {{ var.value.ml_model_bucket }} \
              --epochs 30
        """,
    )

    def evaluate_models(**ctx):
        import boto3

        s3 = boto3.client("s3")
        obj = s3.get_object(Bucket=S3_BUCKET, Key="shadow/lgbm_metrics.json")
        metrics = json.loads(obj["Body"].read())
        auc = metrics["auc_roc"]
        ctx["ti"].xcom_push("shadow_auc", auc)
        if auc < 0.80:
            raise ValueError(f"Shadow AUC {auc:.4f} < 0.80 gate — retrain rejected")
        return "shadow_mode"

    evaluate = PythonOperator(
        task_id="evaluate_models",
        python_callable=evaluate_models,
    )

    shadow_mode = BashOperator(
        task_id="shadow_mode_24h",
        bash_command="""
            curl -X POST http://ml-service:50052/admin/shadow/enable \
              -H 'X-Admin-Key: {{ var.value.admin_api_key }}'
            sleep 86400
        """,
        execution_timeout=timedelta(hours=25),
    )

    def promote_shadow(**ctx):
        import boto3

        s3 = boto3.client("s3")
        s3.copy_object(
            Bucket=S3_BUCKET,
            CopySource={"Bucket": S3_BUCKET, "Key": "shadow/lgbm.onnx"},
            Key="champion/lgbm.onnx",
        )
        s3.copy_object(
            Bucket=S3_BUCKET,
            CopySource={"Bucket": S3_BUCKET, "Key": "shadow/gnn.pt"},
            Key="champion/gnn.pt",
        )
        import requests

        requests.post(
            "http://ml-service:50052/admin/model/reload",
            headers={"X-Admin-Key": os.environ.get("ADMIN_API_KEY", "")},
            timeout=30,
        )

    promote = PythonOperator(
        task_id="promote_to_champion",
        python_callable=promote_shadow,
    )

    # DAG Wiring
    psi_check >> [skip_retrain, retrain_needed]
    retrain_needed >> data_prep >> [train_lgbm, train_gnn] >> evaluate
    evaluate >> shadow_mode >> promote
