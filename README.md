# Health Connect to Webhook

![HC Webhook](image.png)

<table role="presentation">
  <tr>
    <td valign="middle"><a href="https://play.google.com/store/apps/details?id=com.hcwebhook.app"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="90"></a></td>
    <td valign="middle">&nbsp;&nbsp;</td>
    <td valign="middle"><a href="https://apps.apple.com/app/health-webhook/id6763619597"><img src="https://tools.applemediaservices.com/api/badges/download-on-the-app-store/black/en-us?size=250x83" alt="Download on the App Store" height="60"></a></td>
  </tr>
</table>

An Android app that bridges Google Fit, Samsung Health, Fitbit, and other health apps to your webhooks via Health Connect APIs, enabling seamless integration with your custom endpoints and services.

## Overview

HC Webhook connects Google Health Connect to your webhook infrastructure. Health Connect aggregates health data from apps like Google Fit, Samsung Health, Fitbit, and more into a unified API. This app reads that aggregated data and sends it to your configured webhook URLs on your chosen sync strategy (interval-based or scheduled times), making it easy to integrate health data from multiple sources into your own systems, analytics platforms, or third-party services.

## How It Works

1. **Health Apps** (Google Fit, Samsung Health, Fitbit, etc.) sync data to **Health Connect**
2. **Health Connect** aggregates all health data into a unified API
3. **HC Webhook** reads data from Health Connect using your selected sync mode (interval or scheduled)
4. **HC Webhook** sends the data to your configured **webhook URLs**
5. Your **custom services** receive and process the health data

## Supported Health Apps

Health Connect aggregates data from these popular health and fitness apps:

- **Google Fit** - Activity tracking, workouts, and health metrics
- **Samsung Health** - Comprehensive health and fitness tracking
- **Fitbit** - Activity, sleep, and heart rate data
- **MyFitnessPal** - Nutrition and calorie tracking
- **Strava** - Running and cycling activities
- **Nike Run Club** - Running activities and workouts
- **Withings** - Weight, blood pressure, and activity tracking
- **Garmin Connect** - Fitness and health data from Garmin devices
- **Polar** - Heart rate and fitness tracking
- **Oura** - Sleep, activity, and recovery data
- **And many more...** - Any app that syncs to Health Connect

> **Note**: You don't need to install all these apps. Just ensure the apps you use are syncing their data to Health Connect, and HC Webhook will automatically access that unified data.

## Screenshots

| Home screen | Data Types | Webhooks URLs | Webhook Logs |
| :---------: | :--------: | :-----------: | :----------: |
| <img src="screenshots/1.png" width="240" alt="Home screen"> | <img src="screenshots/2.png" width="240" alt="Data Types"> | <img src="screenshots/3.png" width="240" alt="Webhooks URLs"> | <img src="screenshots/4.png" width="240" alt="Webhook Logs"> |

## Features

- 🔄 **Flexible Background Sync** - Choose between interval-based sync (WorkManager) or fixed-time scheduled syncs (AlarmManager)
- 🎯 **Selective Data Types** - Choose which health data types to sync (23 supported types)
- 🔗 **Multiple Webhooks** - Send data to multiple webhook URLs simultaneously
- 📊 **Manual Sync** - Trigger immediate data synchronization on demand
- 🌍 **Multi-language Support** - Fully localized in 10 languages with manual override support
- 📝 **Webhook Logs** - View detailed logs of all webhook requests and responses
- 🔐 **Permission Management** - Granular Health Connect permission handling
- 💾 **Settings Backup** - Export/import webhook configs, data type selections, and sync schedule
- 🎨 **Modern UI** - Built with Jetpack Compose and Material 3 design
- ⚡ **Real-time Status** - Visual indicators for permission status and sync state
- 💬 **Feedback** - Easy access to provide feedback and suggestions through the app menu

## Supported Languages

The app is fully localized and supports 10 languages. You can manually override the app language from the **About** screen:

- 🇬🇧 English (Default)
- 🇮🇳 Tamil
- 🇫🇷 French
- 🇩🇪 German
- 🇪🇸 Spanish
- 🇵🇹 Portuguese
- 🇨🇳 Chinese (Simplified)
- 🇯🇵 Japanese
- 🇰🇷 Korean
- 🇮🇹 Italian

## Supported Health Data Types

The app supports reading and syncing the following health data types from Health Connect:

