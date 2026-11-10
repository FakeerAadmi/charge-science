package com.example.chargescience.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Raw timestamped telemetry sample captured during an active charging session.
 */
@Entity(
    tableName = "telemetry_samples",
    foreignKeys = [
        ForeignKey(
            entity = ChargingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "timestamp"])
    ]
)
data class TelemetrySampleEntity(
    @PrimaryKey(autoGenerate = true)
    val sampleId: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val voltageMv: Int,
    val currentMa: Int,
    val powerWatts: Float,
    val temperatureCelsius: Float,
    val batteryLevel: Int,
    val thermalStatus: Int // 0=None, 1=Light, 2=Moderate, 3=Severe, 4=Critical
)
