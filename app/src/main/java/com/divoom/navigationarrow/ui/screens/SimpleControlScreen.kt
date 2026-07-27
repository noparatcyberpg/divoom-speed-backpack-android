package com.divoom.navigationarrow.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divoom.navigationarrow.domain.model.ConnectionState
import com.divoom.navigationarrow.domain.model.PixelFrame
import com.divoom.navigationarrow.ui.components.PixelCanvas
import com.divoom.navigationarrow.ui.theme.AccentGreen
import com.divoom.navigationarrow.ui.theme.AccentOrange
import com.divoom.navigationarrow.ui.theme.AccentRed
import com.divoom.navigationarrow.ui.theme.PrimaryNeon
import com.divoom.navigationarrow.ui.theme.SecondaryNeon
import com.divoom.navigationarrow.ui.theme.SurfaceDark
import com.divoom.navigationarrow.ui.theme.SurfaceVariantDark
import com.divoom.navigationarrow.ui.theme.TextSecondary
import com.divoom.navigationarrow.viewmodel.MainViewModel
import com.divoom.navigationarrow.viewmodel.PairedDeviceInfo

@Composable
fun SimpleControlScreen(
    viewModel: MainViewModel,
    onRequestBluetoothPermission: () -> Unit,
    onRequestMotionPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val devices by viewModel.pairedDevices.collectAsState()
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val displayState by viewModel.displayState.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val discoveryError by viewModel.deviceDiscoveryError.collectAsState()
    val notificationAccessGranted by viewModel.notificationAccessGranted.collectAsState()
    val navigationModeEnabled by viewModel.navigationModeEnabled.collectAsState()
    val lastNavigationInstruction by viewModel.lastNavigationInstruction.collectAsState()
    val reconnectMessage by viewModel.reconnectMessage.collectAsState()
    val motionPermissionGranted by viewModel.motionPermissionGranted.collectAsState()
    val motionState by viewModel.motionState.collectAsState()

    val selectedDevice = devices.firstOrNull { it.address == selectedAddress }
    val ready = connectionState is ConnectionState.Connected && diagnostics.sessionInitialized
    val previewFrame = displayState.lastSuccessfullySentFrame
        ?: displayState.pendingFrame
        ?: PixelFrame.clear()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Divoom Arrow",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "เชื่อมต่อกระเป๋า แล้วกดลูกศรที่ต้องการ",
                color = TextSecondary
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2608)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ก่อนเชื่อมต่อ ให้ปิดแอป Divoom Official เพื่อไม่ให้แย่งสัญญาณกัน",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFFFE2A8),
                    fontSize = 13.sp
                )
            }
        }

        item {
            ConnectionCard(
                connectionState = connectionState,
                ready = ready,
                busy = busy,
                selectedDevice = selectedDevice,
                lastError = diagnostics.lastError,
                reconnectMessage = reconnectMessage,
                onConnect = { viewModel.connectDevice() },
                onDisconnect = { viewModel.disconnectDevice() }
            )
        }

        if (ready) {
            item {
                NavigationModeCard(
                    ready = true,
                    accessGranted = notificationAccessGranted,
                    motionGranted = motionPermissionGranted,
                    enabled = navigationModeEnabled,
                    hasMotionFix = motionState.hasFix,
                    isStopped = motionState.isStopped,
                    lastInstruction = lastNavigationInstruction?.text,
                    lastManeuver = lastNavigationInstruction?.maneuver?.thaiLabel,
                    onOpenAccessSettings = { viewModel.openNavigationAccessSettings() },
                    onRequestMotionPermission = onRequestMotionPermission,
                    onEnabledChange = { viewModel.setNavigationModeEnabled(it) },
                    onOpenGoogleMaps = { viewModel.openGoogleMaps() }
                )
            }
        }

        if (!ready) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("เลือกกระเป๋า", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "ค้นหาได้เลย ไม่จำเป็นต้อง Pair ก่อน",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isScanning) viewModel.stopDeviceScan()
                            else viewModel.scanForDevices()
                        }
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "ค้นหาใหม่")
                        }
                    }
                }
            }

            if (devices.isEmpty()) {
                item {
                    EmptyDeviceCard(
                        isScanning = isScanning,
                        error = discoveryError,
                        onScan = { viewModel.scanForDevices() },
                        onRequestPermission = onRequestBluetoothPermission
                    )
                }
            } else {
                items(
                    count = devices.size,
                    key = { devices[it].address }
                ) { index ->
                    val device = devices[index]
                    DeviceChoice(
                        device = device,
                        selected = selectedAddress == device.address,
                        onClick = { viewModel.selectDevice(device.address) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("หน้าจอกระเป๋า", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = when {
                                displayState.pendingFrame != null -> "กำลังส่ง…"
                                displayState.lastError != null -> "ส่งไม่สำเร็จ"
                                displayState.lastSuccessfullySentFrame != null -> "ส่งแล้ว"
                                else -> "ตัวอย่าง"
                            },
                            color = when {
                                displayState.lastError != null -> AccentRed
                                displayState.lastSuccessfullySentFrame != null -> AccentGreen
                                else -> TextSecondary
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    PixelCanvas(
                        bitmap = previewFrame,
                        modifier = Modifier.fillMaxWidth(0.62f)
                    )
                    if (displayState.lastError != null) {
                        Text(
                            text = displayState.lastError.orEmpty(),
                            color = AccentRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Text("เลือกลูกศร", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                if (navigationModeEnabled) "ปิดโหมด Google Maps ก่อนเลือกสัญลักษณ์เอง"
                else if (ready) "กดแล้วภาพจะถูกส่งขึ้นกระเป๋าทันที"
                else "เชื่อมต่อกระเป๋าก่อนเพื่อเริ่มใช้งาน",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ArrowActionButton(
                    label = "ตรงไป",
                    icon = Icons.Default.ArrowUpward,
                    enabled = ready && !busy && !navigationModeEnabled,
                    color = PrimaryNeon,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.sendTestPattern("Arrow Straight") }
                )
                ArrowActionButton(
                    label = "กลับรถ",
                    icon = Icons.AutoMirrored.Filled.Undo,
                    enabled = ready && !busy && !navigationModeEnabled,
                    color = AccentOrange,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.sendTestPattern("U-Turn") }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ArrowActionButton(
                    label = "เลี้ยวซ้าย",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    enabled = ready && !busy && !navigationModeEnabled,
                    color = SecondaryNeon,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.sendTestPattern("Arrow Left") }
                )
                ArrowActionButton(
                    label = "เลี้ยวขวา",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = ready && !busy && !navigationModeEnabled,
                    color = SecondaryNeon,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.sendTestPattern("Arrow Right") }
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { viewModel.sendTestPattern("Clear Screen") },
                enabled = ready && !busy && !navigationModeEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Text("  ล้างหน้าจอ")
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ความสว่าง", fontWeight = FontWeight.Bold)
                        Text("$brightness%", color = PrimaryNeon, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = brightness.toFloat(),
                        onValueChange = { viewModel.setBrightness(it.toInt()) },
                        onValueChangeFinished = { viewModel.sendBrightness() },
                        enabled = ready && !busy,
                        valueRange = 10f..100f
                    )
                    Text(
                        "เลื่อนแล้วปล่อยเพื่อส่งค่าความสว่าง",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun NavigationModeCard(
    ready: Boolean,
    accessGranted: Boolean,
    motionGranted: Boolean,
    enabled: Boolean,
    hasMotionFix: Boolean,
    isStopped: Boolean,
    lastInstruction: String?,
    lastManeuver: String?,
    onOpenAccessSettings: () -> Unit,
    onRequestMotionPermission: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onOpenGoogleMaps: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) Color(0xFF123A2C) else SurfaceDark
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (enabled) AccentGreen else SurfaceVariantDark,
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = if (enabled) AccentGreen else PrimaryNeon,
                    modifier = Modifier.size(28.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text("ตาม Google Maps", fontWeight = FontWeight.Bold)
                    Text(
                        "ลูกศรเลี้ยวขึ้นก่อนถึง 100 ม. • ปรับจังหวะตามรถ",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                if (accessGranted && motionGranted) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange,
                        enabled = ready
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            when {
                !accessGranted -> {
                    Text(
                        "อนุญาตครั้งเดียว แล้วเปิดสิทธิ์ให้ Divoom Arrow ในหน้าถัดไป",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onOpenAccessSettings,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("อนุญาตคำสั่งนำทาง")
                    }
                }

                !motionGranted -> {
                    Text(
                        "อนุญาตการเคลื่อนไหวเพื่อปรับความไวของแอนิเมชัน โดยจะไม่แสดงตัวเลข",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onRequestMotionPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("อนุญาตการเคลื่อนไหว")
                    }
                }

                !ready -> {
                    Text(
                        "เชื่อมต่อกระเป๋าให้พร้อม แล้วจึงเปิดโหมดนี้",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                enabled -> {
                    Text(
                        "เปิดอยู่ • กลับไปที่ Google Maps แล้วกดเริ่มนำทางได้เลย",
                        color = AccentGreen,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Text(
                        text = when {
                            isStopped -> "รถหยุดอยู่ • กำลังสุ่มการ์ตูนกวนๆ บนกระเป๋า"
                            !hasMotionFix -> "กำลังจับการเคลื่อนไหว…"
                            else -> "สัญลักษณ์กำลังเล่นต่อเนื่องและปรับจังหวะอัตโนมัติ"
                        },
                        color = if (isStopped) AccentOrange else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isStopped) FontWeight.Bold else FontWeight.Normal
                    )
                    if (lastInstruction != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            lastManeuver ?: "คำสั่งล่าสุด",
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            lastInstruction,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenGoogleMaps,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("เปิด Google Maps")
                    }
                }

                else -> {
                    Text(
                        "เปิดสวิตช์ แล้วใช้ Google Maps นำทางตามปกติ",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    connectionState: ConnectionState,
    ready: Boolean,
    busy: Boolean,
    selectedDevice: PairedDeviceInfo?,
    lastError: String?,
    reconnectMessage: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    val statusText = when {
        ready -> "พร้อมใช้งาน"
        reconnectMessage != null -> reconnectMessage
        busy || connectionState is ConnectionState.Connecting -> "กำลังเชื่อมต่อและเตรียมหน้าจอ…"
        connectionState is ConnectionState.Error -> "เชื่อมต่อไม่สำเร็จ"
        connectionState is ConnectionState.Connected -> "เชื่อมต่อแล้ว แต่เตรียมหน้าจอไม่สำเร็จ"
        else -> "ยังไม่ได้เชื่อมต่อ"
    }
    val statusColor = when {
        ready -> AccentGreen
        reconnectMessage != null -> PrimaryNeon
        busy || connectionState is ConnectionState.Connecting -> PrimaryNeon
        connectionState is ConnectionState.Error || lastError != null -> AccentRed
        else -> TextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (ready) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold)
                    Text(
                        selectedDevice?.name ?: "ยังไม่ได้เลือกอุปกรณ์",
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
                if (ready) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                }
            }
            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                )
                Text(
                    "ครั้งแรกใช้เวลาประมาณ 3 วินาที",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            } else {
                Spacer(Modifier.height(14.dp))
                if (ready) {
                    OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                        Text("ยกเลิกการเชื่อมต่อ")
                    }
                } else {
                    Button(
                        onClick = onConnect,
                        enabled = selectedDevice != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (connectionState is ConnectionState.Error || lastError != null)
                                "ลองเชื่อมต่ออีกครั้ง"
                            else "เชื่อมต่อกระเป๋า"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDeviceCard(
    isScanning: Boolean,
    error: String?,
    onScan: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isScanning) Icons.Default.Search else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = PrimaryNeon,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isScanning) "กำลังค้นหากระเป๋าใกล้เคียง…"
                else error ?: "ยังไม่พบกระเป๋า Divoom",
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = if (error?.contains("อนุญาต") == true) onRequestPermission else onScan,
                enabled = !isScanning
            ) {
                Text(if (error?.contains("อนุญาต") == true) "อนุญาต Bluetooth" else "ค้นหาอีกครั้ง")
            }
        }
    }
}

@Composable
private fun DeviceChoice(
    device: PairedDeviceInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF123A3D) else SurfaceDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (selected) PrimaryNeon else SurfaceVariantDark,
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.Bold)
                Text(
                    "${device.bondState}${device.rssi?.let { "  •  สัญญาณ $it dBm" } ?: ""}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ArrowActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(82.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
