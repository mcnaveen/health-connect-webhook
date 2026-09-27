# API Reference

Integrations and automations can rely on these specs (kept in sync with the app code).

## Webhook payload

JSON `POST` body and optional **Protobuf / gRPC** delivery (`HealthWebhook.Deliver`).

- Full field tables, units, and examples: [webhook.md](webhook.md)
- Schema: [`proto/hcwebhook/v1/health_payload.proto`](../proto/hcwebhook/v1/health_payload.proto)
- Example receivers: [`server/example/`](../server/example/)

The JSON body is one object: always `timestamp` (when the payload was built) and
`app_version`, plus optional snake_case arrays per data type (each key omitted if
there are no records in that batch). Background sync reads a rolling 48-hour window
and, by default, only sends records written or changed in Health Connect since the
last successful sync per type (the first run has no prior watermark).

Delivery is `POST` with `Content-Type: application/json; charset=utf-8` by default.
Per webhook you can switch to Protobuf / gRPC. Delivery includes short retry
handling (up to 3 attempts with exponential backoff); if it still fails, data is
retried on the next successful sync trigger (manual, interval, or scheduled).

## Local HTTP server

`GET` endpoints (`/`, `/latest`, `/ping`), query parameters, listen binding, and
default port **8787**. Returns the same JSON schema as the webhook payload.

- Details and pull-vs-sync semantics: [local-http.md](local-http.md)

## gRPC example servers

Docker one-liner, Python / TypeScript / Go / PHP receivers, metadata auth
(`x-api-key`), and TLS notes: [`server/example/README.md`](../server/example/README.md)
