# Health Connect to Webhook (Beta)

![HC Webhook](image.png)

An Android application that seamlessly connects Health Connect data to your webhooks, enabling automated health data synchronization to your custom endpoints.

## Overview

HC Webhook is a powerful Android app that bridges the gap between Google Health Connect and your webhook infrastructure. It automatically reads health data from Health Connect and sends it to your configured webhook URLs at customizable intervals, making it easy to integrate health data into your own systems, analytics platforms, or third-party services.

## Features

- 🔄 **Automated Background Sync** - Configurable sync intervals (minimum 15 minutes) using WorkManager
- 🎯 **Selective Data Types** - Choose which health data types to sync (17 supported types)
- 🔗 **Multiple Webhooks** - Send data to multiple webhook URLs simultaneously
- 📊 **Manual Sync** - Trigger immediate data synchronization on demand
- 📝 **Webhook Logs** - View detailed logs of all webhook requests and responses
- 🔐 **Permission Management** - Granular Health Connect permission handling
- 🎨 **Modern UI** - Built with Jetpack Compose and Material 3 design
- ⚡ **Real-time Status** - Visual indicators for permission status and sync state

## Supported Health Data Types

The app supports reading and syncing the following health data types from Health Connect:

1. **Steps** - Daily step count
2. **Sleep** - Sleep sessions with stages
3. **Heart Rate** - Heart rate measurements
4. **Distance** - Distance traveled
5. **Active Calories** - Calories burned during activity
6. **Total Calories** - Total calories burned
7. **Weight** - Body weight measurements
8. **Height** - Height measurements
9. **Blood Pressure** - Systolic and diastolic readings
10. **Blood Glucose** - Blood glucose levels
11. **Oxygen Saturation** - SpO2 measurements
12. **Body Temperature** - Body temperature readings
13. **Respiratory Rate** - Breathing rate measurements
14. **Resting Heart Rate** - Resting heart rate data
15. **Exercise Sessions** - Workout and exercise data
16. **Hydration** - Water intake tracking
17. **Nutrition** - Nutritional information (calories, protein, carbs, fat)

## Requirements

- **Android 8.0 (API 26)** or higher
- **Google Health Connect** app installed and set up
- **Internet connection** for webhook delivery

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
   - Set your preferred sync interval (minimum 15 minutes)

4. **Save Configuration**
   - Tap "Save Configuration" to start automatic syncing

### Manual Sync

- Tap the "Sync Now" button in the Manual Sync section to immediately sync all enabled data types to your webhooks

### Viewing Logs

- Access webhook logs from the menu (⋮) → "Webhook Log"
- View detailed information about each webhook request, including timestamps, status codes, and response data

## Configuration

### Sync Interval

- Minimum: 15 minutes
- Recommended: 30-60 minutes for most use cases
- The app uses WorkManager for reliable background syncing

### Webhook Format

The app sends health data to your webhooks in JSON format. Each webhook request includes:

- Timestamp of the sync
- Data type information
- Health data records (filtered to only include new data since last sync)
- Metadata about the sync operation

> **⚠️ Warning**: Internet retry functionality is not implemented yet. If a webhook request fails due to network issues, the app will not automatically retry. Use at your own risk.

### Data Privacy

- All health data remains on your device until explicitly sent to your configured webhooks
- The app only reads data that you explicitly grant permission for
- No data is sent to third-party services except your configured webhooks
- You can revoke permissions at any time through Android settings

## Known Limitations

- ⚠️ **Internet Retry Not Implemented** - The app does not currently implement automatic retry logic for failed webhook requests due to network issues. If a sync fails due to internet connectivity problems, it will not be automatically retried. This feature may be added in future releases. **Use at your own risk.**
- ⚠️ **No Day Limitation** - There is currently no limitation on the date range or number of days of historical data that can be synced. This may result in large data transfers for users with extensive health data history. **Use at your own risk.**

## Technical Details

### Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Health Data**: Health Connect SDK (AndroidX)
- **Background Work**: WorkManager
- **Networking**: OkHttp
- **Serialization**: Kotlinx Serialization

### Key Components

- `MainActivity` - Main configuration UI
- `HealthConnectManager` - Handles Health Connect data reading
- `SyncManager` - Manages data synchronization logic
- `SyncWorker` - Background worker for periodic syncing
- `WebhookManager` - Handles webhook HTTP requests
- `PreferencesManager` - Manages app configuration and preferences
- `LogsActivity` - Displays webhook request/response logs

### Permissions

The app requires the following permissions:

- Health Connect read permissions (for each selected data type)
- `READ_HEALTH_DATA_IN_BACKGROUND` - For background data access
- `INTERNET` - For webhook delivery

## Development

### Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/hcwebhook/app/
│   │   │   ├── MainActivity.kt          # Main UI
│   │   │   ├── HealthConnectManager.kt  # Health Connect integration
│   │   │   ├── SyncManager.kt          # Sync logic
│   │   │   ├── SyncWorker.kt           # Background worker
│   │   │   ├── WebhookManager.kt       # Webhook HTTP client
│   │   │   ├── PreferencesManager.kt    # Configuration storage
│   │   │   ├── LogsActivity.kt         # Log viewer
│   │   │   └── AboutActivity.kt        # About screen
│   │   └── res/                         # Resources
│   └── test/                            # Unit tests
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

**Source Code Only - All Rights Reserved**

This project is provided as source code for personal use and educational purposes only. 

**Restrictions:**
- ❌ **No Republishing** - You may not republish, redistribute, or share this code in any form
- ❌ **No Monetization** - You may not use this code for commercial purposes or monetize any derivative works
- ✅ **Source Code Only** - This repository contains source code only, not a published application

You are permitted to:
- View and study the source code
- Build and use the application for personal use
- Learn from the implementation

Any other use requires explicit written permission from the author.

## Privacy & Security

- HC Webhook does not collect, store, or transmit any personal data to third-party services
- All health data remains on your device until sent to your configured webhooks
- Webhook URLs are stored locally on your device
- You have full control over which data types are synced and where they are sent

## Support

For issues, feature requests, or questions, please open an issue on GitHub.

## Acknowledgments

- Built with [Health Connect](https://developer.android.com/guide/health-and-fitness/health-connect) by Google
- UI designed with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Powered by [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for reliable background processing

---

**Note**: This app requires Health Connect to be installed and properly configured on your device. Health Connect is available on Android 14+ devices or can be installed from the Play Store on compatible devices.

