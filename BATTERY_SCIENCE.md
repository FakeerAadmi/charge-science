# Battery Science & Electrochemical Foundations
## Lithium-Ion Degradation, Coulomb Counting, USB Thermodynamics & Wireless Physics

This document provides the electrochemical and thermodynamic foundation for **Charge Science**, specifically calibrated for modern smartphone lithium-ion / lithium-polymer pouch cells (reference: **Google Pixel 8a**, 4,492 mAh nominal capacity, Maxim MAX77759 companion PMIC / ModelGauge m5 architecture).

---

## 1. Capacity Estimation Physics

### 1.1 Coulomb Counting (Current Integration)
Coulomb counting integrates instantaneous current flowing into or out of the cell:
$$Q(t) = Q(t_0) + \int_{t_0}^t I(\tau) \, d\tau$$

In discrete time with non-uniform sampling interval $\Delta t_k = t_{k+1} - t_k$:
$$Q_k = Q_0 + \sum_{k=1}^N \frac{I_k + I_{k+1}}{2} \cdot \Delta t_k$$

**Error Propagation & Drift:**
Any current sensor has a zero-mean measurement noise $\nu_k \sim \mathcal{N}(0, \sigma^2)$ and an offset bias $\epsilon_{\text{bias}}$. The accumulated error grows:
$$E_Q(t) = \int_0^t \epsilon_{\text{bias}} \, d\tau + \int_0^t \nu(\tau) \, d\tau \approx \epsilon_{\text{bias}} \cdot t + \sigma \sqrt{t}$$

Even a tiny $5\text{ mA}$ sensor offset produces $120\text{ mAh}$ error in 24 hours ($\approx 2.7\%$ total capacity).

### 1.2 Dedicated Hardware Fuel Gauge (ModelGauge m5) Architecture
The Pixel 8a features a dedicated Maxim MAX77759 fuel gauge that samples continuously at $100\text{ Hz} - 1\text{ kHz}$ across a calibrated Kelvin-connected current shunt ($5-10\text{ m}\Omega$).
It runs a dynamic servo-mixing algorithm that balances Coulomb counting during dynamic loads with Open Circuit Voltage (OCV) estimation during relaxation periods.

#### `FullCapNom` vs. `FullCapRep` (Understanding `charge_full`)
In Maxim ModelGauge m5 architectures:
1. **`FullCapNom` (Nominal Full Capacity):** The calculated cell capacity under reference temperature ($25^\circ\text{C}$) and standardized discharge rates ($C/20$).
2. **`FullCapRep` (Reported Full Capacity, exposed via sysfs `charge_full`):** Dynamically derated capacity taking into account the cell's current operating temperature, internal impedance growth ($R_{\text{int}}$), and recent discharge load patterns.

**Crucial Scientific Distinction:**
On a Pixel 8a with 782 cycles, reading `charge_full = 3,998 mAh` versus nominal `4,492 mAh` does **not** mean the battery has an immutable $89\%$ State of Health. If sampled at elevated temperature ($>35^\circ\text{C}$) or after high-drain usage, `FullCapRep` shifts dynamically. Presenting the raw ratio as an immutable health metric is misleading.

### 1.3 Userspace Power Integration (Trapezoidal Rule)
Instantaneous power $P(t) = V_{\text{term}}(t) \cdot I_{\text{batt}}(t)$. Because Android system wakeups and CPU scheduling produce non-uniform sampling intervals $\Delta t_i = t_{i+1} - t_i$, taking a simple arithmetic mean of sampled power induces up to $18\%$ skew during fast-charging taper phases.

Charge Science computes average session power via trapezoidal numerical integration:
$$\bar{P} = \frac{1}{t_N - t_0} \sum_{i=0}^{N-1} \frac{P(t_i) + P(t_{i+1})}{2} \cdot (t_{i+1} - t_i)$$

Total net energy delivered to the battery rail is:
$$E_{\text{net}} = \int_{t_0}^{t_N} P(\tau) \, d\tau \approx \sum_{i=0}^{N-1} \frac{P(t_i) + P(t_{i+1})}{2} \cdot \frac{t_{i+1} - t_i}{3600} \quad [\text{Wh}]$$

---

## 2. Physical Impossibility of Battery-Side Cable Resistance Estimation

A prevalent pseudoscientific claim in amateur battery tools is calculating USB-C cable resistance from Android `BatteryManager` readings ($R = \Delta V / \Delta I$).

### Mathematical & Circuit Proof of Impossibility:
Along the physical charging path:
$$V_{\text{charger}} = V_{\text{bus\_in}} + I_{\text{cable}} \cdot R_{\text{cable}}$$
$$\implies R_{\text{cable}} = \frac{V_{\text{charger}} - V_{\text{bus\_in}}}{I_{\text{cable}}}$$

