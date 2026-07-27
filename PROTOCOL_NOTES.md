# PROTOCOL_NOTES

## Reference

- Repository: https://github.com/orionparrott/divoom-protocol
- Commit: `f9b661f8e06a6d40be91c800fed656c7402bd2ac` (`f9b661f chore: add PyPI/license/python version badges`)
- PyPI package: `divoom-protocol`
- Target family: Divoom Backpack M / TimeBox-Evo-audio (Anyka 105xE / AkOS)
- Status: community reverse-engineered — **not** Official Divoom API

## BLE UUIDs (from reference `client.py`, confirmed via iOS HCI capture)

| Role | UUID |
|------|------|
| UART Service | `49535343-fe7d-4ae5-8fa9-9fafd205e455` |
| Write characteristic | `49535343-8841-43f4-a8d4-ecbe34729bb3` |
| Notify characteristic | `49535343-1e4d-4bd9-ba61-23c647249616` |

Do **not** use the `ACA3` write characteristic for display on this generation (advertised but inert).

## Envelope (`packet.py`)

```
FE EF AA 55 | content_len_lo content_len_hi | seq_hi seq_lo 00 00 00 | payload | crc_lo crc_hi
```

- `content_len` = 2 (seq) + 3 (zeros) + len(payload) + 2 (crc)
- CRC = uint16 sum of (len_lo + len_hi + all content_data bytes), little-endian

## Session keystone (`session.py`)

1. First command = JSON SetUTC with seq `0x0001`
2. Immediately bump seq to `0x0101` (high byte = session token)
3. Without this jump, GATT ACKs succeed but display commands are ignored

## Static image (`encoder.py` + `commands.static_image`)

1. Preamble: `bd 31 SLOT 01` then `9f CONFIRM` (CONFIRM = SLOT + 0xB1; SLOT += 3 per image)
2. Inner `0x44` payload with mystery bytes `F4 01 00`, palette RGB, LSB-first indices (no bit-reversal)
3. Wrap in FE EF AA 55 envelope
4. App-layer BLE chunking at **138 bytes** (iOS match), independent of MTU

## Solid color shortcut

`45 01 RR GG BB BRI 00 01` (Lighting channel) — used optionally for solid fills; Pixel Art uses `0x44`.

## Init pacing

~60 ms between init commands; ~1 s pause before `deviceInfo`; end with `bd 2f 02` then `0x31` **once** (re-sending disconnects).

## Android port notes

- All bytes treated as unsigned via `and 0xFF`
- Little-endian length/CRC fields
- Kotlin class: `ModernBackpackBleProtocol` (not named “Verified” until physical display confirmed)
- Old Classic `01…02` protocol retained only as historical dead code path removed from UI

## Not used automatically

- DavidVentura/divoom
- d03n3rfr1tz3/hass-divoom
- RomRider/node-divoom-timebox-evo Classic SPP framing

(AA inner frame concepts from node-divoom are used upstream for *animation* uploads only; this round focuses on static image + lighting.)
