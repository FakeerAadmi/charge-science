# Data Model & Storage Architecture
## Project: CHARGE SCIENCE
**Database Engine:** Room 2.6.1 / SQLite (WAL mode)  
**Code Generation:** KSP 2.3.13 (`room.generateKotlin = "true"`)  
**Preferences Engine:** Jetpack DataStore Preferences

---

## 1. Entity Definitions

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
    val wasInterrupted: Boolean = false // Process killed, crash, or unexpected device reboot
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

### 1.3 `ChargingSessionDao` & `TelemetrySampleDao`
Reactive query API returning Kotlin `Flow` for real-time Compose observation:

- `ChargingSessionDao.getAllSessions(): Flow<List<ChargingSessionEntity>>`
- `ChargingSessionDao.getSessionById(sessionId: Long): Flow<ChargingSessionEntity?>`
- `ChargingSessionDao.getLatestActiveSession(): Flow<ChargingSessionEntity?>`
- `TelemetrySampleDao.getSamplesForSession(sessionId: Long): Flow<List<TelemetrySampleEntity>>`
- `TelemetrySampleDao.getRecentSamplesForSession(sessionId: Long, sinceTimestamp: Long): Flow<List<TelemetrySampleEntity>>`

---

## 2. In-Memory Domain Model: `BatteryTelemetry`

Exposed at runtime by `BatteryTelemetryCollector` as `StateFlow<BatteryTelemetry>`:

```kotlin
data class BatteryTelemetry(
    val timestamp: Long = System.currentTimeMillis(),
    val isCharging: Boolean = false,
    val status: Int = 1, // BatteryManager.BATTERY_STATUS_*
    val level: Int = 0, // 0 - 100%
    val voltageMv: Int = 0, // Terminal voltage in mV
    val currentMa: Int = 0, // Net current in mA (+ charge, - discharge)
    val wattage: Float = 0f, // Net battery power: (V * I) in Watts
    val temperatureCelsius: Float = 0f, // Temperature in °C
    val pluggedSource: Int = 0, // AC, USB, Wireless
    val maxChargingVoltageUv: Int = 0, // Negotiated profile limit
    val maxChargingCurrentUa: Int = 0, // Negotiated profile limit
    val negotiatedMaxPowerWatts: Float = 0f, // Max theoretical watts
    val cycleCount: Int = -1, // Hardware cycle count
    val remainingChargeUah: Long = -1L, // Remaining charge counter
    val thermalStatus: Int = 0, // Thermal throttling state
    val thermalHeadroom: Float = 0f, // Margin to severe throttling
    val chargeTimeRemainingMs: Long = -1L
)
```

---

## 3. Database Engine Configuration

### Room Kotlin Code Generation
To prevent javac generic erasure conflicts with Kotlin coroutine `Continuation` stubs in Room 2.6.1 + KSP 2.3+, KSP is configured to generate native Kotlin code:

```kotlin
ksp {
    arg("room.generateKotlin", "true")
}
```

### Threading & Concurrency
- All write operations execute within transaction scopes on `Dispatchers.IO`.
- WAL (Write-Ahead Logging) is automatically enabled by Room on Android SQLite, enabling concurrent reads from Jetpack Compose UI while telemetry samples are inserted in the background at 1 Hz.