Where:
- $V_{\text{charger}}$: Regulated voltage output at the power brick terminals.
- $R_{\text{cable}}$: Round-trip loop resistance ($V_{\text{BUS}}$ wire + $\text{GND}$ return wire + contact pins).
- $V_{\text{bus\_in}}$: Input voltage arriving at the phone's Type-C port.
- $I_{\text{cable}}$: Current flowing through the cable wire.

### The PMIC Switching Buck Disconnect:
Modern smartphones (including Google Pixel 8a) use **high-efficiency switching buck converters** (not linear LDO regulators):
$$(V_{\text{bus\_in}} \cdot I_{\text{cable}}) \cdot \eta_{\text{PMIC}} = (V_{\text{batt}} \cdot I_{\text{batt}}) + P_{\text{sys}}$$

Consequently:
$$I_{\text{batt}} = I_{\text{cable}} \cdot \left(\frac{V_{\text{bus\_in}}}{V_{\text{batt}}}\right) \cdot \eta_{\text{PMIC}} - \frac{P_{\text{sys}}}{V_{\text{batt}}}$$

Because $V_{\text{bus\_in}}$ is negotiated at $9.0\text{V}$ and $V_{\text{batt}} \approx 3.8 - 4.45\text{V}$:
- **$I_{\text{batt}} \ne I_{\text{cable}}$:** A cable current of $2.0\text{A}$ at $9\text{V}$ ($18\text{W}$) yields $\sim 4.1\text{A}$ into the battery cell!
- $V_{\text{charger}}$ varies dynamically under load due to regulation droop and PPS requests.
- $V_{\text{bus\_in}}$ is locked behind kernel ADC drivers and is invisible to userspace.
- Dynamic system load $P_{\text{sys}}$ (CPU, display, modem) fluctuates continuously.

With **4 dynamic unknowns** and only **2 knowns** ($V_{\text{batt}}, I_{\text{batt}}$), calculating $R_{\text{cable}}$ from userspace battery telemetry is mathematically indeterminate. Charge Science explicitly refuses to produce fabricated resistance metrics without an external inline BLE power meter.

---

## 3. Wireless Charging Thermodynamics (Pixel 8a Qi Baseline)

### 3.1 Hardware Capability
- **Inductive Receiver Coil:** Standard Qi 1.x Baseline & Extended Power Profile.
- **Hardware Wattage Ceiling:** **7.5W max**.
- **Qi2 Magnetic Power Profile (MPP):** Not supported on Pixel 8a. Magnetic cases provide mechanical retention only; electrical negotiation remains standard Qi 7.5W.
- **Transmitter Wall Power:** Inaccessible from Android framework due to electrical air-gap isolation.

### 3.2 Thermal Load Coefficient ($\Theta$)
Wireless charging converts power across three lossy stages:
1. Transmitter AC inverter & coil eddy currents ($\sim 80\%$).
2. Air-gap magnetic coupling ($k \approx 0.65 - 0.85$).
3. Phone receiver synchronous rectifier & PMIC buck converter.

Because the receiver coil and rectifier sit immediately against the phone's battery pouch, wireless charging produces a much higher thermal rise per stored Joule. Charge Science defines the **Thermal Load Coefficient ($\Theta$)**:
$$\Theta = \frac{\Delta T_{\text{max}}}{E_{\text{net\_Wh}}} \quad \left[\frac{^\circ\text{C}}{\text{Wh}}\right]$$

On Pixel 8a:
- **18W Wired USB-PD:** $\Theta \approx 0.4 - 0.7^\circ\text{C}/\text{Wh}$
- **7.5W Wireless Qi:** $\Theta \approx 1.5 - 2.8^\circ\text{C}/\text{Wh}$ (over **3× higher thermal burden** per Wh stored).

---

## 4. Controlled Charging Session Comparison Principles

Directly comparing two charging sessions without electrochemical controls produces misleading conclusions.

### Required Scientific Controls:
1. **Starting State of Charge Matching ($|\Delta \text{SOC}_{\text{start}}| \le 15\%$):**
   Lithium-ion cells follow non-linear Constant Current / Constant Voltage (CC-CV) saturation curves. A session charging from $10\% \to 60\%$ operates almost entirely in full-power CC mode ($15 - 18\text{W}$), whereas a session from $60\% \to 100\%$ operates in CV saturation taper ($12\text{W} \to 2\text{W}$). Declaring Charger A "faster" than Charger B when starting SOCs differ is unscientific.
