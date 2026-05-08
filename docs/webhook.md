# Webhook payload reference

HC Webhook delivers health data as a **single JSON object** in the HTTP request body. The same structure is used for **remote webhooks** (POST) and for responses from the **optional local HTTP server** (GET), so one schema covers both.

Implementation: `buildJsonPayload` in `app/src/main/java/com/hcwebhook/app/SyncManager.kt`.

## HTTP request (remote webhooks)

| Property | Value |
|----------|--------|
| Method | `POST` |
| `Content-Type` | `application/json; charset=utf-8` |
| Body | UTF-8 JSON object (see below) |

The app may attach **custom headers** per webhook URL as configured in the app. There is no built-in signature or auth header unless you add it there.

Successful delivery is any HTTP **2xx** response. Failed requests are retried briefly (a few attempts with backoff); if all attempts fail, the next manual, interval, or scheduled sync can try again with the same incremental rules.

## Root JSON object

| Field | Type | Always present | Description |
|-------|------|----------------|-------------|
| `timestamp` | string | yes | ISO-8601 instant when the payload was built (`Instant.now()`), not the time range of the health records. |
| `app_version` | string | yes | Human-readable app version name from the running build. |
| All other keys | array | no | One array per enabled data type that has **at least one** record in this payload. Omitted keys mean “no records in this batch” for that type—not “disabled in settings.” |

Arrays are **never** empty: if a type has no data for this sync, the key is omitted entirely.

### Time fields

Unless noted otherwise, time-valued fields use **`java.time.Instant.toString()`** (ISO-8601 UTC with `Z`, e.g. `2026-05-09T12:34:56.789Z`).

### Which records appear (incremental vs full window)

- **Background / manual sync (default)**  
  Reads within a rolling **48-hour** window (`HealthConnectManager`) and, for each enabled type, applies the **last successful sync instant** for that type so only **new or updated** records (relative to that watermark) are included. First sync has no watermark, so everything in the window can appear.

- **Explicit range** (e.g. local HTTP `?days=7` or a chosen start/end)  
  Uses the requested window and **does not** apply last-sync filtering; the payload can contain all records in that range for enabled types.

Only types the user enabled **and** granted Health Connect permission for are read; others simply produce no arrays.

---

## Arrays and record shapes

Keys use **snake_case**. Numeric types follow Kotlin serialization to JSON (e.g. integers as JSON numbers, doubles as JSON numbers).

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

| Field | Type | Description |
|-------|------|-------------|
| `bpm` | number (integer) | Beats per minute. |
| `time` | string | Sample time. |

### `heart_rate_variability` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `rmssd_millis` | number | RMSSD in milliseconds. |
| `time` | string | Sample time. |

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

| Field | Type | Description |
|-------|------|-------------|
| `percentage` | number | SpO₂ (0–100 scale as provided by Health Connect). |
| `time` | string | Measurement time. |

### `body_temperature` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `celsius` | number | Temperature in °C. |
| `time` | string | Measurement time. |

### `skin_temperature` — array of objects

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `time` | string | yes | Sample instant. |
| `delta_celsius` | number | yes | Delta from baseline in °C. |
| `baseline_celsius` | number | no | Present when the source record includes a baseline. |
| `measurement_location` | number (integer) | yes | AndroidX `SkinTemperatureRecord` location constant (e.g. unknown / finger / toe / wrist). See [SkinTemperatureRecord](https://github.com/androidx/androidx/blob/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/SkinTemperatureRecord.kt) in AndroidX. |

Health Connect can store multiple samples per interval; the app emits **one JSON object per sample**, repeating baseline and location when applicable.

### `respiratory_rate` — array of objects

| Field | Type | Description |
|-------|------|-------------|
| `rate` | number | Breaths per minute (or as provided by Health Connect). |
| `time` | string | Measurement time. |

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

---

## Local HTTP server

Endpoints such as `http://<device-ip>:<port>/` return JSON built with the **same** `buildJsonPayload` logic (possibly over a different time window). See the main [README](../README.md) for URL patterns (`/`, `/?days=…`, `/latest`, `/ping`).

## Schema stability

Field names and units are defined by the app version that produced the payload (`app_version`). When upgrading integrations, compare `app_version` or pin your parser to documented keys above; new Health Connect types or fields may be added in future releases.
