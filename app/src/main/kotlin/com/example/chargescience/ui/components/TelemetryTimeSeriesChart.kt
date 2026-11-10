package com.example.chargescience.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.max

/**
 * Pure Jetpack Compose Canvas time-series chart.
 * Renders verified, non-interpolated raw measurement points with scientific axes.
 */
@Composable
fun TelemetryTimeSeriesChart(
    title: String,
    unit: String,
    dataPoints: List<Pair<Long, Float>>, // (timestampMs, value)
    lineColor: Color,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No data samples recorded"
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
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header with current/latest reading
            val latestValue = dataPoints.lastOrNull()?.second
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (latestValue != null) {
                    Text(
                        text = String.format(Locale.US, "%.1f %s", latestValue, unit),
                        style = MaterialTheme.typography.titleMedium,
                        color = lineColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (dataPoints.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                val values = dataPoints.map { it.second }
                val minVal = values.minOrNull() ?: 0f
                val maxVal = values.maxOrNull() ?: 1f
                val range = max(maxVal - minVal, 0.001f)

                val startTimestamp = dataPoints.first().first
                val endTimestamp = dataPoints.last().first
                val timeSpanMs = max(endTimestamp - startTimestamp, 1000L)

                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                Column {
                    // Axis labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "MAX: %.1f %s", maxVal, unit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = String.format(Locale.US, "MIN: %.1f %s", minVal, unit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Hardware accelerated Canvas chart
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        val width = size.width
                        val height = size.height

                        // Horizontal baseline and mid-gridline
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, 0f),
                            end = Offset(width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, height / 2f),
                            end = Offset(width, height / 2f),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, height),
                            end = Offset(width, height),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Path calculation connecting discrete real samples
                        val path = Path()
                        dataPoints.forEachIndexed { index, (timeMs, value) ->
                            val normalizedX = (timeMs - startTimestamp).toFloat() / timeSpanMs * width
                            val normalizedY = height - ((value - minVal) / range * height)

                            if (index == 0) {
                                path.moveTo(normalizedX, normalizedY)
                            } else {
                                path.lineTo(normalizedX, normalizedY)
                            }
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Time axis indicators
                    val elapsedMinutes = (timeSpanMs / 60000L).toInt()
                    val elapsedSeconds = ((timeSpanMs % 60000L) / 1000L).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = String.format(Locale.US, "Duration: %dm %02ds (%d pts)", elapsedMinutes, elapsedSeconds, dataPoints.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
