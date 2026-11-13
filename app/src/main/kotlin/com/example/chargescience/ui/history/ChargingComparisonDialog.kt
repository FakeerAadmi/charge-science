package com.example.chargescience.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.domain.comparison.ChargingComparisonEngine
import com.example.chargescience.domain.comparison.ComparisonValidity
import com.example.chargescience.domain.comparison.SessionComparisonResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChargingComparisonDialog(
    sessionA: ChargingSessionEntity,
    sessionB: ChargingSessionEntity,
    onDismiss: () -> Unit
) {
    val comparison = ChargingComparisonEngine.compareSessions(sessionA, sessionB)
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CONTROLLED COMPARISON LAB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Session #${sessionA.id} vs Session #${sessionB.id}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Validity Badge & Warnings
                    ValidityBanner(comparison)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scientific Verdict
                    VerdictCard(comparison.scientificVerdict)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Side-by-side comparison table
                    Text(
                        text = "METRICS BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ComparisonRow(
                        label = "Starting SOC",
                        valA = "${sessionA.startSoc}%",
                        valB = "${sessionB.startSoc}%",
                        delta = "${sessionA.startSoc - sessionB.startSoc}%"
                    )

                    ComparisonRow(
                        label = "Ending SOC",
                        valA = "${sessionA.endSoc ?: sessionA.startSoc}%",
                        valB = "${sessionB.endSoc ?: sessionB.startSoc}%",
                        delta = "${(sessionA.endSoc ?: sessionA.startSoc) - (sessionB.endSoc ?: sessionB.startSoc)}%"
                    )

                    ComparisonRow(
                        label = "Active Duration",
                        valA = "${sessionA.totalDurationSeconds / 60}m ${sessionA.totalDurationSeconds % 60}s",
                        valB = "${sessionB.totalDurationSeconds / 60}m ${sessionB.totalDurationSeconds % 60}s",
                        delta = "${comparison.durationDeltaSeconds / 60}m"
                    )

                    ComparisonRow(
                        label = "Average Net Power",
                        valA = String.format(Locale.US, "%.2f W", sessionA.averagePowerWatts),
                        valB = String.format(Locale.US, "%.2f W", sessionB.averagePowerWatts),
                        delta = String.format(Locale.US, "%+.2f W", comparison.avgPowerDeltaWatts)
                    )

                    ComparisonRow(
                        label = "Peak Power Observed",
                        valA = String.format(Locale.US, "%.2f W", sessionA.maxPowerWatts),
                        valB = String.format(Locale.US, "%.2f W", sessionB.maxPowerWatts),
                        delta = String.format(Locale.US, "%+.2f W", comparison.peakPowerDeltaWatts)
                    )

                    ComparisonRow(
                        label = "Net Absorbed Energy",
                        valA = String.format(Locale.US, "%.2f Wh", comparison.energyDeliveredWhA),
                        valB = String.format(Locale.US, "%.2f Wh", comparison.energyDeliveredWhB),
                        delta = String.format(Locale.US, "%+.2f Wh", comparison.energyDeltaWh)
                    )

                    ComparisonRow(
                        label = "Temperature Rise",
                        valA = String.format(Locale.US, "+%.1f°C", comparison.tempRiseCelsiusA),
                        valB = String.format(Locale.US, "+%.1f°C", comparison.tempRiseCelsiusB),
                        delta = String.format(Locale.US, "%+.1f°C", comparison.tempRiseCelsiusA - comparison.tempRiseCelsiusB)
                    )

                    ComparisonRow(
                        label = "Thermal Load Coeff (Θ)",
                        valA = String.format(Locale.US, "%.1f°C/Wh", comparison.thermalLoadCoeffA),
                        valB = String.format(Locale.US, "%.1f°C/Wh", comparison.thermalLoadCoeffB),
                        delta = String.format(Locale.US, "%+.1f", comparison.thermalLoadCoeffA - comparison.thermalLoadCoeffB)
                    )

                    ComparisonRow(
                        label = "Data Confidence (S_conf)",
                        valA = String.format(Locale.US, "%.1f%%", sessionA.confidenceScore),
                        valB = String.format(Locale.US, "%.1f%%", sessionB.confidenceScore),
                        delta = String.format(Locale.US, "%+.1f%%", sessionA.confidenceScore - sessionB.confidenceScore)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Method & Timestamp
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "SESSION DETAILS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Session A (#${sessionA.id}): ${dateFormat.format(Date(sessionA.startTimestamp))} • ${sessionA.protocolName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Session B (#${sessionB.id}): ${dateFormat.format(Date(sessionB.startTimestamp))} • ${sessionB.protocolName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close Comparison")
                }
            }
        }
    }
}

@Composable
private fun ValidityBanner(result: SessionComparisonResult) {
    val bgColor = when (result.validity) {
        ComparisonValidity.HIGH_FIDELITY_CONTROLLED -> MaterialTheme.colorScheme.primaryContainer
        ComparisonValidity.COMPARABLE_WITH_CAVEATS -> MaterialTheme.colorScheme.secondaryContainer
        ComparisonValidity.UNCONTROLLED_MISLEADING -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (result.validity) {
        ComparisonValidity.HIGH_FIDELITY_CONTROLLED -> MaterialTheme.colorScheme.onPrimaryContainer
        ComparisonValidity.COMPARABLE_WITH_CAVEATS -> MaterialTheme.colorScheme.onSecondaryContainer
        ComparisonValidity.UNCONTROLLED_MISLEADING -> MaterialTheme.colorScheme.onErrorContainer
    }
    val badgeIcon = when (result.validity) {
        ComparisonValidity.HIGH_FIDELITY_CONTROLLED -> Icons.Rounded.CheckCircle
        ComparisonValidity.COMPARABLE_WITH_CAVEATS -> Icons.Rounded.Info
        ComparisonValidity.UNCONTROLLED_MISLEADING -> Icons.Rounded.Warning
    }
    val title = when (result.validity) {
        ComparisonValidity.HIGH_FIDELITY_CONTROLLED -> "HIGH-FIDELITY CONTROLLED COMPARISON"
        ComparisonValidity.COMPARABLE_WITH_CAVEATS -> "COMPARABLE WITH SCIENTIFIC CAVEATS"
        ComparisonValidity.UNCONTROLLED_MISLEADING -> "UNCONTROLLED COMPARISON (INCONCLUSIVE)"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = contentColor)
            }

            if (result.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                result.warnings.forEach { warning ->
                    Text(
                        text = "• $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun VerdictCard(verdict: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SCIENTIFIC VERDICT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = verdict,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    valA: String,
    valB: String,
    delta: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Δ (A - B): $delta",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = valA,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Session A",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = valB,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Session B",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
