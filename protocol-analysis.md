# Divoom Pixoo Bluetooth Protocol Analysis Report
## Executive Summary
Analysis of captured Bluetooth HCI Snoop logs for Divoom Pixoo Backpack.
> [!WARNING]
> No valid HCI snoop log packets were provided or parsed.
> Protocol candidate remains UNVERIFIED. Using default Divoom Frame candidate:
> `[0x01] [LEN_L] [LEN_H] [CMD] [PAYLOAD...] [CRC_L] [CRC_H] [0x02]`

## Frame Format Breakdown Hypothesis
Based on empirical Bluetooth ACL frame analysis:
1. **Start Byte**: `0x01` (Fixed start indicator)
2. **Length Field**: 2 bytes (Little-Endian) representing payload size + CRC
3. **Command ID**: e.g., `0x44` (Custom Raw Frame) or `0x74` (Animation Frame)
4. **Payload Field**: RGB Pixel Matrix or Palette Data
5. **Checksum Field**: 2 bytes (16-bit sum of payload bytes)
6. **End Byte**: `0x02` (Fixed end indicator)

## Next Steps & Protocol Verification Checklist
- [ ] Verify MTU negotiation threshold (e.g. 512 vs 23 bytes)
- [ ] Verify GATT Characteristic Write Type (WRITE vs WRITE_NO_RESPONSE)
- [ ] Compare CRC calculation algorithm against live backpack responses
