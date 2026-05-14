# Local HTTP server reference

When **Local HTTP server** is enabled in the app, HC Webhook listens on your configured TCP port and answers requests with JSON. The server runs in a **foreground service** (persistent notification) while the feature is on.

Implementation: `LocalHttpServerManager` in `app/src/main/java/com/hcwebhook/app/LocalTcpServerManager.kt`, service in `LocalHttpServerService.kt`. Health JSON is built by `SyncManager.getRealtimeJsonPayload` / `buildJsonPayload` (same shape as [webhook payloads](./webhook.md)).

## Binding and network

| Property | Value |
|----------|--------|
| Listen address | `0.0.0.0` (all interfaces on the device) |
| Port | User-configurable, **1024–65535**; default **8787** if unset or invalid (`PreferencesManager`) |
| Protocol | HTTP/1.1 (not HTTPS) |
| Authentication | Optional Bearer token — see auth section below |

Reach the server from another machine with `http://<device-lan-ip>:<port>/…`. Use the phone's Wi‑Fi IP (not `127.0.0.1` from another host). From the device itself you may use `http://127.0.0.1:<port>/…`.

Reliability depends on Android (Doze, Wi‑Fi sleep, OEM battery limits) and the foreground service staying alive. For automation that must not miss polls, keep the device on power or relax battery optimization for HC Webhook where needed.

## HTTP behavior

| Property | Value |
|----------|--------|
| Supported methods | **GET**, **POST**, **OPTIONS** |
| Response `Content-Type` | `application/json; charset=utf-8` |
| Connection | `Connection: close` (one response per connection) |
| Client read timeout | 5 seconds per connection |
| CORS | `Access-Control-Allow-Origin: *` on all responses; OPTIONS preflight returns 204 |

## Authentication

If **Local HTTP auth** is enabled in settings, every request (except OPTIONS preflight) must include:

```
Authorization: Bearer <token>
```

The token is generated in settings and can be regenerated at any time. Without it, the server returns **401**.

## Endpoints

### `GET /ping`

Liveness check. Does not read Health Connect.

**200** body:

```json
{"status":"ok"}
```

---

### `GET /`

**On-demand Health Connect read** for all **enabled** data types in the app. Does **not** use last-sync watermarks: always returns records in the queried time window.

| Query | Effect |
|-------|--------|
| *(none)* | Time range: from **now minus 48 hours** through **now** |
| `days=<N>` | `N` must be a **positive integer**. Range: from **now minus N days** through **now** |

**200** — full health payload JSON (see [webhook.md](./webhook.md)).

**500** — `{"status":"error","message":"…"}` when reading fails.

**Side effect:** a successful `GET /` also updates the in-memory "latest" cache.

---

### `GET /latest`

Returns the **last published** JSON string from memory — whatever was last stored by a successful `GET /` or a background webhook sync.

**200** — cached payload, or `{"status":"no_data"}` if nothing was ever published in this process.

---

### `GET /logs`

Returns webhook dispatch logs stored on device.

| Query | Effect |
|-------|--------|
| `limit=<N>` | Max entries to return (1–500). Default **50** |
| `success=true\|false` | Filter to successful or failed entries only |
| `dataType=<type>` | Filter by data type (e.g. `steps`, `heart_rate`) |
| `since=<timestamp>` | Only entries with `timestamp` ≥ this Unix ms value |

**200** body:

```json
{
  "status": "ok",
  "count": 3,
  "logs": [
    {
      "id": "…",
      "timestamp": 1715000000000,
      "url": "https://example.com",
      "success": true,
      "statusCode": 200,
      "dataType": "steps",
      "recordCount": 42,
      "responseTimeMs": 312,
      "syncType": "auto",
      "errorMessage": null
    }
  ]
}
```

Logs are ordered newest-first (same order as they appear in the app). The device stores at most 100 logs total.

> **`url` is redacted to `scheme://host` only.** Path segments and query params are stripped so embedded API keys and tokens (e.g. IFTTT `/with/key/…`, `?token=…`) are never exposed over the network.

---

### `GET /stats`

Aggregate summary computed from all stored logs.

**200** body:

```json
{
  "status": "ok",
  "total": 48,
  "success": 45,
  "failure": 3,
  "successRatePct": 93,
  "avgResponseTimeMs": 284,
  "lastSyncTime": 1715000000000
}
```

`avgResponseTimeMs` and `lastSyncTime` are `null` if no data is available.

---

