package com.example.chargescience.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "battery_capacity_estimates",
    foreignKeys = [
        ForeignKey(
            entity = ChargingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("timestamp")
    ]
)
data class BatteryCapacityEstimateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val startSoc: Int,
    val endSoc: Int,
    val deltaSoc: Int,
    val deltaChargeCounterUah: Long,
    val calculatedCapacityMah: Float,
    val estimatedSohPercent: Float,
    val averageTempCelsius: Float,
    val maxTempCelsius: Float,
    val cycleCountAtSession: Int,
    val eligibilityStatus: String, // "VALID", "REJECTED_LOW_DELTA_SOC", "REJECTED_THERMAL_INSTABILITY", "REJECTED_INTERRUPTED"
    val rejectionReason: String? = null
)
