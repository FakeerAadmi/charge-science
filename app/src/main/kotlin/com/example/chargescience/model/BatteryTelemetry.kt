package com.example.chargescience.model

import android.os.BatteryManager

/**
 * Domain representation of real-time battery and charging telemetry.
 * Sourced directly from hardware fuel gauges, Android Thermal HAL, and BatteryManager.
 */
data class BatteryTelemetry(
    val timestamp: Long = System.currentTimeMillis(),
    val isCharging: Boolean = false,
    val status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
    val health: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val level: Int = 0, // State of Charge (0 - 100%)
    val voltageMv: Int = 0, // Terminal voltage in millivolts
    val currentMa: Int = 0, // Net battery current in mA (+ charge, - discharge)
    val wattage: Float = 0f, // Net chemical power into cell: (voltageMv * currentMa) / 1,000,000
    val temperatureCelsius: Float = 0f, // Battery temperature in °C
    val pluggedSource: Int = 0, // 0 = Unplugged, 1 = AC, 2 = USB, 4 = Wireless, 8 = Dock
    val maxChargingVoltageUv: Int = 0, // Negotiated profile voltage in µV
    val maxChargingCurrentUa: Int = 0, // Negotiated profile current in µA
    val negotiatedMaxPowerWatts: Float = 0f, // Theoretical negotiated power limit
    val cycleCount: Int = -1, // Hardware cycle count (API 34+)
    val chargeCounterUah: Long = -1L, // Remaining charge counter in µAh
    val thermalStatus: Int = 0, // PowerManager.THERMAL_STATUS_*
    val thermalHeadroom: Float = Float.NaN, // Margin to severe throttling (<1.0 safe, >=1.0 throttled)
    val chargeTimeRemainingMs: Long = -1L, // Estimated ms to full
    val protocolName: String = "Discharging" // Human-readable classified protocol
) {
    val isUsbPd: Boolean
        get() = maxChargingVoltageUv > 5_000_000

    val isWireless: Boolean
        get() = pluggedSource == BatteryManager.BATTERY_PLUGGED_WIRELESS

    val isFastCharging: Boolean
        get() = isCharging && (wattage >= 10.0f || negotiatedMaxPowerWatts >= 15.0f)

    val chargeStateDescription: String
        get() = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> if (isFastCharging) "Fast Charging" else "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Plugged In (Not Charging)"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            else -> "Unknown"
        }
}
