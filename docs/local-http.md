# Local HTTP server reference

When **Local HTTP server** is enabled in the app, HC Webhook listens on your configured TCP port and answers **GET** requests with JSON. The server runs in a **foreground service** (persistent notification) while the feature is on.

Implementation: `LocalHttpServerManager` in `app/src/main/java/com/hcwebhook/app/LocalTcpServerManager.kt`, service in `LocalHttpServerService.kt`. Health JSON is built by `SyncManager.getRealtimeJsonPayload` / `buildJsonPayload` (same shape as [webhook payloads](./webhook.md)).

## Binding and network

| Property | Value |
|----------|--------|
| Listen address | `0.0.0.0` (all interfaces on the device) |
| Port | User-configurable, **1024–65535**; default **8787** if unset or invalid (`PreferencesManager`) |
| Protocol | HTTP/1.1 (not HTTPS) |
| Authentication | None — use only on **trusted LANs** |

Reach the server from another machine with `http://<device-lan-ip>:<port>/…`. Use the phone’s Wi‑Fi IP (not `127.0.0.1` from another host). From the device itself you may use `http://127.0.0.1:<port>/…`.

Reliability depends on Android (Doze, Wi‑Fi sleep, OEM battery limits) and the foreground service staying alive. For automation that must not miss polls, keep the device on power or relax battery optimization for HC Webhook where needed.

## HTTP behavior

| Property | Value |
|----------|--------|
| Supported method | **GET** only |
| Other methods | **405** with JSON `{"status":"error","message":"Only GET is supported"}` |
| Response `Content-Type` | `application/json; charset=utf-8` |
| Connection | `Connection: close` (one response per connection) |
| Client read timeout | 5 seconds per connection |

Request headers after the first line are read until a blank line; they are **not** interpreted (no CORS, auth, or custom routing from headers).

## Endpoints

### `GET /ping`

Liveness check. Does not read Health Connect.

**200** body:

```json
{"status":"ok"}
```

---

### `GET /`

**On-demand Health Connect read** for all **enabled** data types in the app. Does **not** use last-sync watermarks: `lastSyncTimestamps` is always empty, so you get records in the queried time window (see below).

| Query | Effect |
|-------|--------|
| *(none)* | Time range: from **now minus 48 hours** through **now** (same default window as `HealthConnectManager.readHealthData` when `timeRangeDays`, `start`, and `end` are all omitted). |
| `days=<N>` | `N` must be a **positive integer**. Range: from **now minus N days** through **now**. Parsed from the raw query string; only the `days=` parameter is used (other `&`-separated params are ignored if present). |

**200** — full health payload JSON (see [webhook.md](./webhook.md)): always includes `timestamp` and `app_version`; other keys only if that type has data.

**500** — JSON such as `{"status":"error","message":"…"}` when reading fails (e.g. Health Connect error, no data types enabled). The message is taken from the exception; escape handling is minimal, so very unusual error text could produce imperfect JSON.

**Side effect:** a successful **`GET /`** also **updates the in-memory “latest” cache** (`LocalHttpServerManager.publishPayload`) with the same JSON returned to the client.

---

### `GET /latest`

Returns the **last published** JSON string from memory — whatever was last stored by:

- a successful **`GET /`**, or  
- a successful **webhook sync** (`performSync`), which also calls `publishPayload`.

**200** body:

- The cached payload string (valid JSON), **or**
- If nothing was ever published in this process: `{"status":"no_data"}`

**Does not** hit Health Connect on its own; use **`GET /`** for a fresh read.

---

### Any other path

**404** body:

```json
{"status":"error","message":"Use / for data or /ping"}
```

*(The message does not mention `/latest`, but `/latest` is supported as above.)*

---

## Relationship to webhooks

| Aspect | Webhooks (POST) | Local HTTP (`GET /`) |
|--------|------------------|------------------------|
| Trigger | Interval / schedule / manual sync | Your client polls |
| Time window (default) | Last **48 h** with **incremental** per-type watermarks | Last **48 h**, **full** read in window (no watermarks) |
| `GET /?days=N` | N/A | **N** full days back from now |
| JSON schema | [webhook.md](./webhook.md) | Same |

`GET /` uses `SyncManager.getRealtimeJsonPayload`; background sync uses `performSync` with incremental timestamps when no explicit range is passed (see [webhook.md](./webhook.md)).

## Requirements on the device

- Local HTTP enabled in settings and foreground service running.  
- Health Connect available and permissions granted for the types you enabled.  
- At least one **data type** enabled in the app; otherwise `GET /` can fail with an error (no types to read).

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
GET /latest HTTP/1.1
Host: 192.168.1.25:8787
```

## Schema stability

Same as webhooks: use `app_version` in the JSON body and the field reference in [webhook.md](./webhook.md).
