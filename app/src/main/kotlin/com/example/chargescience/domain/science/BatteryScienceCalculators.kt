package com.example.chargescience.domain.science

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Validated scientific parameters and calculations for Google Pixel 8a (akita)
 */
object BatteryScienceCalculators {

    const val PIXEL_8A_DESIGN_CAPACITY_UAH = 4_486_000L // sysfs charge_full_design
    const val PIXEL_8A_NOMINAL_MAH = 4492
    const val COULOMBIC_EFFICIENCY = 0.992 // NMC/Graphite cell efficiency
    const val MIN_SWING_SOC_FOR_SOH = 70 // Require at least 70% swing for calibrated health
    const val CONFIDENCE_THRESHOLD_PERCENT = 85.0f

    /**
     * Calculates net instantaneous battery chemical power in Watts.
     * P = V * I = (mV / 1000) * (mA / 1000) = (mV * mA) / 1,000,000.
     * Sentinels (Int.MIN_VALUE or <= 0) return 0f.
     */
    fun calculateInstantaneousPower(voltageMv: Int, currentMa: Int): Float {
        if (voltageMv <= 0 || currentMa == Int.MIN_VALUE || currentMa == 0) {
            return 0.0f
        }
        return (voltageMv / 1000.0f) * (abs(currentMa) / 1000.0f)
    }

    /**
     * Computes time-weighted average power across non-uniform sampling timestamps.
     * Uses trapezoidal numerical integration of Joules over elapsed seconds.
     *
     * @param samples List of Pair(timestampMs, powerWatts)
     */
    fun calculateWeightedAveragePower(samples: List<Pair<Long, Float>>): Float {
        if (samples.isEmpty()) return 0.0f
        if (samples.size == 1) return samples.first().second

        var totalJoules = 0.0
        val startTimeMs = samples.first().first
        val endTimeMs = samples.last().first
        val totalDurationSec = (endTimeMs - startTimeMs) / 1000.0

        if (totalDurationSec <= 0.0) return samples.first().second

        for (i in 1 until samples.size) {
            val (t0, p0) = samples[i - 1]
            val (t1, p1) = samples[i]
            val dtSec = (t1 - t0) / 1000.0
            if (dtSec > 0) {
                totalJoules += ((p0 + p1) / 2.0) * dtSec
            }
        }

        return (totalJoules / totalDurationSec).toFloat()
    }

    /**
     * Calculates the statistical Session Confidence Score (S_conf in [0, 100]).
     */
    fun calculateSessionConfidence(
        deltaSoc: Int,
        minTemp: Float,
        maxTemp: Float,
        avgTemp: Float,
        maxThermalStatus: Int,
        reachedFullTaper: Boolean,
        hardwareCounterValid: Boolean
    ): Float {
        // 1. Depth of swing factor (0 to 100)
        val cDeltaSoc = when {
            deltaSoc >= 80 -> 100.0f
            deltaSoc <= 20 -> 0.0f
            else -> ((deltaSoc - 20) / 60.0f) * 100.0f
        }

        // 2. Thermal stability factor (0 to 100)
        val tempDev = abs(avgTemp - 25.0f)
        val tempSwing = maxTemp - minTemp
        val throttlePenalty = if (maxThermalStatus >= 2) 35.0f else 0.0f
        val cThermal = max(0.0f, min(100.0f, 100.0f - (3.0f * tempDev) - (2.5f * tempSwing) - throttlePenalty))

        // 3. CV Saturation / Rest factor (0 to 100)
        val cRest = when {
            reachedFullTaper -> 100.0f
            deltaSoc >= 75 -> 75.0f
            deltaSoc >= 50 -> 40.0f
            else -> 10.0f
        }

        // 4. Data source integrity
        val cSource = if (hardwareCounterValid) 100.0f else 30.0f

        return (0.40f * cDeltaSoc) + (0.25f * cThermal) + (0.20f * cRest) + (0.15f * cSource)
    }

    /**
     * Calculates estimated Full Charge Capacity (FCC in µAh) from hardware charge counter delta.
     */
    fun estimateFullChargeCapacityUah(
        deltaSoc: Int,
        startCounterUah: Long,
        endCounterUah: Long
    ): Long {
        if (deltaSoc <= 0 || startCounterUah <= 0 || endCounterUah <= startCounterUah) {
            return 0L
        }
        val deltaQAbsorbed = (endCounterUah - startCounterUah).toDouble()
        val deltaQStored = deltaQAbsorbed * COULOMBIC_EFFICIENCY
        val deltaSocFraction = deltaSoc / 100.0
        return (deltaQStored / deltaSocFraction).toLong()
    }
}
