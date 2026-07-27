package com.divoom.navigationarrow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divoom.navigationarrow.domain.model.ConnectionState
import com.divoom.navigationarrow.domain.model.PixelFrame
import com.divoom.navigationarrow.ui.components.LogViewer
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DisplayTestScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val displayState by viewModel.displayState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val logs by viewModel.logList.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val debugMode by viewModel.debugMode.collectAsState()

    val previewFrame: PixelFrame =
        displayState.lastSuccessfullySentFrame
            ?: displayState.pendingFrame
            ?: PixelFrame.clear()

    val testButtons = listOf(
        "Solid Red",
        "Solid Green",
        "Solid Blue",
        "Checkerboard",
        "Arrow Left",
        "Arrow Right",
        "Send 50",
        "Clear Screen"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange)
                    Spacer(modifier.padding(start = 8.dp))
                    Text(
                        text = "กรุณาปิดหรือ Force Stop แอป Divoom Official ก่อนทดสอบ\nเพื่อป้องกันแอปทั้งสองแย่งการเชื่อมต่อกับกระเป๋า",
                        color = Color(0xFFFED7AA),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Text("Main — Connection / Test Display / Diagnostics", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Connection section
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Connection", fontWeight = FontWeight.Bold, color = PrimaryNeon)
                    val cs = when (connectionState) {
                        is ConnectionState.Connected -> "CONNECTED ${(connectionState as ConnectionState.Connected).deviceName}"
                        is ConnectionState.Connecting -> "CONNECTING"
                        is ConnectionState.Error -> "ERROR: ${(connectionState as ConnectionState.Error).message}"
                        else -> "DISCONNECTED"
                    }
                    Text("State: $cs")
                    Text("Services discovered: ${diagnostics.servicesDiscovered}")
                    Text("Session initialized: ${diagnostics.sessionInitialized}")
                    Text("Service UUID: ${diagnostics.serviceUuid ?: "-"}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("Write Char: ${diagnostics.characteristicUuid ?: "-"}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("MTU: ${diagnostics.mtu ?: "-"}")
                    Spacer(modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.connectDevice() }, enabled = !busy, modifier = Modifier.weight(1f)) {
                            Text(if (debugMode) "Connect" else "Connect+Init", fontSize = 12.sp)
                        }
                        if (debugMode) {
                            Button(
                                onClick = { viewModel.initializeSession() },
                                enabled = !busy,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                            ) { Text("Initialize", fontSize = 12.sp) }
                        }
                        Button(
                            onClick = { viewModel.disconnectDevice() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                        ) { Text("Disconnect", fontSize = 12.sp) }
                    }
                    Button(
                        onClick = { viewModel.copyLogsToClipboard() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Copy Log") }
                }
            }
        }

        // Preview
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Last successfully sent frame",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "(Not a live read of the backpack framebuffer)",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier.height(8.dp))
                PixelCanvas(bitmap = previewFrame, modifier = Modifier.fillMaxWidth(0.72f))
                Spacer(modifier.height(6.dp))
                val sentAt = displayState.lastSentAt?.let {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
                } ?: "-"
                Text("Packets: ${displayState.lastPacketCount} · Bytes: ${displayState.lastBytesSent} · At: $sentAt", fontSize = 12.sp)
                Text(
                    text = displayState.lastSendResult ?: displayState.lastError ?: "No frame sent yet",
                    fontSize = 12.sp,
                    color = if (displayState.lastError != null) AccentRed else AccentGreen
                )
            }
        }

        // Test Display
        item {
            Text("Test Display", fontWeight = FontWeight.Bold, color = PrimaryNeon)
            Spacer(modifier.height(6.dp))
            testButtons.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { name ->
                        val color = when (name) {
                            "Solid Red" -> AccentRed
                            "Solid Green" -> AccentGreen
                            "Solid Blue" -> PrimaryNeon
                            "Clear Screen" -> SurfaceVariantDark
                            else -> SecondaryNeon
                        }
                        Button(
                            onClick = { viewModel.sendTestPattern(name) },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            Text("Brightness: $brightness", fontSize = 12.sp)
            Slider(
                value = brightness.toFloat(),
                onValueChange = { viewModel.setBrightness(it.toInt()) },
                valueRange = 0f..100f
            )
            Button(
                onClick = { viewModel.sendBrightness() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Apply Brightness") }
        }

        // Diagnostics
        item {
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Diagnostics", fontWeight = FontWeight.Bold, color = PrimaryNeon)
                    Text("Queue/Sent/Failed: ${diagnostics.packetsQueued} / ${diagnostics.packetsSent} / ${diagnostics.packetsFailed}")
                    Text("Last packet HEX:", fontSize = 11.sp)
                    Text(
                        text = diagnostics.lastPacketHex ?: "-",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    Text("Last GATT status: ${diagnostics.lastGattStatus ?: "-"}")
                    Text("Last error: ${diagnostics.lastError ?: "-"}", color = AccentOrange, fontSize = 12.sp)
                }
            }
        }

        item {
            LogViewer(
                logs = logs,
                onCopyLogs = { viewModel.copyLogsToClipboard() },
                onExportLogs = { viewModel.exportLogs() },
                onClearLogs = { viewModel.clearLogs() }
            )
        }
    }
}
