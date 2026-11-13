# Charge Science — Settings & Instrumentation Architecture Specification
### Comprehensive Technical Architecture for User Configuration & Diagnostics Controls

---

## 1. Executive Summary & Design Principles

Charge Science is an empirical diagnostic instrument, not an opaque utility application. Every configurable setting must directly map to an observable physical phenomenon (electrochemical kinetics, thermal diffusion, PMIC control loops, or hardware registers).

### Core Architectural Principles
1. **Physical Transparency**: Settings never toggle "placebo algorithms" or "battery booster" modes. Every setting controls real Android hardware APIs, Linux sysfs nodes, sampling loop rates, or analytical confidence thresholds.
2. **Local-First & Zero-Network Guarantee**: Charge Science deliberately omits `android.permission.INTERNET`. All settings and preferences are stored exclusively on-device in Android Jetpack DataStore Preferences and Room SQLite.
3. **Graceful Degradation**: On devices where specific hardware registers (such as raw microampere fuel gauge polling or PPS PDO query tables) are restricted by OEM firmware, settings provide clear fallback transparency rather than simulated data.

---

## 2. Category-by-Category Settings Specification

### Category 1: Telemetry Acquisition & Sampling Engine
Controls the frequency, precision, and power footprint of background hardware polling.

| Setting Identifier | Type | Default | Android API / Hardware Target | Scientific Rationale & Functionality |
| :--- | :--- | :--- | :--- | :--- |
| `telemetry_sample_rate_active_ms` | `Int` (Dropdown: 500ms, 1000ms, 2000ms) | `1000ms` (1.0 Hz) | `BatteryManager.getIntProperty()`, `PowerManager.ThermalStatusListener` | Determines time-series granularity during active charging sessions. 1.0 Hz is the sweet spot for capturing transient thermal throttling steps without measurable CPU overhead. |
| `telemetry_sample_rate_background_ms` | `Int` (Dropdown: 5s, 10s, 30s) | `10000ms` (0.1 Hz) | `ChargingTelemetryService` timer | Polling rate when the screen is off or app is backgrounded. Minimizes power draw to $< 0.15\text{ mA}$ during overnight charging sessions. |
| `session_cpu_wakelock_enabled` | `Boolean` (Switch) | `true` | `PowerManager.newWakeLock(PARTIAL_WAKE_LOCK)` | Prevents Android's Doze governor from freezing the background telemetry service during low-power charging stages, preventing missing telemetry samples. |
| `direct_fuel_gauge_polling` | `Boolean` (Switch) | `true` | `BATTERY_PROPERTY_CURRENT_NOW`, `BATTERY_PROPERTY_CHARGE_COUNTER` | Queries Maxim MAX77759 registers directly in microamps and microvolts instead of relying on `ACTION_BATTERY_CHANGED` broadcasts (which only fire on 1% SOC jumps). |
| `adc_transient_spike_filter` | `Boolean` (Switch) | `true` | `RunningMedianFilter(windowSize = 3)` | Rejects instantaneous fuel gauge ADC bus glitch spikes ($\pm 500\text{ mA}$ single-sample anomalies caused by CPU burst clocks). |

---

### Category 2: Battery Health & Electrochemical Safety
Configures the degradation algorithms, health models, and electrochemical threshold alerts.

