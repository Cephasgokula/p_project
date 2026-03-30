"""
Builds a heterogeneous graph from applicant-device-IP-postcode relationships.
Nodes: applicants, devices, IP subnets, postal codes.
Edges: shared attributes (same device = edge, same IP subnet = edge, etc.)
"""
import torch
from torch_geometric.data import HeteroData
from typing import List, Dict
import numpy as np


def build_graph(applicants: List[Dict]) -> HeteroData:
    """
    applicants: list of dicts with keys:
        id, income, dti, employment_months, age, credit_bureau_score,
        device_fp, ip_hash, postal_code
    """
    data = HeteroData()

    # Applicant node features
    features = np.array(
        [
            [
                a["income"] / 100000,
                a["dti"],
                a["employment_months"] / 120,
                a["age"] / 60,
                a["credit_bureau_score"] / 900,
            ]
            for a in applicants
        ],
        dtype=np.float32,
    )
    data["applicant"].x = torch.tensor(features)

    # Device nodes (deduplicated)
    devices = list({a["device_fp"] for a in applicants})
    device_idx = {d: i for i, d in enumerate(devices)}
    data["device"].x = torch.ones(len(devices), 1)

    # Edges: applicant -> device
    src, dst = [], []
    for i, a in enumerate(applicants):
        src.append(i)
        dst.append(device_idx[a["device_fp"]])
    if src:
        data["applicant", "uses", "device"].edge_index = torch.tensor([src, dst])
        data["device", "rev_uses", "applicant"].edge_index = torch.tensor([dst, src])

    # IP subnet edges (same /24 subnet = connected)
    subnet_groups: Dict[str, List[int]] = {}
    for i, a in enumerate(applicants):
        subnet = a["ip_hash"][:16]
        subnet_groups.setdefault(subnet, []).append(i)
    ip_src, ip_dst = [], []
    for group in subnet_groups.values():
        if len(group) > 1:
            for j in range(len(group)):
                for k in range(j + 1, len(group)):
                    ip_src.extend([group[j], group[k]])
                    ip_dst.extend([group[k], group[j]])
    if ip_src:
        data["applicant", "same_subnet", "applicant"].edge_index = torch.tensor(
            [ip_src, ip_dst]
        )

    return data
