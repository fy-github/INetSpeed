package com.ikuai.inetspeed.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.feature.history.components.ChartBar
import com.ikuai.inetspeed.feature.history.components.TrendChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val measurements by viewModel.filteredMeasurements.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("历史记录") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // 时间范围选择
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryViewModel.TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = timeRange == range,
                        onClick = { viewModel.setTimeRange(range) },
                        label = { Text(range.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (measurements.isEmpty()) {
                EmptyState()
            } else {
                // 趋势图
                TrendChart(
                    data = buildChartData(measurements),
                    modifier = Modifier.height(160.dp),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 协议筛选
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = viewModel.filterProtocol.collectAsState().value == null,
                        onClick = { viewModel.setFilterProtocol(null) },
                        label = { Text("全部") },
                    )
                    FilterChip(
                        selected = viewModel.filterProtocol.collectAsState().value == "tcp",
                        onClick = { viewModel.setFilterProtocol("tcp") },
                        label = { Text("TCP") },
                    )
                    FilterChip(
                        selected = viewModel.filterProtocol.collectAsState().value == "udp",
                        onClick = { viewModel.setFilterProtocol("udp") },
                        label = { Text("UDP") },
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "最近测试",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(measurements, key = { it.id }) { measurement ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteMeasurement(measurement)
                                    true
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                        ) {
                            MeasurementCard(
                                measurement = measurement,
                                onClick = { onNavigateToDetail(measurement.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementCard(
    measurement: TestMeasurement,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 方向图标
            Box(modifier = Modifier.padding(end = 10.dp)) {
                Text(
                    text = if (measurement.direction == "reverse") "↑" else "↓",
                    fontSize = 18.sp,
                    color = if (measurement.direction == "reverse") {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%.1f Mbps", measurement.throughputMbps),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = "${measurement.protocol.uppercase()} · ${measurement.serverName} · ${formatTime(measurement.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (measurement.latencyMs != null) {
                Text(
                    text = "${measurement.latencyMs.toInt()}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("暂无测试记录", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("完成测速后记录会自动保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun buildChartData(measurements: List<TestMeasurement>): List<ChartBar> {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val grouped = measurements.groupBy { dateFormat.format(Date(it.timestamp)) }
    return grouped.entries.takeLast(7).map { (date, records) ->
        val avgMbps = records.map { it.throughputMbps }.average()
        ChartBar(label = date, value = avgMbps)
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3600_000 -> "${diff / 60_000}分钟前"
        diff < 86400_000 -> "${diff / 3600_000}小时前"
        else -> SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
