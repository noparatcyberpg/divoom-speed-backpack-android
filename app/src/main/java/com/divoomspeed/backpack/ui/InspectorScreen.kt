package com.divoomspeed.backpack.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InspectorScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.appSettings.collectAsState()
    val caps by viewModel.deviceCapabilities.collectAsState()
    var hexInput by remember { mutableStateOf("01 04 00 45 00 49 00 02") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Bluetooth Inspector",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Device Name: ${caps?.deviceName ?: settings.deviceName}", fontWeight = FontWeight.Bold)
                    Text(text = "MAC Address: ${caps?.deviceAddress ?: settings.deviceAddress}")
                    Text(text = "Device Type: ${caps?.deviceType?.name ?: "Unknown"}")
                    Text(text = "Bond State: ${if (caps?.isBonded == true) "Bonded" else "Not Bonded"}")
                    Text(text = "Bluetooth Class: ${caps?.bluetoothClass ?: "N/A"}")
                    Text(
                        text = "Protocol Status: ${if (caps?.protocolVerified == true) caps?.protocolName else "Protocol not verified"}",
                        color = if (caps?.protocolVerified == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Discovered SDP UUIDs:", style = MaterialTheme.typography.titleMedium)
            if (caps?.sdpUuids.isNullOrEmpty()) {
                Text(text = "No SDP UUIDs discovered", style = MaterialTheme.typography.bodySmall)
            } else {
                caps?.sdpUuids?.forEach { uuid ->
                    Text(text = "• $uuid", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "BLE Services & Characteristics:", style = MaterialTheme.typography.titleMedium)
            if (caps?.bleCapabilities.isNullOrEmpty()) {
                Text(text = "No BLE Characteristics discovered", style = MaterialTheme.typography.bodySmall)
            } else {
                caps?.bleCapabilities?.forEach { ble ->
                    Text(
                        text = "Service: ${ble.serviceUuid}\nChar: ${ble.characteristicUuid} (Write: ${ble.canWrite}, WriteNoResp: ${ble.canWriteNoResponse})",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Manual HEX Packet Tester:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = hexInput,
                onValueChange = { hexInput = it },
                label = { Text("HEX Bytes (Space separated)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { viewModel.sendRawHexPacket(hexInput) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Send Packet")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { hexInput = "" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }
        }
    }
}
