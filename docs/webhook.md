# Webhook payload reference

HC Webhook can deliver health data in two formats, chosen per webhook:

1. **JSON** (default) — HTTP `POST` with a JSON body (this document’s schema).
2. **Protobuf / gRPC** — unary gRPC `HealthWebhook.Deliver` using the published schema in [`proto/hcwebhook/v1/health_payload.proto`](../proto/hcwebhook/v1/health_payload.proto).

The optional local HTTP server still uses **JSON only**.

JSON implementation: `buildJsonPayload` in `app/src/main/java/com/hcwebhook/app/SyncManager.kt`.  
Protobuf implementation: `ProtobufPayloadBuilder` + `GrpcWebhookClient`.

## HTTP request (JSON webhooks)

| Property | Value |
|----------|--------|
| Method | `POST` |
| `Content-Type` | `application/json; charset=utf-8` |
| Body | UTF-8 JSON object (see below) |

The app may attach **custom headers** per webhook URL as configured in the app. There is no built-in signature or auth header unless you add it there.

Successful delivery is any HTTP **2xx** response. Failed requests are retried briefly (a few attempts with backoff); if all attempts fail, the next manual, interval, or scheduled sync can try again with the same incremental rules.

With more than one webhook, the sync counts as successful when at least one webhook gets the data. A webhook that failed keeps its payloads on the device and gets them first, in order, at the start of the next sync, with `syncType` `outbox` in the webhook log. The app keeps up to 16 MB of waiting payloads per webhook and drops the oldest past that. A webhook that changes between JSON and gRPC drops its waiting payloads.

## gRPC delivery (Protobuf)

| Property | Value |
|----------|--------|
| Protocol | gRPC over HTTP/2 |
| Package / service | `hcwebhook.v1.HealthWebhook` |
| RPC | `Deliver(HealthPayload) returns (DeliverResponse)` |
| Success | `DeliverResponse.ok == true` |
| Schema file | [`proto/hcwebhook/v1/health_payload.proto`](../proto/hcwebhook/v1/health_payload.proto) |
| App encoder | `ProtobufPayloadBuilder` |
| App client | `GrpcWebhookClient` |
| Example servers | [`server/example/`](../server/example/) |

### Configure in the app

1. Open **Webhooks** → edit a webhook.
2. Set **Delivery format** to **Protobuf / gRPC**.
3. Enter a **gRPC target** (see table below).
4. Optional: add custom headers (become gRPC metadata; keys are lowercased).
5. Optional: tap **Share schema (.proto)** to export the bundled schema via the system share sheet.
6. Tap **Test**.

### Target URL forms

| Example | Transport | Notes |
|---------|-----------|--------|
| `http://192.168.1.10:50051` | Plaintext | Best for LAN demos |
| `192.168.1.10:50051` | Plaintext | Port ≠ 443 ⇒ plaintext |
| `https://hooks.example.com` | TLS | Default port 443 |
| `hooks.example.com:443` | TLS | Explicit TLS port |
| `hooks.example.com` | TLS | Bare host defaults to port 443 + TLS |

### Authentication

There is no built-in signature scheme. Use custom webhook headers.

Example: set header `x-api-key: supersecret`. The gRPC client attaches it as metadata. Example servers accept the same value via env `API_KEY`.

### TLS

- **LAN:** use plaintext (`http://IP:50051`).
- **Production:** terminate TLS with a trusted certificate (reverse proxy recommended).
- Self-signed certs usually fail on Android until the device trusts the CA. Prefer Let’s Encrypt (or similar).

See [`server/example/README.md`](../server/example/README.md) for Docker, auth, and TLS recipes.

### Example servers (runnable)

| Stack | Command (from `server/example`) |
|-------|----------------------------------|
| Python (Docker) | `docker compose --profile python up --build` |
| TypeScript (Docker) | `docker compose --profile typescript up --build` |
| Python (local) | see [`server/example/python/README.md`](../server/example/python/README.md) |
| TypeScript (local) | see [`server/example/typescript/README.md`](../server/example/typescript/README.md) |
| Go | see [`server/example/go/README.md`](../server/example/go/README.md) |
| PHP | see [`server/example/php/README.md`](../server/example/php/README.md) |

