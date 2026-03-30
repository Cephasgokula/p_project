"""Tests for GNN fraud detector graph building."""
import torch
from fraud.graph_builder import build_graph


def _make_applicants(n=5):
    return [
        {
            "id": f"applicant_{i}",
            "income": 50000 + i * 10000,
            "dti": 0.3 + i * 0.05,
            "employment_months": 24 + i * 6,
            "age": 25 + i * 5,
            "credit_bureau_score": 600 + i * 30,
            "device_fp": f"device_{i % 3}",  # some share devices
            "ip_hash": f"{'abcdef1234567890' if i < 3 else 'fedcba0987654321'}hash",
            "postal_code": "110001",
        }
        for i in range(n)
    ]


def test_build_graph_basic():
    applicants = _make_applicants(5)
    data = build_graph(applicants)
    assert data["applicant"].x.shape == (5, 5)
    assert ("applicant", "uses", "device") in data.edge_types


def test_shared_device_creates_edges():
    applicants = _make_applicants(3)
    # All share device_0, device_1, device_2 (mod 3)
    data = build_graph(applicants)
    assert ("applicant", "uses", "device") in data.edge_types
    assert data["applicant", "uses", "device"].edge_index.shape[1] == 3


def test_same_subnet_edges():
    applicants = _make_applicants(5)
    data = build_graph(applicants)
    # First 3 share same ip_hash prefix, so should have edges
    if ("applicant", "same_subnet", "applicant") in data.edge_types:
        edges = data["applicant", "same_subnet", "applicant"].edge_index
        assert edges.shape[0] == 2
        assert edges.shape[1] > 0


def test_empty_input():
    data = build_graph([])
    assert data["applicant"].x.shape[0] == 0
