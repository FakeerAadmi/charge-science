# Testing Strategy & Verification
## Project: CHARGE SCIENCE

---

## 1. Physical Device Verification (Google Pixel 8a)

Physical verification is paramount because battery and charging telemetry depends on the device PMIC, kernel drivers, and hardware thermistors.

### 1.1 Direct Telemetry Invariance Tests
Every metric displayed in the UI is verified against concurrent ADB dumpsys readings:
- **Voltage:** UI millivolts match `adb shell dumpsys battery | grep voltage` within $\pm 10\text{ mV}$.
- **Current:** UI milliamperes match `adb shell dumpsys batteryproperties` or `cmd battery get current_now` ($\mu\text{A} / 1000$).
- **Temperature:** UI temperature matches `adb shell dumpsys battery | grep temperature` ($0.1^\circ\text{C}$).
- **Cycle Count:** UI cycle count matches `adb shell cat /sys/class/power_supply/maxfg/cycle_count` ($782$ on reference device).
- **Negotiated Profile:** Advertised maximum voltage/current in UI matches `mPortSinkPowerProfileMatches` in `adb shell dumpsys usb` ($9.0\text{V} @ 2.2\text{A} \to 19.8\text{W}$).

### 1.2 Sign Convention Assertions
- When plugged in and charging: `BATTERY_PROPERTY_CURRENT_NOW > 0`.
- When unplugged: `BATTERY_PROPERTY_CURRENT_NOW < 0` (or 0 during deep idle).
- Net Wattage must never display a negative sign when charging.

### 1.3 End-to-End Real Session Validation
Verified on Google Pixel 8a (`akita`, Android 17 / SDK 37 Preview):
1. Plugged into 30W USB-PD adapter.
2. `ChargingTelemetryService` started in foreground (`specialUse`), acquired `PARTIAL_WAKE_LOCK`.
3. Sampled 1 Hz observations into Room `telemetry_samples` table.
4. Unplugged device: state machine detected discharge transition, released WakeLock, calculated trapezoidal average power, computed $S_{\text{conf}}$, finalized session in `charging_sessions` table.
5. Session successfully queried and rendered on History screen and Session Detail Dialog.

---

## 2. Unit Testing Strategy

Implemented in JUnit 4 test suites:

### 2.1 `BatteryScienceCalculatorsTest` (7 Tests)
- `powerCalculation_withNominalUnits_returnsAccurateWatts`: Verifies $P = V \times I$ with correct units ($mV \to V$, $\mu A \to A$).
- `powerCalculation_negativeCurrent_returnsZeroOrNegativeWattage`: Verifies discharge sign handling.
- `trapezoidalAveragePower_withUniformIntervals_returnsExpectedIntegral`: Verifies numerical integration under constant dt.
- `trapezoidalAveragePower_withNonUniformIntervals_avoidsArithmeticMeanSkew`: Proves trapezoidal integration eliminates arithmetic mean skew during non-uniform sampling intervals.
- `confidenceScore_shallowInterruptedSession_returnsLowConfidence`: Asserts short sessions with low $\Delta\text{SOC}$ and throttling receive $< 40\%$ confidence.
- `confidenceScore_deepCompleteSession_returnsHighConfidence`: Asserts full CC-CV charging cycles receive $> 80\%$ confidence.
- `protocolInference_classifiesAccurately`: Tests USB-PD classification across edge profiles ($5\text{V} \implies \text{USB}$, $9\text{V} \implies \text{USB-PD}$, $0\text{V} \implies \text{Unknown}$).

### 2.2 `ChargingSessionStateMachineTest` (6 Tests)
- `sessionStartsOnFirstChargingTelemetry`: Asserts initial charging event triggers new active session creation in database.
- `sessionFinalizesOnDischarge`: Asserts transition from charging to discharging sets `endTimestamp`, computes duration, and sets `isCompleted = true`.
- `chargingSourceTransition_rollsOverToNewSession`: Asserts changing from AC to USB or Wireless rolls over session automatically.
- `trapezoidalPowerAccumulation_updatesRunningAverage`: Asserts incoming telemetry updates running time-weighted average power.
- `rebootRecovery_recoversIncompleteSessions`: Asserts incomplete sessions left open by app termination or device reboot are cleanly closed on restart.
- `interruptionDetection_tracksDisconnects`: Asserts unexpected process kills mark `wasInterrupted = true`.

### 2.3 `ChargingProtocolClassifierTest` (5 Tests)
- Fixed 9V USB-PD, PPS Candidate, Type-C High Power, USB BC 1.2, and Qi EPP.

### 2.4 `ChargingComparisonEngineTest` (3 Tests)
- Valid pairing, baseline mismatch warnings ($|\Delta\text{SOC}| > 15\%$), and energy calculation.

### 2.5 `DataExportTest` (3 Tests)
- RFC 4180 CSV escaping, session CSV serialization, and JSON schema structure.

### 2.6 `WirelessDiagnosticEngineTest` (5 Tests)
- `testStableCompletionReturnsStableOutcome`: Uninterrupted wireless charging returns stable outcome.
- `testModeAPhoneThermalThrottleCutoff`: Mode A thermal cutoff ($T_{\text{max}} \ge 41.5^\circ\text{C}$, gradual throttle) identified as `PHONE_THERMAL_THROTTLE`.
- `testModeBTransmitterFodOrPadOverheat`: Mode B abrupt cutoff with cool battery ($T < 38^\circ\text{C}$) identified as `TX_SHUTDOWN_FOD`.
- `testPowerCouplingCollapseFromMisalignment`: High power variance identified as `POWER_COUPLING_COLLAPSE`.
- `testMagneticRingProfileRiskTiers`: Correct risk classification for continuous ferritic steel vs segmented NdFeB.

### 2.7 `BatteryHealthLaboratoryEngineTest` (5 Tests)
- `testValidChargingSessionCapacityCalculation`: Verifies accurate Coulomb-counted capacity and SoH against nominal 4,492 mAh.
- `testRejectsLowDeltaSocSession`: Rejects $\Delta\text{SOC} < 30\%$ to bound quantization error.
- `testRejectsThermalInstabilitySession`: Rejects $\Delta T > 8^\circ\text{C}$ or $T_{\text{max}} > 45^\circ\text{C}$.
- `testRejectsInterruptedSession`: Rejects interrupted charging sessions.
- `testMultiSessionHealthSummaryComputation`: Multi-session weighted regression and thermal exposure categorization.

**Total Test Count:** 34 tests passing cleanly.
- `interruptedSessionRecovery_finalizesUnclosedSessions`: Asserts app launch / reboot scans for unclosed active sessions and closes them with `wasInterrupted = true`.
- `confidenceScoreCalculated_onSessionCompletion`: Asserts session closure computes and persists $S_{\text{conf}}$.
