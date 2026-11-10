package com.example.chargescience.diagnostics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.chargescience.model.BatteryTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * High-precision hardware battery telemetry collector.
 * Connects directly to Android BatteryManager, sticky ACTION_BATTERY_CHANGED broadcasts,
 * and Android Thermal HAL listeners.
 */
class BatteryTelemetryCollector(private val context: Context) {

    private val applicationContext = context.applicationContext
    private val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _telemetry = MutableStateFlow(BatteryTelemetry())
    val telemetry: StateFlow<BatteryTelemetry> = _telemetry.asStateFlow()

    @Volatile
    private var cachedBatteryIntent: Intent? = null

    @Volatile
    private var currentThermalStatus: Int = 0

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                cachedBatteryIntent = intent
                updateTelemetry()
            }
        }
    }

    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        currentThermalStatus = status
        updateTelemetry()
    }

    init {
        // Initial sticky intent capture
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        cachedBatteryIntent = applicationContext.registerReceiver(batteryReceiver, filter)

        // Register thermal status listener (API 29+)
        try {
            powerManager.addThermalStatusListener(thermalListener)
        } catch (_: Exception) {
            // Graceful fallback for restricted environments
        }

        // Start periodic hardware sampling loop
        scope.launch {
            while (isActive) {
                updateTelemetry()
                // Adaptive poll rate: 1000ms when charging, 3000ms when unplugged to conserve power
                val delayMs = if (_telemetry.value.isCharging) 1000L else 3000L
                delay(delayMs)
            }
        }
    }

    @Synchronized
    private fun updateTelemetry() {
        val intent = cachedBatteryIntent ?: return

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) // Millivolts
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val temperatureCelsius = tempRaw / 10.0f // Tenths of degree C to Celsius
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        // Negotiated capabilities from broadcast extras (API 28+)
        val maxVoltageUv = intent.getIntExtra("max_charging_voltage", 0)
        val maxCurrentUa = intent.getIntExtra("max_charging_current", 0)
        val negotiatedMaxPowerWatts = if (maxVoltageUv > 0 && maxCurrentUa > 0) {
            (maxVoltageUv / 1_000_000.0f) * (maxCurrentUa / 1_000_000.0f)
        } else {
            0.0f
        }

        // Cycle count (API 34+ / Android 14+)
        val cycleCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1)
        } else {
            intent.getIntExtra("android.os.extra.CYCLE_COUNT", -1)
        }

        // Current Now from hardware fuel gauge
        // Sign convention: Positive = charging into battery, Negative = discharging
        val currentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val currentMa = if (currentUa != Int.MIN_VALUE) currentUa / 1000 else 0

        // Remaining Charge Counter
        val chargeCounterUah = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        // Estimated Time Remaining
        val chargeTimeRemainingMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            batteryManager.computeChargeTimeRemaining()
        } else {
            -1L
        }

        // Net Chemical Absorption Power = Voltage (V) * Current (A)
        val netWattage = if (voltageMv > 0 && currentMa != 0) {
            (voltageMv / 1000.0f) * (abs(currentMa) / 1000.0f)
        } else {
            0.0f
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0

        // Thermal Headroom (API 30+)
        val thermalHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                powerManager.getThermalHeadroom(0)
            } catch (_: Exception) {
                Float.NaN
            }
        } else {
            Float.NaN
        }

        // Protocol inference
        val protocol = when {
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Qi Wireless (Max 7.5W on 8a)"
            maxVoltageUv >= 9_000_000 -> "USB Power Delivery (${maxVoltageUv / 1_000_000}V Profile)"
            maxVoltageUv > 5_000_000 -> "USB Power Delivery (${maxVoltageUv / 1_000_000.0f}V Profile)"
            plugged == BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charger (5V Standard)"
            plugged == BatteryManager.BATTERY_PLUGGED_USB -> "Standard USB Port (5V)"
            else -> "Discharging on Battery"
        }

        _telemetry.value = BatteryTelemetry(
            timestamp = System.currentTimeMillis(),
            isCharging = isCharging,
            status = status,
            health = health,
            level = level,
            voltageMv = voltageMv,
            currentMa = currentMa,
            wattage = netWattage,
            temperatureCelsius = temperatureCelsius,
            pluggedSource = plugged,
            maxChargingVoltageUv = maxVoltageUv,
            maxChargingCurrentUa = maxCurrentUa,
            negotiatedMaxPowerWatts = negotiatedMaxPowerWatts,
            cycleCount = cycleCount,
            chargeCounterUah = chargeCounterUah,
            thermalStatus = currentThermalStatus,
            thermalHeadroom = thermalHeadroom,
            chargeTimeRemainingMs = chargeTimeRemainingMs,
            protocolName = protocol
        )

        val formattedPower = String.format(java.util.Locale.US, "%.2f", netWattage)
        android.util.Log.i(
            "ChargeScience",
            "Live Telemetry: Level=$level% | Status=$status | Volts=${voltageMv}mV | NetCurrent=${currentMa}mA | Power=${formattedPower}W | Temp=${temperatureCelsius}°C | Negotiated=${negotiatedMaxPowerWatts}W ($protocol) | Cycles=$cycleCount | ChargeCounter=${chargeCounterUah}uAh | ThermalState=$currentThermalStatus"
        )
    }
}
