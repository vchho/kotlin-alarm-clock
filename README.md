# Clock Alarm App

A clean, modern Android clock and alarm application built with Kotlin and Jetpack Compose. Features a beautiful dashboard UI with the ability to set alarms that repeat daily or every other day.

## Features

- Live digital clock
- Time picker for setting alarms
- Repeat options for:
  - Daily
  - Every Other Day
- Alarm enable/disable toggle
- Notification when alarm fires
- Modern dark Material 3 dashboard UI

## Tech Stack

- Kotlin
- Jetpack Compose
- Android SDK 34
- Gradle
- Material 3

## Prerequisites

Before you build or run the app, install:

- Android Studio
- JDK 17+
- Android SDK with API 34
- An Android emulator or a physical Android device

## Clone the Repository

```bash
git clone https://github.com/vchho/kotlin-alarm-clock.git
cd kotlin-alarm-clock
```

## Open in Android Studio

1. Launch Android Studio
2. Click Open
3. Select the `kotlin-alarm-clock` folder
4. Let Gradle sync and download dependencies

## Run the Application

### Option 1: Run from Android Studio

1. Connect a device or start an emulator
2. Select the device in the toolbar
3. Click the green Run button
4. The app will install and launch automatically

### Option 2: Run from the terminal

```bash
./gradlew installDebug
adb shell am start -n com.example.clockalarmapp/.MainActivity
```

If `adb` is not on your PATH, use the Android SDK platform-tools folder directly.

## Build to an APK

### Debug APK

```bash
./gradlew assembleDebug
```

Generated APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Release APK

```bash
./gradlew assembleRelease
```

Generated APK:

```text
app/build/outputs/apk/release/app-release.apk
```

## Install APK on a Device

### Via adb

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

or

```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Via Android device

1. Copy the APK to the device
2. Open the APK file in the Files app
3. Grant install permission if prompted
4. Tap Install

## Notes

- The app requests notification permission on newer Android versions so alarms can appear properly.
- For exact alarm scheduling, Android may request access to exact alarms on supported versions.
- The app supports both daily alarms and every-other-day alarms.

## Troubleshooting

### Build errors

- Ensure JDK 17+ is installed
- Make sure Android Studio has the Android SDK and platform tools installed
- Re-sync Gradle: File → Sync Project with Gradle Files

### Device not detected

- Enable Developer Mode and USB debugging on the phone
- Check `adb devices`

### App won't run

- Use an emulator or device with Android 8.0+ (API 26+)
- Make sure the app has notification permission enabled
