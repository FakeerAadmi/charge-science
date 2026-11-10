package com.example.chargescience.domain.wireless

import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import kotlin.math.max

enum class ExperimentType(val code: String, val title: String, val description: String) {
    BASELINE_A(
        "BASELINE_A",
        "Experiment A: Baseline",
        "Magnetic ring attached, standard placement on the wireless charging pad."
    ),
    ALIGNMENT_B(
        "ALIGNMENT_B",
        "Experiment B: Coil Alignment",
        "Magnetic ring attached, phone shifted 5-8 mm downward to align with the internal Rx coil below the G logo."
    ),
    RING_REMOVED_C(
        "RING_REMOVED_C",
        "Experiment C: Ring Removed",
        "Magnetic ring removed. Same charging pad, matching starting SOC and initial temperature."
    ),
    ALT_CHARGER_D(
        "ALT_CHARGER_D",
        "Experiment D: Alternative Charger",
        "Test on a different wireless charging pad under matching physical conditions."
    ),
    THERMAL_E(
        "THERMAL_E",
        "Experiment E: Thermal Baseline",
        "Test starting from a cold / resting thermal state (e.g. ambient < 30°C) versus warm baseline."
    ),
    CUSTOM(
        "CUSTOM",
        "Custom Observation",
        "User-defined wireless diagnostic test session."
    )
}

enum class DiagnosticOutcomeType(val label: String, val badgeColorHex: Long) {
    PHONE_THERMAL_THROTTLE("Phone Thermal Throttling / Cutoff", 0xFFE57373),
    TX_SHUTDOWN_FOD("Transmitter FOD or Pad Overheat", 0xFFFFB74D),
    POWER_COUPLING_COLLAPSE("Spatial Misalignment / Coupling Drop", 0xFFFFF176),
    POSSIBLE_ACCESSORY_INTERFERENCE("Accessory Induced Interference", 0xFFFF8A65),
    INSUFFICIENT_EVIDENCE("Insufficient Telemetry Evidence", 0xFF90A4AE),
    STABLE_COMPLETION("Stable Session (No Interruption)", 0xFF81C784)
}

data class DiagnosticAnalysisResult(
    val outcomeType: DiagnosticOutcomeType,
    val supportingEvidence: List<String>,
    val confidenceScore: Float,
    val preInterruptionAvgPowerWatts: Float,
    val preInterruptionMaxTempCelsius: Float,
    val preInterruptionThermalStatus: Int,
    val dropPattern: String, // "ABRUPT", "GRADUAL_THROTTLE", "OSCILLATING", "NONE"
    val elapsedToInterruptionSeconds: Long?,
    val recommendedNextExperiment: String
)

class WirelessDiagnosticEngine {

    companion object {
        const val PRE_INTERRUPTION_WINDOW_SECONDS = 120
        const val MINIMUM_ANALYSIS_SAMPLES = 30
        const val THERMAL_THROTTLE_TEMP_THRESHOLD = 40.5f // Pixel 8a VIRTUAL-SKIN-CHARGE-WLC triggers ~41°C
    }

