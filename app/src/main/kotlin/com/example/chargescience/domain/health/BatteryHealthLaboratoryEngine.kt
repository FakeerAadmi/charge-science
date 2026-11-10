package com.example.chargescience.domain.health

import com.example.chargescience.data.db.entity.BatteryCapacityEstimateEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import kotlin.math.max

data class BatteryHealthSummary(
    val hardwareCycleCount: Int,
    val nominalDesignCapacityMah: Int = 4492,
    val ratedDesignCapacityMah: Int = 4404,
    val latestLearnedCapacityMah: Float?,
    val estimatedSohPercent: Float?,
    val validObservationCount: Int,
    val totalSessionsAnalyzed: Int,
    val confidenceScore: Float,
    val degradationStatus: String,
    val thermalExposureLowHours: Float,
    val thermalExposureModerateHours: Float,
    val thermalExposureElevatedHours: Float,
    val thermalExposureSevereHours: Float,
    val highSocDwellRatio: Float
)

class BatteryHealthLaboratoryEngine {

    companion object {
        const val NOMINAL_DESIGN_CAPACITY_MAH = 4492
        const val RATED_DESIGN_CAPACITY_MAH = 4404
        const val MINIMUM_DELTA_SOC_PERCENT = 30
        const val STRICT_DELTA_SOC_PERCENT = 40
        const val MAX_ALLOWABLE_DELTA_TEMP_CELSIUS = 8.0f
        const val MAX_ALLOWABLE_PEAK_TEMP_CELSIUS = 45.0f
    }

    /**
     * Evaluates a completed charging session for battery capacity estimation.
     * Enforces strict electrochemical gating criteria to filter out noise and quantization distortion.
     */
    fun evaluateSessionCapacity(
        session: ChargingSessionEntity,
        samples: List<TelemetrySampleEntity> = emptyList(),
        currentCycleCount: Int = -1
    ): BatteryCapacityEstimateEntity {
        val endSoc = session.endSoc ?: session.startSoc
        val deltaSoc = endSoc - session.startSoc
        val deltaChargeCounterUah = session.endChargeCounterUah - session.startChargeCounterUah
        val deltaTemp = session.maxTempCelsius - session.startTempCelsius

        // Gate 1: Check Minimum Delta SOC
        if (deltaSoc < MINIMUM_DELTA_SOC_PERCENT) {
            return BatteryCapacityEstimateEntity(
                sessionId = session.id,
                timestamp = session.endTimestamp ?: session.startTimestamp,
                startSoc = session.startSoc,
                endSoc = endSoc,
                deltaSoc = deltaSoc,
                deltaChargeCounterUah = deltaChargeCounterUah,
                calculatedCapacityMah = 0f,
                estimatedSohPercent = 0f,
                averageTempCelsius = session.startTempCelsius,
                maxTempCelsius = session.maxTempCelsius,
                cycleCountAtSession = currentCycleCount,
                eligibilityStatus = "REJECTED_LOW_DELTA_SOC",
                rejectionReason = "ΔSOC ($deltaSoc%) is below 30% threshold. Quantization error exceeds ±3.3%."
            )
        }

        // Gate 2: Check Charge Counter Monotonicity & Validity
        if (deltaChargeCounterUah <= 0L) {
            return BatteryCapacityEstimateEntity(
                sessionId = session.id,
                timestamp = session.endTimestamp ?: session.startTimestamp,
                startSoc = session.startSoc,
                endSoc = endSoc,
                deltaSoc = deltaSoc,
                deltaChargeCounterUah = deltaChargeCounterUah,
                calculatedCapacityMah = 0f,
                estimatedSohPercent = 0f,
                averageTempCelsius = session.startTempCelsius,
                maxTempCelsius = session.maxTempCelsius,
                cycleCountAtSession = currentCycleCount,
                eligibilityStatus = "REJECTED_INVALID_CHARGE_COUNTER",
                rejectionReason = "Charge counter delta is non-positive ($deltaChargeCounterUah µAh). Fuel gauge uncalibrated."
            )
        }

        // Gate 3: Thermal Stability Gate
        if (deltaTemp > MAX_ALLOWABLE_DELTA_TEMP_CELSIUS || session.maxTempCelsius > MAX_ALLOWABLE_PEAK_TEMP_CELSIUS) {
            return BatteryCapacityEstimateEntity(
                sessionId = session.id,
                timestamp = session.endTimestamp ?: session.startTimestamp,
                startSoc = session.startSoc,
                endSoc = endSoc,
                deltaSoc = deltaSoc,
                deltaChargeCounterUah = deltaChargeCounterUah,
                calculatedCapacityMah = 0f,
                estimatedSohPercent = 0f,
                averageTempCelsius = (session.startTempCelsius + session.endTempCelsius) / 2f,
                maxTempCelsius = session.maxTempCelsius,
                cycleCountAtSession = currentCycleCount,
                eligibilityStatus = "REJECTED_THERMAL_INSTABILITY",
                rejectionReason = "Thermal variation (ΔT = ${String.format(java.util.Locale.US, "%.1f", deltaTemp)}°C, Max = ${String.format(java.util.Locale.US, "%.1f", session.maxTempCelsius)}°C) distorts cell capacity."
            )
        }

        // Gate 4: Session Interruption Check
        if (session.wasInterrupted || session.confidenceScore < 0.70f) {
            return BatteryCapacityEstimateEntity(
                sessionId = session.id,
                timestamp = session.endTimestamp ?: session.startTimestamp,
                startSoc = session.startSoc,
                endSoc = endSoc,
                deltaSoc = deltaSoc,
                deltaChargeCounterUah = deltaChargeCounterUah,
                calculatedCapacityMah = 0f,
                estimatedSohPercent = 0f,
                averageTempCelsius = (session.startTempCelsius + session.endTempCelsius) / 2f,
                maxTempCelsius = session.maxTempCelsius,
                cycleCountAtSession = currentCycleCount,
                eligibilityStatus = "REJECTED_INTERRUPTED",
                rejectionReason = "Session was interrupted or had low confidence score (${String.format(java.util.Locale.US, "%.0f", session.confidenceScore * 100)}%)."
            )
        }

        // Calculation: Coulomb-counted usable full capacity
        // C_usable = (ΔQ_µAh / 1000.0) / (ΔSOC / 100.0)
        val deltaQMah = deltaChargeCounterUah / 1000.0f
        val calculatedCapacityMah = deltaQMah / (deltaSoc / 100.0f)
        val estimatedSohPercent = (calculatedCapacityMah / NOMINAL_DESIGN_CAPACITY_MAH.toFloat()) * 100.0f

        return BatteryCapacityEstimateEntity(
            sessionId = session.id,
            timestamp = session.endTimestamp ?: session.startTimestamp,
            startSoc = session.startSoc,
            endSoc = endSoc,
            deltaSoc = deltaSoc,
            deltaChargeCounterUah = deltaChargeCounterUah,
            calculatedCapacityMah = calculatedCapacityMah,
            estimatedSohPercent = estimatedSohPercent,
            averageTempCelsius = (session.startTempCelsius + session.endTempCelsius) / 2f,
            maxTempCelsius = session.maxTempCelsius,
            cycleCountAtSession = currentCycleCount,
            eligibilityStatus = "VALID",
            rejectionReason = null
        )
    }

