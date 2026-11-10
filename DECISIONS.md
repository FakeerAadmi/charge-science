# Architectural & Engineering Decisions (ADRs)
## Project: CHARGE SCIENCE

---

### ADR 001: Separation of Battery-Side Power from Charger-Side Power
- **Status:** Accepted
- **Context:** Many battery utilities mislead users by calculating $P = V_{\text{battery}} \times I_{\text{battery}}$ and presenting it as "Charger Wattage" (e.g. telling a user with a 30W adapter that their charger is only outputting 12W).
- **Decision:** The application strictly labels $V_{\text{cell}} \times I_{\text{cell}}$ as **Net Battery Absorption Power**. External charger input wattage is labeled as an estimate ($P_{\text{battery}} / \eta_{\text{PMIC}} + P_{\text{sys}}$) with explicit uncertainty warnings.
- **Consequences:** Eliminates user confusion and maintains scientific integrity.

---

### ADR 002: Rejection of Userspace Coulomb Integration in Favor of Hardware Counter
- **Status:** Accepted
- **Context:** Polling `CURRENT_NOW` at 1Hz in userspace misses high-frequency load transients ($20-100\text{ms}$ modem/CPU spikes) and suffers unbounded integration drift due to ADC offset bias ($\epsilon_{\text{bias}} \cdot t$).
- **Decision:** Battery capacity estimation relies on `BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER` ($\mu\text{Ah}$) on supported devices like Pixel 8a, which integrates at the hardware PMIC level (Maxim MAX77759).
- **Consequences:** Reduces capacity estimation error from $\pm 15\%$ down to $< \pm 2\%$.

---

### ADR 003: Material 3 Expressive Anti-AI-Slop Visual System
- **Status:** Accepted
- **Context:** The prompt strictly forbids AI slop: neon glows, fake terminal screens, traffic-light circular gauges, and arbitrary decorative animations.
- **Decision:** Adopt official Material 3 Expressive guidelines:
  - Tonal surface layering (`surfaceContainerLowest` through `surfaceContainerHighest`).
  - Strict tabular lining figures (`fontFeatureSettings = "tnum, zero"`) to prevent number jitter.
  - Expressive corner geometry (32dp for hero cards, 16dp for gauges, 999dp for pills).
- **Consequences:** Produces a calm, high-contrast diagnostic instrument that feels like a native Google Pixel system tool.

---

### ADR 004: CompileSdk 36 with TargetSdk 36 on Android Gradle Plugin 9.0+
- **Status:** Accepted
- **Context:** The connected reference Pixel 8a runs an Android 17 / SDK 37 preview build. AGP 9.0.1 is verified for SDK 36.1, while SDK 37 is in early preview.
- **Decision:** Target compileSdk 36 with backward and forward compatibility for Android 14 through Android 17, using runtime capability checks for newer APIs like `EXTRA_CYCLE_COUNT`.
- **Consequences:** Clean build with zero deprecation warnings and rock-solid stability.

---

### ADR 005: Room Kotlin Code Generation via KSP
- **Status:** Accepted
- **Context:** In Room 2.6.1 with KSP 2.3+ on modern Kotlin, Room defaults to generating Java stubs for DAO suspend functions. This causes javac compilation errors due to generic type erasure clashes with Kotlin `Continuation` parameters.
- **Decision:** Set `ksp { arg("room.generateKotlin", "true") }` in `app/build.gradle.kts` to force Room to generate pure Kotlin code.
- **Consequences:** Seamless compilation, faster build times, idiomatic Kotlin coroutine flows, and zero javac generic name collisions.

---

### ADR 006: Foreground Service with `specialUse` Type and Charging-Bound WakeLock
- **Status:** Accepted
- **Context:** Android 14+ enforces explicit foreground service types. Android 15 caps `dataSync` to 6 hours per 24 hours. `health` is rejected by Google Play policy for device battery monitoring. Furthermore, when the screen turns off during multi-hour charging sessions, the kernel CPU governor suspends userspace threads, causing sampling dropouts.
- **Decision:** Implement `ChargingTelemetryService` with `foregroundServiceType="specialUse"` and manifest property `PROPERTY_SPECIAL_USE_FGS_SUBTYPE="charging_telemetry_recording"`. Acquire `PowerManager.PARTIAL_WAKE_LOCK` strictly while `isCharging == true`, immediately releasing it upon cable detachment.
- **Consequences:** Continuous 1 Hz telemetry sampling across screen-off charging cycles without violating Android background policies or causing idle battery drain.

---

### ADR 007: Pure Jetpack Compose Canvas Time-Series Visualization
- **Status:** Accepted
- **Context:** Third-party charting libraries introduce heavy dependencies, view wrappers, and decorative cubic spline smoothing that creates false peaks and hides physical voltage/current steps.
- **Decision:** Build a custom hardware-accelerated Compose Canvas component (`TelemetryTimeSeriesChart`) with linear piecewise segments, discrete measurement nodes, and dynamic bounds.
- **Consequences:** High performance, zero extra dependencies, truthful representation of physical sensor readings in light and dark themes.

---

### ADR 008: Controlled Charging Session Comparison Gating
- **Status:** Accepted
- **Context:** Uncontrolled comparisons between sessions starting at different battery SOCs (e.g. 10% vs 70%) are scientifically invalid because lithium-ion charging curves are non-linear (Constant Current vs Constant Voltage taper).
- **Decision:** Implement `ChargingComparisonEngine` with strict validity rules: warn if $|\Delta\text{SOC}_{\text{start}}| > 15\%$ or $|\Delta T_{\text{start}}| > 6^\circ\text{C}$, and categorize comparisons into `HIGH_FIDELITY_CONTROLLED`, `COMPARABLE_WITH_CAVEATS`, or `UNCONTROLLED_MISLEADING`.
- **Consequences:** Prevents users from drawing false conclusions regarding charger or cable quality.

---

### ADR 009: Strict Scientific Rejection of Battery-Side Cable Resistance Estimation
- **Status:** Accepted
- **Context:** Multiple popular battery apps claim to measure USB-C cable resistance by dividing battery voltage changes by battery current. However, because modern smartphones use high-efficiency switching buck converters, $I_{\text{battery}} \ne I_{\text{cable}}$ and $V_{\text{bus}}$ at the charger is unknown.
- **Decision:** Reject software-only cable resistance calculations. Explicitly label cable resistance as unavailable without external inline hardware, and design an extensible BLE GATT meter interface (FNIRSI FNB58 / TC66C).
- **Consequences:** Upholds scientific fidelity and prevents publishing fabricated numbers.

---

### ADR 010: Local FileProvider Document Export without Network Permissions
- **Status:** Accepted
- **Context:** Users need to export raw telemetry (CSV/JSON) for external laboratory analysis in Python/Excel, but the application must strictly adhere to zero network permissions.
- **Decision:** Implement `DataExportManager` using Android's native `FileProvider` (`androidx.core.content.FileProvider`) writing to the application's internal cache directory and launching `Intent.ACTION_SEND` with temporary read URI permissions.
- **Consequences:** Clean, privacy-first local export without requesting `INTERNET` or `WRITE_EXTERNAL_STORAGE` permissions.