    /**
     * Evaluates a completed or interrupted wireless diagnostic experiment using the preceding
     * 60-120 seconds of telemetry samples.
     */
    fun analyzeInterruption(
        experimentType: ExperimentType,
        ringProfile: MagneticRingProfile,
        samples: List<TelemetrySampleEntity>,
        startTimestamp: Long,
        interruptionTimestamp: Long?,
        startTempCelsius: Float,
        maxTempCelsius: Float,
        maxThermalStatus: Int,
        didDisconnect: Boolean
    ): DiagnosticAnalysisResult {
        if (!didDisconnect || interruptionTimestamp == null) {
            val avgP = if (samples.isNotEmpty()) {
                samples.map { it.powerWatts }.average().toFloat()
            } else {
                0f
            }
            return DiagnosticAnalysisResult(
                outcomeType = DiagnosticOutcomeType.STABLE_COMPLETION,
                supportingEvidence = listOf(
                    "Session charged continuously without power collapse or disconnect.",
                    "Peak temperature reached ${String.format(java.util.Locale.US, "%.1f", maxTempCelsius)}°C with thermal status $maxThermalStatus."
                ),
                confidenceScore = 0.95f,
                preInterruptionAvgPowerWatts = if (avgP.isNaN()) 0f else avgP,
                preInterruptionMaxTempCelsius = maxTempCelsius,
                preInterruptionThermalStatus = maxThermalStatus,
                dropPattern = "NONE",
                elapsedToInterruptionSeconds = null,
                recommendedNextExperiment = "Baseline is stable. No remediation required."
            )
        }

        val elapsedSeconds = (interruptionTimestamp - startTimestamp) / 1000L

        // Extract preceding 60-120s window of samples before interruption
        val windowStart = max(startTimestamp, interruptionTimestamp - (PRE_INTERRUPTION_WINDOW_SECONDS * 1000L))
        val preSamples = samples.filter { it.timestamp in windowStart..interruptionTimestamp }

        if (preSamples.size < MINIMUM_ANALYSIS_SAMPLES) {
            return DiagnosticAnalysisResult(
                outcomeType = DiagnosticOutcomeType.INSUFFICIENT_EVIDENCE,
                supportingEvidence = listOf(
                    "Interruption occurred within ${elapsedSeconds}s of test start.",
                    "Only ${preSamples.size} telemetry samples were captured in the pre-interruption buffer.",
                    "Minimum required for robust thermodynamic analysis is $MINIMUM_ANALYSIS_SAMPLES samples."
                ),
                confidenceScore = 0.35f,
                preInterruptionAvgPowerWatts = 0f,
                preInterruptionMaxTempCelsius = maxTempCelsius,
                preInterruptionThermalStatus = maxThermalStatus,
                dropPattern = "UNKNOWN",
                elapsedToInterruptionSeconds = elapsedSeconds,
                recommendedNextExperiment = "Repeat test ensuring phone rests undisturbed on pad for at least 2 minutes."
            )
        }

        val powers = preSamples.map { it.powerWatts }
        val temps = preSamples.map { it.temperatureCelsius }
        val avgPower = powers.average().toFloat()
        val preMaxTemp = temps.maxOrNull() ?: maxTempCelsius

        // Assess power drop profile in the final 15 samples
        val finalSamples = preSamples.takeLast(15)
        val finalPowers = finalSamples.map { it.powerWatts }
        var decCount = 0
        for (i in 0 until finalPowers.size - 1) {
            if (finalPowers[i + 1] < finalPowers[i]) {
                decCount++
            }
        }
        val isGradualThrottle = decCount > 8
        val powerVariance = calculateVariance(powers)

        val dropPattern = when {
            powerVariance > 1.8f -> "OSCILLATING"
            isGradualThrottle -> "GRADUAL_THROTTLE"
            else -> "ABRUPT"
        }

        val evidence = mutableListOf<String>()
        val elapsedMinutes = elapsedSeconds / 60.0

        evidence.add("Charging interrupted after ${String.format(java.util.Locale.US, "%.1f", elapsedMinutes)} minutes (${elapsedSeconds}s).")
        evidence.add("Pre-interruption average battery power was ${String.format(java.util.Locale.US, "%.2f", avgPower)} W.")
        evidence.add("Pre-interruption peak battery temperature reached ${String.format(java.util.Locale.US, "%.1f", preMaxTemp)}°C (Thermal Status $maxThermalStatus).")

        // Diagnostic Decision Tree
        val outcome: DiagnosticOutcomeType
        val confidence: Float
        val nextExp: String

        val isThermalTriggered = preMaxTemp >= THERMAL_THROTTLE_TEMP_THRESHOLD || maxThermalStatus >= 2 || (maxTempCelsius - startTempCelsius >= 4.0f)

        if (isThermalTriggered) {
            outcome = DiagnosticOutcomeType.PHONE_THERMAL_THROTTLE
            confidence = if (preMaxTemp >= 41.0f) 0.90f else 0.75f
            evidence.add("Battery temperature exceeded 40.5°C threshold where Pixel 8a VIRTUAL-SKIN-CHARGE-WLC triggers emergency thermal throttling.")
            if (isGradualThrottle) {
                evidence.add("Power curve demonstrated gradual thermal rollback before final current cut.")
            }
            if (ringProfile.hasApertureOverlapRisk) {
                evidence.add("Magnetic ring aperture overlap risk detected (${ringProfile.eddyCurrentRiskTier.label}), causing parasitic induction heating.")
            }
            nextExp = when (experimentType) {
                ExperimentType.BASELINE_A -> "Run Experiment C (Ring Removed) under identical starting conditions to verify if ring eddy currents cause the thermal runaway."
                ExperimentType.RING_REMOVED_C -> "Ring is already removed: The wireless pad itself or phone background thermal load is causing overheating. Test with Experiment D (Alternative Charger)."
                else -> "Run Experiment E (Thermal Baseline) starting from cold resting temperature (<28°C)."
            }
        } else if (dropPattern == "ABRUPT" && preMaxTemp < 38.0f) {
            outcome = DiagnosticOutcomeType.TX_SHUTDOWN_FOD
            confidence = 0.82f
            evidence.add("Charging cut off abruptly while phone battery remained cool (<38°C, Thermal Status $maxThermalStatus).")
            evidence.add("Abrupt power cutoff with normal phone temperature indicates the wireless pad shut off power.")
            evidence.add("Likely cause: Transmitter thermal safety cutoff, or Qi Foreign Object Detection (FOD) triggered by eddy current power loss in the ring.")
            nextExp = "Run Experiment C (Ring Removed) to confirm if the charger's FOD protection is tripping on the metal ring."
        } else if (dropPattern == "OSCILLATING") {
            outcome = DiagnosticOutcomeType.POWER_COUPLING_COLLAPSE
            confidence = 0.78f
            evidence.add("Severe power oscillation (variance ${String.format(java.util.Locale.US, "%.2f", powerVariance)}) observed prior to disconnection.")
            evidence.add("Magnetic coupling factor (k) was unstable, indicating the phone was positioned near the marginal edge of the coil field.")
            nextExp = "Run Experiment B (Coil Alignment): Shift phone 5-8 mm downward so the wireless coil (below G logo) sits directly above the pad center."
        } else if (ringProfile.hasApertureOverlapRisk) {
            outcome = DiagnosticOutcomeType.POSSIBLE_ACCESSORY_INTERFERENCE
            confidence = 0.70f
            evidence.add("Interruption occurred after ~10 minutes with ring profile indicating eddy current risk.")
            evidence.add("Eddy current tier: ${ringProfile.eddyCurrentRiskTier.description}")
            nextExp = "Run Experiment C (Ring Removed) to establish baseline behavior without the accessory."
        } else {
            outcome = DiagnosticOutcomeType.INSUFFICIENT_EVIDENCE
            confidence = 0.50f
            evidence.add("Telemetry shows indeterminate disconnect without obvious thermal or coupling signature.")
            nextExp = "Repeat the test and compare with Experiment D on an alternative charging pad."
        }

        return DiagnosticAnalysisResult(
            outcomeType = outcome,
            supportingEvidence = evidence,
            confidenceScore = confidence,
            preInterruptionAvgPowerWatts = avgPower,
            preInterruptionMaxTempCelsius = preMaxTemp,
            preInterruptionThermalStatus = maxThermalStatus,
            dropPattern = dropPattern,
            elapsedToInterruptionSeconds = elapsedSeconds,
            recommendedNextExperiment = nextExp
        )
    }

    private fun calculateVariance(values: List<Float>): Float {
        if (values.size < 2) return 0f
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }
}
