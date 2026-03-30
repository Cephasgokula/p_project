"""
Population Stability Index (PSI) check.
PSI > 0.2 on any top-5 feature -> triggers immediate retrain.
"""
import numpy as np
import pandas as pd
import json
import boto3
import os
import argparse


def compute_psi(baseline: np.ndarray, current: np.ndarray, bins: int = 10) -> float:
    """PSI = sum((actual% - expected%) * ln(actual%/expected%))"""
    eps = 1e-6
    baseline_pct, edges = np.histogram(baseline, bins=bins, density=False)
    current_pct, _ = np.histogram(current, bins=edges, density=False)
    baseline_pct = baseline_pct / (baseline_pct.sum() + eps) + eps
    current_pct = current_pct / (current_pct.sum() + eps) + eps
    return float(
        np.sum((current_pct - baseline_pct) * np.log(current_pct / baseline_pct))
    )


def check_all_features(
    pg_conn_str: str, baseline_s3_key: str = "baseline/features.json"
) -> dict:
    import psycopg2

    conn = psycopg2.connect(pg_conn_str)
    recent_df = pd.read_sql(
        """
        SELECT (a.existing_debt / NULLIF(a.income,0)) AS dti,
               a.employment_months, a.age,
               a.credit_bureau_score
        FROM applicants a
        WHERE a.created_at >= now() - interval '7 days'
    """,
        conn,
    )
    conn.close()

    bucket = os.environ.get("MODEL_BUCKET", "lendiq-models")
    try:
        obj = boto3.client("s3").get_object(Bucket=bucket, Key=baseline_s3_key)
        baseline = json.loads(obj["Body"].read())
    except Exception:
        # No baseline yet — return zeros
        return {col: 0.0 for col in ["dti", "employment_months", "age", "credit_bureau_score"]}

    psi_results = {}
    for col in ["dti", "employment_months", "age", "credit_bureau_score"]:
        base_vals = np.array(baseline.get(col, []))
        current_vals = recent_df[col].dropna().values
        if len(base_vals) > 0 and len(current_vals) > 0:
            psi_results[col] = compute_psi(base_vals, current_vals)
        else:
            psi_results[col] = 0.0

    return psi_results


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--pg-conn", required=True)
    args = parser.parse_args()
    results = check_all_features(args.pg_conn)
    print(json.dumps(results))
