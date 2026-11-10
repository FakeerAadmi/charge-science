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
import com.example.chargescience.diagnostics.BatteryTelemetryCollector
import com.example.chargescience.ui.charger.ChargerIntelligenceScreen
import com.example.chargescience.ui.health.BatteryHealthScreen
import com.example.chargescience.ui.lab.ChargingLabScreen
import com.example.chargescience.ui.overview.OverviewScreen

enum class NavDestination(val label: String) {
    OVERVIEW("Overview"),
    LAB("Lab"),
    CHARGER("Charger"),
    HEALTH("Health")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val collector = remember { BatteryTelemetryCollector(context) }
    val telemetry by collector.telemetry.collectAsStateWithLifecycle()

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
                            text = "Pixel 8a",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            // Clean typography indicator
                            Text(
                                text = destination.label.take(1),
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
                NavDestination.LAB -> ChargingLabScreen(telemetry = telemetry)
                NavDestination.CHARGER -> ChargerIntelligenceScreen(telemetry = telemetry)
                NavDestination.HEALTH -> BatteryHealthScreen(telemetry = telemetry)
            }
        }
    }
}
