package com.example.chargescience.ui.history

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.domain.export.DataExportManager
import com.example.chargescience.ui.components.TelemetryTimeSeriesChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingHistoryScreen(
    sessions: List<ChargingSessionEntity>,
    accessories: List<AccessoryProfileEntity> = emptyList(),
    onLoadSessionSamples: suspend (Long) -> List<TelemetrySampleEntity>,
    onLinkAccessoryToSession: suspend (sessionId: Long, accessoryId: Long?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedSession by remember { mutableStateOf<ChargingSessionEntity?>(null) }
    var detailedSamples by remember { mutableStateOf<List<TelemetrySampleEntity>>(emptyList()) }

    // Compare mode
    var isCompareMode by remember { mutableStateOf(false) }
    var selectedForCompare by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showComparisonDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val accessoriesMap = remember(accessories) { accessories.associateBy { it.id } }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORICAL SESSIONS (${sessions.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Compare button
                        FilledTonalButton(
                            onClick = {
                                if (isCompareMode && selectedForCompare.size == 2) {
                                    showComparisonDialog = true
                                } else {
                                    isCompareMode = !isCompareMode
                                    if (!isCompareMode) selectedForCompare = emptySet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCompareMode && selectedForCompare.size == 2) "Compare (2)" else if (isCompareMode) "Cancel" else "Compare",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        // Export button
                        FilledTonalButton(
                            onClick = { showExportMenu = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Export", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (isCompareMode) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (selectedForCompare.size < 2) "Select 2 sessions to run controlled scientific comparison." else "2 sessions selected. Click 'Compare (2)' above to view results.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            items(sessions, key = { it.id }) { session ->
                val isSelectedForCompare = selectedForCompare.contains(session.id)
                SessionCard(
                    session = session,
                    accessoryNickname = session.accessoryId?.let { accessoriesMap[it]?.nickname },
                    isSelectionMode = isCompareMode,
                    isSelected = isSelectedForCompare,
                    onClick = {
                        if (isCompareMode) {
                            selectedForCompare = if (isSelectedForCompare) {
                                selectedForCompare - session.id
                            } else {
                                if (selectedForCompare.size < 2) selectedForCompare + session.id else selectedForCompare
                            }
                        } else {
                            selectedSession = session
                            scope.launch {
                                detailedSamples = onLoadSessionSamples(session.id)
                            }
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
            accessories = accessories,
            onDismiss = {
                selectedSession = null
                detailedSamples = emptyList()
            },
            onExportCsv = {
                scope.launch {
                    val csv = withContext(Dispatchers.Default) {
                        DataExportManager.exportTelemetrySamplesCsv(session, detailedSamples)
                    }
                    val file = DataExportManager.writeExportFile(context, "session_${session.id}_samples.csv", csv)
                    val intent = DataExportManager.createShareIntent(context, file, "text/csv")
                    context.startActivity(Intent.createChooser(intent, "Share Session Telemetry CSV"))
                }
            },
            onAssignAccessory = { accessoryId ->
                scope.launch {
                    onLinkAccessoryToSession(session.id, accessoryId)
                    selectedSession = session.copy(accessoryId = accessoryId)
                }
            }
        )
    }

    // Comparison Dialog
    if (showComparisonDialog && selectedForCompare.size == 2) {
        val idList = selectedForCompare.toList()
        val sessionA = sessions.firstOrNull { it.id == idList[0] }
        val sessionB = sessions.firstOrNull { it.id == idList[1] }

        if (sessionA != null && sessionB != null) {
            ChargingComparisonDialog(
                sessionA = sessionA,
                sessionB = sessionB,
                onDismiss = {
                    showComparisonDialog = false
                    isCompareMode = false
                    selectedForCompare = emptySet()
                }
            )
        }
    }

    // Global Export Menu Dialog
    if (showExportMenu) {
        ExportOptionsDialog(
            onDismiss = { showExportMenu = false },
            onExportSessionsCsv = {
                scope.launch {
                    showExportMenu = false
                    val csv = withContext(Dispatchers.Default) {
                        DataExportManager.exportSessionsCsv(sessions, accessoriesMap)
                    }
                    val file = DataExportManager.writeExportFile(context, "charge_science_sessions.csv", csv)
                    val intent = DataExportManager.createShareIntent(context, file, "text/csv")
                    context.startActivity(Intent.createChooser(intent, "Share Charging Sessions CSV"))
                }
            },
            onExportFullJson = {
                scope.launch {
                    showExportMenu = false
                    val fullData = withContext(Dispatchers.IO) {
                        sessions.map { s ->
                            s to onLoadSessionSamples(s.id)
                        }
                    }
                    val json = withContext(Dispatchers.Default) {
                        DataExportManager.exportCompleteJson(fullData, accessories)
                    }
                    val file = DataExportManager.writeExportFile(context, "charge_science_laboratory_export.json", json)
                    val intent = DataExportManager.createShareIntent(context, file, "application/json")
                    context.startActivity(Intent.createChooser(intent, "Share Laboratory Diagnostic JSON"))
                }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: ChargingSessionEntity,
    accessoryNickname: String? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
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
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onClick() },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = session.protocolName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (accessoryNickname != null) {
                    Text(
                        text = accessoryNickname,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "GAIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val sign = if (socGain >= 0) "+" else ""
                    Text(text = "$sign$socGain% (${session.startSoc}% → $endSoc%)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                Column {
                    Text(text = "DURATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${durationMin}m ${durationSec}s", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }

                Column {
                    Text(text = "AVG POWER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = String.format(Locale.US, "%.1f W", session.averagePowerWatts), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }

                Column {
                    Text(text = "PEAK TEMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = String.format(Locale.US, "%.1f°C", session.maxTempCelsius), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SessionDetailDialog(
    session: ChargingSessionEntity,
    samples: List<TelemetrySampleEntity>,
    accessories: List<AccessoryProfileEntity> = emptyList(),
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit = {},
    onAssignAccessory: (Long?) -> Unit = {}
) {
    var showAccessoryPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SESSION #${session.id} TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = session.protocolName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        FilledTonalButton(
                            onClick = onExportCsv,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Export CSV", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = onDismiss) {
                            Text("✕", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Power curve
                    TelemetryTimeSeriesChart(
                        title = "Net Battery Power Curve",
                        unit = "W",
                        dataPoints = samples.map { Pair(it.timestamp, it.powerWatts) },
                        lineColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Temperature curve
                    TelemetryTimeSeriesChart(
                        title = "Thermal Evolution",
                        unit = "°C",
                        dataPoints = samples.map { Pair(it.timestamp, it.temperatureCelsius) },
                        lineColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Accessory Tagging
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "ASSIGNED ACCESSORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val currentAcc = accessories.firstOrNull { it.id == session.accessoryId }
                                Text(
                                    text = currentAcc?.nickname ?: "Unassigned (Click to tag charger/pad)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            FilledTonalButton(onClick = { showAccessoryPicker = true }) {
                                Text("Assign")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comprehensive Session Stats
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "SCIENTIFIC SESSION SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val energyWh = session.averagePowerWatts * (session.totalDurationSeconds / 3600f)

                            Text(
                                text = String.format(
                                    Locale.US,
                                    "• Net Energy Delivered: %.2f Wh\n" +
                                            "• Peak Battery Power: %.2f W\n" +
                                            "• Average Battery Power: %.2f W\n" +
                                            "• Peak Current: %,d mA\n" +
                                            "• Average Current: %,d mA\n" +
                                            "• Temperature: %.1f°C → %.1f°C (Max: %.1f°C)\n" +
                                            "• Valid 1Hz Samples: %,d\n" +
                                            "• Confidence Score (S_conf): %.1f%%",
                                    energyWh,
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

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close Inspector")
                }
            }
        }
    }

    if (showAccessoryPicker) {
        Dialog(onDismissRequest = { showAccessoryPicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Select Accessory Profile", style = MaterialTheme.typography.titleMedium)

                    if (accessories.isEmpty()) {
                        Text(
                            text = "No accessories created yet. Go to Charger tab to create accessory profiles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        accessories.forEach { acc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAssignAccessory(acc.id)
                                        showAccessoryPicker = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = acc.nickname, style = MaterialTheme.typography.titleSmall)
                                        Text(text = "${acc.type} • ${acc.claimedWattage}W", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (session.accessoryId == acc.id) {
                                        Text("✓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            onAssignAccessory(null)
                            showAccessoryPicker = false
                        }) {
                            Text("Clear Assignment")
                        }
                        TextButton(onClick = { showAccessoryPicker = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExportSessionsCsv: () -> Unit,
    onExportFullJson: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Export Telemetry & Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "All exports are generated locally and shared via Android's native document interface without any network transmission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onExportSessionsCsv),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Charging Sessions (CSV)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Aggregated session durations, average/peak power, thermal extremes, energy Wh, and confidence ratings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onExportFullJson),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Complete Diagnostic Dataset (JSON)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Full laboratory export including device hardware metadata, accessories, and complete 1 Hz time-series sample arrays for every session.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
