"""
LightGBM training script.
Run by Airflow retraining DAG.
Reads from PostgreSQL decisions table, trains on 90-day window.
"""
import lightgbm as lgb
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score
import boto3
import os
import json
import argparse
from onnxmltools import convert_lightgbm
from onnxconverter_common.data_types import FloatTensorType


def load_data(pg_conn_str: str, days: int = 90) -> pd.DataFrame:
    import psycopg2

    query = f"""
        SELECT a.income, a.existing_debt, a.employment_months, a.age,
               a.credit_bureau_score,
               app.amount, app.term_months, app.purpose,
               d.outcome
        FROM decisions d
        JOIN applications app ON app.id = d.application_id
        JOIN applicants a     ON a.id = app.applicant_id
        WHERE d.decided_at >= now() - interval '{days} days'
          AND d.outcome IN ('APPROVE', 'DECLINE')
    """
    return pd.read_sql(query, psycopg2.connect(pg_conn_str))


def engineer_features(df: pd.DataFrame) -> tuple:
    PURPOSE_MAP = {"home": 1, "vehicle": 2, "personal": 3, "business": 4}
    df["dti"] = df["existing_debt"] / df["income"].clip(lower=1)
    df["purpose_risk"] = df["purpose"].map(PURPOSE_MAP).fillna(3)
    df["label"] = (df["outcome"] == "DECLINE").astype(int)
    features = [
        "dti",
        "employment_months",
        "age",
        "credit_bureau_score",
        "amount",
        "term_months",
        "purpose_risk",
    ]
    return df[features], df["label"]


def train(args):
    df = load_data(args.pg_conn)
    X, y = engineer_features(df)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    params = {
        "objective": "binary",
        "num_leaves": 63,
        "learning_rate": 0.05,
        "n_estimators": 500,
        "min_child_samples": 50,
        "feature_fraction": 0.8,
        "bagging_fraction": 0.8,
        "bagging_freq": 5,
        "reg_alpha": 0.1,
        "reg_lambda": 0.1,
        "metric": "auc",
    }
    model = lgb.LGBMClassifier(**params)
    model.fit(
        X_train,
        y_train,
        eval_set=[(X_test, y_test)],
        callbacks=[lgb.early_stopping(50), lgb.log_evaluation(100)],
    )

    auc = roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])
    print(f"AUC-ROC on holdout: {auc:.4f}")

    # Export to ONNX
    initial_types = [("input", FloatTensorType([None, X_train.shape[1]]))]
    onnx_model = convert_lightgbm(model.booster_, initial_types=initial_types)
    shadow_path = "/tmp/shadow_lgbm.onnx"
    with open(shadow_path, "wb") as f:
        f.write(onnx_model.SerializeToString())

    # Upload shadow model to S3
    boto3.client("s3").upload_file(shadow_path, args.bucket, "shadow/lgbm.onnx")
    print(f"Shadow model uploaded to s3://{args.bucket}/shadow/lgbm.onnx")

    # Save booster as text for SHAP
    booster_path = "/tmp/shadow_lgbm.txt"
    model.booster_.save_model(booster_path)
    boto3.client("s3").upload_file(booster_path, args.bucket, "shadow/lgbm.txt")

    # Save metrics
    metrics = {"auc_roc": auc, "n_train": len(X_train), "n_test": len(X_test)}
    metrics_path = "/tmp/train_metrics.json"
    with open(metrics_path, "w") as f:
        json.dump(metrics, f)
    boto3.client("s3").upload_file(metrics_path, args.bucket, "shadow/lgbm_metrics.json")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--pg-conn", required=True)
    parser.add_argument("--bucket", default="lendiq-models")
    train(parser.parse_args())
