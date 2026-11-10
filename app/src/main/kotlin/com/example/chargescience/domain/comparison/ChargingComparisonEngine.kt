package com.example.chargescience.domain.comparison

import com.example.chargescience.data.db.entity.ChargingSessionEntity
import kotlin.math.abs
import kotlin.math.max

/**
 * Result of comparing two charging sessions.
 * Implements strict scientific controls to avoid misleading comparisons between
 * uncontrolled charging events.
 */
data class SessionComparisonResult(
    val sessionA: ChargingSessionEntity,
    val sessionB: ChargingSessionEntity,
    val validity: ComparisonValidity,
    val warnings: List<String>,
    val durationDeltaSeconds: Long, // sessionA - sessionB (negative means A was faster)
    val avgPowerDeltaWatts: Float, // sessionA - sessionB (positive means A had higher power)
    val peakPowerDeltaWatts: Float,
    val energyDeliveredWhA: Float,
    val energyDeliveredWhB: Float,
    val energyDeltaWh: Float,
    val tempRiseCelsiusA: Float,
    val tempRiseCelsiusB: Float,
    val thermalLoadCoeffA: Float, // °C rise per Wh delivered
    val thermalLoadCoeffB: Float,
    val scientificVerdict: String
)

enum class ComparisonValidity {
    HIGH_FIDELITY_CONTROLLED,
    COMPARABLE_WITH_CAVEATS,
    UNCONTROLLED_MISLEADING
}

object ChargingComparisonEngine {

