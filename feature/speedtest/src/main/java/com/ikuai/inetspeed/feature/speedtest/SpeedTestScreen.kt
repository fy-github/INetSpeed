package com.ikuai.inetspeed.feature.speedtest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.feature.speedtest.components.GaugeCanvas
import com.ikuai.inetspeed.feature.speedtest.components.MetricCards
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestState

@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val config by viewModel.config.collectAsState()
    val isExpertMode by viewModel.isExpertMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 服务器选择（简化版）
        ServerSelector(
            serverName = "hk-iperf.ikuai.com:5201",
            isRecommended = true,
            onClick = { /* TODO: 跳转服务器选择 */ },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 仪表盘
        GaugeCanvas(
            speedMbps = when (val s = state) {
                is SpeedTestState.Running -> s.currentMbps
                is SpeedTestState.Completed -> s.throughputMbps
                else -> 0.0
            },
            modifier = Modifier.height(160.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 协议/方向显示
        Text(
            text = "${config.direction.toDisplayName()} · ${config.protocol.toDisplayName()} · ${config.ipVersion.toDisplayName()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 指标卡片
        MetricCards(
            latencyMs = when (val s = state) {
                is SpeedTestState.Running -> s.latencyMs
                is SpeedTestState.Completed -> s.latencyMs
                else -> null
            },
            jitterMs = when (val s = state) {
                is SpeedTestState.Running -> s.jitterMs
                is SpeedTestState.Completed -> s.jitterMs
                else -> null
            },
            packetLossPercent = when (val s = state) {
                is SpeedTestState.Running -> s.packetLossPercent
                is SpeedTestState.Completed -> s.packetLossPercent
                else -> null
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 协议/方向切换（简单模式隐藏，或测试未运行时显示）
        AnimatedVisibility(visible = isExpertMode || state is SpeedTestState.Idle) {
            ProtocolDirectionChips(
                protocol = config.protocol,
                direction = config.direction,
                onProtocolChange = { viewModel.updateProtocol(it) },
                onDirectionChange = { viewModel.updateDirection(it) },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 专家模式切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            FilterChip(
                selected = isExpertMode,
                onClick = { viewModel.toggleMode() },
                label = { Text(if (isExpertMode) "专家模式" else "简单模式") },
            )
        }

        // 专家模式参数面板
        AnimatedVisibility(visible = isExpertMode) {
            ExpertParamsPanel(
                config = config,
                onConfigChange = { viewModel.updateConfig(it) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 状态相关 UI
        when (val s = state) {
            is SpeedTestState.Idle -> {
                StartTestButton(onClick = { viewModel.startTest("hk-iperf.ikuai.com", 5201, "香港节点") })
            }
            is SpeedTestState.Preparing -> {
                CircularProgressIndicator()
                Text("准备中...", modifier = Modifier.padding(top = 8.dp))
            }
            is SpeedTestState.Running -> {
                ProgressIndicator(s.progressPercent, s.elapsedSeconds, config.durationSeconds)
                Spacer(modifier = Modifier.height(8.dp))
                CancelButton(onClick = { viewModel.cancelTest() })
            }
            is SpeedTestState.Cancelling -> {
                CircularProgressIndicator()
                Text("取消中...", modifier = Modifier.padding(top = 8.dp))
            }
            is SpeedTestState.Completed -> {
                CompletedInfo(s)
                Spacer(modifier = Modifier.height(8.dp))
                StartTestButton(onClick = { viewModel.resetToIdle() }, label = "重新测试")
            }
            is SpeedTestState.Failed -> {
                ErrorInfo(s.errorCode, s.message)
                Spacer(modifier = Modifier.height(8.dp))
                StartTestButton(onClick = { viewModel.resetToIdle() }, label = "重试")
            }
            is SpeedTestState.Cancelled -> {
                Text("测试已取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                StartTestButton(onClick = { viewModel.resetToIdle() })
            }
        }
    }
}

@Composable
private fun ServerSelector(
    serverName: String,
    isRecommended: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp),
            ) {
                Text("●", color = MaterialTheme.colorScheme.tertiary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(serverName, style = MaterialTheme.typography.bodyMedium)
                if (isRecommended) {
                    Text("推荐", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProtocolDirectionChips(
    protocol: Protocol,
    direction: Direction,
    onProtocolChange: (Protocol) -> Unit,
    onDirectionChange: (Direction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Protocol.entries.forEach { p ->
            FilterChip(
                selected = protocol == p,
                onClick = { onProtocolChange(p) },
                label = { Text(p.toDisplayName()) },
            )
        }
        Direction.entries.forEach { d ->
            FilterChip(
                selected = direction == d,
                onClick = { onDirectionChange(d) },
                label = { Text(d.toDisplayName()) },
            )
        }
    }
}

@Composable
private fun ExpertParamsPanel(
    config: SpeedTestConfig,
    onConfigChange: (SpeedTestConfig) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("专家参数", style = DurationTextStyle, modifier = Modifier.padding(bottom = 8.dp))
            Text("测试时长: ${config.durationSeconds}s", style = MaterialTheme.typography.bodySmall)
            Text("并发线程: ${config.parallelStreams}", style = MaterialTheme.typography.bodySmall)
            Text("UDP 带宽: ${config.udpBandwidth ?: "不限"}", style = MaterialTheme.typography.bodySmall)
            Text("窗口大小: ${config.windowSize ?: "默认"}", style = MaterialTheme.typography.bodySmall)
            Text("缓冲区: ${config.bufferLength ?: "默认"}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val DurationTextStyle = MaterialTheme.typography.titleSmall

@Composable
private fun ProgressIndicator(percent: Int, elapsed: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(progress = { percent / 100f })
        Text(
            text = "${elapsed}s / ${total}s",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StartTestButton(onClick: () -> Unit, label: String = "开始测试") {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(label, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text("取消测试", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun CompletedInfo(state: SpeedTestState.Completed) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("测试完成", style = MaterialTheme.typography.titleMedium)
            Text(
                text = String.format("%.1f Mbps", state.throughputMbps),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ErrorInfo(errorCode: com.ikuai.inetspeed.core.data.error.ErrorCode, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(errorCode.userMessage, style = MaterialTheme.typography.bodyMedium)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

private fun Protocol.toDisplayName(): String = when (this) {
    Protocol.TCP -> "TCP"
    Protocol.UDP -> "UDP"
    Protocol.SCTP -> "SCTP"
}

private fun Direction.toDisplayName(): String = when (this) {
    Direction.FORWARD -> "正向"
    Direction.REVERSE -> "反向"
}

private fun com.ikuai.inetspeed.core.data.model.IpVersion.toDisplayName(): String = when (this) {
    com.ikuai.inetspeed.core.data.model.IpVersion.IPV4 -> "IPv4"
    com.ikuai.inetspeed.core.data.model.IpVersion.IPV6 -> "IPv6"
}
