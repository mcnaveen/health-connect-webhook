# HC Webhook plugins (independent server-side gateway)

This experimental extension lives **outside the Android app**. It consumes the existing **JSON POST webhook** contract; no change to `app/`, `SyncManager.kt`, or the upstream app's default settings is necessary. The public fork's `main` should remain a clean upstream mirror. Keep extension work in `dev/plugins` until reviewed.

## Included proof of concept

- `POST /webhook`: authenticated JSON receiver; 16 MiB default limit; 2xx only after all enabled plugins finish.
- `GET /health`: basic liveness, no health data.
- `GET /plugins`: authenticated enabled plugin names.
- `GET /reports/daily?day=YYYY-MM-DD`: authenticated UTC ingestion counts by record type.
- `summary` plugin: stores **only counts and SHA-256 batch digests**, not raw health records or values. Identical retries are counted only once. SQLite remains local.
- Explicit plugin allowlist in `gateway.py`; add new plugin implementations under `hc_plugins/plugins/` and register them in `PluginManager`.

**Reports count accepted records, NOT daily steps, calories, unique Health Connect records, or medical measurements.** A new sync with the same records and a different payload envelope can be counted again. Record-level deduplication, corrections and deletions require additional schema-aware plugins.

## Optional historical record storage (OFF by default)

The `record_store` plugin is **disabled by default**. It stores raw health measurements in a separate local SQLite file, `/data/health_records.sqlite3`, inside the existing Docker named volume (or `.data/health_records.sqlite3` without Docker). **This file is not encrypted at rest**. Do not upload it to GitHub, share it, or expose database files to any internet-accessible service. Configure encrypted device backups, access controls and a retention policy before long-term use. The existing summary-only database is unchanged.

After reviewing the privacy implications, opt in by setting `GATEWAY_ENABLE_RECORD_STORE=1` in `extensions/.env` and recreating the gateway container (`docker compose up --build -d`). The existing API key and webhook configuration remain unchanged. The authenticated endpoint `GET /storage/stats` then provides **counts only**, not raw personal health data.

Record semantics:
- Stable `metadata.id` is the primary deduplication/upsert identity scoped by data type. A newer `last_modified_time` can replace an earlier version, while delayed older versions are ignored.
- Records without stable IDs use aggregate time-bucket keys when detectable; otherwise a content fingerprint. Edits of unidentified records cannot always be matched. This is not yet a medically accurate canonical dataset.
- Each `deleted` event has a `type` and an `id`; matching raw records are removed and an ID tombstone blocks stale retransmissions. **Even a later legitimate re-creation using the same ID is blocked** until an explicit reconciliation procedure is built.
- Idempotency is enforced per batch digest and per record key. The table is updated transactionally per batch.
- Existing 14,783 records previously summarized **cannot be recovered** from the summary DB. To ingest them into this optional plugin, an explicit Health Connect historical re-sync/backfill is required. Avoid large backfills before testing.
- **Do not use the stored rows to calculate health totals yet**. Overlapping sources, aggregates, deletions of prior buckets, historical corrections and local-day boundaries require a source-aware consolidation plugin. This module is experimental and designed as a first-stage storage layer.

### Disabling storage

Set `GATEWAY_ENABLE_RECORD_STORE=0` and recreate the container. This prevents future writes but **does not erase the previously created database**. Plan deletion or backup explicitly. Never run `docker compose down -v` casually: it deletes the entire named volume, including the summary database.

## Quick start without Docker

Requires Python 3.11+; no third-party dependencies. From this directory:

1. Set a strong random `GATEWAY_API_KEY` (24+ characters). On PowerShell: `$env:GATEWAY_API_KEY = '<your unique random secret>'`.
2. Run `python gateway.py` (listens on 127.0.0.1:8788 by default).
3. In another shell: `curl -H 'x-api-key: YOUR_SECRET' http://127.0.0.1:8788/plugins`.
4. Run tests: `python -m unittest discover -s tests -v`.

## Docker (Windows/Linux)

From `extensions/`, copy `.env.example` to `.env`, **replace** the placeholder with a random secret, then run `docker compose up --build -d`. The sample publishes only to host loopback; it will **not** be reachable from a phone on the LAN until you intentionally configure a secure route. Data is stored in the named `summary-data` volume. Never commit `.env` or the volume contents.

To connect an Android phone, prefer a trusted TLS reverse proxy or an authenticated private tunnel. If you explicitly expose port 8788 on a trusted LAN, restrict access with host firewall rules and use transport encryption where feasible. **Do not expose this HTTP listener on the public internet.** Then add a JSON webhook in HC Webhook with target `https://YOUR_PRIVATE_GATEWAY/webhook` and a custom header `x-api-key` equal to `GATEWAY_API_KEY`. Test delivery in the app.

## Compatibility and sync strategy

1. Keep fork `main` aligned with `mcnaveen:main` using GitHub's **Sync fork**. Do not merge experimental changes into `main` while it is intended as a clean mirror.
2. Develop only in `dev/plugins` or feature branches based on it. Merge newer `main` into the development branch after testing.
3. Treat `docs/webhook.md` and `proto/` as upstream protocol references. Contract-test additions before deploying them.
4. Keep ingestion opt-in. Users choose which HC record types to transmit. Limit retention and protect health data by default.
5. Server-side plugins cannot add native Android screens or Health Connect permissions. Such features would require upstream changes or a separately built Android variant.

## Plugin contract

Each plugin has a unique `name` and `handle(payload: dict, batch_id: str)` method. It must be **idempotent by batch_id**, not leak sensitive data, and raise on failure so delivery can be retried. The manager copies the payload for each plugin. Add integration tests whenever a plugin changes the accepted payload semantics. Multiple plugins are processed sequentially, **not in one database transaction**; a retry can run a plugin again after another plugin failed, which is why idempotency is mandatory.

This code is distributed as part of the upstream AGPL-3.0-only fork; preserve its license and notices. The original repo is at https://github.com/mcnaveen/health-connect-webhook.
