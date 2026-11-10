package com.example.chargescience.ui.health

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chargescience.data.db.entity.BatteryCapacityEstimateEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.domain.health.BatteryHealthLaboratoryEngine
import com.example.chargescience.model.BatteryTelemetry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BatteryHealthScreen(
    telemetry: BatteryTelemetry,
    estimates: List<BatteryCapacityEstimateEntity> = emptyList(),
    sessions: List<ChargingSessionEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val healthEngine = remember { BatteryHealthLaboratoryEngine() }

    val healthSummary = remember(telemetry.cycleCount, estimates, sessions) {
        healthEngine.computeHealthSummary(
            hardwareCycleCount = telemetry.cycleCount,
            allEstimates = estimates,
            allSessions = sessions
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BATTERY HEALTH & ELECTROCHEMICAL LABORATORY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Core Health & Capacity Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "State of Health (SoH)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest
                    ) {
                        Text(
                            text = "[STATISTICAL ESTIMATE]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED SOH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = healthSummary.estimatedSohPercent?.let {
                                String.format(Locale.US, "%.1f%%", it)
                            } ?: "Calibrating",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Confidence: ${String.format(Locale.US, "%.0f", healthSummary.confidenceScore * 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Column {
                        Text(
                            text = "USABLE CAPACITY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = healthSummary.latestLearnedCapacityMah?.let {
                                String.format(Locale.US, "%,.0f", it)
                            } ?: "—",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "mAh usable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Hardware vs Design Matrix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HARDWARE CYCLES ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ) {
                                Text(
                                    text = "[HW]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = if (telemetry.cycleCount >= 0) "${telemetry.cycleCount}" else "Unavailable",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "From Maxim MAX77759",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DESIGN NOMINAL ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = "[SPEC]",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "4,492",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "mAh (4,404 rated min)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Thermal Stress Exposure Distribution Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Charging Temperature Exposure",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "High thermal exposure during charging accelerates Solid Electrolyte Interphase (SEI) layer growth according to the Arrhenius relationship.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("< 30°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "%.1fh", healthSummary.thermalExposureLowHours), style = MaterialTheme.typography.titleMedium)
                        Text("Optimal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("30 - 38°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "%.1fh", healthSummary.thermalExposureModerateHours), style = MaterialTheme.typography.titleMedium)
                        Text("Normal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Column {
                        Text("38 - 42°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "%.1fh", healthSummary.thermalExposureElevatedHours), style = MaterialTheme.typography.titleMedium)
                        Text("Elevated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Column {
                        Text("> 42°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "%.1fh", healthSummary.thermalExposureSevereHours), style = MaterialTheme.typography.titleMedium)
                        Text("High Stress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Scientific Capacity Estimation Methodology Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Coulomb Counting Gating Methodology",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Charge Science calculates usable capacity using Coulomb counting integration (ΔQ / ΔSOC) across qualified charging sessions. To avoid mathematical distortion, sessions are strictly filtered:\n\n" +
                            "• ΔSOC ≥ 30%: Smaller charge swings amplify 1% integer quantization error beyond ±3.3%.\n" +
                            "• ΔT ≤ 8°C & T_max ≤ 45°C: Excessive temperature swings warp apparent chemical capacity.\n" +
                            "• Continuity & Monotonicity: Sessions with interruptions, process kills, or discrete fuel-gauge recalibration jumps are rejected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Capacity Observations Log
        if (estimates.isNotEmpty()) {
            Text(
                text = "CAPACITY OBSERVATIONS (${estimates.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            estimates.take(10).forEach { est ->
                val isValid = est.eligibilityStatus == "VALID"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isValid) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isValid) 0.25f else 0.1f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isValid) {
                                    String.format(Locale.US, "%,.0f mAh (%.1f%% SoH)", est.calculatedCapacityMah, est.estimatedSohPercent)
                                } else {
                                    "Observation Filtered"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(est.timestamp))
                            Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Span: ${est.startSoc}% → ${est.endSoc}% (Δ${est.deltaSoc}%) • Temp: ${String.format(Locale.US, "%.1f", est.averageTempCelsius)}°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isValid && est.rejectionReason != null) {
                            Text(
                                text = "Reason: ${est.rejectionReason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
