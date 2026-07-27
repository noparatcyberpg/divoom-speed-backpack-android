# CONNECTION_DIAGNOSIS — why BLE connects but the screen never changes

**Date:** 2026-07-23  
**Project:** `com.divoom.navigationarrow` (existing codebase under OneDrive `divoom`, copied into workspace for fix)  
**Protocol generation chosen:** Modern Backpack M / TimeBox-Evo-audio (`FE EF AA 55` + seq high-byte session token) from [orionparrott/divoom-protocol](https://github.com/orionparrott/divoom-protocol) @ `f9b661f8e06a6d40be91c800fed656c7402bd2ac`

---

## What the old code did correctly

| Item | Evidence |
|------|----------|
| BLE GATT connect path | `BleDivoomTransport.connect()` → `BluetoothDevice.connectGatt()` |
| Request MTU then discover services | `onConnectionStateChange` → `requestMtu(512)` → `onMtuChanged` → `discoverServices()` |
| Service discovery logging | `logDiscoveredServices()` prints service/char UUIDs + READ/WRITE/WRITE_NO_RESPONSE/NOTIFY/INDICATE |
| Write queue + wait for `onCharacteristicWrite` when using WRITE_TYPE_DEFAULT | `startWriteLoop()` + `writeAckSignal` |
| Chunk by negotiated MTU − 3 | `sendPacket()` |
| Thai Force-Stop warning for Official app | `BluetoothInspectorScreen` banner |
| 16×16 preview canvas in-app | `PixelCanvas` / `DisplayTestScreen` |

---

## What the old code did wrong (root causes)

### 1. Wrong protocol generation (primary)

`VerifiedDivoomBackpackProtocol` wraps payloads in the **Classic Timebox/Pixoo SPP** frame:

`0x01 | LEN_L LEN_H | payload | CRC | 0x02` with `0x03`-escaping.

That matches `build_spp_packet()` in the reference repo (Classic RFCOMM), **not** the modern BLE envelope:

`FE EF AA 55 | len_lo len_hi | seq_hi seq_lo 00 00 00 | payload | crc_lo crc_hi`

Modern Backpack M firmware ACKs GATT writes but **silently ignores** Classic frames on the BLE UART characteristic.

**Files:** `VerifiedDivoomBackpackProtocol.kt` (entire class), `protocol-analysis.md` (documents the Classic hypothesis as “UNVERIFIED”).

### 2. No Divoom session initialization / handshake (critical)

GATT `STATE_CONNECTED` ≠ display path unlocked.

Reference protocol requires:

1. JSON `Device/SetUTC` as **first** command with seq `0x0001`
2. **Keystone:** bump seq so high byte becomes `0x01` (`0x0101` onward)
3. ~30-command iOS-verbatim init (timesync, `bd` queries, registerController, font blob, `bd 2f 02` / `0x31` unlock)

Old app never sent SetUTC or any init sequence. Device BLE stack ACK’d writes; AkOS display driver treated controller as unauthenticated and dropped every display command.

**Files:** `BleDivoomTransport.kt` (no `initializeSession`), `MainViewModel.sendTestPattern()` (encode → send only).

### 3. Wrong / ambiguous write characteristic selection

Reference confirmed write UUID:

- Service: `49535343-fe7d-4ae5-8fa9-9fafd205e455`
- Write: `49535343-8841-43f4-a8d4-ecbe34729bb3` (**8841**)
- Notify: `49535343-1e4d-4bd9-ba61-23c647249616`
- Note: `ACA3` may advertise as writable but is **inert for display** on this generation.

Old code auto-selected the **first** writable characteristic (`BleDivoomTransport.logDiscoveredServices` lines ~272–278), which can pick the wrong char.

### 4. `sendPacket` returned success before writes completed

`sendPacket()` enqueued chunks and immediately returned `Result.success(Unit)` without waiting for the write loop. UI logged “Successfully sent” even when the queue later failed/timed out.

### 5. Preferring WRITE_NO_RESPONSE blindly

Write loop always preferred `PROPERTY_WRITE_NO_RESPONSE` when present. Reference uses write-without-response for streaming paints (OK for this generation) but still requires correct envelope + init. Combined with (1)(2), this was not the sole failure.

### 6. No app-layer 138-byte chunking

Reference: iOS splits at **138 bytes** regardless of MTU; oversized single writes may be dropped silently. Old code only chunked by MTU−3.

### 7. Classic transport as default

`MainViewModel` defaulted `activeTransport = classicTransport` and `_selectedMode = CLASSIC`. Dual-mode Backpack M needs **BLE GATT** for the modern protocol.

### 8. Preview updated before send success

`sendTestPattern()` set `_currentBitmap` before confirming packet delivery.

### 9. Auto-reconnect / FGS permissions (out of scope noise)

Exponential auto-reconnect and FGS permissions were present; not the display failure, but stripped/disabled for this focused round.

---

## Why the screen didn’t change (summary)

1. Connected over GATT ✓  
2. Wrote *something* to a writable characteristic (maybe wrong UUID) ✓/✗  
3. Packets used Classic `01…02` framing ✗  
4. Never unlocked session via SetUTC + seq high-byte token ✗  
5. Device ACKed ATT writes; display driver ignored payload ✗  

**Result:** “HEX send has no effect” and “connect works but screen doesn’t change” — expected with this mismatch.

---

## Files / areas to fix

| Area | Action |
|------|--------|
| `domain/protocol/*` | Replace Classic encoder with `ModernBackpackBleProtocol` (envelope, session, static image, lighting, init) |
| `domain/transport/BleDivoomTransport.kt` | Wait for connect+discover; select 8841 UUID; session init; Mutex write queue; wait for write complete; 138-byte app chunking; diagnostics |
| `domain/transport/DivoomTransport.kt` | Match required interface (`initializeSession`, `writePacket`, `diagnostics`) |
| `viewmodel/MainViewModel.kt` | BLE default; Connect / Initialize split; send only after init; preview only on success |
| UI screens | Inspector + Main/Test Display per PHASE 2/7; Thai Force-Stop tip |
| Unit tests | Packet/encoder vectors vs Python reference |
| Manifest | Remove unused FGS permissions |

---

## Protocol generation chosen

**Modern Backpack M BLE** (`orionparrott/divoom-protocol`), commit `f9b661f8e06a6d40be91c800fed656c7402bd2ac`.

**Not auto-selected:** DavidVentura/divoom, hass-divoom, node-divoom-timebox-evo Classic SPP — used only as comparative notes. The reference itself reuses AA-frame *inner* layout concepts from node-divoom for animation uploads; static image path is the modern `0x44` + `F4 01 00` encoder.

---

## Confirmed vs unconfirmed from real device (this environment)

| Fact | Status |
|------|--------|
| Reference UUIDs / envelope / SetUTC keystone / encoder math | Confirmed in upstream docs + unit tests of reference |
| This machine’s physical Backpack GATT dump (name, MAC, bond, MTU, services) | **Unconfirmed** — no live device attached during this agent run |
| Screen actually turns red/green/arrow after fix | **Pending physical test** |
| Whether user’s bag is Backpack M vs older Pixoo Classic | **Unconfirmed** — Inspector must capture live GATT; if UUIDs differ, do not invent replacements |

Inspector “Copy/Export Device Report” is required on first hardware run to confirm UUID match before claiming display success.