1. **Steps** - Daily step count
2. **Sleep** - Sleep sessions with stages
3. **Heart Rate** - Heart rate measurements
4. **Heart Rate Variability (RMSSD)** - Heart rate variability in milliseconds
5. **Distance** - Distance traveled
6. **Active Calories** - Calories burned during activity
7. **Total Calories** - Total calories burned
8. **Weight** - Body weight measurements
9. **Height** - Height measurements
10. **Blood Pressure** - Systolic and diastolic readings
11. **Blood Glucose** - Blood glucose levels
12. **Oxygen Saturation** - SpO2 measurements
13. **Body Temperature** - Body temperature readings
14. **Respiratory Rate** - Breathing rate measurements
15. **Resting Heart Rate** - Resting heart rate data
16. **Exercise Sessions** - Workout and exercise data
17. **Hydration** - Water intake tracking
18. **Nutrition** - Nutritional information (calories, protein, carbs, fat)
19. **Basal Metabolic Rate** - Basal energy expenditure
20. **Body Fat** - Body fat percentage measurements
21. **Lean Body Mass** - Lean body mass measurements
22. **VO2 Max** - Cardiorespiratory fitness measurements
23. **Bone Mass** - Bone mass measurements

## Requirements

- **Android 8.0 (API 26)** or higher
- **Google Health Connect** app installed and set up
- **Internet connection** for webhook delivery

> **Note**: Health Connect aggregates data from multiple health apps (Google Fit, Samsung Health, Fitbit, etc.). You don't need to directly connect to these apps - just ensure they're syncing to Health Connect, and this app will automatically access that unified data.

## Installation

### From Source

1. Clone this repository:

```bash
git clone https://github.com/mcnaveen/health-connect-webhook
cd health-connect-webhook
```

2. Open the project in Android Studio (Arctic Fox or later recommended)

3. Sync Gradle dependencies

4. Build and run the app on your device or emulator

### Downloads

#### Easy (Recommended)

<table role="presentation">
  <tr>
    <td valign="middle"><a href="https://play.google.com/store/apps/details?id=com.hcwebhook.app"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="90"></a></td>
    <td valign="middle">&nbsp;&nbsp;</td>
    <td valign="middle"><a href="https://apps.apple.com/app/health-webhook/id6763619597"><img src="https://tools.applemediaservices.com/api/badges/download-on-the-app-store/black/en-us?size=250x83" alt="Download on the App Store" height="60"></a></td>
  </tr>
</table>

### Install via Obtainium

