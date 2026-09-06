# HC Webhook — example gRPC servers

End-to-end examples for receiving **Protobuf / gRPC** deliveries from the HC Webhook Android app.

The app calls:

```text
hcwebhook.v1.HealthWebhook / Deliver
```

Your server must return:

```text
DeliverResponse { ok: true }
```

If `ok` is false or the RPC fails, the app retries.

---

## Quick start (Docker — recommended)

From the repo root (or from `server/example`):

```bash
cd server/example

# Python (default demo)
docker compose --profile python up --build

# Or TypeScript
docker compose --profile typescript up --build
```

Then in the app:

1. Delivery format → **Protobuf / gRPC**
2. Target → `http://YOUR_LAN_IP:50051`
3. Tap **Test**

Optional shared-secret auth:

```bash
API_KEY=supersecret docker compose --profile python up --build
```

In the app, add header:

| Key | Value |
|-----|-------|
| `x-api-key` | `supersecret` |

(The app maps custom headers to gRPC metadata. Keys are lowercased.)

---

## Languages

| Language | Folder | Needs codegen? | Easiest local run |
|----------|--------|----------------|-------------------|
| [Python](python/) | `python/` | Yes (`./generate.sh`) | Docker or venv |
| [TypeScript](typescript/) | `typescript/` | No (loads `.proto` at runtime) | Docker or `npm start` |
| [Go](go/) | `go/` | Yes (`./generate.sh`) | `go run .` |
| [PHP](php/) | `php/` | Yes + PHP gRPC extension | Harder; see PHP README |

Schema file (source of truth):

[`../../proto/hcwebhook/v1/health_payload.proto`](../../proto/hcwebhook/v1/health_payload.proto)

The Android app also ships the same file in assets. Use **Share schema (.proto)** in the webhook editor to export it without opening a website.

---

## App setup (checklist)

1. Phone and computer on the **same Wi‑Fi** (for LAN plaintext).
2. Edit a webhook → set **Delivery format** to **Protobuf / gRPC**.
3. Set the target:

   | Goal | Target example |
   |------|----------------|
   | LAN plaintext | `http://192.168.1.10:50051` |
   | TLS production | `https://hooks.example.com` (port 443) |
   | Host only, TLS | `hooks.example.com:443` |

4. Optional auth: add custom header `x-api-key` = your secret (must match server `API_KEY`).
5. Optional: **Share schema (.proto)** and build your own server from it.
6. Tap **Test**.

### How the app chooses TLS vs plaintext

| Target | Transport |
|--------|-----------|
| `https://…` | TLS |
| `host:443` or bare `host` | TLS |
| `http://…` | Plaintext |
| `host:50051` (port ≠ 443) | Plaintext |

---

## Authentication (metadata)

Examples support an optional shared secret via environment variable `API_KEY`.

- Metadata key: `x-api-key`
- App side: Webhook **custom header** with the same key/value
- If `API_KEY` is empty, auth is disabled (handy for first LAN test)

Python:

```bash
API_KEY=supersecret python main.py
```

TypeScript:

```bash
API_KEY=supersecret npm start
```

Docker:

```bash
API_KEY=supersecret docker compose --profile python up --build
```

---

## TLS (production path)

LAN demos use plaintext. Production should terminate TLS.

### Option A — reverse proxy (recommended)

Put Caddy / nginx / Traefik / Cloudflare in front with a real certificate. Proxy HTTP/2 gRPC to the example on `127.0.0.1:50051`.

App target: `https://your.domain`

### Option B — server TLS files (Python / TypeScript examples)

1. Create a certificate (self-signed demo only):

```bash
cd server/example
./scripts/gen-self-signed-tls.sh
```

2. Run with cert paths:

```bash
cd python
TLS_CERT_FILE=../tls/server.crt TLS_KEY_FILE=../tls/server.key python main.py
```

3. App target: `https://YOUR_HOST:50051`

**Important:** Android will reject untrusted self-signed certs unless you install a user CA or use a publicly trusted cert. For real devices, prefer Option A with Let’s Encrypt (or similar).

Docker + TLS mount:

```bash
TLS_CERT_FILE=/tls/server.crt TLS_KEY_FILE=/tls/server.key \
  docker compose --profile python up --build
```

(`docker-compose.yml` mounts `./tls` at `/tls`.)

---

## Success and failure

| Server behavior | App result |
|-----------------|------------|
| RPC OK and `ok: true` | Success |
| RPC OK and `ok: false` | Failure (retry) |
| `UNAUTHENTICATED` / other gRPC error | Failure (retry rules apply) |
| Connection refused / timeout | Failure (retry) |

---

## Field reference

Message fields match the JSON webhook schema documented in:

[`docs/webhook.md`](../../docs/webhook.md)

---

## Troubleshooting

| Symptom | Check |
|---------|--------|
| Connection refused | Server listening? Same Wi‑Fi? Correct LAN IP? Firewall? |
| TLS / SSL errors | Use plaintext `http://IP:50051` on LAN, or a trusted cert |
| `UNAUTHENTICATED` | Header `x-api-key` matches server `API_KEY` |
| `ok=false` | Server must return `ok: true` |
| Proto import errors (Python/Go) | Re-run `./generate.sh` |
| TypeScript “Proto not found” | Run `npm start` from `server/example/typescript` |

---

## Layout

```text
server/example/
  README.md                 ← this file
  docker-compose.yml
  scripts/gen-self-signed-tls.sh
  python/                   ← Dockerfile + generate.sh + main.py
  typescript/               ← Dockerfile + src/server.ts
  go/
  php/
  tls/                      ← created by gen-self-signed-tls.sh (gitignored)
```
