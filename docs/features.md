# Features

- **Flexible background sync** — interval-based (WorkManager) or fixed-time
  scheduled syncs (AlarmManager).
- **Selective data types** — choose which of the 31 supported types to sync.
- **Multiple webhooks** — send to several webhook URLs at once.
- **JSON or Protobuf/gRPC delivery** — per webhook (`HealthWebhook.Deliver`).
- **Local HTTP server** — expose realtime Health Connect JSON on your LAN for
  agents, scripts, and automation tools.
- **Manual sync** — trigger an immediate sync on demand.
- **Sync triggers** — a home screen widget, a Quick Settings tile and a broadcast
  intent for Tasker / MacroDroid. See [Automation](#automation).
- **Webhook logs** — timestamps, status codes, and response bodies for every request.
- **Permission management** — granular Health Connect permission handling.
- **Settings backup** — export/import webhook configs, data type selections, and
  sync schedule.
- **Multi-language** — 10 languages with manual override.
- **Modern UI** — Jetpack Compose and Material 3.
- **In-app feedback** — submit bugs and ideas from the About screen; stored locally
  and synced to [FeedbackJar](https://hc-webhook.feedbackjar.com/).

## Automation

Three ways to start a sync without opening the app. They all run a normal sync to
your configured webhooks, and they share a one-minute limit: a second trigger within
a minute of the last accepted one is ignored (the tile says "Try again in a minute").

- **Widget** — "Sync status" on the home screen: last sync time and summary, a sync
  button, and a tap to open the app. It updates after every sync.
- **Quick Settings tile** — "Sync health data". Add it by editing the Quick Settings
  panel. On Android 10+ it shows the last sync time.
- **Broadcast intent** — for Tasker, MacroDroid or a computer:
  - Tasker: **System → Send Intent**, Action `com.hcwebhook.app.ACTION_SYNC`,
    Package `com.hcwebhook.app`, Target **Broadcast Receiver**.
  - MacroDroid: **Send Intent** with the same action and package, target **Broadcast**.
  - adb: `adb shell am broadcast -a com.hcwebhook.app.ACTION_SYNC -p com.hcwebhook.app`

  The package is required: Android does not deliver the broadcast without it. The
  intent carries no settings; it only starts a sync.

## Supported health apps

Health Connect aggregates data from apps like Google Fit, Samsung Health, Fitbit,
MyFitnessPal, Strava, Nike Run Club, Withings, Garmin Connect, Polar, Oura, and
any other app that syncs to Health Connect. You don't need all of them — just make
sure the apps you use sync to Health Connect.

## Supported languages

English (default), Tamil, French, German, Spanish, Portuguese, Chinese
(Simplified), Japanese, Korean, Italian, Arabic. Override from the **About** screen.

## Supported health data types

31 types read and synced from Health Connect:

Steps · Sleep (with stages) · Heart Rate · Heart Rate Variability (RMSSD) ·
Distance · Active Calories · Total Calories · Weight · Height · Blood Pressure ·
Blood Glucose · Oxygen Saturation · Body Temperature · Skin Temperature (delta
from baseline when provided) · Respiratory Rate · Resting Heart Rate · Exercise
Sessions · Hydration · Nutrition (calories, protein, carbs, fat, sugar, sodium,
fiber, name) · Basal Metabolic Rate · Body Fat · Lean Body Mass · VO2 Max · Bone
Mass · Menstruation Flow · Menstruation Period · Intermenstrual Bleeding ·
Ovulation Test · Cervical Mucus · Sexual Activity · Basal Body Temperature · Mindfulness
