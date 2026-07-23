# Divoom Speed Backpack Android App

[![Android CI](https://github.com/noparatcyberpg/divoom-speed-backpack-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/noparatcyberpg/divoom-speed-backpack-android/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.org/badge/License-MIT-yellow.svg)](LICENSE)

A native Android application written in Kotlin to stream real-time GPS speed directly to **Divoom Pixoo Backpack**, **Backpack M**, and **Divoom Cyberbag** displays via Bluetooth (Classic RFCOMM / BLE GATT).

![App Mockup](https://via.placeholder.com/800x400.png?text=Divoom+Speed+Backpack+App)

## Features

- 🚴 **Real-time Speed Streaming**: Reads GPS speed and converts m/s to km/h with configurable 1s - 2s update intervals.
- 🎨 **Matrix Pixel Font Renderer**: Generates crisp pixel art digits (0-99 in large matrix font, 100-199 in compact matrix font) and `KMH` indicators for 16x16, 32x32, and 64x64 pixel matrices.
- 🌈 **Speed-Based Color Thresholds**:
  - `0 - 30 km/h`: Vibrant Green
  - `31 - 60 km/h`: Energetic Yellow
  - `61 - 90 km/h`: Warning Orange
  - `> 90 km/h`: High-Speed Red
- 🔋 **Background Foreground Service**: Continues tracking speed and updating display even when the screen is off or app is minimized.
- 🔄 **Auto Reconnect Engine**: Automatic exponential backoff re-connection (1s, 2s, 4s, 8s, up to 30s) when Bluetooth signal drops.
- 🔍 **Bluetooth Inspector**: Real-time diagnostic tool for scanning SDP UUIDs, BLE GATT Services/Characteristics, MTU size, and sending manual HEX packets.
- 📋 **Debug Log Manager**: Anonymized logging with copy, share, and export functions.
- 🧪 **Demo Simulation Mode**: Test UI and display rendering offline from 0 to 120 km/h without leaving your desk.

## Architecture

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Repository Pattern
- **Async & Concurrency**: Kotlin Coroutines + StateFlow + Mutex
- **Location API**: Google Play Services `FusedLocationProviderClient`
- **Storage**: AndroidX DataStore Preferences

## Quick Setup & Installation

### Requirements
- Android 8.0 (API Level 26) or higher.
- Bluetooth & GPS Location permissions enabled.

### Building from Source
```bash
git clone https://github.com/noparatcyberpg/divoom-speed-backpack-android.git
cd divoom-speed-backpack-android
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

## Disclaimer

This application uses unofficial community protocols for Divoom devices. See [PROTOCOL_NOTES.md](PROTOCOL_NOTES.md) for detailed packet specifications.

## License

This project is licensed under the [MIT License](LICENSE).
