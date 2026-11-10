package com.example.chargescience.ui.charger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chargescience.model.BatteryTelemetry

@Composable
fun ChargerIntelligenceScreen(
    telemetry: BatteryTelemetry,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CHARGER & CABLE INTELLIGENCE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Active Connection Analysis
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Negotiated Power Delivery Protocol",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = telemetry.protocolName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                val explanation = if (telemetry.isUsbPd) {
                    "The connected charger has negotiated a USB Power Delivery 3.0 power contract at ${telemetry.maxChargingVoltageUv / 1_000_000}V (up to ${telemetry.negotiatedMaxPowerWatts}W ceiling). The Pixel 8a hardware ceiling is 18W wired."
                } else if (telemetry.isWireless) {
                    "Operating over Qi Inductive Charging. On Google Pixel 8a, wireless charging is hardware-constrained to a maximum 7.5W reception rate."
                } else if (telemetry.isCharging) {
                    "Standard 5V charging profile detected. USB-PD high-voltage contract is not active."
                } else {
                    "No external charger connected."
                }

                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Technical Hardware Measurement Boundaries
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Hardware Measurement Boundaries",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "In accordance with scientific integrity:\n\n" +
                            "• Cable E-Marker & Vendor IDs: Android restricts raw USB-PD CC-wire packets to the Linux kernel / System-signed services. An unprivileged app cannot read e-marker chips without external hardware.\n" +
                            "• Charger Power vs Battery Power: The displayed wattage measures net chemical current entering the battery. Charger wall draw includes conversion losses and device system loads.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
