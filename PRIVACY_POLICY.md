# Privacy Policy

**Health Connect Webhook**
_Last updated: April 3, 2026_

## Overview

Health Connect Webhook is an open-source Android application that reads health data from Android Health Connect and forwards it to webhook URLs that you configure. The app is designed with privacy as a core principle — your health data stays under your control.

## Data We Access

With your explicit permission, the app may read the following health data types from Android Health Connect:

- Steps
- Sleep sessions
- Heart rate & heart rate variability (HRV)
- Distance
- Active calories burned & total calories burned
- Weight & height
- Blood pressure
- Blood glucose
- Oxygen saturation (SpO₂)
- Body temperature
- Respiratory rate
- Resting heart rate
- Exercise sessions
- Hydration
- Nutrition

You choose which data types to enable. The app only reads data for the types you have explicitly granted permission for.

## How Your Data Is Used

- **Health data is read locally** on your device from Android Health Connect.
- **Data is sent only to webhook URLs you configure** — URLs you enter yourself (e.g., your own server, Home Assistant, n8n, etc.).
- **The app does not transmit your data to any third-party servers**, analytics services, or the app developer.
- **No data is stored in the cloud** by this app.

## Data Storage

- Webhook URLs, custom headers, and sync schedules are stored **locally on your device** using Android's SharedPreferences.
- Health data is **not persisted** by the app — it is read and forwarded to your webhook in real time.
- **Webhook delivery logs** (timestamps, HTTP status, and response summaries from your webhooks) are stored **only on your device**, in the same local storage, for display inside the app. The app keeps at most the **100 most recent** log entries; older entries are discarded automatically when new ones are added.

## Data Retention

- **On your device:** Settings (webhook URLs, headers, schedules, enabled data types, last-sync markers) and webhook logs are kept **until you delete them, clear app data, or uninstall the app**. There is **no automatic expiry** for settings other than the webhook log rotation described above.
- **Health records:** The app does **not** store Health Connect records on the device beyond what Android and Health Connect already maintain. Payloads are built in memory and sent to your webhooks when you sync.
- **Developer / cloud:** The app **does not** send your health data or settings to servers operated by the developer. Data you forward to **your own webhook endpoints** is subject to **those systems’** retention and deletion practices — the developer does not control that storage.

## How to Delete Your Data

You can remove data associated with this app as follows:

1. **Webhook logs (on device):** Open the app → **Menu (⋮)** → **Webhook Log** → use **Clear** / **Clear All Logs** to delete stored log entries immediately.
2. **All app data on the device (settings, logs, last-sync state):** On your Android device go to **Settings → Apps → HC Webhook → Storage** (or **App info**) → **Clear data** / **Clear storage**. Alternatively, **uninstall** the app, which removes the app’s local data from the device.
3. **Stop the app from reading health data:** Revoke or adjust permissions in **Settings → Health Connect** (or your device’s Health Connect privacy screen) for this app.
4. **Data already received by your webhooks:** Because payloads are sent only to URLs **you** configure, **deletion of copies on those servers** must be handled through **your** backend, automation, or provider — contact the operator of each endpoint if you need data removed there.

For privacy questions or to request clarification (not applicable for on-device deletion — use the steps above), you may open an issue on the GitHub repository listed under **Contact** below.

## Permissions

The app requests the following permissions:

- **Health Connect read permissions** — to access the health data types you enable.
- **Internet** — to send data to your configured webhook URLs.
- **Receive boot completed** — to reschedule sync alarms after device restart.
- **Schedule exact alarms** — to run syncs at your configured times.

All Health Connect permissions are granted through the standard Android Health Connect permission dialog and can be revoked at any time via Android Settings → Health Connect.

## Third-Party Services

The app does not integrate with any third-party analytics, advertising, or crash reporting SDKs. The only network requests made by the app are the webhook POST requests to URLs you explicitly configure.

## Children's Privacy

This app is not directed at children under the age of 13 and does not knowingly collect data from children.

## Open Source

The full source code of this app is available on GitHub:
[https://github.com/mcnaveen/health-connect-webhook](https://github.com/mcnaveen/health-connect-webhook)

You can verify exactly what the app does with your data by reviewing the source code.

## Changes to This Policy

If this policy is updated, the new version will be committed to the repository and the "Last updated" date above will be changed.

## Contact

If you have any questions about this privacy policy, please open an issue on the GitHub repository.
