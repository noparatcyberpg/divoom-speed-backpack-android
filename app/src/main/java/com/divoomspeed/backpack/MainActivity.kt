package com.divoomspeed.backpack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.divoomspeed.backpack.logging.DebugLogger
import com.divoomspeed.backpack.ui.DeviceScreen
import com.divoomspeed.backpack.ui.InspectorScreen
import com.divoomspeed.backpack.ui.LogsScreen
import com.divoomspeed.backpack.ui.MainScreen
import com.divoomspeed.backpack.ui.MainViewModel
import com.divoomspeed.backpack.ui.SettingsScreen
import com.divoomspeed.backpack.ui.TestDisplayScreen

import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            DebugLogger.i("Permission", "All required runtime permissions granted")
        } else {
            DebugLogger.w("Permission", "Some runtime permissions were denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            var selectedTab by remember { mutableStateOf("home") }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == "home",
                            onClick = { selectedTab = "home" },
                            icon = { Icon(Icons.Default.Speed, contentDescription = "Speed") },
                            label = { Text("Speed") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == "device",
                            onClick = { selectedTab = "device" },
                            icon = { Icon(Icons.Default.Bluetooth, contentDescription = "Device") },
                            label = { Text("Device") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == "test",
                            onClick = { selectedTab = "test" },
                            icon = { Icon(Icons.Default.Tv, contentDescription = "Test") },
                            label = { Text("Test") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == "inspector",
                            onClick = { selectedTab = "inspector" },
                            icon = { Icon(Icons.Default.BugReport, contentDescription = "Inspector") },
                            label = { Text("Inspector") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == "logs",
                            onClick = { selectedTab = "logs" },
                            icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                            label = { Text("Logs") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == "settings",
                            onClick = { selectedTab = "settings" },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                    }
                }
            ) { innerPadding ->
                val modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                when (selectedTab) {
                    "home" -> MainScreen(
                        viewModel = viewModel,
                        onNavigateToDevicePicker = { selectedTab = "device" },
                        onNavigateToInspector = { selectedTab = "inspector" },
                        onNavigateToTestDisplay = { selectedTab = "test" },
                        onNavigateToLogs = { selectedTab = "logs" },
                        onNavigateToSettings = { selectedTab = "settings" }
                    )
                    "device" -> DeviceScreen(
                        viewModel = viewModel,
                        onDeviceSelected = { selectedTab = "home" }
                    )
                    "test" -> TestDisplayScreen(viewModel = viewModel)
                    "inspector" -> InspectorScreen(viewModel = viewModel)
                    "logs" -> LogsScreen(viewModel = viewModel)
                    "settings" -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
