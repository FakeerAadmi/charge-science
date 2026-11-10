# Design System Specification: Material 3 Expressive
## Project: CHARGE SCIENCE
**Target Device Reference:** Google Pixel 8a (6.1" Actua OLED 1080x2400 120Hz)

---

## 1. Anti-AI-Slop Instrumentation Philosophy

Consumer battery applications commonly resort to "AI slop" or pseudoscientific visuals:
- **Prohibited:**
  - Radial neon glows, pulsing colors, and cyan-to-purple gaming aesthetics.
  - Skewed analog gauge needles and traffic-light circular rings (green/orange/red) that induce false alarm.
  - Fake terminal monospace screens that degrade touch ergonomics.
  - Proportional fonts whose digits jitter horizontally during 2–5Hz data updates.
- **Charge Science Principles:**
  - **Quiet Precision:** Data is structured in subtle tonal containers (`surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`) rather than heavy drop shadows.
  - **Dynamic Adaptation:** Leverages Google's Material You dynamic tonal palettes extracted from the user's Pixel wallpaper.
  - **Tabular Rigor:** Live telemetry strictly uses tabular lining figures (`fontFeatureSettings = "tnum, zero"`).
  - **Strict Semantic Color Assignment:**
    - **Wattage & Power:** `primary` / `primaryContainer`
    - **Electrical Telemetry ($V, I$):** `secondary` / `secondaryContainer`
    - **Thermal State:** `tertiary` / `tertiaryContainer`
    - **Hardware Faults:** `error` / `errorContainer` (strictly reserved for $T > 45^\circ\text{C}$, over-voltage, or port anomalies—never for normal low battery).

---

## 2. Typography Scale

Configured in `com.example.chargescience.ui.theme.Type.kt`:

| Style Role | Font Weight | Size / LineHeight | Feature Settings | Primary Usage |
|---|---|---|---|---|
| `displayLarge` | Bold (700) | 57sp / 64sp | `tnum, zero` | Hero Wattage reading (`18.4`) |
| `headlineLarge` | SemiBold (600) | 32sp / 40sp | `tnum, zero` | Primary gauge numerals (`3,840`, `2,200`) |
| `headlineMedium` | Medium (500) | 28sp / 36sp | normal | Units adjacent to hero (`W`) |
| `titleMedium` | SemiBold (600) | 16sp / 24sp | `tnum` | Secondary metrics & list card titles |
| `labelSmall` | Bold (700) | 11sp / 16sp | letterSpacing = 1.2sp | Technical tags (`USB-PD 9V`, `PPS ACTIVE`) |
| `bodyMedium` | Normal (400) | 14sp / 20sp | normal | Descriptive explanations & diagnostic tips |

---

## 3. Expressive Geometry Hierarchy

- **Extra Large (`32dp`):** Hero Wattage Container, bottom sheet modal surfaces.
- **Large (`24dp`):** Primary telemetry cards, interactive graph viewports.
- **Medium (`16dp`):** Secondary nested gauge cards (Voltage, Current, Temp, Level).
- **Small (`8dp`):** Micro-chips, table cells, protocol tag pills.
- **Full Pill (`999dp`):** Segmented choice buttons (`Power`, `Current`, `Voltage`, `Temp`, `SOC`), status indicators.

---

## 4. Time-Series Visualization & Canvas Guidelines

Implemented in `com.example.chargescience.ui.components.TelemetryTimeSeriesChart.kt`:

1. **Native Hardware-Accelerated Compose Canvas:**
   - Zero heavyweight charting libraries with slow reflection or complex view wrappers.
   - Drawn directly via `Canvas(modifier = ...)` with `drawPoints` and `drawPath`.

2. **Honest Scientific Rendering:**
   - **No Spline/Bezier Over-Smoothing:** Over-smoothed cubic splines create artificial peaks and valleys that do not exist in physical voltage or current steps. Data is rendered using accurate linear segments connecting raw timestamps.
   - **Discrete Observation Highlights:** Individual measurement points are visible along the polyline path with distinct stroke caps.
   - **Explicit Gridlines & Scales:** Automatic horizontal guide rules with dynamic min/max bounds and unit labels formatted with tabular figures.
   - **Empty & Single-Point States:** Clear, elegant placeholder state with prompt to connect charger when fewer than 2 samples exist.

---

## 5. Overview Screen Information Hierarchy

Organized strictly according to diagnostic priority:
1. **Hero Power Container (`surfaceContainerHigh`, `32dp` corners):**
   - Live instantaneous battery power ($W$).
   - Live status badge (Charging / Discharging / Full).
2. **State of Charge & Progress Pill:**
   - Battery level percentage ($0 - 100\%$) and charging source classification (`AC (USB-PD)`, `USB`, `Wireless Qi`).
3. **Primary Electrical Dual-Card ($V, I$):**
   - Left: Terminal Voltage ($mV$).
   - Right: Current ($mA$) with directional sign ($+$ charge, $-$ discharge).
4. **Thermodynamic & Throttle Card:**
   - Battery Temperature ($^\circ\text{C}$).
   - Android Thermal Status enum (None, Light, Moderate, Severe).
5. **Negotiated Protocol Ceiling:**
   - Max negotiated profile limits ($V_{\text{max}} \times I_{\text{max}} = P_{\text{negotiated}}$).
   - Real-time comparison against actual battery intake.

---

## 6. Navigation Architecture

Employs Android's **NavigationSuiteScaffold**:
- **Compact Portrait (Pixel 8a normal):** Bottom Navigation Bar.
- **Expanded / Landscape:** Left-hand Navigation Rail.
- **Five Primary Destinations:**
  1. `Overview` (`Icons.Outlined.Bolt`): Live charging dashboard & hero power.
  2. `Lab` (`Icons.Outlined.ShowChart`): Real-time time-series curves and thermodynamic loss analyzer.
  3. `Charger` (`Icons.Outlined.Usb`): Negotiated power contracts and cable diagnostics.
  4. `Health` (`Icons.Outlined.BatteryChargingFull`): Cycle count, ModelGauge m5 capacity analysis, calibration gating.
  5. `History` (`Icons.Outlined.History`): Recorded charging sessions list & full detail inspection dialog.
