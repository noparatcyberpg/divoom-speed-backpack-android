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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divoom.navigationarrow.domain.model.ConnectionState
import com.divoom.navigationarrow.domain.protocol.ProtocolUuids
import com.divoom.navigationarrow.ui.components.LogViewer
import com.divoom.navigationarrow.ui.theme.AccentGreen
import com.divoom.navigationarrow.ui.theme.AccentOrange
import com.divoom.navigationarrow.ui.theme.PrimaryNeon
import com.divoom.navigationarrow.ui.theme.SurfaceDark
import com.divoom.navigationarrow.ui.theme.TextSecondary
import com.divoom.navigationarrow.viewmodel.MainViewModel

@Composable
fun BluetoothInspectorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val logs by viewModel.logList.collectAsState()
    val selectedAddress by viewModel.selectedAddress.collectAsState()
    val debugMode by viewModel.debugMode.collectAsState()
    val advancedMode by viewModel.advancedMode.collectAsState()
    val busy by viewModel.busy.collectAsState()

    var customServiceUuid by remember { mutableStateOf(ProtocolUuids.SERVICE_STRING) }
    var customCharUuid by remember { mutableStateOf(ProtocolUuids.WRITE_CHAR_STRING) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AccentOrange,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "กรุณาปิดหรือ Force Stop แอป Divoom Official ก่อนทดสอบ\nเพื่อป้องกันแอปทั้งสองแย่งการเชื่อมต่อกับกระเป๋า",
                        color = Color(0xFFFED7AA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            Text(
                text = "Bluetooth Inspector",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "BLE GATT only — modern Backpack M protocol",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Debug Mode (separate Connect / Initialize)")
                Switch(checked = debugMode, onCheckedChange = { viewModel.setDebugMode(it) })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Advanced Mode (manual characteristic)")
                Switch(checked = advancedMode, onCheckedChange = { viewModel.setAdvancedMode(it) })
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Paired devices",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.refreshPairedDevices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        items(pairedDevices) { device ->
            val selected = device.address == selectedAddress
            Card(
                onClick = { viewModel.selectDevice(device.address) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) Color(0xFF1B3A2F) else SurfaceDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (selected) AccentGreen else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(device.name, fontWeight = FontWeight.Bold, color = PrimaryNeon)
                    Text("MAC: ${device.address}", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "Type: ${device.deviceType} · Bond: ${device.bondState}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            Text("Connection controls", fontWeight = FontWeight.Bold)
            Spacer(modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.connectDevice() },
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) { Text("Connect") }
                if (debugMode) {
                    Button(
                        onClick = { viewModel.initializeSession() },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) { Text("Initialize") }
                }
                Button(
                    onClick = { viewModel.disconnectDevice() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D))
                ) { Text("Disconnect") }
            }
            Spacer(modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.discoverServices() }, modifier = Modifier.weight(1f)) {
                    Text("Discover Services", fontSize = 11.sp)
                }
                Button(onClick = { viewModel.requestMtu() }, modifier = Modifier.weight(1f)) {
                    Text("Request MTU", fontSize = 11.sp)
                }
            }
            Spacer(modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.copyDeviceReport() }, modifier = Modifier.weight(1f)) {
                    Text("Copy Report", fontSize = 11.sp)
                }
                Button(onClick = { viewModel.exportDeviceReport() }, modifier = Modifier.weight(1f)) {
                    Text("Export Report", fontSize = 11.sp)
                }
                Button(onClick = { viewModel.clearLogs() }, modifier = Modifier.weight(1f)) {
                    Text("Clear Log", fontSize = 11.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Device", fontWeight = FontWeight.Bold, color = PrimaryNeon)
                    val cs = when (connectionState) {
                        is ConnectionState.Connected -> "CONNECTED"
                        is ConnectionState.Connecting -> "CONNECTING"
                        is ConnectionState.Error -> "ERROR"
                        else -> "DISCONNECTED"
                    }
                    Text("Name: ${diagnostics.deviceName ?: "-"}")
                    Text("MAC: ${diagnostics.deviceAddress ?: selectedAddress ?: "-"}")
                    Text("Type: ${diagnostics.deviceType ?: "-"}")
                    Text("Bond: ${diagnostics.bondState ?: "-"}")
                    Text("Connection: $cs")
                    Text("RSSI: ${diagnostics.rssi?.toString() ?: "-"}")
                    Spacer(modifier.height(8.dp))
                    Text("GATT", fontWeight = FontWeight.Bold, color = PrimaryNeon)
                    Text("Services discovered: ${diagnostics.servicesDiscovered}")
                    Text("Session initialized: ${diagnostics.sessionInitialized}")
                    Text("MTU: ${diagnostics.mtu ?: "-"}")
                    Text("Service: ${diagnostics.serviceUuid ?: ProtocolUuids.SERVICE_STRING}")
                    Text("Write char: ${diagnostics.characteristicUuid ?: "-"}")
                    Text("Write type: ${diagnostics.writeType ?: "-"}")
                    Text("Notifications: ${diagnostics.notificationsEnabled}")
                    Text("Last GATT status: ${diagnostics.lastGattStatus ?: "-"}")
                    Text(
                        "Last error: ${diagnostics.lastError ?: "-"}",
                        color = AccentOrange,
                        fontSize = 12.sp
                    )
                    Spacer(modifier.height(6.dp))
                    Text(
                        text = diagnostics.gattReport
                            ?: "(Discover services to list UUIDs / properties)",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }
        }

        if (advancedMode) {
            item {
                Text("Advanced: manual characteristic", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = customServiceUuid,
                    onValueChange = { customServiceUuid = it },
                    label = { Text("Service UUID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = customCharUuid,
                    onValueChange = { customCharUuid = it },
                    label = { Text("Characteristic UUID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.selectBleWriteChar(
                            customServiceUuid.trim(),
                            customCharUuid.trim()
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Apply Characteristic") }
                Text(
                    text = "Normal mode auto-selects ${ProtocolUuids.WRITE_CHAR_STRING}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
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
