package com.example.chargescience.domain.prediction

import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

data class ChargingForecast(
    val timeTo80Minutes: Int,
    val timeToFullMinutes: Int,
    val chemicalEnergyWh: Float,
    val wallEnergyWh: Float,
    val peakPowerWatts: Float,
    val efficiencyPercent: Int,
    val timeTo80Formatted: String,
    val timeToFullFormatted: String
)

data class DualChargingForecast(
    val wired: ChargingForecast,
    val wireless: ChargingForecast,
    val energyOverheadPercent: Int, // E.g. wireless requires +37% more grid energy
    val timeDifferenceMinutes: Int   // Difference in full charging time
)

data class EnduranceForecast(
    val activeMixedSotMinutes: Int,
    val lightSotMinutes: Int,
    val heavySotMinutes: Int,
    val videoPlaybackMinutes: Int,
    val standbyHours: Float,
    val activeMixedSotFormatted: String,
    val lightSotFormatted: String,
    val heavySotFormatted: String,
    val videoPlaybackFormatted: String,
    val standbyFormatted: String
)

object ChargingPredictionEngine {

    const val NOMINAL_CAPACITY_MAH = 4492
    const val NOMINAL_VOLTAGE_V = 3.88f
    const val TOTAL_BATTERY_ENERGY_WH = (NOMINAL_CAPACITY_MAH / 1000f) * NOMINAL_VOLTAGE_V // ≈ 17.43 Wh

    // Power & Efficiency Constants
    const val WIRED_PEAK_POWER_WATTS = 18.0f
    const val WIRED_AVG_CC_POWER_WATTS = 15.0f
    const val WIRED_END_TO_END_EFFICIENCY = 0.82f // 82% (adapter + buck converter)

    const val WIRELESS_PEAK_POWER_WATTS = 7.5f
    const val WIRELESS_AVG_CC_POWER_WATTS = 4.5f
    const val WIRELESS_END_TO_END_EFFICIENCY = 0.60f // 60% (induction coil + rectifier + adapter)

    // Pixel 8a Discharge Current Profiles (mA)
    const val DRAIN_ACTIVE_MIXED_MA = 480  // Mixed 120Hz display, social/browser/apps
    const val DRAIN_LIGHT_READING_MA = 350 // 60Hz text reading, Wi-Fi, low brightness
    const val DRAIN_HEAVY_INTENSIVE_MA = 850 // 3D gaming, 4K camera, high 5G load
    const val DRAIN_VIDEO_PLAYBACK_MA = 380 // 1080p video streaming
    const val DRAIN_STANDBY_MA = 18        // Screen-off Doze standby