    /**
     * Aggregates multiple valid capacity estimates into a robust State of Health model.
     */
    fun computeHealthSummary(
        hardwareCycleCount: Int,
        allEstimates: List<BatteryCapacityEstimateEntity>,
        allSessions: List<ChargingSessionEntity>
    ): BatteryHealthSummary {
        val validEstimates = allEstimates.filter { it.eligibilityStatus == "VALID" && it.calculatedCapacityMah in 2500f..5500f }

        val latestCapacity: Float?
        val sohPercent: Float?
        val confidence: Float

        if (validEstimates.isNotEmpty()) {
            val totalWeight = validEstimates.sumOf { (it.deltaSoc * it.deltaSoc).toDouble() }
            val weightedCap = validEstimates.sumOf {
                (it.calculatedCapacityMah * (it.deltaSoc * it.deltaSoc)).toDouble()
            } / totalWeight

            latestCapacity = weightedCap.toFloat()
            sohPercent = (latestCapacity / NOMINAL_DESIGN_CAPACITY_MAH.toFloat()) * 100.0f
            confidence = when {
                validEstimates.size >= 5 -> 0.92f
                validEstimates.size >= 3 -> 0.85f
                else -> 0.70f
            }
        } else {
            latestCapacity = null
            sohPercent = null
            confidence = if (hardwareCycleCount >= 0) 0.50f else 0.20f
        }

        // Thermal stress exposure calculation across historical sessions
        var lowTempSeconds = 0L
        var modTempSeconds = 0L
        var elevatedTempSeconds = 0L
        var severeTempSeconds = 0L
        var highSocSeconds = 0L
        var totalTrackedSeconds = 0L

        for (session in allSessions) {
            val dur = max(0L, session.totalDurationSeconds)
            totalTrackedSeconds += dur
            val avgT = (session.startTempCelsius + session.endTempCelsius) / 2f
            when {
                avgT < 30.0f -> lowTempSeconds += dur
                avgT in 30.0f..38.0f -> modTempSeconds += dur
                avgT in 38.01f..42.0f -> elevatedTempSeconds += dur
                else -> severeTempSeconds += dur
            }
            if ((session.endSoc ?: session.startSoc) >= 80) {
                highSocSeconds += dur / 2 // Approximate proportion of session spent in upper 20%
            }
        }

        val highSocRatio = if (totalTrackedSeconds > 0) highSocSeconds.toFloat() / totalTrackedSeconds.toFloat() else 0f

        val degradationStatus = when {
            sohPercent != null && sohPercent >= 90f -> "Normal (Minimal Aging)"
            sohPercent != null && sohPercent >= 80f -> "Moderate Degradation"
            sohPercent != null -> "Elevated Wear (Replacement Advisory)"
            hardwareCycleCount >= 500 -> "High Cycles (Degradation Expected)"
            hardwareCycleCount >= 200 -> "Normal Cycle Progression"
            else -> "Baseline / Low Cycle Count"
        }

        return BatteryHealthSummary(
            hardwareCycleCount = hardwareCycleCount,
            nominalDesignCapacityMah = NOMINAL_DESIGN_CAPACITY_MAH,
            ratedDesignCapacityMah = RATED_DESIGN_CAPACITY_MAH,
            latestLearnedCapacityMah = latestCapacity,
            estimatedSohPercent = sohPercent,
            validObservationCount = validEstimates.size,
            totalSessionsAnalyzed = allSessions.size,
            confidenceScore = confidence,
            degradationStatus = degradationStatus,
            thermalExposureLowHours = lowTempSeconds / 3600f,
            thermalExposureModerateHours = modTempSeconds / 3600f,
            thermalExposureElevatedHours = elevatedTempSeconds / 3600f,
            thermalExposureSevereHours = severeTempSeconds / 3600f,
            highSocDwellRatio = highSocRatio
        )
    }
}
