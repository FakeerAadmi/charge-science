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

### Phase 3: Accessory Intelligence, Charging Protocols & Data Export (Completed)
- [x] Detailed protocol classification model (`ChargingProtocolClassifier`) distinguishing USB-PD Fixed 9V, PPS candidate states, Type-C Rp current modes, USB BC 1.2, and Qi Baseline/EPP.
- [x] Strict scientific honesty regarding cable resistance and e-marker: documented why battery-side estimation is mathematically impossible without external inline power meters.
- [x] Wireless charging intelligence: characterized Pixel 8a 7.5W Qi ceiling, Qi BPP vs EPP, and thermal load coefficient ($\Theta$).
- [x] Accessory profile database (`AccessoryProfileEntity` in Room Version 2) supporting user nicknames, claimed wattage, and session association.
- [x] Controlled Session Comparison Lab (`ChargingComparisonEngine` & `ChargingComparisonDialog`) enforcing starting SOC matching ($|\Delta\text{SOC}| \le 15\%$) and thermal baseline controls.
- [x] Local data export engine (`DataExportManager`) with RFC 4180 CSV export for samples/sessions and complete structured JSON export via Android native `FileProvider` and `Intent.ACTION_SEND`.
- [x] Expanded test suite: 24 passing unit tests covering protocol classification, comparison validity rules, CSV escaping, and JSON generation.
- [x] Verified live on physical Google Pixel 8a.

### Phase 4: External Hardware Meter Integration & Deep Health Calibration (Next Phase)
- [ ] Implement Bluetooth Low Energy (BLE) GATT client for FNIRSI FNB58 and TC66C inline power meters.
- [ ] Real-time external Vbus, Ibus, and Kelvin cable resistance display when BLE meter is connected.
- [ ] E-Marker decoder for 5A / 240W EPR cable identification.
- [ ] Multi-cycle ModelGauge m5 electrochemical aging and capacity fade laboratory curves.
