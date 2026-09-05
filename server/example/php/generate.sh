#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PROTO_DIR="$ROOT/proto"
OUT_DIR="$(cd "$(dirname "$0")" && pwd)/gen"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Message classes (protobuf)
protoc \
  -I "$PROTO_DIR" \
  --php_out="$OUT_DIR" \
  "$PROTO_DIR/hcwebhook/v1/health_payload.proto"

# gRPC service stubs (requires protoc-gen-php-grpc on PATH)
if ! command -v protoc-gen-php-grpc >/dev/null 2>&1; then
  echo "protoc-gen-php-grpc not found on PATH."
  echo "Install the gRPC PHP plugin, then re-run ./generate.sh"
  echo "Messages were still generated under $OUT_DIR"
  exit 1
fi

protoc \
  -I "$PROTO_DIR" \
  --php_out="$OUT_DIR" \
  --php-grpc_out="$OUT_DIR" \
  "$PROTO_DIR/hcwebhook/v1/health_payload.proto"

echo "Generated stubs in $OUT_DIR"
