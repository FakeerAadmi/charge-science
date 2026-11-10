package com.example.chargescience.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted profile for charging accessories (power bricks, cables, wireless pads, battery banks).
 * Supports automatic matching against observed voltage/current profile limits,
 * user-assigned nicknames, and claimed vs verified capabilities.
 */
@Entity(tableName = "accessory_profiles")
data class AccessoryProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // CHARGER, CABLE, WIRELESS_PAD, POWER_BANK
    val nickname: String, // User-defined name, e.g. "Google 30W USB-C Charger", "Anker 737 GaN"
    val manufacturer: String = "Unknown",
    val model: String = "Unknown",
    val claimedWattage: Float = 0f,
    val verifiedProtocols: String = "", // Comma-separated or JSON string, e.g. "USB-PD Fixed 9V, PPS"
    val maxObservedPowerWatts: Float = 0f,
    val firstSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    companion object {
        const val TYPE_CHARGER = "CHARGER"
        const val TYPE_CABLE = "CABLE"
        const val TYPE_WIRELESS_PAD = "WIRELESS_PAD"
        const val TYPE_POWER_BANK = "POWER_BANK"
    }
}