You can easily install and update **HC Webhook** using [Obtainium](https://github.com/ImranR98/Obtainium).

1.  Install **Obtainium** on your Android device.
2.  Tap **"Add App"**.
3.  Enter the repository URL: `https://github.com/mcnaveen/health-connect-webhook`
4.  Allow Obtainium to scan for releases.
5.  Tap **Install** / **Update**.


### Building the APK

```bash
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## Usage

### Initial Setup

1. **Install Health Connect** (if not already installed)
   - Download from [Google Play Store](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata)

2. **Grant Permissions**
   - Open the app and tap "Grant Permissions"
   - Select the health data types you want to sync
   - Grant the required permissions in Health Connect

3. **Configure Webhooks**
   - Add one or more webhook URLs (must start with `http://` or `https://`)
   - Select which data types to sync
   - Choose sync mode:
     - **Interval Mode**: set your preferred sync interval (minimum 15 minutes)
     - **Scheduled Mode**: configure one or more fixed times of day

4. **Save Configuration**
   - Tap "Save Configuration" to start automatic syncing

### Manual Sync

- Tap the "Sync Now" button in the Manual Sync section to immediately sync all enabled data types to your webhooks

### Viewing Logs

- Access webhook logs from the menu (⋮) → "Webhook Log"
- View detailed information about each webhook request, including timestamps, status codes, and response data

### Providing Feedback

- Access the feedback form from the menu (⋮) → "Feedback"
- Share your thoughts, suggestions, or report issues directly through the feedback portal

## Configuration

### Sync Interval

- **Interval Mode** (WorkManager)
  - Minimum: 15 minutes
  - Recommended: 30-60 minutes for most use cases
- **Scheduled Mode** (AlarmManager)
  - Sync at specific times of day (default: Morning 08:00 and Evening 21:00)
  - Add, remove, and toggle individual schedule entries
  - Uses exact alarms when available (Android 12+ permission dependent), with safe fallback

### Webhook Format

The app sends health data to your webhooks in JSON format. Each webhook request includes:

- Timestamp of the sync
- Data type information
- Health data records (filtered to only include new data since last sync)
- Metadata about the sync operation

> **Note**: Webhook delivery includes short retry handling (up to 3 attempts with exponential backoff). If delivery still fails, data is retried on the next successful sync trigger (manual, interval, or scheduled).

### Data Privacy

- All health data remains on your device until explicitly sent to your configured webhooks
- The app only reads data that you explicitly grant permission for
- No data is sent to third-party services except your configured webhooks
- You can revoke permissions at any time through Android settings

## Known Limitations

- ⚠️ **Offline Handling** - The app attempts to retry failed webhook requests briefly (3 retries). If the internet is unavailable, the sync fails safely and data is retried on the next successful sync trigger (manual, interval, or scheduled).
- 🕒 **48-Hour Lookback** - To ensure performance and relevance, the app scans for health data within a rolling 48-hour window. Data older than 48 hours may not be synced if the app was not running or configured during that time.

## Technical Details

### Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Health Data**: Health Connect SDK (AndroidX)
- **Background Work**: WorkManager
- **Scheduled Alarms**: AlarmManager (exact alarms where available)
- **Networking**: OkHttp
- **Serialization**: Kotlinx Serialization

### Key Components

- `MainActivity` - Main Entry Point & Navigation Host
- `HealthConnectManager` - Handles Health Connect data reading
- `SyncManager` - Manages data synchronization logic
- `SyncWorker` - Background worker for periodic syncing
- `ScheduledSyncManager` - Manages AlarmManager schedules for fixed-time syncing
- `ScheduledSyncReceiver` - Receives alarm broadcasts and triggers scheduled syncs
- `WebhookManager` - Handles webhook HTTP requests
- `PreferencesManager` - Manages app configuration and preferences
- `ConfigurationScreen` - Main settings UI
- `LogsScreen` - Displays webhook request/response logs
- `AboutScreen` - App info, feedback links, and settings export/import

### Permissions

The app requires the following permissions:

- Health Connect read permissions (for each selected data type)
- `READ_HEALTH_DATA_IN_BACKGROUND` - For background data access
- `READ_HEALTH_DATA_HISTORY` - To read historical records within the supported lookback window
- `INTERNET` - For webhook delivery
- `RECEIVE_BOOT_COMPLETED` - To restore scheduled syncs after reboot/app update
- `SCHEDULE_EXACT_ALARM` - For accurate scheduled sync times on supported Android versions

## Development

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/hcwebhook/app/
│   │   │   ├── MainActivity.kt          # Main Entry Point
│   │   │   ├── HCWebhookApplication.kt  # Application Class
│   │   │   ├── HealthConnectManager.kt  # Health Connect Logic
│   │   │   ├── SyncManager.kt           # Sync Logic & Scheduling
│   │   │   ├── SyncWorker.kt            # WorkManager Background Task
│   │   │   ├── WebhookManager.kt        # HTTP Client
│   │   │   ├── PreferencesManager.kt    # DataStore Preferences
│   │   │   ├── ScheduledSyncManager.kt  # Alarm Manager Logic
│   │   │   ├── ScheduledSyncReceiver.kt # Broadcast Receiver
│   │   │   ├── components/              # UI Components
│   │   │   ├── screens/                 # Composable Screens
│   │   │   └── ui/                      # Theme & Color
│   │   └── res/                         # Resources
└── build.gradle.kts                     # App-level build config
```

### Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).

```text
   Copyright 2026 MC.Naveen

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

## Privacy & Security

- HC Webhook does not collect, store, or transmit any personal data to third-party services
- All health data remains on your device until sent to your configured webhooks
- Webhook URLs are stored locally on your device
- You have full control over which data types are synced and where they are sent

## Support

For issues, feature requests, or questions, you can:

- Open an issue on GitHub
- Provide feedback directly through the app: Menu (⋮) → "Feedback" or visit [https://hc-webhook.feedbackjar.com/](https://hc-webhook.feedbackjar.com/)

## Acknowledgments

- Built with [Health Connect](https://developer.android.com/guide/health-and-fitness/health-connect) by Google
- UI designed with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Powered by [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable background processing

---

**Note**: This app requires Health Connect to be installed and properly configured on your device. Health Connect is available on Android 14+ devices or can be installed from the Play Store on compatible devices.
