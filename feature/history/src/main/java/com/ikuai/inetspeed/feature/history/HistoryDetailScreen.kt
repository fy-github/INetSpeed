package com.ikuai.inetspeed.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryDetailScreen(
    measurementId: Long,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val measurements by viewModel.allMeasurements.collectAsState()
    val measurement = measurements.find { it.id == measurementId }

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            CockpitHeader(title = "测试详情")

            if (measurement != null) {
                CockpitPanel(overline = "测试概览") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CockpitMetricTile("下行", String.format("%.1f Mbps", measurement.throughputMbps), Modifier.weight(1f))
                        CockpitMetricTile("延迟", measurement.latencyMs?.let { "${it.toInt()}ms" } ?: "--", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                        CockpitMetricTile("丢包", measurement.packetLossPercent?.let { "${it.toInt()}%" } ?: "--", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                    }
                }

                CockpitPanel(overline = "服务器信息") {
                    DetailRow("服务器", measurement.serverName)
                    DetailRow("地址", "${measurement.serverAddress}:${measurement.serverPort}")
                    DetailRow("协议", measurement.protocol.uppercase())
                    DetailRow("方向", if (measurement.direction == "FORWARD") "正向" else "反向")
                    DetailRow("IP 版本", measurement.ipVersion)
                }

                CockpitPanel(overline = "测试参数") {
                    DetailRow("时长", "${measurement.durationSeconds}秒")
                    DetailRow("并发流", "${measurement.parallelStreams}")
                    DetailRow("状态", measurement.status)
                    measurement.errorCode?.let { DetailRow("错误码", it) }
                }

                measurement.uploadMbps?.let {
                    CockpitPanel(overline = "上传数据") {
                        DetailRow("上传速率", String.format("%.1f Mbps", it))
                    }
                }

                measurement.jitterMs?.let {
                    CockpitPanel(overline = "UDP 数据") {
                        DetailRow("抖动", String.format("%.2f ms", it))
                        measurement.packetLossPercent?.let { loss -> DetailRow("丢包率", String.format("%.1f%%", loss)) }
                    }
                }

                CockpitPanel(overline = "时间信息") {
                    DetailRow("测试时间", formatDate(measurement.timestamp))
                }
            } else {
                Text(
                    text = "未找到测试记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
