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
                      │  (Overview, Lab, Charger, Health)    │
                      └──────────────────▲───────────────────┘
                                         │ StateFlow<UiState>
                                         │
                      ┌──────────────────┴───────────────────┐
                      │          ViewModel Layer             │
                      │  Coroutines + State Invariant Checks │
                      └──────────────────▲───────────────────┘
                                         │ Flow<BatteryTelemetry>
                                         │
                      ┌──────────────────┴───────────────────┐
                      │          Repository Layer            │
                      │  TelemetryRepository + Local Cache   │
                      └──────────▲────────────────▲──────────┘
                                 │                │
            ┌────────────────────┴───┐        ┌───┴────────────────────┐
            │   Diagnostics Engine   │        │     Storage Engine     │
            │  BatteryManager HAL    │        │  Room DB (SQLite)      │
            │  Thermal HAL AIDL      │        │  DataStore Preferences │
            │  Intent Broadcasts     │        │  ChargingSessionDao    │
            └────────────────────────┘        └────────────────────────┘
```

---

## 2. Key Architectural Layers

### 2.1 Diagnostics Engine (`com.example.chargescience.diagnostics`)
- **`BatteryTelemetryCollector`:**
  - Registers sticky broadcast receiver for `Intent.ACTION_BATTERY_CHANGED`.
  - Samples `BatteryManager` properties (`BATTERY_PROPERTY_CURRENT_NOW`, `BATTERY_PROPERTY_CHARGE_COUNTER`, `BATTERY_PROPERTY_CAPACITY`) on an adaptive interval ($1Hz$ while charging, paused or low-rate when unplugged).
  - Listens to Android Thermal HAL via `PowerManager.addThermalStatusListener`.
  - Exposes an asynchronous `Flow<BatteryTelemetry>` with thread safety via `Dispatchers.Default`.

### 2.2 Domain & Data Layer (`com.example.chargescience.data`)
- **`TelemetryRepository`:**
  - Merges live hardware flow with database-backed historical session tracking.
  - Automatically detects charging session boundaries (Plugged in $\to$ Session Start, Unplugged $\to$ Session End with summary calculation).
- **`AppDatabase` (Room SQLite):**
  - Stores `ChargingSessionEntity` (start/end timestamp, starting/ending SoC, energy absorbed in Wh, peak wattage, thermal range, confidence score).
  - Stores `TelemetrySampleEntity` (time-series points for session graph scrubbing).

### 2.3 UI & Presentation Layer (`com.example.chargescience.ui`)
- **Compose Navigation:**
  - Five core routes: `Overview`, `ChargingLab`, `ChargerIntelligence`, `BatteryHealth`, `History`.
- **Material 3 Expressive Componentry:**
  - `HeroWattageCard` with tabular layout formatting.
  - `TelemetryGaugeCard` with clean tonal elevation.
  - Hardware-accelerated Canvas time-series charts.
