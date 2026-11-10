package com.example.chargescience.ui.lab

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
import java.util.Locale

@Composable
fun ChargingLabScreen(
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
            text = "CHARGING PERFORMANCE & THERMODYNAMICS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Thermodynamics Analysis Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Power & Thermodynamic Partitioning",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                val estPmicLoss = if (telemetry.isCharging && telemetry.wattage > 0) {
                    telemetry.wattage * 0.08f / 0.92f // Assuming ~92% PMIC efficiency
                } else 0f

                Text(
                    text = String.format(
                        Locale.US,
                        "• Net Battery Chemical Power: %.2f W\n" +
                        "• Estimated Internal PMIC Heat Loss: ~%.2f W\n" +
                        "• Joulean Heat (I²R cell dissipation): ~%.2f W",
                        telemetry.wattage,
                        estPmicLoss,
                        if (telemetry.isCharging) (telemetry.currentMa / 1000f) * (telemetry.currentMa / 1000f) * 0.035f else 0f
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Note: The internal switching PMIC converts negotiated 9V USB-PD to ~3.8V-4.4V battery voltage at ~92% efficiency. The PMIC's thermal dissipation is the primary driver of chassis heating during fast charge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Throttling Detection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (telemetry.thermalStatus >= 2) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Charging Throttling Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (telemetry.thermalStatus >= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                val isCvPhase = telemetry.voltageMv >= 4350
                val throttleExplanation = when {
                    telemetry.thermalStatus >= 2 -> "Active Thermal Throttling: Android Thermal HAL has signaled thermal mitigation (Status ${telemetry.thermalStatus}). Charge current is actively constrained."
                    isCvPhase -> "Constant Voltage (CV) Saturation Taper: Battery cell potential has reached saturation limit (~4.35V-4.4V). Current taper is electrochemical, not thermal."
                    telemetry.isFastCharging -> "Normal Constant Current (CC) Phase: Maximum allowable negotiated current flowing safely into cell."
                    telemetry.isCharging -> "Low-rate Charge: Operating at baseline standard rate."
                    else -> "Device is not currently charging."
                }

                Text(
                    text = throttleExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
