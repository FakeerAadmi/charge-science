package com.example.chargescience.ui.lab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.model.BatteryTelemetry
import com.example.chargescience.ui.components.TelemetryTimeSeriesChart
import java.util.Locale

@Composable
fun ChargingLabScreen(
    telemetry: BatteryTelemetry,
    activeSamples: List<TelemetrySampleEntity>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var selectedMetric by remember { mutableStateOf("Power") }
    val metricOptions = listOf("Power", "Current", "Voltage", "Temp", "SOC")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CHARGING LAB & TIME-SERIES CURVES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Live Electrical Summary Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Live Active Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "NET POWER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f W", telemetry.wattage),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = "CURRENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%,d mA", telemetry.currentMa),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        Text(
                            text = "VOLTAGE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%,d mV", telemetry.voltageMv),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        Text(
                            text = "TEMP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f°C", telemetry.temperatureCelsius),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Metric Selector (Segmented Row)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            metricOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selectedMetric == option,
                    onClick = { selectedMetric = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = metricOptions.size)
                ) {
                    Text(text = option, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Active Time-Series Chart based on selection
        when (selectedMetric) {
            "Power" -> {
                val powerPoints = activeSamples.map { Pair(it.timestamp, it.powerWatts) }
                TelemetryTimeSeriesChart(
                    title = "Battery-Side Power Curve",
                    unit = "W",
                    dataPoints = powerPoints,
                    lineColor = MaterialTheme.colorScheme.primary,
                    emptyMessage = if (telemetry.isCharging) "Sampling 1 Hz power points..." else "Connect charger to record power curve"
                )
            }
            "Current" -> {
                val currentPoints = activeSamples.map { Pair(it.timestamp, it.currentMa.toFloat()) }
                TelemetryTimeSeriesChart(
                    title = "Battery Current Flow",
                    unit = "mA",
                    dataPoints = currentPoints,
                    lineColor = MaterialTheme.colorScheme.secondary,
                    emptyMessage = if (telemetry.isCharging) "Sampling 1 Hz current points..." else "Connect charger to record current flow"
                )
            }
            "Voltage" -> {
                val voltagePoints = activeSamples.map { Pair(it.timestamp, it.voltageMv.toFloat()) }
                TelemetryTimeSeriesChart(
                    title = "Terminal Cell Voltage Curve",
                    unit = "mV",
                    dataPoints = voltagePoints,
                    lineColor = MaterialTheme.colorScheme.primary,
                    emptyMessage = if (telemetry.isCharging) "Sampling 1 Hz voltage points..." else "Connect charger to record voltage curve"
                )
            }
            "Temp" -> {
                val tempPoints = activeSamples.map { Pair(it.timestamp, it.temperatureCelsius) }
                TelemetryTimeSeriesChart(
                    title = "Battery Pouch Temperature",
                    unit = "°C",
                    dataPoints = tempPoints,
                    lineColor = MaterialTheme.colorScheme.tertiary,
                    emptyMessage = if (telemetry.isCharging) "Sampling 1 Hz thermal points..." else "Connect charger to record thermal curve"
                )
            }
            "SOC" -> {
                val socPoints = activeSamples.map { Pair(it.timestamp, it.batteryLevel.toFloat()) }
                TelemetryTimeSeriesChart(
                    title = "State of Charge (SOC)",
                    unit = "%",
                    dataPoints = socPoints,
                    lineColor = MaterialTheme.colorScheme.primary,
                    emptyMessage = if (telemetry.isCharging) "Sampling 1 Hz SOC points..." else "Connect charger to record SOC curve"
                )
            }
        }

        // Thermodynamics Analysis Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Thermodynamic Partitioning",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                val estPmicLoss = if (telemetry.isCharging && telemetry.wattage > 0) {
                    telemetry.wattage * 0.08f / 0.92f // Assuming ~92% PMIC efficiency
                } else 0f

                Text(
                    text = String.format(
                        Locale.US,
                        "• Net Battery Chemical Power: %.2f W\n" +
                        "• Estimated PMIC Heat Loss: ~%.2f W\n" +
                        "• Joulean Heat (I²R cell dissipation): ~%.2f W",
                        telemetry.wattage,
                        estPmicLoss,
                        if (telemetry.isCharging) (telemetry.currentMa / 1000f) * (telemetry.currentMa / 1000f) * 0.035f else 0f
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    style = MaterialTheme.typography.titleMedium,
                    color = if (telemetry.thermalStatus >= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

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
