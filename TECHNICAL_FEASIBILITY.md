# Technical Feasibility & Telemetry Specification
## Google Pixel 8a Reference Hardware (Android 14, 15 & 17 Preview)

This document classifies every charging, electrical, and battery metric into four strict scientific categories:
1. **DIRECTLY MEASURABLE:** Available from a reliable, verified system source.
2. **DERIVED:** Calculated from direct measurements using deterministic physical laws.
3. **ESTIMATED:** Inferred from available measurements using statistical models, with documented uncertainty.
4. **UNAVAILABLE:** Cannot be obtained on unrooted devices without specialized external test equipment.

---

## 1. Metric Classification Matrix

| Telemetry Field | Classification | System Source / API | Unit & Range | Sampling Rate | Known Constraints & Physics |
|---|---|---|---|---|---|
| **Cell Terminal Voltage** | DIRECTLY MEASURABLE | `BatteryManager.EXTRA_VOLTAGE` | Millivolts ($mV$), $\sim 3400 - 4450mV$ | Broadcast on change ($\sim 0.5 - 2Hz$) | Battery-side terminal voltage, includes ohmic $IR$ drop. NOT USB Vbus. |
| **Instantaneous Current** | DIRECTLY MEASURABLE | `BATTERY_PROPERTY_CURRENT_NOW` | Microamperes ($\mu A$), $\pm 5000mA$ | Polling ($1 - 4Hz$) | Positive = charging into battery, Negative = discharging. Sourced from Maxim MAX77759 shunt. |
| **Average Current** | DIRECTLY MEASURABLE | `BATTERY_PROPERTY_CURRENT_AVERAGE` | Microamperes ($\mu A$) | Polling ($1 - 4Hz$) | 10–30s rolling average from hardware fuel gauge register. |
| **Battery State of Charge** | DIRECTLY MEASURABLE | `BATTERY_PROPERTY_CAPACITY` / `EXTRA_LEVEL` | Integer percent ($0 - 100\%$) | Broadcast on change | Filtered Coulomb counter + OCV state from PMIC. |
| **Remaining Charge** | DIRECTLY MEASURABLE | `BATTERY_PROPERTY_CHARGE_COUNTER` | Microampere-hours ($\mu Ah$) | Polling | Supported on Pixel 8a. $\sim 4,492,000 \mu Ah$ nominal design. |
| **Battery Temperature** | DIRECTLY MEASURABLE | `BatteryManager.EXTRA_TEMPERATURE` | Tenths of $^\circ C$ ($0.1^\circ C$) | Broadcast on change | Sourced from internal battery pouch NTC thermistor. |
| **Power Source Type** | DIRECTLY MEASURABLE | `BatteryManager.EXTRA_PLUGGED` | Bitmask (AC, USB, Wireless, Dock) | Broadcast on change | AC indicates high-power wall adapter; Wireless indicates Qi inductive coil. |
| **Max Charging Voltage** | DIRECTLY MEASURABLE | `EXTRA_MAX_CHARGING_VOLTAGE` (`"max_charging_voltage"`) | Microvolts ($\mu V$), e.g. $9,000,000\mu V$ | Broadcast on change | Negotiated USB-PD profile voltage limit. |
| **Max Charging Current** | DIRECTLY MEASURABLE | `EXTRA_MAX_CHARGING_CURRENT` (`"max_charging_current"`) | Microamperes ($\mu A$), e.g. $2,200,000\mu A$ | Broadcast on change | Negotiated USB-PD profile current limit. |
| **Charge Cycle Count** | DIRECTLY MEASURABLE | `BatteryManager.EXTRA_CYCLE_COUNT` | Integer cycles | Broadcast on change (API 34+) | Hardware fuel gauge non-volatile cycle count register. |
| **Thermal Throttling Code** | DIRECTLY MEASURABLE | `PowerManager.OnThermalStatusChangedListener` | Integer enum ($0 - 6$) | Event callback | Direct from Android Thermal HAL AIDL 3. |
| **Thermal Headroom** | DIRECTLY MEASURABLE | `PowerManager.getThermalHeadroom(forecast)` | Float ($0.0 - 1.5+$) | Polling (max 1Hz) | $1.0$ indicates severe throttling threshold. |
| **Net Battery Power** | DERIVED | $P_{cell} = V_{cell} \times I_{cell}$ | Watts ($W$) | $1 - 2Hz$ | Pure physical power delivered into battery chemistry. |
| **Max Power Ceiling** | DERIVED | $P_{max} = V_{max} \times I_{max}$ | Watts ($W$) | Static per connection | Theoretical upper limit negotiated with USB-PD power brick. |
| **Protocol Classification** | DERIVED | Logic: $V_{max} > 5V \implies \text{USB-PD}$ | Categorical | Static per connection | Deterministic on Pixel hardware (no proprietary VOOC/HyperCharge). |
| **Full Capacity (SOH)** | ESTIMATED | $\Delta Q / \Delta SOC$ with gating filter | Millampere-hours ($mAh$) | Per validated session | Error bounded: $\pm 3 - 5\%$ under high-confidence sessions ($S_{conf} \ge 85\%$). |
| **Charger Input Power** | ESTIMATED | $P_{in} \approx (P_{cell} + P_{sys}) / \eta_{pmic}$ | Watts ($W$) | $1Hz$ | Uncertainty $\pm 15 - 25\%$; requires modeling PMIC efficiency ($\sim 90 - 93\%$) and display/SoC load. |
| **Cable Resistance** | ESTIMATED | $R_{cable} \approx \Delta V_{drop} / \Delta I$ | Milliohms ($m\Omega$) | Transient detection | Requires step current transitions. |
| **Time to Full** | ESTIMATED | `BatteryManager.computeChargeTimeRemaining()` | Milliseconds ($ms$) | Polling | Computed by Android BatteryStats CC-CV model. |
| **USB Energy Counter** | UNAVAILABLE | `BATTERY_PROPERTY_ENERGY_COUNTER` | nWh | N/A | Returns `Long.MIN_VALUE` on Pixel Tensor hardware. Must be integrated in software. |
| **Raw Vbus / Ibus Pins** | UNAVAILABLE | Direct USB-C hardware pins | Volts / Amperes | N/A | Restricted to kernel driver (`tcpm`). Third-party apps cannot access Vbus ADC. |
| **USB PDO / APDO Packets** | UNAVAILABLE | USB-PD CC-line packet sniffer | Digital packets | N/A | Requires `@SystemApi` `MANAGE_USB` (Signature/Privileged) or external hardware sniffer. |
| **Cable E-Marker ID** | UNAVAILABLE | SOP' / SOP'' CC wire messages | Hex vendor IDs | N/A | Not exposed to unprivileged userspace applications. |
| **Qi Transmitter Telemetry**| UNAVAILABLE | Qi coil AC voltage / frequency | V / kHz | N/A | Pad power and coil alignment metrics are not transmitted over Qi BPP/EPP to Android framework. |

---

## 2. Mathematical Power Derivations

### Battery-Side Power vs. Charger-Side Power
$$P_{\text{battery}} = \left(\frac{V_{\text{cell}}}{1{,}000}\right) \times \left(\frac{I_{\text{cell}}}{1{,}000{,}000}\right) \quad [\text{Watts}]$$

$$P_{\text{charger}} = \frac{P_{\text{battery}} + P_{\text{system}}}{\eta_{\text{PMIC}}} + I_{\text{cable}}^2 R_{\text{cable}} \quad [\text{Watts}]$$

Where:
- $V_{\text{cell}}$ is read in millivolts from `EXTRA_VOLTAGE`.
- $I_{\text{cell}}$ is read in microamperes from `BATTERY_PROPERTY_CURRENT_NOW`.
- $P_{\text{system}}$ is the dynamic phone power consumption (Display + Tensor G3 SoC + Modem).
- $\eta_{\text{PMIC}}$ is the PMIC buck regulator switching efficiency ($88\% - 94\%$).

**Crucial Engineering Rule:** The app shall **never** display $P_{\text{battery}}$ as "Charger Power". It is always labeled as **"Net Battery Power"**.
