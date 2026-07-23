package com.divoomspeed.backpack.ui

import android.graphics.Color
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TestDisplayScreen(
    viewModel: MainViewModel
) {
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
                text = "Test Display Suite",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Send test frames to verify display protocol and color mapping.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Solid Test Colors:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.sendTestColor(Color.RED) },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Solid Red")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.sendTestColor(Color.GREEN) },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Solid Green")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.sendTestColor(Color.BLUE) },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Blue),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Solid Blue")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Sample Speed Numbers:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.sendTestSpeedNumber(0) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("0 km/h")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.sendTestSpeedNumber(10) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("10 km/h")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.sendTestSpeedNumber(50) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("50 km/h")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.sendTestSpeedNumber(99) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("99 km/h")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { viewModel.sendTestSpeedNumber(120) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("120 km/h")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.sendClearScreen() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Clear Display Screen")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.startDemoMode() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Auto Speed Demo Mode (0-120 km/h)")
            }
        }
    }
}
