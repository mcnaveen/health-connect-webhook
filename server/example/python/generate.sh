#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PROTO_DIR="$ROOT/proto"
OUT_DIR="$(cd "$(dirname "$0")" && pwd)/gen"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

python -m grpc_tools.protoc \
  -I "$PROTO_DIR" \
  --python_out="$OUT_DIR" \
  --grpc_python_out="$OUT_DIR" \
  --pyi_out="$OUT_DIR" \
  "$PROTO_DIR/hcwebhook/v1/health_payload.proto"

# Make packages importable
touch "$OUT_DIR/__init__.py"
mkdir -p "$OUT_DIR/hcwebhook" "$OUT_DIR/hcwebhook/v1"
touch "$OUT_DIR/hcwebhook/__init__.py" "$OUT_DIR/hcwebhook/v1/__init__.py"

# grpc_tools writes hcwebhook/v1/*.py under OUT_DIR when paths match package
if [[ -f "$OUT_DIR/hcwebhook/v1/health_payload_pb2.py" ]]; then
  :
elif [[ -f "$OUT_DIR/health_payload_pb2.py" ]]; then
  mv "$OUT_DIR/health_payload_pb2.py" "$OUT_DIR/hcwebhook/v1/"
  mv "$OUT_DIR/health_payload_pb2_grpc.py" "$OUT_DIR/hcwebhook/v1/"
  mv "$OUT_DIR/health_payload_pb2.pyi" "$OUT_DIR/hcwebhook/v1/" 2>/dev/null || true
fi

# Fix relative import inside generated grpc file (Python 3 package layout)
GRPC_FILE="$OUT_DIR/hcwebhook/v1/health_payload_pb2_grpc.py"
if [[ -f "$GRPC_FILE" ]]; then
  if grep -q "import health_payload_pb2 as" "$GRPC_FILE"; then
    sed -i.bak 's/import health_payload_pb2 as/from . import health_payload_pb2 as/' "$GRPC_FILE"
    rm -f "$GRPC_FILE.bak"
  fi
fi

echo "Generated stubs in $OUT_DIR"
