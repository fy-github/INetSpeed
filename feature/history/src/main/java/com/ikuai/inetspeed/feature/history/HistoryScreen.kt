package com.ikuai.inetspeed.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.designsystem.components.CockpitCurve
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import com.ikuai.inetspeed.core.designsystem.components.CockpitSegmentedControl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val measurements by viewModel.filteredMeasurements.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val filterProtocol by viewModel.filterProtocol.collectAsState()
    val avg = measurements.map { it.throughputMbps }.average().takeIf { !it.isNaN() } ?: 0.0
    val max = measurements.maxOfOrNull { it.throughputMbps } ?: 0.0

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "历史",
                subtitle = "Telemetry Archive · 速度趋势与记录回放",
                status = "${measurements.size} 条",
            )
            CockpitPanel(title = "筛选", overline = "Range") {
                CockpitSegmentedControl(
                    options = HistoryViewModel.TimeRange.entries.map { it.label },
                    selectedIndex = HistoryViewModel.TimeRange.entries.indexOf(timeRange),
                    onSelected = { viewModel.setTimeRange(HistoryViewModel.TimeRange.entries[it]) },
                )
                CockpitSegmentedControl(
                    options = listOf("全部", "TCP", "UDP"),
                    selectedIndex = when (filterProtocol) {
                        "tcp" -> 1
                        "udp" -> 2
                        else -> 0
                    },
                    onSelected = {
                        viewModel.setFilterProtocol(
                            when (it) {
                                1 -> "tcp"
                                2 -> "udp"
                                else -> null
                            },
                        )
                    },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitMetricTile("记录", measurements.size.toString(), Modifier.weight(1f))
                CockpitMetricTile("均值", "${formatNumber(avg)} Mbps", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                CockpitMetricTile("峰值", "${formatNumber(max)} Mbps", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }

            CockpitCurve(
                title = "吞吐量趋势",
                valueLabel = if (measurements.isEmpty()) "等待记录" else "最近 ${measurements.take(24).size} 次测试",
                samples = chartSamples(measurements),
                color = MaterialTheme.colorScheme.primary,
            )

            CockpitPanel(
                modifier = Modifier.weight(1f),
                title = "最近测试",
                overline = "Records",
            ) {
                if (measurements.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(measurements, key = { it.id }) { measurement ->
                            MeasurementRow(
                                measurement = measurement,
                                onClick = { onNavigateToDetail(measurement.id) },
                                onDelete = { viewModel.deleteMeasurement(measurement) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementRow(
    measurement: TestMeasurement,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CockpitDot(if (measurement.protocol == "udp") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${formatNumber(measurement.throughputMbps)} Mbps",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = "${measurement.protocol.uppercase()} · ${measurement.serverName.ifBlank { measurement.serverAddress }} · ${formatTime(measurement.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = measurement.latencyMs?.let { "${it.toInt()}ms" } ?: measurement.direction.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(54.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("暂无测试记录", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("完成测速后记录会自动保存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun chartSamples(measurements: List<TestMeasurement>): List<Float> {
    if (measurements.isEmpty()) return List(18) { 0.25f + (it % 4) * 0.04f }
    return measurements.take(24).map { it.throughputMbps.toFloat().coerceAtLeast(0.1f) }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000}分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000}小时前"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatNumber(value: Double): String = String.format(Locale.US, "%.1f", value)
