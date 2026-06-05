package com.ikuai.inetspeed.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CockpitScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gridColor = if (dark) Color.White.copy(alpha = 0.045f) else Color(0xFF0F3148).copy(alpha = 0.07f)
    val scanColor = if (dark) Color.White.copy(alpha = 0.033f) else Color(0xFF0F3148).copy(alpha = 0.04f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (dark) {
                        listOf(Color(0xFF121925), Color(0xFF070A0F))
                    } else {
                        listOf(Color(0xFFF8FBFD), Color(0xFFE6F0F5))
                    },
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = if (dark) Color(0xFF54E6FF).copy(alpha = 0.035f) else Color(0xFF0084A0).copy(alpha = 0.035f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.5f, size.height * 0.24f),
            )
            val grid = 22.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += grid
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += grid
            }
            val scan = 7.dp.toPx()
            y = 0f
            while (y < size.height) {
                drawLine(scanColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += scan
            }
        }
        content()
    }
}

@Composable
fun CockpitPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    overline: String? = null,
    content: @Composable () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val panelColor = if (dark) Color.White.copy(alpha = 0.052f) else Color.White.copy(alpha = 0.75f)
    val insetColor = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.70f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                RoundedCornerShape(8.dp),
            )
            .background(panelColor, RoundedCornerShape(8.dp)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(insetColor, Offset(1f, 1f), Offset(size.width - 1f, 1f), strokeWidth = 1f)
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (title != null || overline != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        overline?.let {
                            Text(
                                text = it.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        title?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun CockpitHeader(
    title: String,
    subtitle: String,
    status: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CockpitStatusPill(status)
            action?.invoke()
        }
    }
}

@Composable
fun CockpitStatusPill(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)),
                RoundedCornerShape(8.dp),
            )
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
fun CockpitMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(
        modifier = modifier
            .heightIn(min = 57.dp)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
            .background(if (dark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CockpitActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val fillBrush = if (destructive) {
        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error))
    } else {
        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(
                if (enabled) fillBrush else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)),
                RoundedCornerShape(8.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (destructive || enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
fun CockpitSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val trackColor = if (dark) Color.White.copy(alpha = 0.045f) else Color.White.copy(alpha = 0.70f)
    val inactiveText = if (dark) Color(0xFFBDD0D8) else Color(0xFF263B49)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
            .background(trackColor, RoundedCornerShape(8.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(6.dp),
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (selected) Color.Transparent else Color.Transparent,
                        ),
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onSelected(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else inactiveText,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun CockpitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
    trailing: @Composable (() -> Unit)? = null,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        label?.let {
            Text(
                text = it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 39.dp)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
                        .background(if (dark) Color(0xFF02080D).copy(alpha = 0.46f) else Color.White.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                            )
                        }
                        innerTextField()
                    }
                    trailing?.invoke()
                }
            },
        )
    }
}

@Composable
fun CockpitCurve(
    title: String,
    valueLabel: String,
    samples: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 118.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    CockpitPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
        ProgressLine(samples = samples, color = color)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            if (samples.size < 2) return@Canvas
            val max = samples.maxOrNull()?.coerceAtLeast(1f) ?: 1f
            val step = size.width / (samples.size - 1)
            val points = samples.mapIndexed { index, sample ->
                Offset(index * step, size.height - (sample / max).coerceIn(0f, 1f) * size.height)
            }
            val path = Path()
            val fill = Path()
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                    fill.moveTo(point.x, point.y)
                } else {
                    val previous = points[index - 1]
                    val c1 = Offset(previous.x + step * 0.42f, previous.y)
                    val c2 = Offset(point.x - step * 0.42f, point.y)
                    path.cubicTo(c1.x, c1.y, c2.x, c2.y, point.x, point.y)
                    fill.cubicTo(c1.x, c1.y, c2.x, c2.y, point.x, point.y)
                }
            }
            fill.lineTo(size.width, size.height)
            fill.lineTo(0f, size.height)
            fill.close()
            drawPath(fill, color.copy(alpha = if (height > 40.dp) 0.12f else 0.07f))
            drawPath(path, color.copy(alpha = 0.18f), style = Stroke(width = if (height > 40.dp) 9.dp.toPx() else 6.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, color.copy(alpha = 0.96f), style = Stroke(width = if (height > 40.dp) 3.dp.toPx() else 2.4.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun ProgressLine(samples: List<Float>, color: Color) {
    val max = samples.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val percent = (samples.lastOrNull() ?: 0f) / max
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color.White.copy(alpha = 0.09f), RoundedCornerShape(999.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(percent.coerceIn(0.08f, 1f))
                .height(6.dp)
                .background(
                    Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, Color(0xFF9589FF))),
                    RoundedCornerShape(999.dp),
                ),
        )
    }
}

@Composable
fun CockpitKeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CockpitDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, RoundedCornerShape(999.dp)),
    )
}

@Composable
fun CockpitBottomNav(
    destinations: List<TopLevelDestination>,
    selectedRoute: String?,
    onDestinationClick: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navBackground = if (dark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.75f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 13.dp, vertical = 8.dp)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)), RoundedCornerShape(8.dp))
            .background(navBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = selectedRoute == destination.route
                CockpitNavItem(
                    icon = if (selected) destination.selectedIcon else destination.unselectedIcon,
                    label = destination.label,
                    selected = selected,
                    onClick = { onDestinationClick(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CockpitNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
