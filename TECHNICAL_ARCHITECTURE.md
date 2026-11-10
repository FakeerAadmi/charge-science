# Technical Architecture Specification
## Project: CHARGE SCIENCE
**Language & Platform:** Kotlin 2.3+ / Jetpack Compose / Android 14+ (Target SDK 36, Reference: Pixel 8a)

---

## 1. Architectural Overview

Charge Science implements a modern, reactive, offline-first MVVM architecture adhering to Clean Architecture principles without superfluous multi-module bloat.

```
                      ┌──────────────────────────────────────┐
                      │            UI Layer                  │
                      │  Jetpack Compose + M3 Expressive     │
                      │  (Overview, Lab, Charger, Health,    │
                      │   History & Session Detail Dialog)   │
                      └──────────────────▲───────────────────┘
                                         │ StateFlow<UiState>
                                         │
                      ┌──────────────────┴───────────────────┐
                      │          Session Engine              │
                      │      ChargingSessionManager          │
                      │  (State Machine, Interruption        │
                      │   Recovery & Live Sample Aggregator) │
                      └──────────────────▲───────────────────┘
                                         │
         ┌───────────────────────────────┼───────────────────────────────┐
         │ Flow<BatteryTelemetry>        │ Room SQLite CRUD              │
         ▼                               ▼                               ▼
┌────────────────────────┐   ┌────────────────────────┐   ┌────────────────────────┐
│   Diagnostics Engine   │   │     Storage Engine     │   │   Background Engine    │
│  BatteryManager HAL    │   │  Room 2.6+ (KSP)       │   │ ChargingTelemetryService│
│  Thermal HAL AIDL 3    │   │  ChargingSessionDao    │   │ (FGS: specialUse,      │
│  Sticky Intent Stream  │   │  TelemetrySampleDao    │   │  Partial WakeLock)     │
└────────────────────────┘   └────────────────────────┘   └────────────────────────┘
```

---

## 2. Key Architectural Layers

### 2.1 Diagnostics Engine (`com.example.chargescience.diagnostics`)
- **`BatteryTelemetryCollector`:**
  - Registers sticky broadcast receiver for `Intent.ACTION_BATTERY_CHANGED`.
  - Samples `BatteryManager` properties (`BATTERY_PROPERTY_CURRENT_NOW`, `BATTERY_PROPERTY_CHARGE_COUNTER`, `BATTERY_PROPERTY_CAPACITY`) on an adaptive interval: $1Hz$ while charging, $3s$ when unplugged to conserve battery.
  - Listens to Android Thermal HAL via `PowerManager.OnThermalStatusChangedListener` and `getThermalHeadroom(0)`.
  - Exposes an asynchronous `Flow<BatteryTelemetry>` with thread safety via `Dispatchers.Default`.

### 2.2 Background Telemetry Service (`ChargingTelemetryService`)
- Operates under Android 14/15/17 compliant `foregroundServiceType="specialUse"` with `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="charging_telemetry_recording" />`.
- Holds a `PARTIAL_WAKE_LOCK` strictly while actively charging to prevent CPU suspend when the screen goes dark, ensuring gapless 1 Hz time-series curves.
- Posts an ongoing, silent (`IMPORTANCE_LOW`, `setOnlyAlertOnce(true)`) status bar notification displaying live wattage, voltage, current, and temperature.
- Immediately stops foreground and releases the wake lock upon `ACTION_POWER_DISCONNECTED`.

### 2.3 Charging Session State Machine (`com.example.chargescience.domain.session`)
- **`ChargingSessionManager`:**
  - Detects plug-in, charge start, charge stop, unplug, and charging method switching (e.g., AC to USB or Wireless).
  - Handles process crashes and device reboots via `recoverInterruptedSessions()`, resolving the session end time to the timestamp of the last recorded sample.
  - Batches and persists raw `TelemetrySampleEntity` records into Room.
  - Computes time-weighted average power ($\int P dt / \Delta t$) using trapezoidal integration to eliminate sample-rate throttling bias.
  - Computes the statistical Session Confidence Score ($S_{\text{conf}}$).

### 2.4 Database & Persistence Layer (`com.example.chargescience.data.db`)
- **`AppDatabase` (Room SQLite with KSP):**
  - Configured with `room.generateKotlin = true` to generate native Kotlin bytecode.
  - `ChargingSessionEntity`: Stores session metadata, duration, power extremes, thermal status transitions, and confidence score.
  - `TelemetrySampleEntity`: Indexed by `sessionId` and `(sessionId, timestamp)` for high-speed time-series scrubbing.

### 2.5 UI Layer (`com.example.chargescience.ui`)
- **Five Primary Destinations:**
  1. `Overview`: Prioritized hierarchy (Power $\to$ SOC% $\to$ Method $\to$ Temp $\to$ Voltage/Current $\to$ Negotiated Ceiling).
  2. `Lab`: Interactive segmented selector (`Power`, `Current`, `Voltage`, `Temp`, `SOC`) with hardware-accelerated Compose Canvas time-series charts.
  3. `History`: List of past sessions with SOC gain, duration, average power, and detailed time-series inspection dialog.
  4. `Charger`: Protocol verification, negotiated limits, and physical boundary explanations.
  5. `Health`: Hardware cycle counts, nominal capacity, and confidence calibration rules.
