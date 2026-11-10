# Charge Science ⚡🔬
### Android Charging Diagnostics & Battery Intelligence

Charge Science is a native Android application designed for Google Pixel devices (reference platform: **Pixel 8a** on Android 15/17) that provides real-time, scientifically rigorous diagnostic instrumentation of wired and wireless charging, thermal throttling dynamics, and battery electrochemical state of health.

Unlike consumer battery utilities that display speculative calculations, animated traffic-light gauges, or artificial battery scores, Charge Science maintains strict fidelity to physical measurements:
- Clearly distinguishing **battery chemical absorption power** from **charger-side bus power**.
- Capturing high-resolution current and voltage telemetry via hardware fuel gauges (Maxim MAX77759).
- Observing USB Power Delivery (USB PD) and Programmable Power Supply (PPS) profiles.
- Detecting thermal throttling boundaries in real time via Android Thermal HAL listeners.
- Computing battery health and full charge capacity estimates bounded by statistical confidence scoring.

---

## Key Capabilities

1. **Live Diagnostics Dashboard (Overview)**
   - Instantaneous net battery wattage ($P_{cell} = V_{cell} \times I_{cell}$) with tabular, non-jittering typography (`tnum`).
   - Charging profile detection: Negotiated max voltage, max current, and power ceiling.
   - Real-time battery temperature, millivolt terminal voltage, and milliampere current flow.
   - Charging state and connection type (USB-PD, Standard AC, USB, Qi Wireless).

2. **Charging Lab**
   - High-frequency time-series curves for power ($W$), voltage ($V$), and current ($A$).
   - Thermal correlation: Battery temperature vs. charge current reduction.
   - Throttling detection: Real-time alerting when PMIC or thermal governor caps current.

3. **Charger & Cable Intelligence**
   - Negotiated USB Power Delivery contract verification (e.g., 9.0V @ 2.2A, 19.8W).
   - Detection of PPS capability vs. fixed voltage PDO steps.
   - Transparent distinction between directly measured telemetry, derived metrics, and external hardware requirements.

4. **Battery Health Laboratory**
   - Equivalent Full Cycle (EFC) tracking.
   - Full Charge Capacity (FCC) tracking against Pixel 8a nominal design capacity (4,492 mAh).
   - Statistical confidence scoring ($S_{conf}$) requiring depth-of-discharge swing, thermal stability, and constant-voltage saturation.

5. **Charging History & Export**
   - Offline, local-first SQLite/Room storage of charge sessions.
   - Thermodynamic extremes, energy absorbed ($Wh$), and charging efficiency metrics.

---

## Design System

Built strictly with **Material 3 Expressive**:
- Dynamic Color (Material You) with high-contrast OLED dark theme optimized for diagnostic instrumentation.
- Tonal elevation hierarchy (`surfaceContainerLowest` through `surfaceContainerHighest`) eliminating faux-3D drop shadows.
- Expressive corner geometry (32dp hero containers, 16dp gauge cards, 999dp segmented controls).
- Tabular lining figures (`tnum, zero`) preventing horizontal layout shift during 2–5Hz telemetry updates.
- Strictly anti-AI-slop: zero neon halos, zero fake terminal skins, zero arbitrary circular progress meters.

---

## Project Structure

```
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/example/chargescience/
│   │   │   ├── data/             # Room DB, DataStore, Telemetry Repository
│   │   │   ├── diagnostics/      # Hardware Telemetry Collector & Broadcaster
│   │   │   ├── model/            # Battery, Charging & Thermal Domain Models
│   │   │   ├── ui/               # Jetpack Compose UI (Overview, Lab, Health, Charger, History)
│   │   │   │   ├── components/   # Material 3 Expressive reusable widgets
│   │   │   │   └── theme/        # M3 Expressive Theme, Typography & Dynamic Colors
│   │   │   └── MainActivity.kt
│   │   └── AndroidManifest.xml
├── docs/                         # Specifications & Engineering Records
│   ├── PRODUCT_REQUIREMENTS.md
│   ├── TECHNICAL_ARCHITECTURE.md
│   ├── TECHNICAL_FEASIBILITY.md
│   ├── BATTERY_SCIENCE.md
│   ├── DESIGN_SYSTEM.md
│   ├── DATA_MODEL.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── TESTING_STRATEGY.md
│   └── DECISIONS.md
```

---

## License
Proprietary & Confidential. Built for Google Pixel 8a Diagnostics & Battery Intelligence.
