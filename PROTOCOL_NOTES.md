# Divoom Bluetooth Protocol Specification & Notes

> [!WARNING]
> The protocol implementations in this application are derived from community reverse-engineering efforts and reference repositories. Divoom devices are unofficial implementations and protocol framing may vary across device revisions.

## Reference Sources

* **hass-divoom**: [d03n3rfr1tz3/hass-divoom](https://github.com/d03n3rfr1tz3/hass-divoom)
  - Referenced file: `custom_components/divoom/devices/backpack.txt`
* **divoom-python**: [DavidVentura/divoom](https://github.com/DavidVentura/divoom)
* **divoom-protocol**: Python community package

## Packet Structure (Legacy & Backpack RFCOMM / BLE)

```text
[START_BYTE 0x01] [LENGTH_LE 2-bytes] [COMMAND_BYTE] [PAYLOAD ...] [CHECKSUM_LE 2-bytes] [END_BYTE 0x02]
```

### Protocol Mechanics
1. **Start Byte**: `0x01`
2. **Length**: 2 bytes (Little Endian), calculated as `Payload Length + 2 (Checksum) + 1 (Cmd)`
3. **Command IDs**:
   - `0x44`: Set Static Image / Matrix Data
   - `0x74`: Set Brightness
   - `0x45`: Clear Screen / Reset Display Mode
4. **Checksum Calculation**:
   - Sum of all payload bytes + command byte
   - 16-bit integer (Little Endian)
5. **Byte Escaping (CRC/Payload Escaping)**:
   - If payload byte equals `0x01`, `0x02`, or `0x03`, it is escaped according to Divoom frame rules:
     - `0x01` -> `0x03 0x04`
     - `0x02` -> `0x03 0x05`
     - `0x03` -> `0x03 0x06`
6. **End Byte**: `0x02`

## Verification Status

| Device Model | Bluetooth Mode | Default Service / UUID | Verification Status |
| :--- | :--- | :--- | :--- |
| Pixoo Backpack (Original) | Classic RFCOMM | Channel 1 / 00001101-0000-1000-8000-00805F9B34FB | Community Protocol Implemented |
| Pixoo Backpack M | BLE / RFCOMM | Custom Service / 0000ffe0-0000-1000-8000-00805f9b34fb | Needs Hardware Verification |
| Divoom Cyberbag | Dual Mode | Auto-Detect | Needs Hardware Verification |
