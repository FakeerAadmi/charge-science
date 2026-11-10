package com.example.chargescience.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HardwareSpecsCard(
    cycleCount: Int,
    chargeCounterUah: Long,
    thermalStatus: Int,
    thermalHeadroom: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "HARDWARE & THERMAL TELEMETRY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CYCLE COUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (cycleCount >= 0) "$cycleCount cycles" else "Unavailable",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "DESIGN CAPACITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "4,492 mAh",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "COULOMB COUNTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (chargeCounterUah > 0) "${chargeCounterUah / 1000} mAh" else "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val thermalDesc = when (thermalStatus) {
                0 -> "None (Optimal Thermal Ceiling)"
                1 -> "Light (UX Normal)"
                2 -> "Moderate (Charge Current Throttled)"
                3 -> "Severe (Heavy Thermal Throttle)"
                4 -> "Critical (Emergency Safety Guard)"
                else -> "State $thermalStatus"
            }

            Text(
                text = "Thermal Throttle State: $thermalDesc",
                style = MaterialTheme.typography.bodySmall,
                color = if (thermalStatus >= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        }
    }
}
