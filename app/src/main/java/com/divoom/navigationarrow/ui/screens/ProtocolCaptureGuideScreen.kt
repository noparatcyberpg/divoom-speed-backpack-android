package com.divoom.navigationarrow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divoom.navigationarrow.ui.theme.AccentGreen
import com.divoom.navigationarrow.ui.theme.PrimaryNeon
import com.divoom.navigationarrow.ui.theme.SurfaceDark
import com.divoom.navigationarrow.ui.theme.SurfaceVariantDark
import com.divoom.navigationarrow.ui.theme.TextSecondary

@Composable
fun ProtocolCaptureGuideScreen(
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "1. เปิด Settings มือถือ -> Developer Options (โหมดนักพัฒนา)",
        "2. สวิตช์เปิด 'Enable Bluetooth HCI snoop log' (บันทึกบันทึก Bluetooth HCI)",
        "3. ปิดการใช้งาน Bluetooth และเปิดใหม่อีกครั้งเพื่อเริ่มบันทึกไฟล์ log",
        "4. เปิดแอปพลิเคชัน Divoom Official",
        "5. เชื่อมต่อกระเป๋า แล้วส่งภาพสีแดงล้วน (Solid Red 16x16)",
        "6. ส่งภาพสีเขียวล้วน (Solid Green 16x16)",
        "7. ส่งภาพลูกศรเลี้ยวซ้าย (Arrow Left)",
        "8. ส่งภาพลูกศรเลี้ยวขวา (Arrow Right)",
        "9. ปิดสวิตช์ Bluetooth HCI snoop log ใน Developer Options",
        "10. ดึงไฟล์ btsnoop_hci.log ออกมาจากตัวเครื่อง (ใช้ Bug Report หรือ adb pull) แล้วนำไฟล์มาใช้วิเคราะห์ร่วมกับ Python Script ใน tools/analyze_hci_log.py"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Protocol Capture Guide",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "คำแนะนำการเก็บ Bluetooth HCI Snoop Log จากแอป Divoom เพื่อวิเคราะห์ Protocol",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ขั้นตอนการเก็บ Bluetooth Capture Log:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNeon
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    steps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryNeon)
                                    .width(6.dp)
                                    .height(6.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "วิธีใช้งาน Python Tool:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "รันคำสั่งวิเคราะห์ไฟล์ log ในเครื่องของคุณ:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "python tools/analyze_hci_log.py --input btsnoop_hci.log --mac YOUR_DIVOOM_MAC",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = PrimaryNeon,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF070B14))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
