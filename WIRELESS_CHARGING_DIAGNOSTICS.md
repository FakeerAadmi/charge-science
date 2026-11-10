# Wireless Charging Diagnostics, Coil Electrodynamics & Magnetic Ring Investigation
## Reference Platform: Google Pixel 8a (`akita`, Tensor G3, Android 14/15/17 Preview)

---

## 1. Executive Summary & Physical Diagnosis

A physical Google Pixel 8a with an aftermarket magnetic ring attached directly to its rear casing experiences wireless charging termination after approximately 10 minutes of active charging.

This investigation demonstrates that the termination is governed by **two distinct physical phenomena**:
1. **Geometric Coil Misalignment:** The physical center of the Pixel 8a's internal Qi receiver (Rx) coil is situated at $Y \approx 62.2\text{ mm}$ from the bottom of the device (level with the lower edge of the Volume Down button). The rear cosmetic Google "G" logo is positioned at $Y \approx 70.0\text{ mm}$. When an aftermarket MagSafe ring is attached concentric with the "G" logo, it introduces an **axial eccentric offset of $+7.8\text{ mm}$**, forcing the ring directly into the high-flux induction zone of the Qi coils.
2. **Inductive Joule Dissipation & Thermal Cut-off (Mode A vs. Mode B):**
   - **Mode A (Phone Thermal Throttle):** A continuous or unsegmented ferritic steel ring experiences severe high-frequency eddy current heating ($P_{\text{eddy}} \approx 0.8 - 1.8\text{ W}$), heating the rear casing to $> 50^\circ\text{C}$. This heat conducts into the battery pouch and triggers the Android Thermal HAL's **`VIRTUAL-SKIN-CHARGE-WLC` threshold at $41.0^\circ\text{C} - 42.0^\circ\text{C}$**. The phone's thermal daemon commands the PMIC to clamp charging current to $0\text{ mA}$ (`dc_icl = 0`), while `EXTRA_PLUGGED` remains `WIRELESS`.
   - **Mode B (Transmitter FOD / Pad Overheat):** Dynamic power loss accounting in the Qi transmitter ($\Delta P_{\text{loss}} = P_{\text{tx}} - P_{\text{rx}}$) drifts past the safety threshold ($500\text{ mW}$) as the ring's electrical resistance rises with temperature ($\alpha \approx +0.004/^\circ\text{C}$), or the pad's internal NTC thermistor trips, causing the pad to cut the RF inverter abruptly (`EXTRA_PLUGGED` transitions instantly to `0`).

---

## 2. Physical Layout & Geometric Coordinates

### 2.1 Google Pixel 8a Dimensions
- **Chassis Dimensions:** $152.1\text{ mm}$ (height) $\times\ 72.7\text{ mm}$ (width) $\times\ 8.9\text{ mm}$ (thickness).
- **Horizontal Centerline:** $X = 36.35\text{ mm}$.
- **Camera Visor (Bar):** Spans entire width, lower step at $Y \approx 109.6\text{ mm}$ ($\approx 42.5\text{ mm}$ from top edge).
- **Volume Down Button:** Lower physical edge at $Y \approx 62.0\text{ mm}$.
- **Google "G" Logo:** Center at $X = 36.35\text{ mm}, Y \approx 70.0\text{ mm}$.

### 2.2 Qi Receiving (Rx) Coil Physical Placement
- **Coil Center Coordinates:** $X = 36.35\text{ mm}, Y \approx 62.2\text{ mm}$.
- **Vertical Distance from Camera Visor:** $\approx 47.4\text{ mm}$ down from lower step.
- **Physical Landmark:** Level with the lower boundary of the Volume Down button.
- **Coil Geometry:** Outer Diameter $\approx 42.5\text{ mm}$, Inner Diameter $\approx 20.5\text{ mm}$, planar spiral Litz wire backed by a $48 \times 48\text{ mm}$ flexible sintered ferrite sheet.

### 2.3 The "G" Logo Alignment Disparity
$$\Delta Y = Y_{\text{G\_logo}} - Y_{\text{Rx\_coil}} \approx 70.0\text{ mm} - 62.2\text{ mm} = +7.8\text{ mm}$$

When a magnetic ring (Inner Diameter $\approx 44 - 46\text{ mm}$, Outer Diameter $\approx 54 - 56\text{ mm}$) is placed concentric with the "G" logo:
- The upper arc of the coil has $\approx 9.3\text{ mm}$ clearance from the ring.
- The lower arc of the coil **intersects and overlaps the metallic ring by $\approx 6.3\text{ mm}$**!
- The metallic ring is dragged directly across the active winding zone of maximum magnetic flux density $\mathbf{B}_{\text{max}}$.

---

## 3. Electromagnetic Dynamics: Neodymium vs. Ferritic Steel

### 3.1 Material Comparison

| Parameter | Segmented Neodymium (NdFeB) Array | Passive Ferritic Stainless Steel (AISI 430) |
| :--- | :--- | :--- |
| **Physical Construction** | 36–38 discrete arc magnets separated by resin/epoxy | Single-piece continuously stamped solid metal ring |
| **Electrical Circuit** | Open circuit (interrupted perimeter) | Closed loop (shorted secondary single-turn transformer) |
| **Magnetic Nature** | Permanent magnet ($B_0 \approx 0.2\text{ T}$) | Soft ferromagnetic ($\mu_r \approx 500 - 1500$) |
| **AC Eddy Currents** | Microscopic intra-grain only ($P < 50\text{ mW}$) | Macroscopic continuous loop currents ($P > 1\text{ W}$) |
| **Skin Depth at 150 kHz** | $\delta \approx 160\,\mu\text{m}$ (segmented) | $\delta \approx 31.8\,\mu\text{m}$ (extreme AC resistance) |
| **Coupling Degradation** | Minimal if aligned ($k \approx 0.70$) | Severe ($k \approx 0.40 - 0.50$ via flux shunting) |