2. **Thermal Baseline Matching ($|\Delta T_{\text{start}}| \le 6^\circ\text{C}$):**
   Pre-existing cell heat triggers Android Thermal HAL throttling earlier, reducing average power independently of charger quality.
3. **Session Integrity:**
   Both sessions must be uninterrupted by app force-stops or reboots, with data confidence $S_{\text{conf}} \ge 75\%$.

---

## 5. Qi Electrodynamics, Magnetic Ring Physics & Failure Mechanisms

### 5.1 Geometric Coil Offset Disparity
On the Google Pixel 8a (`akita`, dimensions $152.1 \times 72.7 \times 8.9\text{ mm}$):
- **Receiver (Rx) Coil Center:** $X = 36.35\text{ mm}$ (centered), $Y \approx 62.2\text{ mm}$ from bottom edge (level with bottom edge of Volume Down button).
- **Rear "G" Logo Center:** $X = 36.35\text{ mm}$, $Y \approx 70.0\text{ mm}$.
- **Offset ($\Delta Y$):** $+7.8\text{ mm}$.
Attaching an aftermarket MagSafe conversion ring centered on the cosmetic "G" logo forces a $7.8\text{ mm}$ vertical misalignment, pulling the metallic ring directly into the coil's primary induction winding path.

### 5.2 High-Frequency AC Skin Depth & Eddy Heating
At Qi frequencies ($f \approx 150\text{ kHz}$):
- Skin depth in passive ferritic stainless steel (AISI 430, $\mu_r \approx 1000$):
  $$\delta = \sqrt{\frac{\rho}{\pi f \mu_0 \mu_r}} \approx 31.8\,\mu\text{m}$$
- The continuous stamped steel ring forms a shorted single-turn secondary transformer winding, generating $0.8 - 1.8\text{ W}$ of parasitic Joule heating ($P = I_{\text{eddy}}^2 R_{\text{AC}}$).
- In contrast, segmented Neodymium (NdFeB) arrays feature physical dielectric boundaries between magnet blocks, preventing continuous perimeter eddy loops.

### 5.3 The 10-Minute Interruption Dynamics
- **Mode A (Phone Thermal Throttle):** Ring heat conducts through the chassis. When the virtual sensor `VIRTUAL-SKIN-CHARGE-WLC` crosses $41.0^\circ\text{C} - 42.0^\circ\text{C}$ (matching the device thermal time constant $\tau \approx 10\text{ min}$), the Android Thermal HAL clamps charge current to zero (`dc_icl = 0`). The phone stays `PLUGGED_WIRELESS` but net charging ceases.
- **Mode B (Transmitter FOD Trip / Pad Overheat):** Ring resistance increases with temperature ($\alpha \approx +0.004/^\circ\text{C}$), pushing power loss $\Delta P_{\text{loss}} = P_{\text{tx}} - P_{\text{rx}} > 500\text{ mW}$ (FOD trip) or tripping the pad's internal thermistor. The pad cuts RF power abruptly, causing `EXTRA_PLUGGED` to drop to `0` while the phone battery is still cool ($< 38^\circ\text{C}$).

---

## 6. Maxim ModelGauge m5 & Defensible Capacity Estimation

### 6.1 Algorithmic Dual-Engine
The Maxim MAX77759 combines high-speed continuous Coulomb counting (via Kelvin shunt) with thermodynamic Open-Circuit Voltage (OCV) table lookups during relaxation. Filtered reported capacity (`RepCap`) tracks remaining usable charge, while `FullCapNom` and `FullCapRep` evaluate chemical aging.

### 6.2 Coulomb Counting Estimation & Quantization Gating
Usable capacity across qualified charging windows is derived via:
$$C_{\text{est}} = \frac{\Delta Q}{\Delta\text{SOC}} = \frac{Q(t_{\text{end}}) - Q(t_{\text{start}})}{\text{SOC}(t_{\text{end}}) - \text{SOC}(t_{\text{start}})}$$
- **Quantization Error Bound:** $\epsilon_{\text{quant}} \approx \frac{1\%}{\Delta\text{SOC}}$.
  - $\Delta\text{SOC} < 30\% \implies \epsilon_{\text{quant}} > 3.3\%$ (**Strictly Rejected**).
  - $\Delta\text{SOC} \ge 40\% \implies \epsilon_{\text{quant}} \le 2.5\%$ (**Laboratory Grade**).
- **Thermal Rejection:** $\Delta T > 8^\circ\text{C}$ or $T_{\text{max}} > 45^\circ\text{C}$ rejected due to positive temperature coefficient of capacity ($\sim +0.6\%/^\circ\text{C}$).
- **Monotonicity & Continuity:** Sessions with process interruptions or discrete fuel-gauge recalibration jumps are rejected.
