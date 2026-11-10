package com.example.chargescience.ui.charger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import com.example.chargescience.domain.protocol.ChargingProtocolClassifier
import com.example.chargescience.model.BatteryTelemetry
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargerIntelligenceScreen(
    telemetry: BatteryTelemetry,
    accessories: List<AccessoryProfileEntity> = emptyList(),
    onSaveAccessory: suspend (AccessoryProfileEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var showAddAccessoryDialog by remember { mutableStateOf(false) }

    val protocolInfo = remember(telemetry.pluggedSource, telemetry.maxChargingVoltageUv, telemetry.maxChargingCurrentUa) {
        ChargingProtocolClassifier.classify(
            chargeSource = telemetry.pluggedSource,
            maxVoltageUv = telemetry.maxChargingVoltageUv,
            maxCurrentUa = telemetry.maxChargingCurrentUa
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACCESSORY & PROTOCOL INTELLIGENCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            FilledTonalButton(
                onClick = { showAddAccessoryDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("+ Add Accessory", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Active Connection & Protocol Analysis Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Negotiated Power Contract",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = if (telemetry.isCharging) "ACTIVE SINK" else "IDLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = protocolInfo.protocolName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = protocolInfo.technicalExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "Evidence: ${protocolInfo.measurementEvidence}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "CONTRACT CEILING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "%.1f W", protocolInfo.negotiatedCeilingWatts),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(text = "VOLTAGE CEILING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${protocolInfo.negotiatedVoltageMv / 1000} V",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column {
                        Text(text = "CURRENT CEILING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${protocolInfo.negotiatedCurrentMa} mA",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Scientific Metric Taxonomy Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Scientific Telemetry Taxonomy",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                TaxonomyRow(
                    badge = "MEASURED",
                    badgeColor = MaterialTheme.colorScheme.primaryContainer,
                    badgeTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "Battery Rail Voltage, Current & Temperature",
                    description = "Directly polled from the Maxim MAX77759 hardware fuel gauge Kelvin shunt and battery NTC thermistor."
                )

                Spacer(modifier = Modifier.height(8.dp))

                TaxonomyRow(
                    badge = "DERIVED",
                    badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                    badgeTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "Net Battery Power & Negotiated Ceiling",
                    description = "P_batt = V_cell × I_batt (deterministic Joules entering cell chemistry). Negotiated ceiling = V_max × I_max from USB-PD profile."
                )

                Spacer(modifier = Modifier.height(8.dp))

                TaxonomyRow(
                    badge = "ESTIMATED",
                    badgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                    badgeTextColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Charger Input Power",
                    description = "Estimated as (P_batt + P_sys) / η_PMIC with documented uncertainty (~15%). Not directly measurable from userspace."
                )

                Spacer(modifier = Modifier.height(8.dp))

                TaxonomyRow(
                    badge = "UNAVAILABLE",
                    badgeColor = MaterialTheme.colorScheme.errorContainer,
                    badgeTextColor = MaterialTheme.colorScheme.onErrorContainer,
                    title = "Cable Resistance, E-Marker & Wall Power",
                    description = "Requires external hardware. Switching buck PMIC decouples battery current from cable current; AC wall power is physically isolated."
                )
            }
        }

        // Cable Intelligence & Physical Impossibility Disclosure
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Cable Resistance & E-Marker Reality",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Why apps cannot measure USB-C cable resistance from battery telemetry:\n\n" +
                            "1. The Switching Buck Disconnect: The phone's PMIC converts 9V Vbus down to ~4V battery rail at ~92% switching efficiency. In this circuit, I_battery ≠ I_cable (e.g. 2A on Vbus becomes ~4.1A into battery cell).\n" +
                            "2. Unknown Input Voltage: True Vbus at the charger output port droops under load and varies by ±5%. The phone only measures internal battery voltage.\n" +
                            "3. Mathematical Indeterminacy: With 4 dynamic unknowns (V_charger, V_bus, PMIC efficiency, system drain) and only 2 battery observables, solving for R_cable is mathematically impossible.\n\n" +
                            "To measure true cable resistance, an inline USB-C power meter (e.g., FNIRSI FNB58 or TC66C via Bluetooth BLE) is required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Wireless Qi Charging Intelligence Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Wireless Charging Intelligence (Pixel 8a)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Hardware Ceiling: 7.5W max reception rate over Qi Extended Power Profile (EPP). Fallback 5W on Baseline Power Profile (BPP).\n" +
                            "• Qi2 Magnetic Power Profile (MPP): Not supported on Pixel 8a hardware. MagSafe-style cases provide mechanical alignment only, not 15W Qi2 signaling.\n" +
                            "• Transmitter Isolation: Wireless charging pad input power cannot be monitored from Android; transmitter gross wall draw is electrically isolated across the inductive air gap.\n" +
                            "• Thermal Signature: Typical thermal rise during wireless charging is 2.5× to 4× higher per Wh delivered than wired USB-PD.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Saved Accessory Profiles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Saved Accessory Profiles (${accessories.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (accessories.isEmpty()) {
                    Text(
                        text = "No custom accessories saved. Add nicknames for your chargers, cables, and wireless pads to track performance across sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    accessories.forEach { accessory ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = accessory.nickname, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = "${accessory.type} • ${if (accessory.claimedWattage > 0f) "${accessory.claimedWattage}W claimed" else "Standard"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (accessory.notes.isNotBlank()) {
                                    Text(text = accessory.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAddAccessoryDialog) {
        AddAccessoryDialog(
            onDismiss = { showAddAccessoryDialog = false },
            onSave = { entity ->
                scope.launch {
                    onSaveAccessory(entity)
                    showAddAccessoryDialog = false
                }
            }
        )
    }
}

@Composable
private fun TaxonomyRow(
    badge: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    badgeTextColor: androidx.compose.ui.graphics.Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddAccessoryDialog(
    onDismiss: () -> Unit,
    onSave: (AccessoryProfileEntity) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccessoryProfileEntity.TYPE_CHARGER) }
    var manufacturer by remember { mutableStateOf("") }
    var claimedWattage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Accessory Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Accessory Nickname (e.g. Anker 30W GaN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AccessoryProfileEntity.TYPE_CHARGER to "Charger",
                        AccessoryProfileEntity.TYPE_CABLE to "Cable",
                        AccessoryProfileEntity.TYPE_WIRELESS_PAD to "Qi Pad"
                    ).forEach { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Manufacturer (e.g. Google, Anker)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = claimedWattage,
                    onValueChange = { claimedWattage = it },
                    label = { Text("Claimed Maximum Wattage (e.g. 30)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Observed Port (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nickname.isNotBlank()) {
                                onSave(
                                    AccessoryProfileEntity(
                                        type = type,
                                        nickname = nickname.trim(),
                                        manufacturer = if (manufacturer.isBlank()) "Unknown" else manufacturer.trim(),
                                        claimedWattage = claimedWattage.toFloatOrNull() ?: 0f,
                                        notes = notes.trim()
                                    )
                                )
                            }
                        },
                        enabled = nickname.isNotBlank()
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}