    /**
     * Compares two recorded charging sessions with electrochemical rigor.
     */
    fun compareSessions(
        sessionA: ChargingSessionEntity,
        sessionB: ChargingSessionEntity
    ): SessionComparisonResult {
        val warnings = mutableListOf<String>()

        // 1. Check Starting SOC delta
        val startSocDelta = abs(sessionA.startSoc - sessionB.startSoc)
        if (startSocDelta > 15) {
            warnings.add(
                "Starting SOC differs by $startSocDelta% (${sessionA.startSoc}% vs ${sessionB.startSoc}%). " +
                        "Lithium-ion cells have highly non-linear CC/CV curves; comparing across different battery saturation regions is electrochemically misleading."
            )
        }

        // 2. Check Starting Temperature delta
        val startTempDelta = abs(sessionA.startTempCelsius - sessionB.startTempCelsius)
        if (startTempDelta > 6.0f) {
            warnings.add(
                "Starting temperature differs by %.1f°C (%.1f°C vs %.1f°C). Pre-existing heat accelerates thermal throttling.".format(
                    startTempDelta, sessionA.startTempCelsius, sessionB.startTempCelsius
                )
            )
        }

        // 3. Check for process interruption or low confidence
        if (sessionA.wasInterrupted || sessionB.wasInterrupted) {
            warnings.add("One or both sessions were interrupted by app shutdown, crash, or reboot.")
        }
        if (sessionA.confidenceScore < 60f || sessionB.confidenceScore < 60f) {
            warnings.add("One or both sessions have low data confidence (<60%), likely due to brief duration or missing telemetry.")
        }

        // 4. Determine validity tier
        val validity = when {
            startSocDelta > 25 || sessionA.wasInterrupted || sessionB.wasInterrupted -> ComparisonValidity.UNCONTROLLED_MISLEADING
            startSocDelta > 10 || startTempDelta > 5.0f || sessionA.confidenceScore < 75f || sessionB.confidenceScore < 75f -> ComparisonValidity.COMPARABLE_WITH_CAVEATS
            else -> ComparisonValidity.HIGH_FIDELITY_CONTROLLED
        }

        // 5. Compute electrical & energy deltas
        val durationDelta = sessionA.totalDurationSeconds - sessionB.totalDurationSeconds
        val avgPowerDelta = sessionA.averagePowerWatts - sessionB.averagePowerWatts
        val peakPowerDelta = sessionA.maxPowerWatts - sessionB.maxPowerWatts

        val energyWhA = (sessionA.averagePowerWatts * (sessionA.totalDurationSeconds / 3600f))
        val energyWhB = (sessionB.averagePowerWatts * (sessionB.totalDurationSeconds / 3600f))
        val energyDeltaWh = energyWhA - energyWhB

        // 6. Compute thermal dynamics
        val tempRiseA = max(0f, sessionA.maxTempCelsius - sessionA.startTempCelsius)
        val tempRiseB = max(0f, sessionB.maxTempCelsius - sessionB.startTempCelsius)

        val thermalLoadCoeffA = if (energyWhA > 0.1f) tempRiseA / energyWhA else 0f
        val thermalLoadCoeffB = if (energyWhB > 0.1f) tempRiseB / energyWhB else 0f

        // 7. Formulate verdict
        val isAWired = sessionA.chargeSource == 1
        val isBWired = sessionB.chargeSource == 1
        val isAWireless = sessionA.chargeSource == 4
        val isBWireless = sessionB.chargeSource == 4

        val verdict = when {
            validity == ComparisonValidity.UNCONTROLLED_MISLEADING -> {
                "Uncontrolled Comparison: Significant external factors (SOC range or interruption) prevent declaring one accessory superior. Compare sessions under matching starting conditions."
            }
            isAWired && isBWireless -> {
                "Wired vs Wireless: Wired session delivered %.1fW higher average power (%.1fW vs %.1fW) with a %.1fx lower thermal load coefficient (%.1f°C/Wh vs %.1f°C/Wh), reflecting inductive coil conversion losses.".format(
                    avgPowerDelta, sessionA.averagePowerWatts, sessionB.averagePowerWatts,
                    if (thermalLoadCoeffA > 0f) thermalLoadCoeffB / thermalLoadCoeffA else 1f,
                    thermalLoadCoeffA, thermalLoadCoeffB
                )
            }
            isAWireless && isBWired -> {
                "Wireless vs Wired: Wireless session delivered %.1fW lower average power (%.1fW vs %.1fW) and higher thermal rise per Wh, reflecting magnetic coupling and receiver losses.".format(
                    abs(avgPowerDelta), sessionA.averagePowerWatts, sessionB.averagePowerWatts
                )
            }
            avgPowerDelta > 0.5f -> {
                "Session A delivered higher sustained power (+%.1fW average, +%.1fW peak). Energy delivery was %.1f%% faster.".format(
                    avgPowerDelta, peakPowerDelta,
                    if (sessionB.averagePowerWatts > 0f) (avgPowerDelta / sessionB.averagePowerWatts) * 100f else 0f
                )
            }
            avgPowerDelta < -0.5f -> {
                "Session B delivered higher sustained power (+%.1fW average, +%.1fW peak). Energy delivery was %.1f%% faster.".format(
                    abs(avgPowerDelta), abs(peakPowerDelta),
                    if (sessionA.averagePowerWatts > 0f) (abs(avgPowerDelta) / sessionA.averagePowerWatts) * 100f else 0f
                )
            }
            else -> {
                "Comparable Performance: Both sessions exhibited equivalent average power (%.1fW vs %.1fW within ±0.5W margin).".format(
                    sessionA.averagePowerWatts, sessionB.averagePowerWatts
                )
            }
        }

        return SessionComparisonResult(
            sessionA = sessionA,
            sessionB = sessionB,
            validity = validity,
            warnings = warnings,
            durationDeltaSeconds = durationDelta,
            avgPowerDeltaWatts = avgPowerDelta,
            peakPowerDeltaWatts = peakPowerDelta,
            energyDeliveredWhA = energyWhA,
            energyDeliveredWhB = energyWhB,
            energyDeltaWh = energyDeltaWh,
            tempRiseCelsiusA = tempRiseA,
            tempRiseCelsiusB = tempRiseB,
            thermalLoadCoeffA = thermalLoadCoeffA,
            thermalLoadCoeffB = thermalLoadCoeffB,
            scientificVerdict = verdict
        )
    }
}
