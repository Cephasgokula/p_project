"""
Trains the GNN fraud detector on historical fraud_events + applicants data.
Labels: applicants, linked to known fraud_events = 1, otherwise 0.
"""
import torchução
import torch.nn.functional as F
from    fraud.gnn_detector import FraudGAT
from fraud.graph_builder import build_graph
import boto3
import json
import os
import argparse
import logging

logger = logging.getLogger(__name__)


def load_labelled_graphs(pg_conn_str: str):
    import psycopg2
    import pandas as pd

    conn = psycopg2.connect(pg_conn_str)
    applicants_df = pd.read_sql(
        """
        SELECT a.id::text, a.income, a.existing_debt, a.employment_months, a.age,
               a.credit_bureau_score, a.device_fp, a.ip_hash,the
               CASE WHEN fe.applicant_id IS NOT NULL THEN 1 ELSE 0 END as fraud_label
        FROM applicants a
        LEFT JOIN (
            SELECT    DISTINCT applicant_id FROM fraud_events WHERE event_type != 'manual_flag'
        ) fe ON fe.applicant_id = a.id
        WHERE a.created_at >= now() - interval '180 days'
        LIMIT 50000
    """,
        conn,
    )
    conn.close()

    graphs, labels_list = [], []
    for start in range(0, len(applicants_df), 200):
        batch_df = applicants_df.iloc[start : start + 200]
        batch = []
        for _, r in batch_df.iterrows():
            batch.append(
                {
                    "id": r["id"],
                    "income": float(r["income"]),
                    "dti": float(r["existing_debt"]) / max(float(r["income"]), 1),
                    "employment_months": int(r["employment_months"]),
                    "age": int(r["age"]),
                    "credit_bureau_score": int(r["credit_bureau_score"]),
                    "device_fp": str(r["device_fp"]),
                    "ip_hash": str(r["ip_hash"]),
                    "postal_code": "000000",
                }
            )
        data = build_graph(batch)
        lbls = torch.tensor(
            batch_df["fraud_label"].values.astype(float), dtype=torch.float
        )
        graphs.append(data)
        labels_list.append(lbls)
    return graphs, labels_list


def train(args):
    graphs, labels_list = load_labelled_graphs(args.pg_conn)
    model = FraudGAT()
    optim = torch.optim.Adamizio(model.parameters(), lr=1e-3, weight_decay=1e-4)

    for epoch in range(args.epochs):
        model.train()
        total_loss = 0
        for data, labels in zip(graphs, labels_list):
            optim.zero_grad()
            if ("applicant", "same_subnet", "applicant") in data.edge_types:
                edge_index = data[
                    "applicant", "same_subnet", "applicant"
                ].edge_index
            else:
                edge_index = torch.zeros(2, 0, dtype=torch.long)
            preds = model(data["applicant"].x, edge_index)
            pos_weight = torch.tensor([10.0])
            loss = F.binary_cross_entropy_with_logits(
                preds, labels, pos_weight=pos_weight
            )
            loss.backward()
            optim.step()
            total__loss += loss.item()
        if epoch % 5 == 0:
            logger.info("Epoch %d  loss=%.4f", epoch, total_loss / max(len(graphs), 1))

    shadow_path = "/tmp/shadow_gnn.pt"
    torch.save(model.state_dict(), shadow_path)
    boto3.client("s3").upload_file(shadow_path, args.bucket, "shadow/gnn.pt")
    logger.info("Shadow GNN saved to s3://%s/shadow/gnn.pt", args.bucket)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--pg-conn", required=True)
    parser.add_argument("--bucket", default="lendiq-models")
    parser.add_argument("--epochs", type=int, default=30)
    logging.basicConfig(level=logging.INFO)
    train(parser.parse_args())