### Generate your own server

Use the published `.proto` with your language’s gRPC tooling:

```bash
# Go
protoc -I proto --go_out=. --go-grpc_out=. proto/hcwebhook/v1/health_payload.proto

# Python
python -m grpc_tools.protoc -I proto --python_out=. --grpc_python_out=. \
  proto/hcwebhook/v1/health_payload.proto
```

Implement `Deliver`, accept `HealthPayload`, return `{ ok: true }`.

Field names and units match the JSON tables below (same logical schema). The proto
form differs where protobuf can be stricter than JSON:

- Record instants are `google.protobuf.Timestamp` and durations are
  `google.protobuf.Duration` (the envelope `timestamp` stays an ISO-8601 string).
- Records that can be a single sample **or** an aggregate — `heart_rate`,
  `heart_rate_variability`, `oxygen_saturation`, `respiratory_rate`,
  `skin_temperature` — carry a `oneof value { sample; aggregate; }` so only one
  form is ever set.
- `RecordMetadata` also carries `id`, `client_record_id`, `client_record_version`,
  `last_modified_time` (record identity for deduplication / update detection) and
  a `zone_offset` oneof — `instant_zone_offset_seconds` for instant records or
  `interval_zone_offset` (start/end) for interval records — the UTC offsets Health
  Connect stores, for local-day aggregation.

### Schema distribution

| Audience | How to get the `.proto` |
|----------|-------------------------|
| App user | **Share schema (.proto)** in the webhook editor |
| Developer / CI | Repo path `proto/hcwebhook/v1/health_payload.proto` |
| Example Docker builds | Copied into the image at build time |

## Root JSON object

