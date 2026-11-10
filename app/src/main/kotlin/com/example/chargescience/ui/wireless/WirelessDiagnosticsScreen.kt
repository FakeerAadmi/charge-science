package com.example.chargescience.ui.wireless

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.data.db.entity.WirelessExperimentEntity
import com.example.chargescience.domain.wireless.*
import com.example.chargescience.model.BatteryTelemetry
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WirelessDiagnosticsScreen(
    telemetry: BatteryTelemetry,
    activeSamples: List<TelemetrySampleEntity>,
    experiments: List<WirelessExperimentEntity>,
    accessories: List<AccessoryProfileEntity>,
    onSaveExperiment: suspend (WirelessExperimentEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var selectedExperimentType by remember { mutableStateOf(ExperimentType.BASELINE_A) }
    var ringOuterDiameter by remember { mutableFloatStateOf(55.0f) }
    var ringInnerDiameter by remember { mutableFloatStateOf(44.0f) }
    var ringThickness by remember { mutableFloatStateOf(1.0f) }
    var ringOffset by remember { mutableFloatStateOf(7.8f) } // Default 7.8mm offset if aligned to G logo
    var isPermanentMagnet by remember { mutableStateOf(true) }
    var ringBrandName by remember { mutableStateOf("") }
    var showRingConfigDialog by remember { mutableStateOf(false) }

    // Test execution state
    var isTestActive by remember { mutableStateOf(false) }
    var testStartTimeMs by remember { mutableLongStateOf(0L) }
    var testStartSoc by remember { mutableIntStateOf(0) }
    var testStartTemp by remember { mutableFloatStateOf(0f) }
    var testStartThermalStatus by remember { mutableIntStateOf(0) }
    var testMaxTemp by remember { mutableFloatStateOf(0f) }
    var testMaxThermalStatus by remember { mutableIntStateOf(0) }
    var activeTestReport by remember { mutableStateOf<DiagnosticAnalysisResult?>(null) }
    var testDurationSeconds by remember { mutableLongStateOf(0L) }

    val ringProfile = remember(ringOuterDiameter, ringInnerDiameter, ringThickness, ringOffset, isPermanentMagnet, ringBrandName) {
        MagneticRingProfile(
            outerDiameterMm = ringOuterDiameter,
            innerDiameterMm = ringInnerDiameter,
            thicknessMm = ringThickness,
            offsetFromCoilCenterMm = ringOffset,
            isPermanentMagnet = isPermanentMagnet,
            brandOrModel = ringBrandName
        )
    }

    val diagnosticEngine = remember { WirelessDiagnosticEngine() }

    // Elapsed timer while active
    LaunchedEffect(isTestActive) {
        if (isTestActive) {
            testStartTimeMs = System.currentTimeMillis()
            testStartSoc = telemetry.level
            testStartTemp = telemetry.temperatureCelsius
            testStartThermalStatus = telemetry.thermalStatus
            testMaxTemp = telemetry.temperatureCelsius
            testMaxThermalStatus = telemetry.thermalStatus
            testDurationSeconds = 0L
            activeTestReport = null

            while (isTestActive) {
                delay(1000L)
                testDurationSeconds = (System.currentTimeMillis() - testStartTimeMs) / 1000L
                if (telemetry.temperatureCelsius > testMaxTemp) {
                    testMaxTemp = telemetry.temperatureCelsius
                }
                if (telemetry.thermalStatus > testMaxThermalStatus) {
                    testMaxThermalStatus = telemetry.thermalStatus
                }

                // Check for interruption: was wireless, but disconnected or charging power collapsed
                if (!telemetry.isCharging || telemetry.wattage <= 0.05f) {
                    if (testDurationSeconds > 10) {
                        val interruptionTime = System.currentTimeMillis()
                        val result = diagnosticEngine.analyzeInterruption(
                            experimentType = selectedExperimentType,
                            ringProfile = ringProfile,
                            samples = activeSamples,
                            startTimestamp = testStartTimeMs,
                            interruptionTimestamp = interruptionTime,
                            startTempCelsius = testStartTemp,
                            maxTempCelsius = testMaxTemp,
                            maxThermalStatus = testMaxThermalStatus,
                            didDisconnect = true
                        )
                        activeTestReport = result
                        isTestActive = false

                        val experimentEntity = WirelessExperimentEntity(
                            experimentType = selectedExperimentType.code,
                            label = selectedExperimentType.title,
                            startTimestamp = testStartTimeMs,
                            endTimestamp = interruptionTime,
                            durationSeconds = testDurationSeconds,
                            startSoc = testStartSoc,
                            endSoc = telemetry.level,
                            startTempCelsius = testStartTemp,
                            maxTempCelsius = testMaxTemp,
                            endTempCelsius = telemetry.temperatureCelsius,
                            startThermalStatus = testStartThermalStatus,
                            maxThermalStatus = testMaxThermalStatus,
                            interruptionTimestamp = interruptionTime,
                            elapsedToInterruptionSeconds = testDurationSeconds,
                            didDisconnect = true,
                            cutoffReason = result.outcomeType.name,
                            preInterruptionAvgPowerWatts = result.preInterruptionAvgPowerWatts,
                            preInterruptionMaxTempCelsius = result.preInterruptionMaxTempCelsius,
                            preInterruptionThermalStatus = result.preInterruptionThermalStatus,
                            preInterruptionDropPattern = result.dropPattern,
                            ringOuterDiameterMm = ringOuterDiameter,
                            ringInnerDiameterMm = ringInnerDiameter,
                            ringThicknessMm = ringThickness,
                            ringOffsetMm = ringOffset,
                            ringIsPermanentMagnet = isPermanentMagnet,
                            ringBrandOrModel = ringBrandName,
                            diagnosticOutcome = result.outcomeType.label,
                            confidenceScore = result.confidenceScore,
                            userNotes = null
                        )
                        onSaveExperiment(experimentEntity)
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "WIRELESS CHARGING DIAGNOSTICS & COIL INTELLIGENCE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Experiment Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Diagnostic Experiment Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Select a controlled test configuration to isolate coil misalignment, thermal cutoffs, or magnetic ring eddy current heating.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Experiment Type Selection
                ExperimentType.entries.forEach { exp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedExperimentType == exp,
                            onClick = { if (!isTestActive) selectedExperimentType = exp },
                            enabled = !isTestActive
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = exp.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = exp.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Magnetic Ring Configuration Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Magnetic Ring Profile",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (ringProfile.isPermanentMagnet) "Segmented NdFeB • ${ringProfile.offsetFromCoilCenterMm}mm offset" else "Ferritic Stainless Steel • ${ringProfile.offsetFromCoilCenterMm}mm offset",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = when (ringProfile.eddyCurrentRiskTier) {
                                EddyRiskTier.CRITICAL_UNSEGMENTED_METAL_IN_FLUX -> MaterialTheme.colorScheme.errorContainer
                                EddyRiskTier.HIGH_MAGNETIC_ARRAY_OVERLAP -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                EddyRiskTier.MODERATE_PERIPHERAL_EDDY -> MaterialTheme.colorScheme.tertiaryContainer
                                EddyRiskTier.LOW_ALIGNED_SEGMENTED -> MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Eddy Risk: ${ringProfile.eddyCurrentRiskTier.label}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showRingConfigDialog = true },
                        enabled = !isTestActive
                    ) {
                        Text("Configure")
                    }
                }
            }
        }

        // Active Test Control & Real-time HUD Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTestActive) "TEST IN PROGRESS" else "DIAGNOSTIC TEST READY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isTestActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isTestActive) {
                                val mins = testDurationSeconds / 60
                                val secs = testDurationSeconds % 60
                                String.format(Locale.US, "%02d:%02d", mins, secs)
                            } else {
                                "00:00"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            if (isTestActive) {
                                // Manual completion
                                val endTime = System.currentTimeMillis()
                                val result = diagnosticEngine.analyzeInterruption(
                                    experimentType = selectedExperimentType,
                                    ringProfile = ringProfile,
                                    samples = activeSamples,
                                    startTimestamp = testStartTimeMs,
                                    interruptionTimestamp = null,
                                    startTempCelsius = testStartTemp,
                                    maxTempCelsius = testMaxTemp,
                                    maxThermalStatus = testMaxThermalStatus,
                                    didDisconnect = false
                                )
                                activeTestReport = result
                                isTestActive = false
                            } else {
                                isTestActive = true
                            }
                        },
                        colors = if (isTestActive) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        }
                    ) {
                        Text(if (isTestActive) "End Test" else "Start Test")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("POWER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "%.2f W", telemetry.wattage), style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text("TEMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "%.1f°C", telemetry.temperatureCelsius), style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text("THERMAL HAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("Status ${telemetry.thermalStatus}", style = MaterialTheme.typography.titleMedium)
                    }
                    Column {
                        Text("HEADROOM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = if (telemetry.thermalHeadroom.isNaN()) "N/A" else String.format(Locale.US, "%.2f", telemetry.thermalHeadroom),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Diagnostic Report Card (shown after test concludes or disconnects)
        activeTestReport?.let { report ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIAGNOSTIC OUTCOME",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = Color(report.outcomeType.badgeColorHex).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = report.outcomeType.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(report.outcomeType.badgeColorHex),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Confidence Score: ${String.format(Locale.US, "%.0f", report.confidenceScore * 100)}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Supporting Telemetry Evidence:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    report.supportingEvidence.forEach { item ->
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Recommended Next Step:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = report.recommendedNextExperiment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Historical Experiments Section
        if (experiments.isNotEmpty()) {
            Text(
                text = "SAVED EXPERIMENT LOGS (${experiments.size})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            experiments.forEach { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = exp.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(exp.startTimestamp))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Outcome: ${exp.diagnosticOutcome ?: "None"} • Duration: ${exp.durationSeconds / 60}m ${exp.durationSeconds % 60}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Temp: ${exp.startTempCelsius}°C → ${exp.maxTempCelsius}°C • SOC: ${exp.startSoc}% → ${exp.endSoc}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    // Magnetic Ring Configuration Dialog
    if (showRingConfigDialog) {
        AlertDialog(
            onDismissRequest = { showRingConfigDialog = false },
            title = { Text("Magnetic Ring Physical Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter dimensions and placement relative to the phone center. Pixel 8a Rx coil sits at Y ≈ 62.2mm from bottom (near volume down), while the G logo sits at Y ≈ 70.0mm (+7.8mm higher).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = ringBrandName,
                        onValueChange = { ringBrandName = it },
                        label = { Text("Brand / Model (e.g. ESR HaloLock)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ring Material:", style = MaterialTheme.typography.bodySmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isPermanentMagnet,
                                onClick = { isPermanentMagnet = true }
                            )
                            Text("NdFeB Magnet", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = !isPermanentMagnet,
                                onClick = { isPermanentMagnet = false }
                            )
                            Text("Passive Steel", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    OutlinedTextField(
                        value = ringOffset.toString(),
                        onValueChange = { ringOffset = it.toFloatOrNull() ?: ringOffset },
                        label = { Text("Vertical Offset from Coil (mm)") },
                        supportingText = { Text("7.8mm if aligned with G logo; 0mm if level with Volume Down") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ringOuterDiameter.toString(),
                            onValueChange = { ringOuterDiameter = it.toFloatOrNull() ?: ringOuterDiameter },
                            label = { Text("Outer Ø (mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ringInnerDiameter.toString(),
                            onValueChange = { ringInnerDiameter = it.toFloatOrNull() ?: ringInnerDiameter },
                            label = { Text("Inner Ø (mm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRingConfigDialog = false }) {
                    Text("Save Profile")
                }
            }
        )
    }
}
