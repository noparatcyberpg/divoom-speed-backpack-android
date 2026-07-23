# Divoom Speed Backpack (แอปพลิเคชัน Android ภาษาไทย)

แอปพลิเคชัน Android Native (ภาษา Kotlin) สำหรับส่งข้อมูลความเร็วจาก GPS แบบเรียลไทม์ไปยังหน้าจอกระเป๋า **Divoom Pixoo Backpack**, **Backpack M** หรือ **Divoom Cyberbag** ผ่านทาง Bluetooth

## ฟีเจอร์หลัก

1. **ส่งความเร็ว GPS แบบเรียลไทม์**: แปลงความเร็วจาก m/s เป็น km/h พร้อมระบบกรองความเร็วต่ำ (< 2 km/h) และ EMA smoothing
2. **วาด Pixel Art ตัวเลขใหญ่**: ใช้ Pixel Font Matrix ขนาด 16x16, 32x32 และ 64x64 อ่านง่ายแม้ขณะขับขี่
3. **เปลี่ยนสีตามความเร็ว**:
   - `0 - 30 km/h`: สีเขียว
   - `31 - 60 km/h`: สีเหลือง
   - `61 - 90 km/h`: สีส้ม
   - `> 90 km/h`: สีแดง
4. **Foreground Service**: ทำงานได้ต่อเนื่องเมื่อพับหน้าจอ ออกจากแอป หรือหน้าจอดับ
5. **เชื่อมต่อใหม่อัตโนมัติ (Auto-Reconnect)**: ระบบ Exponential Backoff ลองเชื่อมต่อใหม่เมื่อสัญญาณหลุด (1s, 2s, 4s, 8s สูงสุด 30s)
6. **Bluetooth Inspector**: เมนูตรวจสอบ RFCOMM UUID, BLE GATT Services/Characteristics, MTU และทดลองส่ง HEX Packet
7. **Demo Simulation Mode**: จำลองความเร็ว 0-120 km/h เพื่อทดสอบ UI และการแสดงผลโดยไม่ต้องออกเดินทางจริง

## วิธีการใช้งาน

1. เปิด Bluetooth และ GPS บนมือถือ
2. เปิดแอป **Divoom Speed Backpack** และอนุญาตสิทธิ์ Location, Bluetooth Scan/Connect และ Notification
3. ไปที่หน้า **Select Device** เลือกกระเป๋า Divoom ของคุณ
4. กด **Connect** และกด **Start Realtime** เพื่อเริ่มส่งความเร็วขึ้นกระเป๋า
5. สามารถไปที่หน้า **Test Display** เพื่อทดสอบส่งสีแดง สีเขียว สีน้ำเงิน หรือตัวเลขทดสอบ 50 km/h ขึ้นกระเป๋าได้ทันที

## การสร้าง APK จาก Source Code

```bash
git clone https://github.com/noparatcyberpg/divoom-speed-backpack-android.git
cd divoom-speed-backpack-android
./gradlew assembleDebug
```
ไฟล์ APK จะถูกสร้างที่ `app/build/outputs/apk/debug/app-debug.apk`

## สัญญาณเตือนความเป็นส่วนตัว (Privacy)

- พิกัด ละติจูด/ลองจิจูด จะ**ไม่**ถูกบันทึกลงเครื่องหรือส่งออกนอกอุปกรณ์
- หมายเลข MAC Address ใน Log จะถูกปิดบังบางส่วนเสมอ (`AA:BB:CC:**:**:FF`)
