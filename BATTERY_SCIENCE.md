# Battery Science & Electrochemical Foundations
## Lithium-Ion Degradation, Coulomb Counting & Thermodynamics

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
On a Pixel 8a with 782 cycles, reading `charge_full = 3,998 mAh` versus nominal `4,492 mAh` does **not** simply mean the battery has exactly $89\%$ State of Health. If sampled at elevated temperature ($>35^\circ\text{C}$) or after high-drain usage, `FullCapRep` shifts dynamically. Presenting the raw ratio as an immutable health metric is misleading.

### 1.3 Userspace Power Integration (Trapezoidal Rule)
Instantaneous power $P(t) = V_{\text{term}}(t) \cdot I_{\text{batt}}(t)$. Because Android system wakeups and CPU scheduling produce non-uniform sampling intervals $\Delta t_i = t_{i+1} - t_i$, taking a simple arithmetic mean of sampled power induces up to $18\%$ skew during fast-charging taper phases.

Charge Science computes average session power via trapezoidal numerical integration:
$$\bar{P} = \frac{1}{t_N - t_0} \sum_{i=0}^{N-1} \frac{P(t_i) + P(t_{i+1})}{2} \cdot (t_{i+1} - t_i)$$

Total net energy delivered to the battery rail is:
$$E_{\text{net}} = \int_{t_0}^{t_N} P(\tau) \, d\tau \approx \sum_{i=0}^{N-1} \frac{P(t_i) + P(t_{i+1})}{2} \cdot \frac{t_{i+1} - t_i}{3600} \quad [\text{Wh}]$$

---

## 2. State of Health (SOH) & Degradation Mechanics

### 2.1 SOH Formulations
$$\text{SOH}_{\text{Capacity}} = \frac{\text{FCC}(T_{\text{ref}})}{Q_{\text{design}}} \times 100\%$$
Where:
- $Q_{\text{design}} = 4{,}492\text{ mAh}$ for the Google Pixel 8a.
- $\text{FCC}(T_{\text{ref}})$ is the Full Charge Capacity at $25^\circ\text{C}$ from $4.45\text{V}$ down to $3.2\text{V}$.

End of Life (EOL) for mobile lithium-ion cells is internationally recognized when $\text{FCC} \le 80\% \cdot Q_{\text{design}}$ or internal resistance doubles ($R_{\text{actual}} \ge 2 \cdot R_{\text{fresh}}$).

### 2.2 Equivalent Full Cycles (EFC)
One Equivalent Full Cycle is defined strictly as cumulative discharge throughput divided by design capacity:
$$N_{\text{EFC}} = \frac{1}{Q_{\text{design}}} \sum |\Delta Q_{\text{discharge}}|$$

Degradation per cycle scales non-linearly with Depth of Discharge (DoD):
$$\text{Degradation per cycle} \propto (\Delta \text{DoD})^\beta, \quad \beta \approx 1.8 - 2.2$$

Ten $10\%$ discharge cycles cause significantly less electrochemical damage than a single $100\%$ full deep-discharge cycle.

### 2.3 Primary Degradation Mechanisms
1. **Solid Electrolyte Interphase (SEI) Growth:** Parasitic reduction of organic solvent molecules at the graphite anode binds active lithium ions ($Li^+$) irreversibly. Follows Arrhenius temperature kinetics:
   $$k_{\text{SEI}}(T) = A \cdot \exp\left(-\frac{E_a}{R_u T}\right)$$
   Elevated temperatures ($>35^\circ\text{C}$) combined with high cell potentials ($>4.2\text{V}$) strongly accelerate SEI thickening.
2. **Lithium Plating:** Occurs when the anode overpotential drops below $0\text{V vs. } Li/Li^+$, causing metallic lithium to deposit on the anode instead of intercalating. Accelerated by fast charging at low temperatures ($<15^\circ\text{C}$) or high C-rates near high SOC.
3. **Impedance Rise (Ohmic & Charge Transfer):** Surface film growth increases cell internal resistance $R_{\text{int}}$, causing premature low-voltage shutdown under transient CPU/GPU load bursts.

---

## 3. Charging Thermodynamics & Throttling

### 3.1 Heat Generation (Bernardi Equation)
$$\dot{Q}_{\text{cell}} = \underbrace{I^2 R_{\text{int}}}_{\text{Irreversible Joule Heating}} + \underbrace{I \cdot T \cdot \frac{\partial V_{\text{OCV}}}{\partial T}}_{\text{Reversible Entropic Heating}}$$

At an $18\text{W}$ peak charge rate ($I_{\text{cell}} \approx 4\text{A}$, $R_{\text{int}} \approx 35\text{m}\Omega$):
$$\dot{Q}_{\text{Joule}} = (4\text{A})^2 \times 0.035\Omega = 0.56\text{W}$$

### 3.2 PMIC Heat Dissipation
Converting USB bus voltage ($9\text{V}$) down to battery rail voltage ($3.8 - 4.45\text{V}$) in the switching buck regulator occurs at $\approx 92\%$ efficiency:
$$P_{\text{loss, PMIC}} = 18\text{W} \times (1 - 0.92) = 1.44\text{W}$$

**Scientific Reality:** The PMIC generates **$1.44\text{W}$** of concentrated heat—more than **2.5×** the battery's internal Joule heat ($0.56\text{W}$). Because the PMIC is enclosed within the unventilated phone chassis next to the battery pouch, PMIC dissipation is the dominant driver of battery heating during fast charging.

---

## 4. Session Confidence Scoring Algorithm ($S_{\text{conf}}$)

To prevent presenting noisy, inaccurate capacity estimates, Charge Science computes a session confidence score:
$$S_{\text{conf}} = 0.40 \cdot C_{\Delta \text{SOC}} + 0.25 \cdot C_{\text{thermal}} + 0.20 \cdot C_{\text{rest}} + 0.15 \cdot C_{\text{quality}}$$

- **$C_{\Delta \text{SOC}}$:** $100$ if $\Delta \text{SOC} \ge 70\%$; scaled linearly from $0$ to $100$ between $10\%$ and $70\%$.
- **$C_{\text{thermal}}$:** $\max(0, 100 - 15 \cdot \Delta T_{\text{rise}} - 20 \cdot N_{\text{throttle}})$.
- **$C_{\text{rest}}$:** $100$ if session reached completion ($100\%$ SOC with taper); $50$ if healthy stop above $80\%$; $20$ if interrupted.
- **$C_{\text{quality}}$:** Percentage of valid 1 Hz telemetry samples captured without sampling dropouts: $\frac{N_{\text{valid}}}{N_{\text{valid}} + N_{\text{missing}}} \times 100$.

**Gating Rule:** Only sessions with $S_{\text{conf}} \ge 80\%$ and $\Delta \text{SOC} \ge 70\%$ are qualified for deep battery health recalculation.