| Setting Identifier | Type | Default | Hardware / Domain Target | Scientific Rationale & Functionality |
| :--- | :--- | :--- | :--- | :--- |
| `reference_nominal_capacity_mah` | `Int` (Numeric Input) | `4492 mAh` | `BatteryHealthCalculator` | Reference design capacity for the Pixel 8a (`akita`). Serves as the denominator for State of Health ($\text{SOH} = \frac{\text{FCC}}{\text{Nominal}} \times 100\%$). Custom input allows calibrating replacement batteries. |
| `protective_80_percent_alarm` | `Boolean` (Switch) | `true` | Notification / Ringtone Manager | High-voltage electrolyte oxidation alert. Warns user when battery terminal potential reaches the CV transition knee ($\sim 4.25\text{ V}$ at 80% SOC), preserving cathode structure and cycle longevity. |
| `thermal_throttle_alert_celsius` | `Float` (Slider: 38.0°C – 46.0°C) | `42.0°C` | `BATTERY_PROPERTY_TEMPERATURE` | Warns before severe PMIC thermal throttling is triggered by the Tensor G3 governor. Pixel 8a begins step-down throttling at $\approx 39.5^\circ\text{C}$ and cuts power severely at $43.0^\circ\text{C}$. |
| `min_soc_swing_for_soh_percent` | `Int` (Dropdown: 20%, 30%, 40%, 50%) | `20%` | `BatteryHealthEngine` | Minimum Depth of Discharge ($\Delta\text{SOC}$) required for a session to be admitted into the Full Charge Capacity estimation engine. Discards partial micro-charges which have high statistical error margins. |
| `coulombic_efficiency_factor` | `Float` (Range: 0.95 – 1.00) | `0.992` | `ChargeCounterIntegration` | Accounts for electrochemical parasitic losses ($\eta_c \approx 99.2\%$) during lithium-ion intercalation so coulomb counting does not overestimate stored chemical energy. |

---

### Category 3: Wireless Charging & Magnetic Ring Diagnostics
Dedicated instrumentation for diagnosing inductive coil alignment, eddy current heating, and Qi Foreign Object Detection (FOD) shutdowns.

| Setting Identifier | Type | Default | Diagnostic Target | Scientific Rationale & Functionality |
| :--- | :--- | :--- | :--- | :--- |
| `magnetic_ring_thermal_watchdog` | `Boolean` (Switch) | `true` | $\frac{dT}{dt}$ derivative heating monitor | Detects parasitic eddy currents generated when an aftermarket magnetic ring is misaligned or absorbs the $110 - 205\text{ kHz}$ magnetic field. Alerts if heating rate exceeds $0.80^\circ\text{C/min}$. |
| `qi_fod_sensitivity_mode` | `Enum` (`STANDARD`, `STRICT`) | `STANDARD` | Dropout gradient analyzer | Distinguishes between thermal PMIC throttling (gradual taper) and Qi transmitter Foreign Object Detection shutdown (abrupt $0\text{ mA}$ cliff within $< 2\text{ seconds}$). |
| `qi_epp_renegotiation_logging` | `Boolean` (Switch) | `true` | `BatteryManager.BATTERY_PROPERTY_STATUS` | Flags when the Qi pad silently down-negotiates power from 7.5W EPP to 5.0W Baseline Power Profile (BPP) due to receiver-coil coupling inefficiency. |
| `coil_alignment_acoustic_guidance` | `Boolean` (Switch) | `false` | Real-time audio pitch synthesizer | Generates a subtle variable audio frequency proportional to live cell current ($mA$). Allows blind placement of the phone on a wireless charging pad to maximize magnetic alignment. |

---

### Category 4: Charger, Cable & USB Power Delivery Intelligence
Configures external power supply classification and contract tracking.

| Setting Identifier | Type | Default | Android Subsystem Target | Scientific Rationale & Functionality |
| :--- | :--- | :--- | :--- | :--- |
| `auto_link_known_accessories` | `Boolean` (Switch) | `true` | `AccessoryProfileDao` | Automatically detects when negotiated voltage/current matches a saved charger profile (e.g., Google 30W USB-C PD 3.0 PPS) and links historical sessions to that accessory. |
| `pps_voltage_step_tracker` | `Boolean` (Switch) | `true` | `UsbPortStatus` / PMIC telemetry | Tracks continuous micro-adjustments in negotiated bus voltage ($20\text{ mV}$ increments) to verify true USB Power Delivery Programmable Power Supply (PPS) operation. |
| `cable_resistance_drop_test_prompt` | `Boolean` (Switch) | `true` | `CableDropCalculator` | Prompts user to run a controlled cable drop diagnostic if observed bus current is capped below $2.0\text{ A}$ while cell temperature is cool ($< 32^\circ\text{C}$). |