### 3.2 High-Frequency Skin Depth ($\delta$)
$$\delta = \sqrt{\frac{\rho}{\pi f \mu_0 \mu_r}}$$
At Qi operating frequency $f = 150\text{ kHz}$:
- **Copper Winding:** $\rho = 1.7 \times 10^{-8}\,\Omega\cdot\text{m}, \mu_r = 1 \implies \delta \approx 170\,\mu\text{m}$.
- **Ferritic Steel Ring:** $\rho \approx 6.0 \times 10^{-7}\,\Omega\cdot\text{m}, \mu_r \approx 1000 \implies \delta \approx 31.8\,\mu\text{m}$.

Because the induced current is constrained to an ultra-thin surface layer of only $32\,\mu\text{m}$, the AC resistance $R_{\text{AC}}$ is multiplied by orders of magnitude, converting stray magnetic flux into pure heat.

---

## 4. Qi Foreign Object Detection (FOD) Protocols

1. **Pre-Power Ping Phase (Resonant Q-Factor Degradation):**
   $$Q = \frac{\omega_0 L_{\text{Tx}}}{R_{\text{Tx}} + R_{\text{reflected\_eddy}}}$$
   If $\Delta Q = \frac{Q_{\text{ref}} - Q_{\text{meas}}}{Q_{\text{ref}}} > 20\%$, the Tx halts before delivering high power.
2. **Active Power Loss Accounting (PLA):**
   $$\Delta P_{\text{loss}} = P_{\text{Tx}} - P_{\text{Rx}} = (V_{\text{in}} \cdot I_{\text{in}} - P_{\text{loss\_Tx}}) - (V_{\text{rect}} \cdot I_{\text{rect}} + P_{\text{loss\_Rx}})$$
   - Trip Threshold: $P_{\text{threshold}} \approx 350\text{ mW}$ (BPP 5W) to $500\text{ mW}$ (EPP 7.5W).
   - If $\Delta P_{\text{loss}} > P_{\text{threshold}}$ continuously for $> 1.5\text{ s}$, the Tx trips emergency FOD fault and cuts the RF field.

---

## 5. Telemetry Signatures for Diagnostic Classification

```
                 INTERRUPTED WIRELESS SESSION
                               |
            Is EXTRA_PLUGGED still WIRELESS (4)?
              /                            \
           YES                              NO
           /                                  \
[ Phone Clamped Current ]              [ Transmitter Cut Power ]
Did T_batt exceed 40.5°C?               Did drop happen in < 5s?
   /                \                      /                \
 YES                 NO                  YES                 NO
  |                   |                   |                   |
MODE A:            Indeterminate      Ping FOD       Did T_batt remain < 38°C?
Phone Thermal      Power Rollback     Rejection         /                \
Throttle Cutoff                                       YES                 NO
                                                       |                   |
                                                    MODE B:            Transmitter
                                                  Dynamic FOD /         Thermal
                                                  Pad Overheat          Cutoff
```

### 5.1 Verification Checklist for Telemetry Engine
1. **Mode A (Phone Thermal Throttle):**
   - `BatteryManager.EXTRA_PLUGGED` = `BATTERY_PLUGGED_WIRELESS` (4).
   - `currentMa` drops to $0\text{ mA}$ (or $< +50\text{ mA}$).
   - `temperatureCelsius` $\ge 40.5^\circ\text{C}$ (Pixel 8a `VIRTUAL-SKIN-CHARGE-WLC` threshold).
   - Android Thermal Status = `MODERATE` (2) or `SEVERE` (3).
   - `PowerManager.getThermalHeadroom(0)` $\le 0.05$.
2. **Mode B (Transmitter FOD / Pad Overheat):**
   - `BatteryManager.EXTRA_PLUGGED` drops abruptly from `4` to `0` (Unplugged).
   - `currentMa` drops instantaneously from positive to discharge negative ($-300$ to $-600\text{ mA}$).
   - Phone battery temperature is nominal ($< 38^\circ\text{C}$).
   - Phone thermal status is `NONE` (0) or `LIGHT` (1).
3. **Power Coupling Collapse (Misalignment):**
   - High power variance before drop ($> 1.8\text{ W}^2$).
   - Current hunts between $200\text{ mA}$ and $900\text{ mA}$ before losing lock.

---

## 6. The Guided Experiment Suite

| Experiment | Configuration | Scientific Objective |
| :--- | :--- | :--- |
| **A: Baseline** | Magnetic ring attached, normal placement on pad | Establish exact baseline cutoff time, peak temperature, and drop signature. |
| **B: Alignment** | Magnetic ring attached, phone shifted 5–8 mm downward (level with Vol Down) | Test if eliminating the $+7.8\text{ mm}$ axial offset stabilizes power and reduces ring heating. |
| **C: Ring Removed** | Magnetic ring detached completely, same pad, matching starting SOC and temp | Ground-truth isolation: If the 10-minute cutoff vanishes, the ring is definitively confirmed as the root cause. |
| **D: Alt Charger** | Same ring, but tested on a high-power active-cooled wireless pad | Distinguishes pad internal thermal trip from phone thermal trip. |
| **E: Thermal Baseline** | Starting from cold baseline ($T < 25^\circ\text{C}$) vs elevated ($T > 33^\circ\text{C}$) | Quantifies thermal time-to-cutoff $\Delta t_{\text{cutoff}}(T_0)$. |
