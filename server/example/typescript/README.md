# TypeScript (Node) gRPC example

Implements `hcwebhook.v1.HealthWebhook/Deliver` and returns `ok: true`.

Loads the `.proto` at runtime — **no codegen step**.

## Requirements

- Node.js 18+ (for local run)
- Or Docker

## Run with Docker (recommended)

From `server/example`:

```bash
docker compose --profile typescript up --build
```

With auth:

```bash
API_KEY=supersecret docker compose --profile typescript up --build
```

## Run locally

```bash
cd server/example/typescript
npm install
npm start
```

You must run `npm start` from this directory so the relative path to `proto/` resolves.

Listen: `0.0.0.0:50051`  
App target: `http://YOUR_LAN_IP:50051`

## Environment variables

| Variable | Default | Meaning |
|----------|---------|---------|
| `PORT` | `50051` | Listen port |
| `API_KEY` | _(empty)_ | If set, require gRPC metadata `x-api-key` |
| `TLS_CERT_FILE` | _(empty)_ | PEM certificate path (enables TLS) |
| `TLS_KEY_FILE` | _(empty)_ | PEM private key path |
| `LOG_FULL_JSON` | _(empty)_ | Set to `1` to also print full JSON to the console |

### Auth example

```bash
API_KEY=supersecret npm start
```

App header: `x-api-key: supersecret`

### TLS example

```bash
cd server/example
./scripts/gen-self-signed-tls.sh
cd typescript
TLS_CERT_FILE=../tls/server.crt TLS_KEY_FILE=../tls/server.key npm start
```

## What it logs

On each `Deliver` call it prints a short summary, then **saves the full payload** as JSON under:

```text
server/example/typescript/received/deliver-<timestamp>.json
```

Open that file to inspect all records. Optional full console dump:

```bash
LOG_FULL_JSON=1 npm start
```

## Proto path

Uses:

```text
../../../proto/hcwebhook/v1/health_payload.proto
```

relative to `server/example/typescript` (the process working directory).
