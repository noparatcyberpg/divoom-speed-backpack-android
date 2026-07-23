package com.divoomspeed.backpack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divoomspeed.backpack.bluetooth.BluetoothConnectionState

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToDevicePicker: () -> Unit,
    onNavigateToInspector: () -> Unit,
    onNavigateToTestDisplay: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val settings by viewModel.appSettings.collectAsState()
    val speedReading by viewModel.currentSpeedReading.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val previewFrame by viewModel.previewFrame.collectAsState()
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speed Gauge Display Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REALTIME SPEED",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = speedReading.displayText,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "km/h",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Card (Device & Bluetooth)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Target Device: ${if (settings.deviceName.isNotBlank()) settings.deviceName else "Not Selected"}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MAC Address: ${if (settings.deviceAddress.isNotBlank()) settings.deviceAddress else "--:--:--:--"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Connection Status: ${
                            when (connectionState) {
                                is BluetoothConnectionState.Connected -> "Connected"
                                is BluetoothConnectionState.Connecting -> "Connecting..."
                                is BluetoothConnectionState.Reconnecting -> "Reconnecting..."
                                is BluetoothConnectionState.Error -> "Disconnected / Error"
                                else -> "Disconnected (Press Connect or Start Service)"
                            }
                        }",
                        color = when (connectionState) {
                            is BluetoothConnectionState.Connected -> Color(0xFF4CAF50)
                            is BluetoothConnectionState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (connectionState is BluetoothConnectionState.Connected) {
                            Button(
                                onClick = { viewModel.disconnectDevice() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.connectDevice() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Connect")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Protocol Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Divoom Protocol Model:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(
                        "LEGACY_PIXOO" to "Pixoo Backpack (Classic RFCOMM)",
                        "BACKPACK_M" to "Backpack M (Modern BLE/RFCOMM)",
                        "CYBERBAG" to "Cyberbag (Extended Header)"
                    ).forEach { (key, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.protocolType == key,
                                onClick = { scope.launch { viewModel.settingsRepository.updateProtocolType(key) } }
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (settings.deviceAddress.isBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💡 Quick Setup Guide", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("1. Tap 'Select Device' below to choose your Divoom Bluetooth bag.")
                        Text("2. Tap 'Connect' or 'Start Service' to start sending speed.")
                        Text("3. Tap 'Test Display' to run Demo Mode offline without leaving your room!")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pixel Frame Preview
            Text(
                text = "Live Backpack Frame Preview",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            PixelPreviewComposable(
                frame = previewFrame,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Sync & Bluetooth Monitor Card
            val packetsSent by viewModel.packetsSentCount.collectAsState()
            val lastPacketHex by viewModel.lastPacketHex.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "📡 Backpack Display & Bluetooth Monitor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Total Packets Delivered: $packetsSent packets", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "Last Packet Bytes: $lastPacketHex", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.queryDeviceStatus() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Query Hardware Response (Cmd 0x46)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onNavigateToDevicePicker,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Select Device")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.startSpeedService() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Start Service")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (settings.demoMode) {
                    Button(
                        onClick = { viewModel.stopDemoMode() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Stop Demo Mode (Currently Simulating 0-120 km/h)")
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.startDemoMode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Demo Simulation Mode (Test Indoors)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onNavigateToTestDisplay,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Display")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onNavigateToInspector,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Inspector")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onNavigateToLogs,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Debug Logs")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Settings")
                }
            }
        }
    }
}