### `GET /health`

Server and sync status overview.

**200** body:

```json
{
  "status": "ok",
  "serverUptimeMs": 3600000,
  "lastSyncTime": 1715000000000,
  "lastSuccessfulWebhookTime": 1715000000000,
  "totalLogs": 48,
  "lastSyncSummary": "42 steps, 1 sleep session"
}
```

`lastSyncTime`, `lastSuccessfulWebhookTime`, and `lastSyncSummary` are `null` if not yet available.

---

### `POST /sync`

Triggers an on-demand sync and dispatches to all enabled webhooks. Equivalent to pressing **Sync now** in the app.

| Query | Effect |
|-------|--------|
| *(none)* | Uses the default time range (last 48 h with per-type watermarks) |
| `days=<N>` | Syncs the last N full days |

Request body is ignored.

**200** body (success):

```json
{"status":"ok","result":"success","counts":{"steps":42,"sleep":1}}
```

```json
{"status":"ok","result":"no_data"}
```

```json
{"status":"ok","result":"no_matching_data"}
```

**500** body (sync error):

```json
{"status":"error","message":"…"}
```

> **Note:** this request blocks until the sync and all webhook dispatches complete. Health Connect reads and outbound HTTP posts can take several seconds — configure your HTTP client timeout accordingly (10–30 s recommended).

---

### `GET /server-logs`

In-memory HTTP access log for the local server itself — one entry per request received, capped at the **200 most recent** entries. Entries are lost on server restart.

| Query | Effect |
|-------|--------|
| `limit=<N>` | Max entries to return (1–500). Default **50** |
| `since=<timestamp>` | Only entries with `timestamp` ≥ this Unix ms value |
| `method=GET\|POST` | Filter by HTTP method |
| `path=/sync` | Filter by exact request path |

**200** body:

```json
{
  "status": "ok",
  "count": 2,
  "logs": [
    {
      "id": "…",
      "timestamp": 1715000000000,
      "method": "POST",
      "path": "/sync",
      "statusCode": 200,
      "responseTimeMs": 1843,
      "clientIp": "192.168.1.10"
    }
  ]
}
```

Entries are ordered newest-first. Note that calling `/server-logs` itself is recorded in the log.

---

### Any other path / method

**404** body:

```json
{"status":"error","message":"Unknown endpoint. Available: GET /, /ping, /latest, /logs, /stats, /health, /server-logs; POST /sync"}
```

---

## Relationship to webhooks

| Aspect | Webhooks (POST) | Local HTTP (`GET /`) |
|--------|------------------|------------------------|
| Trigger | Interval / schedule / manual sync | Your client polls |
| Time window (default) | Last **48 h** with **incremental** per-type watermarks | Last **48 h**, **full** read in window (no watermarks) |
| `GET /?days=N` | N/A | **N** full days back from now |
| JSON schema | [webhook.md](./webhook.md) | Same |

`GET /` uses `SyncManager.getRealtimeJsonPayload`; `POST /sync` and background sync use `performSync` with incremental timestamps when no explicit range is passed.

`GET /logs` returns outbound webhook dispatch logs (stored in `PreferencesManager`, persisted across restarts). `GET /server-logs` returns inbound HTTP request logs (in-memory only, cleared on server restart).

## Requirements on the device

- Local HTTP enabled in settings and foreground service running.
- Health Connect available and permissions granted for the types you enabled.
- At least one **data type** enabled in the app; otherwise `GET /` and `POST /sync` can fail.

## Example requests

```http
GET /ping HTTP/1.1
Host: 192.168.1.25:8787
```

```http
GET /?days=7 HTTP/1.1
Host: 192.168.1.25:8787
```

```http
GET /logs?success=false&limit=10 HTTP/1.1
Host: 192.168.1.25:8787
```

```http
GET /stats HTTP/1.1
Host: 192.168.1.25:8787
```

```http
GET /health HTTP/1.1
Host: 192.168.1.25:8787
```

```http
POST /sync?days=1 HTTP/1.1
Host: 192.168.1.25:8787
Authorization: Bearer your-token-here
Content-Length: 0
```

```http
GET /server-logs?limit=20 HTTP/1.1
Host: 192.168.1.25:8787
```

```http
GET /server-logs?path=/sync&since=1715000000000 HTTP/1.1
Host: 192.168.1.25:8787
```

## Schema stability

Same as webhooks: use `app_version` in the JSON body and the field reference in [webhook.md](./webhook.md).
