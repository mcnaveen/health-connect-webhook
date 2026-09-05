#!/usr/bin/env bash
# Create a self-signed cert for local TLS testing.
# App target: https://YOUR_LAN_IP:50051 (device must trust this cert — self-signed
# is for server-side demos / reverse proxies; use a real CA cert in production).
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
OUT="$DIR/tls"
mkdir -p "$OUT"

openssl req -x509 -newkey rsa:2048 -nodes \
  -keyout "$OUT/server.key" \
  -out "$OUT/server.crt" \
  -days 365 \
  -subj "/CN=localhost"

echo "Wrote:"
echo "  $OUT/server.crt"
echo "  $OUT/server.key"
echo
echo "Run (Python example):"
echo "  TLS_CERT_FILE=$OUT/server.crt TLS_KEY_FILE=$OUT/server.key python main.py"
echo
echo "Or Docker:"
echo "  TLS_CERT_FILE=/tls/server.crt TLS_KEY_FILE=/tls/server.key docker compose --profile python up --build"
