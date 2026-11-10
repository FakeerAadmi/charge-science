package com.example.chargescience.ui.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chargescience.model.BatteryTelemetry
import com.example.chargescience.ui.components.HeroWattageCard
import com.example.chargescience.ui.components.TelemetryGaugeCard
import java.util.Locale

@Composable
fun OverviewScreen(
    telemetry: BatteryTelemetry,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Priority 1: Current Charging Power (Hero)
        HeroWattageCard(
            wattage = telemetry.wattage,
            isCharging = telemetry.isCharging,
            protocolName = telemetry.protocolName,
            negotiatedMaxWatts = telemetry.negotiatedMaxPowerWatts
        )

        // Priority 2 & 3: Battery Percentage & Charging Method Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "STATE OF CHARGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${telemetry.level}%",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (telemetry.isCharging) "Direct Fuel Gauge SOC" else "Discharging on Battery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CHARGING METHOD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (telemetry.isCharging) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = if (telemetry.isCharging) telemetry.chargeStateDescription else "Unplugged",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (telemetry.isCharging) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = telemetry.protocolName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Priority 4 & 5: Electrical & Thermal Quad Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryGaugeCard(
                label = "Temperature [Measured]",
                value = String.format(Locale.US, "%.1f", telemetry.temperatureCelsius),
                unit = "°C",
                subText = "Pouch Thermistor (NTC)",
                modifier = Modifier.weight(1f)
            )

            val currentSign = if (telemetry.currentMa > 0) "+" else ""
            TelemetryGaugeCard(
                label = "Current [Measured]",
                value = "$currentSign${String.format(Locale.US, "%,d", telemetry.currentMa)}",
                unit = "mA",
                subText = if (telemetry.isCharging) "Cell Inflow (MAX77759)" else "Cell Outflow",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryGaugeCard(
                label = "Voltage [Measured]",
                value = String.format(Locale.US, "%,d", telemetry.voltageMv),
                unit = "mV",
                subText = "Terminal Cell Potential",
                modifier = Modifier.weight(1f)
            )

            TelemetryGaugeCard(
                label = "Cycles [Hardware]",
                value = if (telemetry.cycleCount >= 0) "${telemetry.cycleCount}" else "—",
                unit = if (telemetry.cycleCount >= 0) "cycles" else "",
                subText = "Fuel Gauge Register",
                modifier = Modifier.weight(1f)
            )
        }

        // Priority 6: Negotiated Power Delivery Ceiling
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "POWER DELIVERY CEILING [NEGOTIATED CONTRACT]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (telemetry.isCharging && telemetry.maxChargingVoltageUv > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "CONTRACT VOLTAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f V", telemetry.maxChargingVoltageUv / 1_000_000.0f),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = "CONTRACT CURRENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f A", telemetry.maxChargingCurrentUa / 1_000_000.0f),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = "POWER LIMIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f W", telemetry.negotiatedMaxPowerWatts),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Advertised profile negotiated between charger and Pixel 8a PMIC. Actual chemical power is lower due to conversion efficiency and charging curve phase.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Text(
                        text = "No active external power contract negotiated.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
