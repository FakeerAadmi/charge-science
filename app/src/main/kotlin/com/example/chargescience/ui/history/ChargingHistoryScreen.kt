package com.example.chargescience.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.ui.components.TelemetryTimeSeriesChart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChargingHistoryScreen(
    sessions: List<ChargingSessionEntity>,
    onLoadSessionSamples: suspend (Long) -> List<TelemetrySampleEntity>,
    modifier: Modifier = Modifier
) {
    var selectedSession by remember { mutableStateOf<ChargingSessionEntity?>(null) }
    var detailedSamples by remember { mutableStateOf<List<TelemetrySampleEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    if (sessions.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "⚡",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "No Charging Sessions Recorded",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Connect your Pixel 8a to a wired or wireless charger to begin recording diagnostic telemetry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "HISTORICAL CHARGING SESSIONS (${sessions.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(sessions, key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    onClick = {
                        selectedSession = session
                        scope.launch {
                            detailedSamples = onLoadSessionSamples(session.id)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Detailed Session Inspection Dialog
    selectedSession?.let { session ->
        SessionDetailDialog(
            session = session,
            samples = detailedSamples,
            onDismiss = {
                selectedSession = null
                detailedSamples = emptyList()
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: ChargingSessionEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val dateString = dateFormat.format(Date(session.startTimestamp))
    val endSoc = session.endSoc ?: session.startSoc
    val socGain = endSoc - session.startSoc

    val durationMin = session.totalDurationSeconds / 60
    val durationSec = session.totalDurationSeconds % 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
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
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (session.confidenceScore >= 80f) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Text(
                        text = if (session.isCompleted) "CONFIDENCE ${session.confidenceScore.toInt()}%" else "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (session.confidenceScore >= 80f) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.protocolName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "GAIN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val sign = if (socGain >= 0) "+" else ""
                    Text(
                        text = "$sign$socGain% (${session.startSoc}% → $endSoc%)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "DURATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${durationMin}m ${durationSec}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "AVG POWER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f W", session.averagePowerWatts),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailDialog(
    session: ChargingSessionEntity,
    samples: List<TelemetrySampleEntity>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SESSION #${session.id} ANALYSIS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = session.protocolName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                // Power curve chart
                val powerPoints = samples.map { Pair(it.timestamp, it.powerWatts) }
                TelemetryTimeSeriesChart(
                    title = "Battery Power Curve",
                    unit = "W",
                    dataPoints = powerPoints,
                    lineColor = MaterialTheme.colorScheme.primary,
                    emptyMessage = "No time-series samples recorded for this session"
                )

                // Temperature curve chart
                val tempPoints = samples.map { Pair(it.timestamp, it.temperatureCelsius) }
                TelemetryTimeSeriesChart(
                    title = "Battery Temperature Curve",
                    unit = "°C",
                    dataPoints = tempPoints,
                    lineColor = MaterialTheme.colorScheme.tertiary,
                    emptyMessage = "No thermal samples recorded"
                )

                // Session Summary breakdown
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Physical Metrics Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = String.format(
                                Locale.US,
                                "• Active Duration: %dm %02ds\n" +
                                "• SOC Gain: %d%% (%d%% → %d%%)\n" +
                                "• Peak Battery Power: %.2f W\n" +
                                "• Average Battery Power: %.2f W\n" +
                                "• Peak Current: %,d mA\n" +
                                "• Average Current: %,d mA\n" +
                                "• Temperature Range: %.1f°C → %.1f°C (Max: %.1f°C)\n" +
                                "• Valid Telemetry Samples: %,d\n" +
                                "• Data Quality / Confidence: %.1f%%",
                                session.totalDurationSeconds / 60,
                                session.totalDurationSeconds % 60,
                                (session.endSoc ?: session.startSoc) - session.startSoc,
                                session.startSoc,
                                session.endSoc ?: session.startSoc,
                                session.maxPowerWatts,
                                session.averagePowerWatts,
                                session.maxCurrentMa,
                                session.averageCurrentMa,
                                session.startTempCelsius,
                                session.endTempCelsius,
                                session.maxTempCelsius,
                                session.validSampleCount,
                                session.confidenceScore
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
