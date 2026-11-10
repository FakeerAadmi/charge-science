package com.example.chargescience.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wireless_experiments",
    foreignKeys = [
        ForeignKey(
            entity = AccessoryProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["accessoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("accessoryId"),
        Index("startTimestamp")
    ]
)
data class WirelessExperimentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val experimentType: String, // "BASELINE_A", "ALIGNMENT_B", "RING_REMOVED_C", "ALT_CHARGER_D", "THERMAL_E", "CUSTOM"
    val label: String,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val durationSeconds: Long = 0L,
    val startSoc: Int = 0,
    val endSoc: Int = 0,
    val startTempCelsius: Float = 0f,
    val maxTempCelsius: Float = 0f,
    val endTempCelsius: Float = 0f,
    val startThermalStatus: Int = 0,
    val maxThermalStatus: Int = 0,
    val interruptionTimestamp: Long? = null,
    val elapsedToInterruptionSeconds: Long? = null,
    val didDisconnect: Boolean = false,
    val cutoffReason: String? = null, // "PHONE_THERMAL_THROTTLE", "TX_DISCONNECT", "POWER_COLLAPSE", "NONE"
    val preInterruptionAvgPowerWatts: Float? = null,
    val preInterruptionMaxTempCelsius: Float? = null,
    val preInterruptionThermalStatus: Int? = null,
    val preInterruptionDropPattern: String? = null, // "ABRUPT", "GRADUAL_THROTTLE", "OSCILLATING"
    val ringOuterDiameterMm: Float = 55.0f,
    val ringInnerDiameterMm: Float = 44.0f,
    val ringThicknessMm: Float = 1.0f,
    val ringOffsetMm: Float = 0.0f,
    val ringIsPermanentMagnet: Boolean = true,
    val ringBrandOrModel: String = "",
    val accessoryId: Long? = null,
    val diagnosticOutcome: String? = null,
    val confidenceScore: Float = 0f,
    val userNotes: String? = null
)
