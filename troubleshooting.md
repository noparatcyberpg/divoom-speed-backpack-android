# Divoom Navigation Arrow - Troubleshooting Guide

## Common Issues & Solutions

### 1. "Bluetooth connection fails or drops immediately"
- **Cause**: The official Divoom app is running in the background and competing for the GATT/RFCOMM socket connection.
- **Solution**:
  1. Open Android Settings -> Apps -> **Divoom**.
  2. Tap **Force Stop** (บังคับหยุด).
  3. Turn Bluetooth OFF and ON again.
  4. Retry connecting in **Bluetooth Inspector**.

---

### 2. "Device connects but display does not update when pressing Arrow Left/Right"
- **Cause**: Incorrect Write Characteristic selected or `UnknownDivoomProtocol` is active.
- **Solution**:
  1. Go to **Display Test** tab.
  2. Ensure the top toggle is set to **Verified Divoom Backpack Protocol**.
  3. Go to **Bluetooth Inspector** tab, check the Live Log Viewer to ensure `writeCharacteristic` or `sendPacket` reports success (`[TX]`).
  4. If using BLE, select the correct Write Characteristic from the discovered GATT list or enter its UUID manually.

---

### 3. "Bluetooth permission denied on Android 12+"
- **Cause**: `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` runtime permissions were not granted.
- **Solution**:
  1. Go to Android Settings -> Apps -> **Divoom Arrow** -> Permissions.
  2. Allow **Nearby Devices** (อุปกรณ์ที่อยู่ใกล้เคียง) and **Location** permissions.

---

### 4. "GATT status 133 or 257 error on Android"
- **Cause**: Android OS internal GATT connection table limit reached.
- **Solution**:
  1. Toggle Airplane Mode ON for 5 seconds, then OFF.
  2. Restart the app.
