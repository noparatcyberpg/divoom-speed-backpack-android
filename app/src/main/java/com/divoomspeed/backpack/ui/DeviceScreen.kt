package com.divoomspeed.backpack.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val isDivoomKeyword: Boolean,
    val bondState: String,
    val type: String
)

@SuppressLint("MissingPermission")
@Composable
fun DeviceScreen(
    viewModel: MainViewModel,
    onDeviceSelected: () -> Unit
) {
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val pairedDevices = remember {
        bluetoothAdapter?.bondedDevices?.map { dev ->
            val name = dev.name ?: "Unknown Device"
            val isDivoom = listOf("Divoom", "Pixoo", "Backpack", "Cyberbag").any {
                name.contains(it, ignoreCase = true)
            }
            val typeStr = when (dev.type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic RFCOMM"
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE GATT"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode"
                else -> "Unknown"
            }
            DiscoveredDevice(
                name = name,
                address = dev.address,
                isDivoomKeyword = isDivoom,
                bondState = "Paired",
                type = typeStr
            )
        } ?: emptyList()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Divoom Backpack",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Paired Bluetooth Devices:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(pairedDevices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                viewModel.saveSelectedDevice(device.address, device.name)
                                onDeviceSelected()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (device.isDivoomKeyword) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row {
                                Text(
                                    text = device.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (device.isDivoomKeyword) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "Divoom Recommended",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "MAC: ${device.address}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Type: ${device.type} | State: ${device.bondState}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
