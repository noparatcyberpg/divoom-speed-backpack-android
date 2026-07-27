# Divoom Navigation Arrow (Native Android Kotlin App)

## Current navigation features

- Reads active turn instructions from Google Maps notifications.
- Shows left/right arrows only when the turn is 100 meters away or closer.
- Keeps straight, U-turn, roundabout, and destination symbols available.
- Adjusts animation pacing from on-device GPS motion without displaying speed.
- Shows randomized cute pixel cartoons while stopped or in traffic.
- Automatically reconnects to the selected Divoom backpack.

แอปพลิเคชัน Android แบบ Native Kotlin สำหรับเชื่อมต่อกระเป๋า **Divoom Pixoo Backpack** (จอ RGB 16×16) ผ่าน Bluetooth เพื่อแสดงผลลูกศรนำทาง Pixel Art

---

## ฟีเจอร์หลักใน PHASE 1 (Bluetooth Inspector & Packet Tester)

1. **Bluetooth Inspector Screen**:
   - แสดงรายการอุปกรณ์ Bluetooth ที่จับคู่ไว้ (Paired Devices)
   - แสดงชื่อ, MAC address, Device Type (BLE / Classic / Dual Mode) และ Bond State
   - ตรวจสอบและค้นหา GATT Services & Characteristics (สำหรับ BLE) พร้อมแสดง Property (`READ`, `WRITE`, `WRITE_NO_RESPONSE`, `NOTIFY`, `INDICATE`)
   - ตรวจสอบ SDP UUIDs และเปิด RFCOMM Socket (สำหรับ Bluetooth Classic)
   - มีระบบ Auto-Reconnect แบบ Exponential Backoff เมื่อการเชื่อมต่อหลุด

2. **HEX Packet Sender**:
   - ช่องทดลองส่ง Packet แบบ HEX ตรงไปยังกระเป๋า
   - จัดคิวการเขียน GATT (Write Queue) รอ Callback ก่อนส่ง Chunk ถัดไป
   - รองรับ MTU Negotiation (ขอสูงสุด 512 bytes)

3. **Display Test (16x16 Pixel Preview)**:
   - แสดงผล Pixel Art แบบ Live Preview ขนาด 16×16 ในแอป
   - ปุ่มกดทดสอบภาพพิกเซล ได้แก่:
     - Solid Red
     - Solid Green
     - Solid Blue
     - Arrow Left (ลูกศรซ้าย)
     - Arrow Right (ลูกศรขวา)
     - Arrow Straight (ตรงไป)
     - U-Turn (กลับรถ)
     - Destination (ถึงจุดหมาย)
     - Clear Screen (ล้างหน้าจอ)

4. **Live Log Viewer & Log Export**:
   - แสดง Log พร้อม Timestamp, UUID, Packet HEX, Connection State และ Error
   - ปุ่ม **Copy Logs** (คัดลอกลง Clipboard) และ **Export Logs** (แชร์ไฟล์/ข้อความ Log)

5. **Protocol Capture Guide & Python Analysis Tool**:
   - หน้าคู่มือแนะนำการเก็บ Bluetooth HCI Snoop Log
   - Python Script (`tools/analyze_hci_log.py`) สำหรับเปรียบเทียบ Packet สีแดง สีเขียว ลูกศรซ้าย และลูกศรขวา

---

## โครงสร้างโปรเจกต์ (Project Tree)

```
divoom/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           └── java/com/divoom/navigationarrow/
│               ├── MainActivity.kt
│               ├── DivoomApp.kt
│               ├── domain/
│               │   ├── model/
│               │   │   ├── ConnectionState.kt
│               │   │   ├── LogEntry.kt
│               │   │   ├── PixelBitmap.kt
│               │   │   └── TestPatterns.kt
│               │   ├── transport/
│               │   │   ├── DivoomTransport.kt
│               │   │   ├── BleDivoomTransport.kt
│               │   │   └── ClassicDivoomTransport.kt
│               │   └── protocol/
│               │       ├── DivoomDisplayProtocol.kt
│               │       ├── UnknownDivoomProtocol.kt
│               │       └── VerifiedDivoomBackpackProtocol.kt
│               ├── viewmodel/
│               │   └── MainViewModel.kt
│               └── ui/
│                   ├── theme/ (Color.kt, Theme.kt, Type.kt)
│                   ├── components/ (PixelCanvas.kt, LogViewer.kt, DeviceItem.kt)
│                   └── screens/ (BluetoothInspectorScreen.kt, HexSenderScreen.kt, DisplayTestScreen.kt, ProtocolCaptureGuideScreen.kt)
├── tools/
│   └── analyze_hci_log.py
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── protocol-analysis.md
├── troubleshooting.md
└── README.md
```

---

## ขั้นตอนการเปิดโปรเจกต์และ Build APK

### Requirements:
- Android Studio Hedgehog (2023.1.1) ขึ้นไป
- JDK 17
- Android SDK 34 (minSdk 26)

### ขั้นตอนเปิดโปรเจกต์ใน Android Studio:
1. เปิดโปรเจกต์ในโฟลเดอร์ `divoom`
2. รอ Android Studio Sync Gradle Dependencies
3. เมนู `Build` -> `Build Bundle(s) / APK(s)` -> `Build APK(s)`
4. ไฟล์ APK จะถูกสร้างไว้ที่:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## ขั้นตอนการทดสอบกับกระเป๋า Divoom จริง

> [!WARNING]
> ก่อนเริ่มทดสอบ ให้สั่ง **Force Stop (บังคับหยุด)** แอป Divoom Official บนมือถือก่อนเสมอ เพื่อป้องกันไม่ให้แอปหลักแย่งการเชื่อมต่อ Bluetooth

1. เปิดแอป **Divoom Navigation Arrow**
2. ในหน้า **Bluetooth Inspector**:
   - เลือกโหมด **BLE (GATT)** หรือ **Bluetooth Classic (RFCOMM)**
   - กดปุ่ม **Connect** ตรงชื่อกระเป๋า Divoom ของคุณ
3. ในหน้า **Display Test**:
   - ตรวจสอบว่า Protocol Toggle อยู่ที่ **Verified Divoom Backpack Protocol**
   - กดปุ่ม **Arrow Left** หรือ **Arrow Right**
   - จอพิกเซลที่กระเป๋า Divoom จะแสดงผลลูกศรตามที่เลือก
4. หากพบปัญหา สามารถกด **Copy Logs** หรือ **Export Logs** เพื่อนำ Log ไปวิเคราะห์เพิ่มเติม