| Field | Type | Always present | Description |
|-------|------|----------------|-------------|
| `timestamp` | string | yes | ISO-8601 instant when the payload was built (`Instant.now()`), not the time range of the health records. |
| `app_version` | string | yes | Human-readable app version name from the running build. |
| `timezone` | string | yes | Zone the payload times are written in: `UTC`, or the phone zone such as `Asia/Kolkata`. See [Time fields](#time-fields). |
| All other keys | array | no | One array per enabled data type that has **at least one** record in this payload. Omitted keys mean “no records in this batch” for that type—not “disabled in settings.” |
| `deleted` | array | no | Records deleted from Health Connect since the last sync. See [Edits and deletions](#edits-and-deletions). |

Arrays are **never** empty: if a type has no data for this sync, the key is omitted entirely.

### Time fields

All times in the JSON payload (`timestamp`, record times, `metadata.last_modified_time`) are ISO-8601 and follow the **Payload timestamps** setting (Settings → App settings):

| Setting | Example | `timezone` |
|---------|---------|------------|
| **UTC** | `2026-05-09T12:34:56.789Z` | `UTC` |
| **Local time** | `2026-05-09T18:04:56.789+05:30` | phone zone, e.g. `Asia/Kolkata` |

Both forms are the same instant; parse them as ISO-8601 with an offset. New installs default to **Local time**. Installs from before this setting keep **UTC**, so existing automations do not break.

Protobuf payloads always use `google.protobuf.Timestamp` (UTC) and are not affected; each record's metadata carries its UTC offset.

### Which records appear (incremental vs full window)

- **Background / manual sync (default)**  
  Reads within a rolling **48-hour** window (`HealthConnectManager`) and, for each enabled type, applies the **last successful sync instant** for that type so only **new** records (relative to that watermark) are included. Records that the last sync sent at that exact instant are not sent again. First sync has no watermark, so everything in the window can appear. The sync also sends records that were **edited**, **written late**, or **deleted** since the last sync, also when they are older than the window. See [Edits and deletions](#edits-and-deletions).

- **Explicit range** (e.g. local HTTP `?days=7` or a chosen start/end)  
  Uses the requested window and **does not** apply last-sync filtering; the payload can contain all records in that range for enabled types. It also **does not move** the last-sync instant, so the next default sync continues where the last default sync stopped.

Only types the user enabled **and** granted Health Connect permission for are read; others simply produce no arrays.

### Edits and deletions

The default sync uses the Health Connect Changes API to find what changed since the last successful sync, for each enabled type:

- **Edited or late records.** An edit keeps the original record time, and a late write (for example a watch that syncs a day late) has an old time, so the time watermark drops them. The app sends them again, with the same `metadata.id` and a newer `metadata.last_modified_time`. Replace the stored record that has the same `id`.
  - **Full** resolution: the changed record.
  - **N-minute** resolution: each bucket that holds a changed record, rebuilt from all its records.
  - **Daily** resolution: the full-day total of each day that holds a changed record.

  Upsert aggregates by type and `start_time` / `time`, so a resent bucket or day replaces the old value.
- **N-minute steps, distance and active calories** can come from the Health Connect aggregate (Data Types → resolution → **Count overlapping apps once**). It is on for new installs and off for installs from before 1.9.28. With it on, data that several apps write (for example a phone app and a watch app) is counted once, by the app priority in Health Connect, so the buckets of a day add up to the daily total. A record that crosses several buckets is spread over them. If the top app writes one record per day, its total is spread evenly over the hours; put a finer source first in Health Connect → Data and access → Steps → Data sources and priority. With it off, a bucket is the sum of the raw records that start in it, from all apps.
- **Deleted records.** The `deleted` array lists one object per record:

  | Field | Type | Description |
  |-------|------|-------------|
  | `type` | string | Data type, as the payload key (e.g. `nutrition`, `heart_rate`). |
  | `id` | string | Health Connect record id. Matches `metadata.id` of the record sent before. |

  Health Connect does not give the time of a deleted record, so the app cannot rebuild buckets or daily totals after a deletion. When you store aggregates, a deletion changes no total until the next full read of that period (for example a manual sync with a time range).

Limits:

- The first sync after an upgrade starts tracking. Changes before it are not sent.
- Changes older than 30 days can be lost: Health Connect expires the tracking token, and the app then starts a new one.
- One sync reads at most 31 old days per type for changes.
- A deletion is sent only when the record is still gone. Some apps (such as Fitbit) delete a record and write it again with the same client record id, which gives the same `id`; the app does not send a deletion for such a record.
- When the read is too large for one request (more than 25,000 records and deletions), the sync sends it as several payloads with the same schema, each with a part of the lists. A webhook counts as reached only when all its parts arrive. If a part fails, the next sync sends all parts again, so upsert by `id` (or by type and time for aggregates).
- When the app cannot read the changes of a type, it keeps the tracking token and reads the same changes again on the next sync.
- Explicit ranges (manual time range, local HTTP) read the full window and do not use change tracking.

### Record metadata

Raw records carry a `metadata` object. N-minute heart rate, HRV, SpO₂, respiratory rate, and skin temperature aggregates carry the metadata of their first sample:

| Field | Type | Description |
|-------|------|-------------|
| `data_origin` | string | Package name of the app that wrote the record. |
| `recording_method` | string | `actively_recorded`, `automatically_recorded`, `manual_entry`, or `unknown`. |
| `device` | object | Optional `manufacturer`, `model`, `type`. |
| `id` | string | Health Connect record id. Use it to deduplicate and to apply edits and `deleted`. |
| `client_record_id` | string | Optional. Id that the source app gave the record. |
| `client_record_version` | number | Optional. Version that the source app gave the record. |
| `last_modified_time` | string | When Health Connect last changed the record. |
| `zone_offset_seconds` | number | Optional. UTC offset in seconds when an instant record was written. |
| `start_zone_offset_seconds` | number | Optional. UTC offset in seconds at the start of an interval record. |
| `end_zone_offset_seconds` | number | Optional. UTC offset in seconds at the end of an interval record. |

---

## Arrays and record shapes

Keys use **snake_case**. Numeric types follow Kotlin serialization to JSON (e.g. integers as JSON numbers, doubles as JSON numbers).

### Sample resolution (heart rate and related series)

These types can emit either a **sample** object or an **aggregate** object, depending on the per-type **sample resolution** setting in the app (`DataResolution` / configuration UI):

| Setting | JSON shape |
|---------|------------|
| **Full** (`0`) | One object per raw sample (value field only). |
| **N minutes** (default **1** for these types) | One object per time bucket with `avg` / `min` / `max` (skin uses `*_delta_celsius`). |

Types: `heart_rate`, `heart_rate_variability`, `oxygen_saturation`, `respiratory_rate`, `skin_temperature`.

On aggregates:

- `time` is the **bucket start** (UTC-aligned epoch seconds for the N-minute window), not the last sample time and not the bucket end.
- The legacy sample field (`bpm`, `rmssd_millis`, `percentage`, `rate`, or `delta_celsius`) is also present and equals `avg` / `avg_delta_celsius`. That restores older parsers that read only the sample field name. The value is still a **bucket average**, not a single measurement.
- To detect shape: treat presence of `avg` / `min` / `max` (or skin `avg_delta_celsius`) as aggregate; do not use presence of `bpm` alone.

Protobuf delivery uses a typed `oneof sample | aggregate` instead of duplicate JSON fields; see the gRPC section above.

### `steps` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `count` | number (integer) | Step count for the interval. |
| `start_time` | string | Interval start. |
| `end_time` | string | Interval end. |

### `sleep` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `session_end_time` | string | End of the sleep session. |
| `duration_seconds` | number (integer) | Total session duration in seconds. |
| `stages` | array | Ordered sleep stages (see below). |

Each **stage** object:

| Field | Type | Description |
|-------|------|-------------|
| `stage` | string | Value from Health Connect / AndroidX (e.g. enum `toString()`). |
| `start_time` | string | Stage start. |
| `end_time` | string | Stage end. |
| `duration_seconds` | number (integer) | Stage length in seconds. |

### `heart_rate` — array of objects

**Sample** (resolution Full):

| Field | Type | Description |
|-------|------|-------------|
| `bpm` | number (integer) | Beats per minute. |
| `time` | string | Sample time. |

**Aggregate** (resolution N minutes):

| Field | Type | Description |
|-------|------|-------------|
| `time` | string | Bucket start. |
| `avg` | number | Average BPM in the bucket. |
| `min` | number | Minimum BPM in the bucket. |
| `max` | number | Maximum BPM in the bucket. |
| `bpm` | number | Same as `avg` (legacy alias). |

### `heart_rate_variability` — array of objects

**Sample** (resolution Full):

| Field | Type | Description |
|-------|------|-------------|
| `rmssd_millis` | number | RMSSD in milliseconds. |
| `time` | string | Sample time. |

**Aggregate** (resolution N minutes):

| Field | Type | Description |
|-------|------|-------------|
| `time` | string | Bucket start. |
| `avg` | number | Average RMSSD in the bucket. |
| `min` | number | Minimum RMSSD in the bucket. |
| `max` | number | Maximum RMSSD in the bucket. |
| `rmssd_millis` | number | Same as `avg` (legacy alias). |

### `distance` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `meters` | number | Distance in meters. |
| `start_time` | string | Interval start. |
| `end_time` | string | Interval end. |

### `active_calories` / `total_calories` — arrays of objects

| Field | Type | Description |
|-------|------|-------------|
| `calories` | number | Energy in kilocalories. |
| `start_time` | string | Interval start. |
| `end_time` | string | Interval end. |

### `weight` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `kilograms` | number | Mass in kg. |
| `time` | string | Measurement time. |

### `height` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `meters` | number | Height in meters. |
| `time` | string | Measurement time. |

### `blood_pressure` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `systolic` | number | Systolic pressure (unit as in Health Connect source). |
| `diastolic` | number | Diastolic pressure. |
| `time` | string | Measurement time. |

### `blood_glucose` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `mmol_per_liter` | number | Concentration in mmol/L. |
| `time` | string | Measurement time. |

### `oxygen_saturation` — array of objects

**Sample** (resolution Full):

| Field | Type | Description |
|-------|------|-------------|
| `percentage` | number | SpO₂ (0–100 scale as provided by Health Connect). |
| `time` | string | Measurement time. |

**Aggregate** (resolution N minutes):

| Field | Type | Description |
|-------|------|-------------|
| `time` | string | Bucket start. |
| `avg` | number | Average SpO₂ in the bucket. |
| `min` | number | Minimum SpO₂ in the bucket. |
| `max` | number | Maximum SpO₂ in the bucket. |
| `percentage` | number | Same as `avg` (legacy alias). |

### `body_temperature` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `celsius` | number | Temperature in °C. |
| `time` | string | Measurement time. |

### `skin_temperature` — array of objects

**Sample** (resolution Full):

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `time` | string | yes | Sample instant. |
| `delta_celsius` | number | yes | Delta from baseline in °C. |
| `baseline_celsius` | number | no | Present when the source record includes a baseline. |
| `measurement_location` | number (integer) | yes | AndroidX `SkinTemperatureRecord` location constant (e.g. unknown / finger / toe / wrist). See [SkinTemperatureRecord](https://github.com/androidx/androidx/blob/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/SkinTemperatureRecord.kt) in AndroidX. |

**Aggregate** (resolution N minutes):

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `time` | string | yes | Bucket start. |
| `avg_delta_celsius` | number | yes | Average delta in the bucket (°C). |
| `min_delta_celsius` | number | yes | Minimum delta in the bucket. |
| `max_delta_celsius` | number | yes | Maximum delta in the bucket. |
| `delta_celsius` | number | yes | Same as `avg_delta_celsius` (legacy alias). |
| `baseline_celsius` | number | no | Present when the source record includes a baseline. |
| `measurement_location` | number (integer) | yes | Same location constant as sample form. |

Health Connect can store multiple samples per interval; with Full resolution the app emits **one JSON object per sample**, repeating baseline and location when applicable. With N-minute resolution, samples in a bucket are averaged into one object.

### `respiratory_rate` — array of objects

**Sample** (resolution Full):

| Field | Type | Description |
|-------|------|-------------|
| `rate` | number | Breaths per minute (or as provided by Health Connect). |
| `time` | string | Measurement time. |

**Aggregate** (resolution N minutes):

| Field | Type | Description |
|-------|------|-------------|
| `time` | string | Bucket start. |
| `avg` | number | Average rate in the bucket. |
| `min` | number | Minimum rate in the bucket. |
| `max` | number | Maximum rate in the bucket. |
| `rate` | number | Same as `avg` (legacy alias). |

### `resting_heart_rate` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `bpm` | number (integer) | Resting heart rate. |
| `time` | string | Measurement time. |

### `exercise` — array of objects

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | yes | Exercise type from Health Connect (string form). |
| `start_time` | string | yes | Session start. |
| `end_time` | string | yes | Session end. |
| `duration_seconds` | number (integer) | yes | Session duration in seconds. |
| `distance_meters` | number | no | If distance type is enabled and linked data exists. |
| `steps` | number (integer) | no | If steps type is enabled and linked data exists. |
| `avg_cadence_spm` | number | no | Average cadence (steps per minute). |
| `max_cadence_spm` | number | no | Max cadence. |
| `stride_length_m` | number | no | Stride length in meters. |

### `hydration` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `liters` | number | Volume in liters. |
| `start_time` | string | Interval start. |
| `end_time` | string | Interval end. |

### `nutrition` — array of objects

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `start_time` | string | yes | Interval start. |
| `end_time` | string | yes | Interval end. |
| `calories` | number | no | Kilocalories when present. |
| `protein_grams` | number | no | |
| `carbs_grams` | number | no | |
| `fat_grams` | number | no | |
| `sugar_grams` | number | no | |
| `sodium_grams` | number | no | |
| `dietary_fiber_grams` | number | no | |
| `name` | string | no | Meal or item label when provided. |
| `meal_type` | string | no | `breakfast`, `lunch`, `dinner` or `snack` when the writing app set one. Omitted for unknown meal types and for merged (bucketed or daily) records. Diary apps such as MyFitnessPal write one unnamed record per meal with identical times; this field tells them apart. |
| `energy_from_fat_calories` | number | no | Kilocalories from fat, when set. |
| further nutrients | number | no | Each only when the writing app set it, in grams like the fields above: `saturated_fat_grams`, `unsaturated_fat_grams`, `monounsaturated_fat_grams`, `polyunsaturated_fat_grams`, `trans_fat_grams`, `cholesterol_grams`, `caffeine_grams`, `vitamin_a_grams`, `vitamin_c_grams`, `vitamin_d_grams`, `vitamin_e_grams`, `vitamin_k_grams`, `thiamin_grams`, `riboflavin_grams`, `niacin_grams`, `vitamin_b6_grams`, `vitamin_b12_grams`, `folate_grams`, `folic_acid_grams`, `biotin_grams`, `pantothenic_acid_grams`, `calcium_grams`, `iron_grams`, `magnesium_grams`, `potassium_grams`, `zinc_grams`, `phosphorus_grams`, `selenium_grams`, `copper_grams`, `manganese_grams`, `chromium_grams`, `molybdenum_grams`, `iodine_grams`, `chloride_grams`. Merged (bucketed or daily) records sum them. In protobuf these arrive in the `nutrients` map. |

### `basal_metabolic_rate` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `watts` | number | Power in watts (Health Connect basal energy representation). |
| `time` | string | Measurement time. |

### `body_fat` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `percentage` | number | Body fat %. |
| `time` | string | Measurement time. |

### `lean_body_mass` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `kilograms` | number | Mass in kg. |
| `time` | string | Measurement time. |

### `vo2_max` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `ml_per_kg_per_min` | number | VO₂ max in mL/(kg·min). |
| `time` | string | Measurement time. |

### `bone_mass` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `kilograms` | number | Mass in kg. |
| `time` | string | Measurement time. |

### `menstruation_flow` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `flow` | number (integer) | Flow intensity (e.g., light, medium, heavy as defined by Health Connect constants). |
| `time` | string | Measurement time. |

### `menstruation_period` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `start_time` | string | Period start time. |
| `end_time` | string | Period end time. |

### `intermenstrual_bleeding` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `time` | string | Occurrence time. |

### `ovulation_test` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `result` | number (integer) | Ovulation test result (as defined by Health Connect constants). |
| `time` | string | Measurement time. |

### `cervical_mucus` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `appearance` | number (integer) | Cervical mucus appearance (as defined by Health Connect constants). |
| `time` | string | Measurement time. |

### `sexual_activity` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `protection_used` | number (integer) | Protection used (as defined by Health Connect constants). |
| `time` | string | Measurement time. |

### `basal_body_temperature` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `celsius` | number | Temperature in °C. |
| `measurement_location` | number (integer) | Measurement location (as defined by Health Connect constants). |
| `time` | string | Measurement time. |

### `mindfulness` — array of objects

Health Connect **Wellness** category. Needs a Health Connect version with mindfulness support (Android 16+, or a recent Health Connect app). On other devices the type sends nothing.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `session_type` | string | yes | `meditation`, `breathing`, `music`, `movement`, `unguided`, or `unknown`. |
| `title` | string | no | Session title from the source app. |
| `notes` | string | no | Session notes from the source app. |
| `start_time` | string | yes | Session start. |
| `end_time` | string | yes | Session end. |
| `duration_seconds` | number (integer) | yes | Session duration in seconds. |

---

## Example (illustrative only)

Shape varies with your enabled types and data:

```json
{
  "timestamp": "2026-05-09T14:00:00.123Z",
  "app_version": "1.2.3",
  "steps": [
    {
      "count": 8421,
      "start_time": "2026-05-08T00:00:00Z",
      "end_time": "2026-05-09T00:00:00Z"
    }
  ],
  "heart_rate": [
    { "bpm": 72, "time": "2026-05-09T08:15:00Z" }
  ]
}
```

Example aggregate object (N-minute resolution; `bpm` equals `avg`):

```json
{
  "heart_rate": [
    {
      "time": "2026-05-09T08:15:00Z",
      "avg": 72,
      "min": 68,
      "max": 80,
      "bpm": 72
    }
  ]
}
```

---

## Local HTTP server

The optional on-device server uses the **same JSON** as webhooks but over **GET** on your LAN. Endpoints, query parameters, binding, and how `/` differs from incremental webhook sync are documented in **[local-http.md](./local-http.md)**.

## Schema stability

Field names and units are defined by the app version that produced the payload (`app_version`). When upgrading integrations, compare `app_version` or pin your parser to documented keys above; new Health Connect types or fields may be added in future releases.
