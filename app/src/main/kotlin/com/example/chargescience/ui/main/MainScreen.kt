package com.example.chargescience.ui.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chargescience.data.db.AppDatabase
import com.example.chargescience.diagnostics.BatteryTelemetryCollector
import com.example.chargescience.diagnostics.ChargingTelemetryService
import com.example.chargescience.domain.session.ChargingSessionManager
import com.example.chargescience.ui.charger.ChargerIntelligenceScreen
import com.example.chargescience.ui.health.BatteryHealthScreen
import com.example.chargescience.ui.history.ChargingHistoryScreen
import com.example.chargescience.ui.lab.ChargingLabScreen
import com.example.chargescience.ui.overview.OverviewScreen
import com.example.chargescience.ui.wireless.WirelessDiagnosticsScreen
import kotlinx.coroutines.flow.emptyFlow

enum class NavDestination(val label: String) {
    OVERVIEW("Overview"),
    LAB("Lab"),
    HISTORY("History"),
    CHARGER("Charger"),
    WIRELESS("Wireless"),
    HEALTH("Health")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val collector = remember { BatteryTelemetryCollector(context) }
    val sessionManager = remember { ChargingSessionManager(database, collector) }

    val telemetry by collector.telemetry.collectAsStateWithLifecycle()
    val activeSession by sessionManager.activeSessionFlow.collectAsStateWithLifecycle()

    // Observe all historical sessions from Room
    val allSessions by database.chargingSessionDao().getAllSessionsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Observe all saved accessories
    val allAccessories by database.accessoryProfileDao().getAllAccessoriesFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Observe all wireless experiments
    val allExperiments by database.wirelessExperimentDao().getAllExperimentsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // Observe all battery capacity estimates
    val allEstimates by database.batteryCapacityEstimateDao().getAllEstimatesFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // If an active session is running, observe its live samples; otherwise, use latest session samples
    val targetSessionId = activeSession?.id ?: allSessions.firstOrNull()?.id
    val liveSamples by remember(targetSessionId) {
        if (targetSessionId != null) {
            database.telemetrySampleDao().getSamplesForSessionFlow(targetSessionId)
        } else {
            emptyFlow()
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Start / stop Foreground Service based on charging state
    LaunchedEffect(telemetry.isCharging) {
        if (telemetry.isCharging) {
            try {
                ChargingTelemetryService.start(context)
            } catch (_: Exception) {
                // Defensive catch for background start limits
            }
        }
    }

    var selectedDestination by remember { mutableStateOf(NavDestination.OVERVIEW) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Charge Science",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = if (telemetry.isCharging) "Charging • Pixel 8a" else "Battery • Pixel 8a",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (telemetry.isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Main Destination Views
            when (selectedDestination) {
                NavDestination.OVERVIEW -> OverviewScreen(telemetry = telemetry)
                NavDestination.LAB -> ChargingLabScreen(
                    telemetry = telemetry,
                    activeSamples = liveSamples
                )
                NavDestination.WIRELESS -> WirelessDiagnosticsScreen(
                    telemetry = telemetry,
                    activeSamples = liveSamples,
                    experiments = allExperiments,
                    accessories = allAccessories,
                    onSaveExperiment = { exp ->
                        database.wirelessExperimentDao().insertExperiment(exp)
                    }
                )
                NavDestination.HISTORY -> ChargingHistoryScreen(
                    sessions = allSessions,
                    accessories = allAccessories,
                    onLoadSessionSamples = { sessionId ->
                        database.telemetrySampleDao().getSamplesForSession(sessionId)
                    },
                    onLinkAccessoryToSession = { sessionId, accessoryId ->
                        database.chargingSessionDao().linkAccessoryToSession(sessionId, accessoryId)
                    }
                )
                NavDestination.CHARGER -> ChargerIntelligenceScreen(
                    telemetry = telemetry,
                    accessories = allAccessories,
                    onSaveAccessory = { accessory ->
                        database.accessoryProfileDao().insertAccessory(accessory)
                    }
                )
                NavDestination.HEALTH -> BatteryHealthScreen(
                    telemetry = telemetry,
                    estimates = allEstimates,
                    sessions = allSessions
                )
            }

            // Floating Frosted Glass Navigation Pill (4 Categories Max)
            FloatingNavPill(
                selectedDestination = selectedDestination,
                onSelectDestination = { selectedDestination = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

/**
 * Floating Navigation Pill with frosted glass appearance.
 * Features 3 core anchor categories + 1 dynamic companion category.
 */
@Composable
private fun FloatingNavPill(
    selectedDestination: NavDestination,
    onSelectDestination: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine active anchor context
    val currentAnchor = when (selectedDestination) {
        NavDestination.OVERVIEW, NavDestination.CHARGER -> NavDestination.OVERVIEW
        NavDestination.LAB, NavDestination.WIRELESS -> NavDestination.LAB
        NavDestination.HISTORY, NavDestination.HEALTH -> NavDestination.HISTORY
    }

    // Dynamic 4th category derived from active anchor
    val dynamicDestination = when (currentAnchor) {
        NavDestination.OVERVIEW -> NavDestination.CHARGER
        NavDestination.LAB -> NavDestination.WIRELESS
        NavDestination.HISTORY -> NavDestination.HEALTH
        else -> NavDestination.CHARGER
    }

    val primaryAnchors = listOf(
        NavDestination.OVERVIEW,
        NavDestination.LAB,
        NavDestination.HISTORY
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .height(56.dp)
                .fillMaxWidth(0.96f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3 Fixed Primary Anchors
            primaryAnchors.forEach { dest ->
                val isSelected = selectedDestination == dest
                NavPillItem(
                    destination = dest,
                    isSelected = isSelected,
                    onClick = { onSelectDestination(dest) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 4th Dynamic Companion Category
            val isDynamicSelected = selectedDestination == dynamicDestination
            NavPillItem(
                destination = dynamicDestination,
                isSelected = isDynamicSelected,
                isDynamic = true,
                onClick = { onSelectDestination(dynamicDestination) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavPillItem(
    destination: NavDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDynamic: Boolean = false
) {
    val icon = when (destination) {
        NavDestination.OVERVIEW -> Icons.Rounded.ElectricBolt
        NavDestination.LAB -> Icons.AutoMirrored.Rounded.ShowChart
        NavDestination.WIRELESS -> Icons.Rounded.Sensors
        NavDestination.CHARGER -> Icons.Rounded.Power
        NavDestination.HISTORY -> Icons.Rounded.History
        NavDestination.HEALTH -> Icons.Rounded.Favorite
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .animateContentSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = destination.label,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (isDynamic) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else if (isDynamic) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
