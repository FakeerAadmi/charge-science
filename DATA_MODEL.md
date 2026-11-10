# Data Model & Storage Architecture
## Project: CHARGE SCIENCE
**Database Engine:** Room 2.6.1 / SQLite (WAL mode, Version 2)  
**Code Generation:** KSP 2.3.13 (`room.generateKotlin = "true"`)  
**Preferences Engine:** Jetpack DataStore Preferences

### 1.4 `WirelessExperimentEntity`
Tracks controlled diagnostic experiments for isolating wireless charging failure modes (misalignment, thermal cutoff, FOD).

```kotlin
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
    indices = [Index("accessoryId"), Index("startTimestamp")]
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
    val cutoffReason: String? = null,
    val preInterruptionAvgPowerWatts: Float? = null,
    val preInterruptionMaxTempCelsius: Float? = null,
    val preInterruptionThermalStatus: Int? = null,
    val preInterruptionDropPattern: String? = null,
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
```

### 1.5 `BatteryCapacityEstimateEntity`
Stores qualified Coulomb-counting capacity estimations and gating rejections.

```kotlin
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
    indices = [Index("sessionId"), Index("timestamp")]
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
```

---

## 4. Database Migrations

- **`MIGRATION_1_2` (V1 -> V2):** Added `accessoryId` foreign key to `charging_sessions` and created `accessory_profiles` table.
- **`MIGRATION_2_3` (V2 -> V3):** Created `wireless_experiments` and `battery_capacity_estimates` tables with foreign keys and indexes.

### 1.1 `ChargingSessionEntity`
Represents an uninterrupted active charging session with comprehensive thermodynamic and electrical telemetry.

```kotlin
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
    val averagePowerWatts: Float = 0f, // Time-weighted trapezoidal average
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
    val confidenceScore: Float = 0f, // 0.0 - 100.0 (S_conf)
    val isCompleted: Boolean = false,
    val wasInterrupted: Boolean = false, // Process killed, crash, or unexpected device reboot
    val accessoryId: Long? = null // Associated accessory profile ID (power brick, wireless pad)
)
```

### 1.2 `TelemetrySampleEntity`
High-resolution raw time-series data points (1 Hz nominal) captured during an active session for graphing in Charging Lab.

```kotlin
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
```

### 1.3 `AccessoryProfileEntity` (Added in Version 2)
Persisted user-managed and auto-correlated charging accessory hardware profiles.

```kotlin
@Entity(tableName = "accessory_profiles")
data class AccessoryProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // CHARGER, CABLE, WIRELESS_PAD, POWER_BANK
    val nickname: String, // e.g. "Google 30W USB-C Charger", "Anker 737 GaN"
    val manufacturer: String = "Unknown",
    val model: String = "Unknown",
    val claimedWattage: Float = 0f,
    val verifiedProtocols: String = "",
    val maxObservedPowerWatts: Float = 0f,
    val firstSeenTimestamp: Long = System.currentTimeMillis(),
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
```

---

## 2. Room Migrations

### `MIGRATION_1_2`
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE charging_sessions ADD COLUMN accessoryId INTEGER DEFAULT NULL")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS accessory_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                nickname TEXT NOT NULL,
                manufacturer TEXT NOT NULL,
                model TEXT NOT NULL,
                claimedWattage REAL NOT NULL,
                verifiedProtocols TEXT NOT NULL,
                maxObservedPowerWatts REAL NOT NULL,
                firstSeenTimestamp INTEGER NOT NULL,
                lastSeenTimestamp INTEGER NOT NULL,
                notes TEXT NOT NULL
            )
        """.trimIndent())
    }
}
```

---

## 3. Data Export Schema (Version 1)

### JSON Laboratory Export Structure
```json
{
  "app_name": "Charge Science",
  "app_version": "1.0",
  "schema_version": 1,
  "export_timestamp_iso": "2026-11-10T17:45:00Z",
  "device_hardware": {
    "manufacturer": "Google",
    "model": "Pixel 8a",
    "code_name": "akita",
    "nominal_design_capacity_mah": 4492,
    "pmic_fuel_gauge": "Maxim MAX77759 ModelGauge m5"
  },
  "accessories": [
    {
      "id": 1,
      "type": "CHARGER",
      "nickname": "Google 30W USB-C",
      "manufacturer": "Google",
      "claimed_wattage": 30.0
    }
  ],
  "sessions": [
    {
      "session_id": 1,
      "start_timestamp_iso": "2026-11-10T17:00:00Z",
      "duration_seconds": 1800,
      "start_soc": 20,
      "end_soc": 75,
      "average_power_watts": 14.82,
      "samples": [
        { "id": 1, "t": 1700000000000, "v": 3850, "i": 2200, "p": 8.47, "temp": 26.5, "soc": 20, "th": 0 }
      ]
    }
  ]
}
```
