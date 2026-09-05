# Python gRPC example

Implements `hcwebhook.v1.HealthWebhook/Deliver` and returns `ok: true`.

## Requirements

- Python 3.10+ (for local run)
- Or Docker (no local Python tooling needed)

## Run with Docker (recommended)

From `server/example`:

```bash
docker compose --profile python up --build
```

With auth:

```bash
API_KEY=supersecret docker compose --profile python up --build
```

## Run locally

```bash
cd server/example/python
python3 -m venv .venv
source .venv/bin/activate          # Windows: .venv\Scripts\activate
pip install -r requirements.txt
./generate.sh
python main.py
```

Listen: `[::]:50051`  
App target: `http://YOUR_LAN_IP:50051`

## Environment variables

| Variable | Default | Meaning |
|----------|---------|---------|
| `PORT` | `50051` | Listen port |
| `API_KEY` | _(empty)_ | If set, require gRPC metadata `x-api-key` |
| `TLS_CERT_FILE` | _(empty)_ | PEM certificate path (enables TLS) |
| `TLS_KEY_FILE` | _(empty)_ | PEM private key path |

### Auth example

```bash
API_KEY=supersecret python main.py
```

In the app webhook headers:

- Key: `x-api-key`
- Value: `supersecret`

### TLS example

```bash
cd server/example
./scripts/gen-self-signed-tls.sh
cd python
TLS_CERT_FILE=../tls/server.crt TLS_KEY_FILE=../tls/server.key python main.py
```

App target: `https://YOUR_HOST:50051`  
(Self-signed certs usually need a trusted CA on the phone. Prefer a reverse proxy with Let’s Encrypt for real devices.)

## What it logs

On each `Deliver` call it prints `timestamp`, `app_version`, and counts for `steps`, `heart_rate`, and `sleep`.

## Regenerate stubs

After changing [`proto/hcwebhook/v1/health_payload.proto`](../../../proto/hcwebhook/v1/health_payload.proto):

```bash
./generate.sh
```

Generated code is written to `gen/` (gitignored).
