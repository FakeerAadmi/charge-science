package com.example.chargescience.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chargescience.ui.components.SegmentedGroup
import com.example.chargescience.ui.components.SegmentedPosition
import com.example.chargescience.ui.components.SegmentedTile
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Interactive Preferences States
    var highFreqSampling by remember { mutableStateOf(true) }
    var wakelockActive by remember { mutableStateOf(true) }
    var directFuelGaugeRead by remember { mutableStateOf(true) }

    var protect80Alarm by remember { mutableStateOf(true) }
    var thermalThresholdCelsius by remember { mutableFloatStateOf(42.0f) }
    var minSocSwingPercent by remember { mutableIntStateOf(20) }

    var magneticRingWatchdog by remember { mutableStateOf(true) }
    var fodSensitivityHigh by remember { mutableStateOf(false) }

    var useTabularFigures by remember { mutableStateOf(true) }
    var unitAmperes by remember { mutableStateOf(false) } // false = mA, true = A

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Diagnostics Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ==========================================
            // 1. TELEMETRY & SAMPLING ENGINE
            // ==========================================
            SegmentedGroup(
                title = "Telemetry & Sampling Engine"
            ) {
                // High frequency sampling toggle
                SegmentedTile(
                    title = "1.0 Hz High-Rate Telemetry",
                    position = SegmentedPosition.TOP,
                    subtitle = if (highFreqSampling) "1,000ms hardware sample rate active" else "Power-save sample rate (0.2 Hz)",
                    icon = Icons.Rounded.Speed,
                    customHeaderTrailing = {
                        Switch(
                            checked = highFreqSampling,
                            onCheckedChange = { highFreqSampling = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Polls BatteryManager and Maxim MAX77759 registers at approximately 1 Hz during active sessions to capture transient current dips and PMIC thermal stepdowns.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // WakeLock during charging sessions
                SegmentedTile(
                    title = "Session CPU WakeLock",
                    position = SegmentedPosition.MIDDLE,
                    subtitle = if (wakelockActive) "Prevents deep Doze while recording" else "OS may suspend sampling in Doze",
                    icon = Icons.Rounded.LockClock,
                    customHeaderTrailing = {
                        Switch(
                            checked = wakelockActive,
                            onCheckedChange = { wakelockActive = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Acquires a PARTIAL_WAKE_LOCK tied to ChargingTelemetryService while the device is plugged in so high-resolution curves are uninterrupted by Android Power HAL.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Direct fuel gauge register polling
                SegmentedTile(
                    title = "Hardware Fuel Gauge Registers",
                    position = SegmentedPosition.BOTTOM,
                    subtitle = if (directFuelGaugeRead) "Reading raw microvolts and microamps" else "Standard Android battery intent broadcasts",
                    icon = Icons.Rounded.Memory,
                    customHeaderTrailing = {
                        Switch(
                            checked = directFuelGaugeRead,
                            onCheckedChange = { directFuelGaugeRead = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Queries BATTERY_PROPERTY_CURRENT_NOW and BATTERY_PROPERTY_CHARGE_COUNTER directly rather than relying on broadcast intents which only update on percentage changes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // ==========================================
            // 2. BATTERY HEALTH & SAFETY THRESHOLDS
            // ==========================================
            SegmentedGroup(
                title = "Battery Health & Safety Thresholds"
            ) {
                // Design nominal capacity
                SegmentedTile(
                    title = "Design Reference Capacity",
                    position = SegmentedPosition.TOP,
                    trailingLabel = "4,492 mAh",
                    subtitle = "Nominal rating for Google Pixel 8a (akita)",
                    icon = Icons.Rounded.BatteryChargingFull,
                    expandedContent = {
                        Text(
                            text = "Used as the 100% SOH denominator. Pixel 8a uses a high-density 17.43 Wh Li-ion pouch cell with a nominal voltage of 3.88V and maximum charge potential of 4.45V.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Protective 80% charge alarm
                SegmentedTile(
                    title = "80% Longevity Alarm",
                    position = SegmentedPosition.MIDDLE,
                    subtitle = if (protect80Alarm) "Audible notification at 80% SOC" else "Alarms disabled",
                    icon = Icons.Rounded.NotificationsActive,
                    customHeaderTrailing = {
                        Switch(
                            checked = protect80Alarm,
                            onCheckedChange = { protect80Alarm = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Mitigates high-voltage electrolyte oxidation. The final 20% of charging subjects cathode material to the highest electrochemical stress and accounts for disproportionate cyclic degradation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Thermal throttling safety threshold
                SegmentedTile(
                    title = "Thermal Alarm Threshold",
                    position = SegmentedPosition.BOTTOM,
                    trailingLabel = String.format(Locale.US, "%.1f °C", thermalThresholdCelsius),
                    subtitle = "Warns before severe PMIC throttling triggers",
                    icon = Icons.Rounded.Thermostat,
                    expandedContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Adjust the high-temperature warning limit. Pixel 8a thermal governor begins throttling charging current at ~39.5°C and applies severe power caps at 43.0°C.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = thermalThresholdCelsius,
                                onValueChange = { thermalThresholdCelsius = it },
                                valueRange = 38.0f..46.0f,
                                steps = 15
                            )
                        }
                    }
                )
            }

            // ==========================================
            // 3. WIRELESS & MAGNETIC PROTECTION
            // ==========================================
            SegmentedGroup(
                title = "Wireless & Magnetic Ring Protection"
            ) {
                // Magnetic ring thermal watchdog
                SegmentedTile(
                    title = "Magnetic Ring Thermal Watchdog",
                    position = SegmentedPosition.TOP,
                    subtitle = if (magneticRingWatchdog) "Auto-detects eddy current thermal rise" else "Thermal rise alerts disabled",
                    icon = Icons.Rounded.Sensors,
                    customHeaderTrailing = {
                        Switch(
                            checked = magneticRingWatchdog,
                            onCheckedChange = { magneticRingWatchdog = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Calculates derivative pouch heating rate (dT/dt). Aftermarket magnetic rings containing ferromagnetic metals generate parasitically induced eddy currents that heat the phone chassis directly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Foreign object detection sensitivity
                SegmentedTile(
                    title = "Qi FOD Sensitivity",
                    position = SegmentedPosition.BOTTOM,
                    trailingLabel = if (fodSensitivityHigh) "High" else "Standard",
                    subtitle = "Foreign Object Detection anomaly threshold",
                    icon = Icons.Rounded.Security,
                    customHeaderTrailing = {
                        Switch(
                            checked = fodSensitivityHigh,
                            onCheckedChange = { fodSensitivityHigh = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Monitors abrupt power dropouts without temperature rises, which indicates that the Qi transmitter's FOD protection circuit tripped due to magnetic ring flux disruption.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // ==========================================
            // 4. INTERFACE & MEASUREMENT UNITS
            // ==========================================
            SegmentedGroup(
                title = "Interface & Display Formats"
            ) {
                // Tabular numeric figures
                SegmentedTile(
                    title = "Tabular Lining Figures (tnum)",
                    position = SegmentedPosition.TOP,
                    subtitle = if (useTabularFigures) "Fixed numeric width prevents jitter" else "Standard proportional numbers",
                    icon = Icons.Rounded.Numbers,
                    customHeaderTrailing = {
                        Switch(
                            checked = useTabularFigures,
                            onCheckedChange = { useTabularFigures = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Enforces OpenType 'tnum' (tabular numbers) and 'zero' (slashed zero) across all live telemetry gauges so layout boundaries remain rock solid during 1 Hz updates.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Current unit format
                SegmentedTile(
                    title = "Current Display Unit",
                    position = SegmentedPosition.BOTTOM,
                    trailingLabel = if (unitAmperes) "Amperes (A)" else "Milliamperes (mA)",
                    subtitle = "Unit scale for cell inflow and outflow",
                    icon = Icons.Rounded.Tune,
                    customHeaderTrailing = {
                        Switch(
                            checked = unitAmperes,
                            onCheckedChange = { unitAmperes = it },
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    expandedContent = {
                        Text(
                            text = "Toggles between high-resolution milliampere values (e.g. 2,350 mA) and engineering decimal ampere values (e.g. 2.35 A).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            // ==========================================
            // 5. DATA & DIAGNOSTICS ARCHIVE
            // ==========================================
            SegmentedGroup(
                title = "Data & Diagnostic Archive"
            ) {
                SegmentedTile(
                    title = "Export Diagnostic Database",
                    position = SegmentedPosition.TOP,
                    trailingLabel = "CSV / JSON",
                    subtitle = "Export all sessions, samples, and experiments",
                    icon = Icons.Rounded.FileDownload,
                    expandedContent = {
                        Text(
                            text = "Generates ISO 8601 stamped telemetry bundles for external laboratory analysis in Python, pandas, or MATLAB. All data remains 100% on-device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                SegmentedTile(
                    title = "Local-Only Diagnostic Privacy",
                    position = SegmentedPosition.BOTTOM,
                    trailingLabel = "Verified",
                    subtitle = "Zero network permissions (android.permission.INTERNET = None)",
                    icon = Icons.Rounded.Shield,
                    expandedContent = {
                        Text(
                            text = "Charge Science does not hold Android Internet permissions. Telemetry, cycle counts, charger serial numbers, and logs never leave this Pixel 8a.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }

            // ==========================================
            // 6. HARDWARE & FIRMWARE ENVIRONMENT
            // ==========================================
            SegmentedGroup(
                title = "Hardware & Calibration Target"
            ) {
                SegmentedTile(
                    title = "Device Platform",
                    position = SegmentedPosition.TOP,
                    trailingLabel = "Google Pixel 8a",
                    subtitle = "Codename akita • Tensor G3",
                    icon = Icons.Rounded.PhoneAndroid,
                    expandedContent = {
                        Text(
                            text = "Calibrated for Tensor G3 PMIC routing, Actua 120Hz display power draw profiles, and Maxim MAX77759 coulomb-counter registers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                SegmentedTile(
                    title = "Charge Science Engine",
                    position = SegmentedPosition.BOTTOM,
                    trailingLabel = "v2.4-m3e",
                    subtitle = "Material 3 Expressive Diagnostic Instrument",
                    icon = Icons.Rounded.Info,
                    expandedContent = {
                        Text(
                            text = "Built strictly with native Kotlin, Jetpack Compose, Room SQLite, and Android Thermal HAL listeners.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
