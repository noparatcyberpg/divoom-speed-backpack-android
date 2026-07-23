# Supported Divoom Devices

Below is the verification matrix for Divoom LED display backpacks and bags.

| Device Model | Target Resolution | Connection Mode | Protocol Version | Verification Status |
| :--- | :---: | :--- | :--- | :--- |
| **Pixoo Backpack** | 16x16 / 32x32 | Classic RFCOMM / BLE | Community Legacy Protocol | Untested (Requires Device) |
| **Backpack M** | 16x16 | BLE GATT / RFCOMM | Modern Backpack Protocol | Untested (Requires Device) |
| **Divoom Cyberbag** | Auto-Detect | Dual Mode | Cyberbag Extended Protocol | Untested (Requires Device) |

> [!NOTE]
> If your device shows `Protocol not verified`, use the built-in **Bluetooth Inspector** tool inside the app to capture GATT Services / Characteristics or SDP UUIDs and submit an issue on GitHub using the [Device Support Template](.github/ISSUE_TEMPLATE/device_support.yml).
