# Testing Strategy & Verification
## Project: CHARGE SCIENCE

---

## 1. Physical Device Verification (Google Pixel 8a)

Physical verification is paramount because battery and charging telemetry depends on the device PMIC, kernel drivers, and hardware thermistors.

### 1.1 Direct Telemetry Invariance Tests
Every metric displayed in the UI must be verified against concurrent ADB dumpsys readings:
- **Voltage:** UI millivolts must match `adb shell dumpsys battery | grep voltage` within $\pm 10mV$.
- **Current:** UI milliamperes must match `adb shell cmd battery get current_now` ($\mu A / 1000$).
- **Temperature:** UI temperature must match `adb shell dumpsys battery | grep temperature` ($0.1^\circ C$).
- **Cycle Count:** UI cycle count must match `adb shell cat /sys/class/power_supply/maxfg/cycle_count` (or `EXTRA_CYCLE_COUNT`).
- **Negotiated Profile:** Advertised maximum voltage/current in UI must match `mPortSinkPowerProfileMatches` in `adb shell dumpsys usb`.

### 1.2 Sign Convention Assertions
- When plugged in and charging: `BATTERY_PROPERTY_CURRENT_NOW > 0`.
- When unplugged: `BATTERY_PROPERTY_CURRENT_NOW < 0` (or 0 during deep idle).
- Net Wattage must never display a negative sign when charging.

---

## 2. Unit Testing Strategy

- **`PowerCalculationTest`:** Verify $P = V \times I$ with correct units ($mV \to V$, $\mu A \to A$). Verify bounds checking and NaN/overflow guards.
- **`ProtocolInferenceTest`:** Test USB-PD classification across edge inputs ($5V \to \text{Standard}$, $9V \to \text{USB-PD}$, $12V \to \text{USB-PD}$).
- **`ConfidenceScoreTest`:** Verify $S_{\text{conf}}$ weighting algorithm returns $<65\%$ for shallow charges ($\Delta SOC < 20\%$) and $\ge 85\%$ for complete CC-CV cycles.
- **`ThermalThrottlingClassificationTest`:** Ensure transitions between `THERMAL_STATUS_NONE` through `THERMAL_STATUS_SEVERE` trigger appropriate diagnostic state flags.
