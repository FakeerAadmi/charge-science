# Product Requirements Document (PRD)
## Project: CHARGE SCIENCE
**Target Device Reference:** Google Pixel 8a (Tensor G3, 4,492 mAh nominal battery, Maxim MAX77759 PMIC)  
**Operating System:** Android 14+ (tested on Android 15 / 17 Preview)

---

## 1. Product Vision & Principles

Charge Science is a precision charging diagnostics instrument, battery health laboratory, and historical telemetry analyser built natively for Android.

### Core Principles
1. **Physical & Scientific Fidelity:** Display only true physical measurements or mathematically bounded derivations. Clearly distinguish battery-side chemical absorption power from external charger power.
2. **Anti-AI-Slop:** Reject neon glows, pseudo-terminal skins, arbitrary circular gauges, and green/yellow/red traffic light gamification. Use subtle Material 3 tonal elevation and tabular lining figures.
3. **Transparent Uncertainty:** Label every estimate with confidence scoring and error bounds. Never claim to identify cables or chargers without factual data.
4. **Offline-First & Zero-Telemetry:** All metrics remain local on the device in Room SQLite storage. No analytics, no account, no network access required.

---

## 2. User Persona & Questions to Answer

The application serves engineers, power users, and everyday Pixel owners who want to understand their hardware:
- *Is my phone charging at its optimum speed?*
- *How many watts is it currently absorbing into the battery cell?*
- *What voltage and current are being negotiated by the charger?*
- *Is my charger or cable limiting charging speed?*
- *Why did charging slow down from 18W to 6W? (Thermal throttling vs. CC-CV saturation taper)*
- *How much battery capacity has degraded over time?*
- *How many equivalent charge cycles has the cell experienced?*
- *Is wireless charging significantly less efficient than wired charging?*

---

## 3. Detailed Functional Modules

### A. Overview Dashboard
- **Hero Wattage Indicator:** Net instantaneous power absorbed by battery ($P_{cell} = V_{cell} \times I_{cell}$). Rendered in DisplayLarge tabular figures (`tnum`).
- **Negotiated Charging Profile:** Advertised max voltage, max current, and power ceiling (e.g., USB-PD 9V @ 2.2A, 19.8W).
- **Quad Telemetry Grid:**
  - Battery Cell Voltage ($mV$)
  - Battery Net Current ($mA$)
  - Battery Temperature ($^\circ C$)
  - Battery State of Charge ($SOC\%$)
- **Connection Telemetry:** Source type (AC, USB, Wireless Qi, Dock), plug duration, and charging state.

### B. Charging Lab
- **Real-Time Curves:** Hardware-accelerated Compose Canvas plotting Wattage, Voltage, Current, and Temperature at 1–2Hz.
- **Throttling Diagnostics:** Automatic detection of thermal throttling:
  - Distinguishes CC-to-CV taper ($V_{cell} \ge 4.35V$) from Thermal Throttling ($T_{batt} \ge 39^\circ C$, `THERMAL_STATUS_MODERATE` or `SEVERE`).
- **Phase Breakdown:** Instantaneous reporting of Constant Current (CC) vs. Constant Voltage (CV) vs. Trickle/Top-off phase.

### C. Charger & Cable Intelligence
- **Protocol Decoding:**
  - Identifies USB Power Delivery (USB-PD Fixed or PPS) when $V_{max} > 5V$.
  - Distinguishes Standard Downstream Port (SDP, 500mA), Dedicated Charging Port (DCP, 1.5A–2.4A), and Type-C 3.0A.
- **Transparent Boundaries:**
  - Clearly informs users that USB e-marker chips and charger manufacturer IDs require USB Host enumeration or privileged system access (`MANAGE_USB`).
  - Estimates cable resistance ($m\Omega$) when sudden current steps produce observable $\Delta V$.

### D. Battery Health Laboratory
- **Cycle Counter:** Hardware fuel gauge cycle count read directly from `EXTRA_CYCLE_COUNT` (API 34+).
- **State of Health (SOH):** Estimated Full Charge Capacity (FCC) compared to factory nominal design (4,492 mAh).
- **Session Confidence Gating ($S_{conf}$):** Only updates health estimates if:
  - $\Delta SOC \ge 70\%$ (deep charge).
  - Thermal stability maintained ($T_{max} - T_{min} \le 12^\circ C$).
  - Full saturation taper reached ($I_{cell} \le C/40$).

### E. Charging History & Diagnostics
- **Local Session Logging:** Start/end timestamps, energy delivered ($Wh$), peak power ($W$), temperature extremes, and confidence rating.
- **Diagnostics Engine:** Explanations of charging bottlenecks, extreme heat warnings, and abnormal drain events.
