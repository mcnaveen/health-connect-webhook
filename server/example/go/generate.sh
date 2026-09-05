#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
PROTO_DIR="$ROOT/proto"
OUT_DIR="$(cd "$(dirname "$0")" && pwd)/gen"
PROTO_FILE="hcwebhook/v1/health_payload.proto"
GO_PKG="github.com/mcnaveen/health-connect-webhook/server/example/go/gen/hcwebhook/v1"

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

protoc \
  -I "$PROTO_DIR" \
  --go_out="$OUT_DIR" \
  --go_opt="M${PROTO_FILE}=${GO_PKG}" \
  --go_opt=paths=source_relative \
  --go-grpc_out="$OUT_DIR" \
  --go-grpc_opt="M${PROTO_FILE}=${GO_PKG}" \
  --go-grpc_opt=paths=source_relative \
  "$PROTO_DIR/$PROTO_FILE"

echo "Generated stubs in $OUT_DIR"
