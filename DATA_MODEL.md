# Data Model & Storage Architecture
## Project: CHARGE SCIENCE
**Database Engine:** Room 2.6+ / SQLite  
**Preferences Engine:** Jetpack DataStore Preferences

---

## 1. Entity Definitions

### 1.1 `ChargingSessionEntity`
Represents an uninterrupted charging event from power connected to power disconnected.

```kotlin
@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val startLevel: Int,
    val endLevel: Int? = null,
    val chargeSource: Int, // 1 = AC (USB-PD/Fast), 2 = USB, 4 = Wireless Qi
    val protocolName: String, // e.g. "USB Power Delivery (Fixed 9V)", "Qi Inductive"
    val maxNegotiatedVoltageMv: Int,
    val maxNegotiatedCurrentMa: Int,
    val peakWattage: Float,
    val averageWattage: Float,
    val energyDeliveredWh: Float,
    val minTemperature: Float,
    val maxTemperature: Float,
    val throttlingDurationMs: Long,
    val confidenceScore: Float, // 0.0 - 100.0 (S_conf)
    val isValidCalibrationSession: Boolean
)
```

### 1.2 `TelemetrySampleEntity`
High-resolution time-series data points captured during an active session for graphing in Charging Lab.

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
    indices = [Index(value = ["sessionId", "timestamp"])]
)
data class TelemetrySampleEntity(
    @PrimaryKey(autoGenerate = true)
    val sampleId: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val voltageMv: Int,
    val currentMa: Int,
    val wattage: Float,
    val temperatureCelsius: Float,
    val batteryLevel: Int,
    val thermalStatus: Int // 0=None, 1=Light, 2=Moderate, 3=Severe
)
```

### 1.3 `BatteryHealthSnapshotEntity`
Periodic health snapshots calibrated across high-confidence sessions.

```kotlin
@Entity(tableName = "battery_health_snapshots")
data class BatteryHealthSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val cycleCount: Int,
    val estimatedCapacityMah: Float,
    val designCapacityMah: Float = 4492f, // Pixel 8a nominal
    val healthPercentage: Float, // (estimated / design) * 100
    val confidenceScore: Float,
    val calibrationSessionId: Long
)
```

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
