# Development Roadmap
## Project: CHARGE SCIENCE

---

### Phase 1: Discovery & Telemetry Feasibility (Completed)
- [x] Connect to physical Google Pixel 8a via Wireless Debugging (Android 15/17 Preview).
- [x] Inspect hardware sysfs nodes (`/sys/class/power_supply/battery`, `maxfg`, `usb`, `tcpm`).
- [x] Determine exact API contracts, units, and limitations for `BatteryManager` and `ACTION_BATTERY_CHANGED`.
- [x] Establish Git repository and remote private GitHub repo (`FakeerAadmi/charge-science`).
- [x] Formulate technical feasibility, battery science, and Material 3 Expressive specifications.

### Phase 2: Foundation & First Working Build (Current Phase)
- [x] Scaffold modern Android project with Gradle Kotlin DSL, Jetpack Compose, AGP 9.0+, compileSdk 36.
- [ ] Implement `BatteryTelemetryCollector` reading real-time voltage, current, temperature, wattage, and cycle count.
- [ ] Build Material 3 Expressive UI theme with dynamic color, tabular typography, and expressive container geometry.
- [ ] Implement Overview screen with Hero Wattage Card and quad electrical telemetry.
- [ ] Build, install debug APK on physical Pixel 8a, and verify live readings match ADB diagnostics.

### Phase 3: Charging Lab & Real-Time Curves
- [ ] Implement Compose hardware-accelerated Canvas time-series graphing ($W$, $V$, $A$, $T$).
- [ ] Implement Thermal Throttling detector correlating `PowerManager.OnThermalStatusChangedListener` with current taper.
- [ ] Differentiate Constant Current (CC), Constant Voltage (CV), and saturation phases.

### Phase 4: Charger & Cable Intelligence
- [ ] Implement USB-PD PDO contract inference engine.
- [ ] Detect PPS variable voltage contracts vs. fixed 5V/9V voltage profiles.
- [ ] Implement empirical cable resistance estimation ($\Delta V / \Delta I$).

### Phase 5: Battery Health Laboratory & Room Database
- [ ] Implement Room database for `ChargingSessionEntity` and `TelemetrySampleEntity`.
- [ ] Implement Session Confidence Score calculation ($S_{\text{conf}}$) to reject partial or noisy charging events.
- [ ] Implement Full Charge Capacity (FCC) tracking and electrochemical aging model.
- [ ] Local session history viewer and raw CSV/JSON export.
