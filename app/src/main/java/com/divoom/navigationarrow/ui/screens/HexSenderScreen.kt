package com.divoom.navigationarrow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divoom.navigationarrow.ui.components.LogViewer
import com.divoom.navigationarrow.ui.theme.TextSecondary
import com.divoom.navigationarrow.viewmodel.MainViewModel

@Composable
fun HexSenderScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val hexInput by viewModel.hexInput.collectAsState()
    val logs by viewModel.logList.collectAsState()
    val busy by viewModel.busy.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "HEX Packet Sender (debug)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sends raw bytes after Connect+Initialize. Prefer Display Test for images.",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        item {
            OutlinedTextField(
                value = hexInput,
                onValueChange = { viewModel.updateHexInput(it) },
                label = { Text("HEX bytes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
            )
            Spacer(modifier.height(8.dp))
            Button(
                onClick = { viewModel.sendHexPacket() },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Send HEX") }
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
