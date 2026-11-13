package com.example.chargescience.ui.overview

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chargescience.domain.prediction.ChargingPredictionEngine
import com.example.chargescience.model.BatteryTelemetry
import com.example.chargescience.ui.components.SegmentedGroup
import com.example.chargescience.ui.components.SegmentedTile
import java.util.Locale
import kotlin.math.abs

@Composable
fun OverviewScreen(
    telemetry: BatteryTelemetry,
    modifier: Modifier = Modifier
) {
    val chargingForecast = remember(telemetry.level) {
        ChargingPredictionEngine.predictCharging(telemetry.level)
    }
    val enduranceForecast = remember(telemetry.level) {
        ChargingPredictionEngine.predictEndurance(telemetry.level)
    }

    var selectedForecastTab by remember(telemetry.isCharging, telemetry.pluggedSource) {
        mutableStateOf(if (telemetry.pluggedSource == 4) 1 else 0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ==========================================
        // 1. LIVE BATTERY & POWER DIAGNOSTICS GROUP
        // ==========================================
        SegmentedGroup(
            title = "Live Diagnostics"
        ) {
            // Net Battery Power Tile
            val powerLabel = String.format(Locale.US, "%.1f W", telemetry.wattage)
            val powerSubtitle = if (telemetry.isCharging) {
                "Charging • ${telemetry.protocolName}"
            } else {
                "Discharging on internal battery"
            }
            val powerSummary = if (telemetry.isCharging) {
                "Direct cell absorption via MAX77759 fuel gauge"
            } else {
                "Active system draw from 4,492 mAh cell"
            }

            SegmentedTile(
                title = if (telemetry.isCharging) "Charging Power" else "Discharge Rate",
                trailingLabel = powerLabel,
                subtitle = powerSubtitle,
                summary = powerSummary,
                icon = Icons.Rounded.ElectricBolt,
                initialExpanded = false,
                expandedContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("VOLTAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = String.format(Locale.US, "%,d mV", telemetry.voltageMv),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Terminal potential", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Column {
                                Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                val currentPrefix = if (telemetry.currentMa > 0) "+" else ""
                                Text(
                                    text = "$currentPrefix${String.format(Locale.US, "%,d mA", telemetry.currentMa)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(if (telemetry.isCharging) "Cell inflow" else "System outflow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TEMPERATURE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = String.format(Locale.US, "%.1f °C", telemetry.temperatureCelsius),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("NTC Thermistor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("CYCLE COUNT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = if (telemetry.cycleCount >= 0) "${telemetry.cycleCount} cycles" else "—",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text("CHARGE COUNTER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = String.format(Locale.US, "%,d mAh", telemetry.chargeCounterUah / 1000),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("THERMAL STATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = "State ${telemetry.thermalStatus}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (telemetry.thermalStatus > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            )

            // State of Charge Tile
            SegmentedTile(
                title = "State of Charge",
                trailingLabel = "${telemetry.level}%",
                subtitle = if (telemetry.isCharging) telemetry.chargeStateDescription else "Operating on battery",
                summary = "Hardware coulomb-counter calibrated to nominal 4,492 mAh cell",
                icon = Icons.Rounded.BatteryChargingFull,
                initialExpanded = false,
                expandedContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Lithium-Ion Operating Range",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Maintaining battery state between 20% and 80% mitigates chemical electrode stress and limits high-potential electrolyte oxidation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        // ==========================================
        // 2. CHARGING FORECAST GROUP
        // ==========================================
        SegmentedGroup(
            title = "Charging Forecast"
        ) {
            // Segmented pill selector (Wired vs Wireless)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Surface(
                        onClick = { selectedForecastTab = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedForecastTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Wired 18W USB-PD",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedForecastTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedForecastTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedForecastTab = 1 },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedForecastTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "Wireless 7.5W Qi",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedForecastTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedForecastTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Crossfade(targetState = selectedForecastTab, label = "ForecastPillCrossfade") { tab ->
                val currentForecast = if (tab == 0) chargingForecast.wired else chargingForecast.wireless

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Time to Full Tile
                    SegmentedTile(
                        title = "Time to 100% Full",
                        trailingLabel = currentForecast.timeToFullFormatted,
                        subtitle = "Constant-Current / Constant-Voltage saturation model",
                        summary = "Protective 80% threshold reached in ${currentForecast.timeTo80Formatted}",
                        icon = Icons.Rounded.Schedule,
                        initialExpanded = false,
                        expandedContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "CC-CV Taper Phase Dynamics",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (tab == 0) {
                                        "Rapid bulk charging delivers up to 18W until 75% SOC, after which the PMIC tapers current exponentially over ~42 minutes to prevent overpotential."
                                    } else {
                                        "Wireless charging is hardware-capped at 7.5W RF input (net cell chemical power ~4.6W). Taper knee begins at ~85% SOC."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    // Energy Consumed & Efficiency Tile
                    val energySubtitle = "${currentForecast.efficiencyPercent}% end-to-end grid efficiency"
                    val energySummary = "${String.format(Locale.US, "%.1f", currentForecast.chemicalEnergyWh)} Wh stored chemically in battery"

                    SegmentedTile(
                        title = "Energy Required",
                        trailingLabel = String.format(Locale.US, "%.1f Wh", currentForecast.wallEnergyWh),
                        subtitle = energySubtitle,
                        summary = energySummary,
                        icon = Icons.Rounded.Speed,
                        initialExpanded = false,
                        expandedContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("WALL SOCKET CONSUMPTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = String.format(Locale.US, "%.1f Wh", currentForecast.wallEnergyWh),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("PEAK INPUT RATING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = String.format(Locale.US, "%.1f W", currentForecast.peakPowerWatts),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (tab == 1 && chargingForecast.energyOverheadPercent > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Wireless charging consumes +${chargingForecast.energyOverheadPercent}% more grid energy and takes ~${chargingForecast.timeDifferenceMinutes}m longer due to inductive magnetic field losses and coil heating.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // ==========================================
        // 3. BATTERY ENDURANCE & SCREEN-ON TIME GROUP
        // ==========================================
        SegmentedGroup(
            title = "Battery Endurance"
        ) {
            // Active Screen-On Time Tile
            SegmentedTile(
                title = "Active Screen-On Time",
                trailingLabel = enduranceForecast.activeMixedSotFormatted,
                subtitle = "120Hz Actua OLED display mixed daily use",
                summary = "Standby sleep: ${enduranceForecast.standbyFormatted} (Screen-off Doze)",
                icon = Icons.Rounded.Schedule,
                initialExpanded = false,
                expandedContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Display & Platform Power Context",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Based on Google Tensor G3 baseline power profiles. Screen draw on the 120Hz LTPO display scales with ambient brightness and refresh rate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            // Usage Scenarios Tile
            SegmentedTile(
                title = "Workload Projections",
                trailingLabel = "3 Modes",
                subtitle = "Reading, Video Streaming & Intensive Load",
                summary = "Reading: ${enduranceForecast.lightSotFormatted} • Video: ${enduranceForecast.videoPlaybackFormatted}",
                icon = Icons.Rounded.Speed,
                initialExpanded = false,
                expandedContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("READING / WEB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = enduranceForecast.lightSotFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("~350 mA (60Hz)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Column {
                                Text("VIDEO PLAYBACK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = enduranceForecast.videoPlaybackFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("1080p Stream", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("HEAVY LOAD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = enduranceForecast.heavySotFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Gaming / 5G", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            )
        }

        // ==========================================
        // 4. POWER DELIVERY CONTRACT GROUP
        // ==========================================
        SegmentedGroup(
            title = "Charger & Power Contract"
        ) {
            val contractTitle = if (telemetry.isCharging && telemetry.maxChargingVoltageUv > 0) {
                String.format(Locale.US, "%.1f W Ceiling", telemetry.negotiatedMaxPowerWatts)
            } else if (telemetry.isCharging) {
                "Standard Power"
            } else {
                "Unplugged"
            }

            val contractSubtitle = if (telemetry.isCharging && telemetry.maxChargingVoltageUv > 0) {
                String.format(
                    Locale.US,
                    "%.1f V @ %.2f A (%s)",
                    telemetry.maxChargingVoltageUv / 1_000_000.0f,
                    telemetry.maxChargingCurrentUa / 1_000_000.0f,
                    telemetry.protocolName
                )
            } else if (telemetry.isCharging) {
                telemetry.protocolName
            } else {
                "No external power connected"
            }

            SegmentedTile(
                title = "Negotiated Contract",
                trailingLabel = contractTitle,
                subtitle = contractSubtitle,
                summary = if (telemetry.isCharging) "Power profile negotiated with Pixel 8a PMIC" else "Device running from internal battery reserve",
                icon = Icons.Rounded.Power,
                initialExpanded = false,
                expandedContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Electrical Bus vs Chemical Power",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "The advertised contract defines the electrical ceiling supplied over the USB-C bus or Qi field. Actual power absorbed by the battery is modulated by the charging stage and thermal throttling governor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
