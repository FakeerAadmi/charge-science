# Development Roadmap
## Project: CHARGE SCIENCE

---

### Phase 1: Discovery & Telemetry Feasibility (Completed)
- [x] Connect to physical Google Pixel 8a via Wireless Debugging (Android 17 / API 37 Preview).
- [x] Inspect hardware sysfs nodes (`/sys/class/power_supply/battery`, `maxfg`, `usb`, `tcpm`).
- [x] Determine exact API contracts, units, and limitations for `BatteryManager` and `ACTION_BATTERY_CHANGED`.
- [x] Establish Git repository and remote private GitHub repo (`FakeerAadmi/charge-science`).
- [x] Formulate technical feasibility, battery science, and Material 3 Expressive specifications.

### Phase 2: Session Intelligence, Historical Telemetry & Data Visualization (Completed)
- [x] Room 2.6.1 SQLite database with KSP 2.3+ Kotlin code generation (`ChargingSessionEntity`, `TelemetrySampleEntity`).
- [x] Charging Session State Machine (`ChargingSessionManager`) handling start, stop, source transitions, and orphan crash/reboot recovery.
- [x] Trapezoidal numerical power integration ($\bar{P} = \frac{1}{\Delta t} \int P(t) dt$) to prevent non-uniform sampling skew.
- [x] Session Confidence Scoring ($S_{\text{conf}}$) evaluating $\Delta\text{SOC}$, thermal stability, CV taper, and sample continuity.
- [x] Foreground Service (`ChargingTelemetryService`) using Android 14/15/17 compliant `specialUse` type and `PARTIAL_WAKE_LOCK` held strictly while actively charging.
- [x] Pure Compose Canvas time-series chart (`TelemetryTimeSeriesChart`) rendering raw linear segments, discrete points, and dynamic bounds.
- [x] Charging Lab screen with multi-metric graph selection ($W, mA, mV, ^\circ\text{C}, \text{SOC}\%$), thermodynamic PMIC loss breakdown, and thermal throttling status.
- [x] History screen displaying past charging sessions with SOC gain, duration, average power, confidence badge, and historical graph dialog.
- [x] Comprehensive JUnit 4 test suite (13 unit tests) covering electrochemical calculators and state machine edge cases.
- [x] Real-device installation and end-to-end charging session verification on Google Pixel 8a.

### Phase 3: Charger & Cable Intelligence (Next Phase)
- [ ] Implement USB-PD PDO contract inference engine and PPS variable voltage detection.
- [ ] Implement empirical cable resistance estimation ($\Delta V / \Delta I$ across step load changes).
- [ ] Wireless charging efficiency analyzer comparing Qi inductive power input vs battery chemical intake.
- [ ] ModelGauge m5 deep battery health calibration engine (gated on $S_{\text{conf}} \ge 80\%$ and $\Delta\text{SOC} \ge 70\%$).
- [ ] Export session telemetry to CSV and JSON formats for external laboratory analysis.