---

### Category 5: Interface, Typography & Measurement Units
Ensures the UI strictly complies with Material 3 Expressive and laboratory instrument standards.

| Setting Identifier | Type | Default | Compose Theme Target | Scientific Rationale & Functionality |
| :--- | :--- | :--- | :--- | :--- |
| `use_tabular_figures_tnum` | `Boolean` (Switch) | `true` | OpenType font feature `tnum, zero` | Uses fixed-width figures and slashed zeros across all numerical displays. Prevents jittery horizontal layout shifts during 1 Hz live telemetry streaming. |
| `current_unit_format` | `Enum` (`MILLIAMPERES`, `AMPERES`) | `MILLIAMPERES` | Numerical formatters | Displays current as integer $mA$ (e.g., $+2{,}350\text{ mA}$) or engineering decimal $A$ (e.g., $+2.35\text{ A}$). |
| `temperature_unit` | `Enum` (`CELSIUS`, `FAHRENHEIT`) | `CELSIUS` | Thermal formatters | Displays thermistor readings in Celsius ($^\circ\text{C}$) or Fahrenheit ($^\circ\text{F}$). Defaults to SI units ($^\circ\text{C}$) for scientific accuracy. |
| `progressive_disclosure_default_expanded` | `Boolean` (Switch) | `false` | `SegmentedTile` default state | Controls whether diagnostic cards open in expanded mode by default or remain compact until explicitly tapped. |

---

### Category 6: Data Persistence, Export & Privacy
Manages diagnostic databases, scientific export formats, and local privacy guarantees.

| Setting Identifier | Type | Default | Storage Subsystem Target | Scientific Rationale & Functionality |
| :--- | :--- | :--- | :--- | :--- |
| `raw_sample_retention_days` | `Int` (Dropdown: 7, 14, 30, 90, Unlimited) | `30 days` | Room SQLite DB Maintenance | Automatically prunes high-frequency 1 Hz raw sample rows older than the specified threshold while permanently retaining high-level session summary metrics. |
| `export_timestamp_format` | `Enum` (`ISO_8601_UTC`, `LOCAL_UNIX_MS`) | `ISO_8601_UTC` | `DataExportManager` | Formats CSV and JSON export timestamps for compatibility with statistical processing tools (Python pandas, R, MATLAB). |
| `purge_diagnostic_database` | `Action` (Modal Confirmation) | N/A | `AppDatabase.clearAllTables()` | Completely purges all Room tables (sessions, samples, accessories, wireless experiments, health estimates) and resets fuel gauge baselines. |

---

## 3. Implementation Blueprint & DataStore Architecture

All settings will be backed by **Jetpack DataStore Preferences** (`androidx.datastore.preferences.core`) wrapped in a clean, reactive Kotlin repository:

```kotlin
// Domain Model for Settings State
data class DiagnosticsPreferences(
    val highFrequencySampling: Boolean = true,
    val sessionWakeLock: Boolean = true,
    val directFuelGaugePolling: Boolean = true,
    val referenceCapacityMah: Int = 4492,
    val protect80Alarm: Boolean = true,
    val thermalAlarmCelsius: Float = 42.0f,
    val minSocSwingPercent: Int = 20,
    val magneticRingWatchdog: Boolean = true,
    val fodSensitivityStrict: Boolean = false,
    val useTabularFigures: Boolean = true,
    val unitAmperes: Boolean = false,
    val temperatureFahrenheit: Boolean = false,
    val rawSampleRetentionDays: Int = 30
)
```

This guarantees:
1. Thread-safe, non-blocking asynchronous reads via Kotlin `StateFlow`.
2. Safe ACID writes on background IO dispatchers.
3. Zero dependency on deprecated `SharedPreferences`.
