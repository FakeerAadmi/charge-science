package com.example.chargescience.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Segmented position within a connected container group.
 * Matches the reference design:
 * - TOP: Curved top (26dp), squared-off bottom (6dp)
 * - MIDDLE: Squared-off on all corners (6dp)
 * - BOTTOM: Squared-off top (6dp), curved bottom (26dp)
 * - SINGLE: Curved on all corners (26dp)
 */
enum class SegmentedPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}

fun getSegmentedShape(
    position: SegmentedPosition,
    outerRadius: Dp = 26.dp,
    innerRadius: Dp = 6.dp
): Shape {
    return when (position) {
        SegmentedPosition.TOP -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        SegmentedPosition.MIDDLE -> RoundedCornerShape(innerRadius)
        SegmentedPosition.BOTTOM -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius
        )
        SegmentedPosition.SINGLE -> RoundedCornerShape(outerRadius)
    }
}

@Composable
fun SegmentedGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp) // Tight 4dp gap matching reference photo
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
            )
        }
        content()
    }
}

@Composable
fun SegmentedTile(
    title: String,
    modifier: Modifier = Modifier,
    position: SegmentedPosition = SegmentedPosition.SINGLE,
    trailingLabel: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    summary: String? = null,
    initialExpanded: Boolean = false,
    customHeaderTrailing: (@Composable RowScope.() -> Unit)? = null,
    expandedContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    val isExpandable = expandedContent != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isExpandable) {
                    Modifier.clickable { expanded = !expanded }
                } else Modifier
            ),
        shape = getSegmentedShape(position),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(220))
        ) {
            // Header Row: Title on Left, Trailing metadata on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (customHeaderTrailing != null) {
                    customHeaderTrailing()
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (trailingLabel != null) {
                            Text(
                                text = trailingLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isExpandable) {
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Subtitle
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Summary Text
            if (summary != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }

            // Progressive disclosure
            if (isExpandable && expanded) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                expandedContent?.invoke(this)
            }
        }
    }
}
