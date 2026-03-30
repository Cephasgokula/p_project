"""
Graph Attention Network (GAT) for fraud ring detection.
3 layers, 8 attention heads per layer.
"""
import torch
import torch.nn as nn
import torch.nn.functional as F
from torch_geometric.nn import GATConv
from fraud.graph_builder import build_graph
from typing import List, Dict
import boto3
import os
import logging

logger = logging.getLogger(__name__)


class FraudGAT(nn.Module):
    """
    Homogeneous GAT for fraud detection.
    Input:  applicant node features (5-dim)
    Output: fraud probability per node (0-1)
    """

    def __init__(self, in_channels=5, hidden=64, heads=8, dropout=0.3):
        super().__init__()
        self.gat1 = GATConv(in_channels, hidden, heads=heads, dropout=dropout)
        self.gat2 = GATConv(hidden * heads, hidden, heads=heads, dropout=dropout)
        self.gat3 = GATConv(hidden * heads, hidden, heads=1, dropout=dropout)
        self.classifier = nn.Linear(hidden, 1)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x, edge_index):
        x = F.elu(self.gat1(x, edge_index))
        x = self.dropout(x)
        x = F.elu(self.gat2(x, edge_index))
        x = self.dropout(x)
        x = F.elu(self.gat3(x, edge_index))
        return torch.sigmoid(self.classifier(x)).squeeze(-1)


class GNNFraudDetector:
    def __init__(self):
        self.model = self._load_model()
        self.model.eval()

    def _load_model(self) -> FraudGAT:
        path = os.environ.get("GNN_MODEL_PATH", "/tmp/gnn.pt")
        if not os.path.exists(path):
            bucket = os.environ.get("MODEL_BUCKET", "lendiq-models")
            try:
                boto3.client("s3").download_file(bucket, "champion/gnn.pt", path)
            except Exception as e:
                logger.warning("Could not download GNN model: %s. Using random weights.", e)
                model = FraudGAT()
                return model
        model = FraudGAT()
        state = torch.load(path, map_location="cpu", weights_only=True)
        model.load_state_dict(state)
        logger.info("GNN model loaded from %s", path)
        return model

    @torch.no_grad()
    def detect(self, applicants: List[Dict]) -> Dict[str, float]:
        """
        Returns {applicant_id: fraud_probability} for each applicant in the batch.
        """
        if not applicants:
            return {}
        data = build_graph(applicants)

        if ("applicant", "same_subnet", "applicant") in data.edge_types:
            edge_index = data["applicant", "same_subnet", "applicant"].edge_index
        else:
            edge_index = torch.zeros(2, 0, dtype=torch.long)

        probs = self.model(data["applicant"].x, edge_index)
        return {
            applicants[i]["id"]: float(probs[i]) for i in range(len(applicants))
        }
