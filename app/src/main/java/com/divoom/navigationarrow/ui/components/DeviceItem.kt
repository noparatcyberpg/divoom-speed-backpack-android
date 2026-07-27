package com.divoom.navigationarrow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.divoom.navigationarrow.ui.theme.AccentGreen
import com.divoom.navigationarrow.ui.theme.PrimaryNeon
import com.divoom.navigationarrow.ui.theme.SurfaceDark
import com.divoom.navigationarrow.ui.theme.SurfaceVariantDark
import com.divoom.navigationarrow.ui.theme.TextSecondary
import com.divoom.navigationarrow.viewmodel.PairedDeviceInfo

@Composable
fun DeviceItem(
    device: PairedDeviceInfo,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(
                width = if (isConnected) 2.dp else 1.dp,
                color = if (isConnected) AccentGreen else SurfaceVariantDark,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "MAC: ${device.address}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row {
                Text(
                    text = "Type: ${device.deviceType}",
                    fontSize = 11.sp,
                    color = PrimaryNeon
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "State: ${device.bondState}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        if (isConnected) {
            OutlinedButton(
                onClick = onDisconnect,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Disconnect")
            }
        } else {
            Button(
                onClick = onConnect,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
            ) {
                Text("Connect", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
