"""
Standalone ONNX export — run after training if you have a saved booster.
"""
import lightgbm as lgb
from onnxmltools import convert_lightgbm
from onnxconverter_common.data_types import FloatTensorType
import boto3
import sys


def export(booster_path: str, bucket: str, s3_key: str):
    booster = lgb.Booster(model_file=booster_path)
    n_features = booster.num_feature()
    initial_types = [("input", FloatTensorType([None, n_features]))]
    onnx_model = convert_lightgbm(booster, initial_types=initial_types)
    local_path = "/tmp/lgbm.onnx"
    with open(local_path, "wb") as f:
        f.write(onnx_model.SerializeToString())
    boto3.client("s3").upload_file(local_path, bucket, s3_key)
    print(f"Exported {n_features}-feature model -> s3://{bucket}/{s3_key}")


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python export_onnx.py <booster_path> <bucket> <s3_key>")
        sys.exit(1)
    export(sys.argv[1], sys.argv[2], sys.argv[3])
