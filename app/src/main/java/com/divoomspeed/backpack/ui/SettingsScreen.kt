package com.divoomspeed.backpack.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.appSettings.collectAsState()
    val scope = rememberCoroutineScope()

    var showAdvanced by remember { mutableStateOf(false) }

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
                text = "App & Display Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Display Resolution
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Display Resolution: ${settings.resolutionWidth}x${settings.resolutionHeight}", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.material3.RadioButton(
                            selected = settings.resolutionWidth == 16,
                            onClick = { scope.launch { viewModel.settingsRepository.updateResolution(16, 16) } }
                        )
                        Text("16x16", modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        androidx.compose.material3.RadioButton(
                            selected = settings.resolutionWidth == 32,
                            onClick = { scope.launch { viewModel.settingsRepository.updateResolution(32, 32) } }
                        )
                        Text("32x32", modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        androidx.compose.material3.RadioButton(
                            selected = settings.resolutionWidth == 64,
                            onClick = { scope.launch { viewModel.settingsRepository.updateResolution(64, 64) } }
                        )
                        Text("64x64", modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
            }

            // Display Rotation
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Rotation: ${settings.rotationDegrees}°", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(0, 90, 180, 270).forEach { deg ->
                            androidx.compose.material3.RadioButton(
                                selected = settings.rotationDegrees == deg,
                                onClick = { scope.launch { viewModel.settingsRepository.updateRotation(deg) } }
                            )
                            Text("$deg°", modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }

            // Brightness Slider
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Display Brightness: ${settings.brightness}%", fontWeight = FontWeight.Bold)
                    Slider(
                        value = settings.brightness.toFloat(),
                        onValueChange = { scope.launch { viewModel.settingsRepository.updateBrightness(it.toInt()) } },
                        valueRange = 0f..100f
                    )
                }
            }

            // Toggles (Show KMH, Speed Smoothing, Auto Reconnect)
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Show 'KMH' Unit Banner", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.showUnit,
                            onCheckedChange = { scope.launch { viewModel.settingsRepository.updateShowUnit(it) } }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Speed Smoothing (EMA Filter)", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.speedSmoothing,
                            onCheckedChange = { scope.launch { viewModel.settingsRepository.updateSpeedSmoothing(it) } }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto Reconnect on Signal Drop", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.autoReconnect,
                            onCheckedChange = { scope.launch { viewModel.settingsRepository.updateAutoReconnect(it) } }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advanced Bluetooth Parameters Collapsible Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Advanced Bluetooth & Protocol Settings", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(
                            checked = showAdvanced,
                            onCheckedChange = { showAdvanced = it }
                        )
                    }

                    if (showAdvanced) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = settings.customRfcommUuid,
                            onValueChange = { scope.launch { viewModel.settingsRepository.updateRfcommSettings(it, settings.customRfcommChannel) } },
                            label = { Text("RFCOMM Service Record UUID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.bleWriteCharacteristic,
                            onValueChange = { scope.launch { viewModel.settingsRepository.updateBleSettings(it, settings.bleWriteWithoutResponse) } },
                            label = { Text("BLE Write Characteristic UUID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