    /**
     * Calculates non-linear CC-CV charging time and grid energy consumption
     * comparing Wired (18W USB-PD) vs. Wireless (7.5W Qi) from current battery level.
     */
    fun predictCharging(
        currentSoc: Int,
        nominalCapacityMah: Int = NOMINAL_CAPACITY_MAH
    ): DualChargingForecast {
        val clampedSoc = currentSoc.coerceIn(0, 100)
        val remainingSocPercent = (100 - clampedSoc) / 100.0f
        val batteryEnergyWh = (nominalCapacityMah / 1000f) * NOMINAL_VOLTAGE_V
        val chemicalEnergyNeededWh = remainingSocPercent * batteryEnergyWh

        // Wired Modeling (CC phase up to 75%, exponential taper to 100%)
        val wiredTimeTo80: Int
        val wiredTimeToFull: Int

        if (clampedSoc >= 100) {
            wiredTimeTo80 = 0
            wiredTimeToFull = 0
        } else if (clampedSoc >= 80) {
            wiredTimeTo80 = 0
            val taperProgress = (100 - clampedSoc) / 25.0f
            wiredTimeToFull = max(1, (42 * taperProgress.pow(1.25f)).roundToInt())
        } else if (clampedSoc >= 75) {
            val progressTo80 = (80 - clampedSoc) / 5.0f
            wiredTimeTo80 = max(1, (8 * progressTo80).roundToInt())
            val taperProgress = (100 - clampedSoc) / 25.0f
            wiredTimeToFull = (42 * taperProgress.pow(1.25f)).roundToInt()
        } else {
            val bulkWh = ((75 - clampedSoc) / 100.0f) * batteryEnergyWh
            val bulkMinutes = (bulkWh / WIRED_AVG_CC_POWER_WATTS) * 60f
            wiredTimeTo80 = (bulkMinutes + 8).roundToInt()
            wiredTimeToFull = (bulkMinutes + 42).roundToInt()
        }

        val wiredWallEnergyWh = chemicalEnergyNeededWh / WIRED_END_TO_END_EFFICIENCY

        val wiredForecast = ChargingForecast(
            timeTo80Minutes = wiredTimeTo80,
            timeToFullMinutes = wiredTimeToFull,
            chemicalEnergyWh = chemicalEnergyNeededWh,
            wallEnergyWh = wiredWallEnergyWh,
            peakPowerWatts = WIRED_PEAK_POWER_WATTS,
            efficiencyPercent = (WIRED_END_TO_END_EFFICIENCY * 100).toInt(),
            timeTo80Formatted = formatDuration(wiredTimeTo80),
            timeToFullFormatted = formatDuration(wiredTimeToFull)
        )

        // Wireless Modeling (7.5W Qi ceiling, CC phase up to 80%, taper to 100%)
        val wirelessTimeTo80: Int
        val wirelessTimeToFull: Int

        if (clampedSoc >= 100) {
            wirelessTimeTo80 = 0
            wirelessTimeToFull = 0
        } else if (clampedSoc >= 80) {
            wirelessTimeTo80 = 0
            val taperProgress = (100 - clampedSoc) / 20.0f
            wirelessTimeToFull = max(1, (45 * taperProgress.pow(1.2f)).roundToInt())
        } else {
            val bulkWh = ((80 - clampedSoc) / 100.0f) * batteryEnergyWh
            val bulkMinutes = (bulkWh / WIRELESS_AVG_CC_POWER_WATTS) * 60f
            wirelessTimeTo80 = bulkMinutes.roundToInt()
            wirelessTimeToFull = (bulkMinutes + 45).roundToInt()
        }

        val wirelessWallEnergyWh = chemicalEnergyNeededWh / WIRELESS_END_TO_END_EFFICIENCY

        val wirelessForecast = ChargingForecast(
            timeTo80Minutes = wirelessTimeTo80,
            timeToFullMinutes = wirelessTimeToFull,
            chemicalEnergyWh = chemicalEnergyNeededWh,
            wallEnergyWh = wirelessWallEnergyWh,
            peakPowerWatts = WIRELESS_PEAK_POWER_WATTS,
            efficiencyPercent = (WIRELESS_END_TO_END_EFFICIENCY * 100).toInt(),
            timeTo80Formatted = formatDuration(wirelessTimeTo80),
            timeToFullFormatted = formatDuration(wirelessTimeToFull)
        )

        val overheadPercent = if (wiredWallEnergyWh > 0.05f) {
            (((wirelessWallEnergyWh - wiredWallEnergyWh) / wiredWallEnergyWh) * 100).roundToInt()
        } else {
            0
        }

        return DualChargingForecast(
            wired = wiredForecast,
            wireless = wirelessForecast,
            energyOverheadPercent = overheadPercent,
            timeDifferenceMinutes = max(0, wirelessTimeToFull - wiredTimeToFull)
        )
    }

    /**
     * Calculates remaining screen-on and standby endurance based on current battery level
     * and empirical Pixel 8a current consumption profiles.
     */
    fun predictEndurance(
        currentSoc: Int,
        usableCapacityMah: Int = NOMINAL_CAPACITY_MAH,
        liveDrainMa: Int = 0
    ): EnduranceForecast {
        val clampedSoc = currentSoc.coerceIn(0, 100)
        val remainingCapacityMah = (clampedSoc / 100.0f) * usableCapacityMah

        // Standard screen-on projections
        val activeMins = ((remainingCapacityMah / DRAIN_ACTIVE_MIXED_MA) * 60f).roundToInt()
        val lightMins = ((remainingCapacityMah / DRAIN_LIGHT_READING_MA) * 60f).roundToInt()
        val heavyMins = ((remainingCapacityMah / DRAIN_HEAVY_INTENSIVE_MA) * 60f).roundToInt()
        val videoMins = ((remainingCapacityMah / DRAIN_VIDEO_PLAYBACK_MA) * 60f).roundToInt()
        val standbyHrs = remainingCapacityMah / DRAIN_STANDBY_MA

        return EnduranceForecast(
            activeMixedSotMinutes = activeMins,
            lightSotMinutes = lightMins,
            heavySotMinutes = heavyMins,
            videoPlaybackMinutes = videoMins,
            standbyHours = standbyHrs,
            activeMixedSotFormatted = formatDuration(activeMins),
            lightSotFormatted = formatDuration(lightMins),
            heavySotFormatted = formatDuration(heavyMins),
            videoPlaybackFormatted = formatDuration(videoMins),
            standbyFormatted = if (standbyHrs >= 48f) {
                String.format(Locale.US, "%.1f days", standbyHrs / 24f)
            } else {
                String.format(Locale.US, "%.0f hrs", standbyHrs)
            }
        )
    }

    private fun formatDuration(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0 min"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }
}
