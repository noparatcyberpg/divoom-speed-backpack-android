# Changelog

All notable changes to this project will be documented in this file.

## [0.1.0] - 2026-07-23

### Added
- Initial project release of **Divoom Speed Backpack**.
- Bluetooth device scanner and paired device connection picker.
- Dynamic transport engine (`ClassicRfcommTransport`, `BleGattTransport`, `AutoDetectDivoomTransport`, `FakeDivoomTransport`).
- Real-time GPS Speed Tracker with Exponential Moving Average (EMA) smoothing and accuracy filtering.
- Pixel Art Renderer with custom 16x16 / 32x32 / 64x64 matrix digit font.
- Dynamic speed color thresholds (Green <= 30 km/h, Yellow <= 60 km/h, Orange <= 90 km/h, Red > 90 km/h).
- Divoom Protocol Encoders for legacy, modern backpack, and cyberbag devices with packet checksum and escaping.
- Background Foreground Service (`SpeedDisplayService`) with sticky notifications and exponential backoff auto-reconnect.
- Diagnostic **Bluetooth Inspector** UI and **Test Display** control suite.
- Structured Debug Logging with MAC address privacy masking.
- GitHub Actions CI/CD workflows and automated unit test suite.
