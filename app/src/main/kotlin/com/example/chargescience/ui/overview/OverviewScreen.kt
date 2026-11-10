package com.example.chargescience.ui.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.chargescience.model.BatteryTelemetry
import com.example.chargescience.ui.components.ContractProfileCard
import com.example.chargescience.ui.components.HardwareSpecsCard
import com.example.chargescience.ui.components.HeroWattageCard
import com.example.chargescience.ui.components.TelemetryGaugeCard
import java.util.Locale

@Composable
fun OverviewScreen(
    telemetry: BatteryTelemetry,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Wattage Readout
        HeroWattageCard(
            wattage = telemetry.wattage,
            isCharging = telemetry.isCharging,
            protocolName = telemetry.protocolName,
            negotiatedMaxWatts = telemetry.negotiatedMaxPowerWatts
        )

        // Quad Telemetry Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryGaugeCard(
                label = "Voltage",
                value = String.format(Locale.US, "%,d", telemetry.voltageMv),
                unit = "mV",
                subText = "Terminal Cell Potential",
                modifier = Modifier.weight(1f)
            )

            val currentSign = if (telemetry.currentMa > 0) "+" else ""
            TelemetryGaugeCard(
                label = "Current",
                value = "$currentSign${String.format(Locale.US, "%,d", telemetry.currentMa)}",
                unit = "mA",
                subText = if (telemetry.isCharging) "Inflow to Anode" else "System Drain",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TelemetryGaugeCard(
                label = "Temperature",
                value = String.format(Locale.US, "%.1f", telemetry.temperatureCelsius),
                unit = "°C",
                subText = "Battery Pouch NTC",
                modifier = Modifier.weight(1f)
            )

            TelemetryGaugeCard(
                label = "Level",
                value = "${telemetry.level}",
                unit = "%",
                subText = "State of Charge",
                modifier = Modifier.weight(1f)
            )
        }

        // Active Power Delivery Contract
        ContractProfileCard(
            protocolName = telemetry.protocolName,
            maxVoltageUv = telemetry.maxChargingVoltageUv,
            maxCurrentUa = telemetry.maxChargingCurrentUa,
            negotiatedMaxWatts = telemetry.negotiatedMaxPowerWatts,
            isCharging = telemetry.isCharging
        )

        // Hardware Fuel Gauge & Thermal Throttle Status
        HardwareSpecsCard(
            cycleCount = telemetry.cycleCount,
            chargeCounterUah = telemetry.chargeCounterUah,
            thermalStatus = telemetry.thermalStatus,
            thermalHeadroom = telemetry.thermalHeadroom
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
