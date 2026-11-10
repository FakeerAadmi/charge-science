package com.example.chargescience.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chargescience.data.db.AppDatabase
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
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
    WIRELESS("Wireless"),
    CHARGER("Charger"),
    HISTORY("History"),
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
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = if (telemetry.isCharging) "Charging • Pixel 8a" else "Battery • Pixel 8a",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (telemetry.isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { selectedDestination = destination },
                        label = {
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            val symbol = when (destination) {
                                NavDestination.OVERVIEW -> "⚡"
                                NavDestination.LAB -> "📈"
                                NavDestination.WIRELESS -> "◎"
                                NavDestination.CHARGER -> "🔌"
                                NavDestination.HISTORY -> "⏱"
                                NavDestination.HEALTH -> "♥"
                            }
                            Text(
                                text = symbol,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selectedDestination == destination) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
        }
    }
}
