package com.example.chargescience.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted charging session in Room SQLite database.
 * Tracks continuous active charging events with comprehensive thermodynamic and electrical telemetry.
 */
@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val startSoc: Int,
    val endSoc: Int? = null,
    val chargeSource: Int, // 1 = AC (USB-PD/Fast), 2 = USB, 4 = Wireless Qi
    val protocolName: String,
    val totalDurationSeconds: Long = 0, // Strictly active charging duration (excluding paused / full soak)
    val averagePowerWatts: Float = 0f,
    val maxPowerWatts: Float = 0f,
    val averageCurrentMa: Int = 0,
    val maxCurrentMa: Int = 0,
    val startTempCelsius: Float = 0f,
    val maxTempCelsius: Float = 0f,
    val endTempCelsius: Float = 0f,
    val startChargeCounterUah: Long = 0,
    val endChargeCounterUah: Long = 0,
    val maxNegotiatedVoltageUv: Int = 0,
    val maxNegotiatedCurrentUa: Int = 0,
    val negotiatedMaxPowerWatts: Float = 0f,
    val thermalThrottleCount: Int = 0,
    val validSampleCount: Int = 0,
    val missingSampleCount: Int = 0,
    val confidenceScore: Float = 0f,
    val isCompleted: Boolean = false,
    val wasInterrupted: Boolean = false, // Process killed, crash, or unexpected device reboot
    val accessoryId: Long? = null // Associated accessory profile ID (power brick, wireless pad)
)
