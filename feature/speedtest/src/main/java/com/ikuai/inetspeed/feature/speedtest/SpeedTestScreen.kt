package com.ikuai.inetspeed.feature.speedtest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.error.ErrorCode
import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.feature.speedtest.components.GaugeCanvas
import com.ikuai.inetspeed.feature.speedtest.components.MetricCards
import com.ikuai.inetspeed.feature.speedtest.components.formatSpeed
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestConfig
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestState

@Composable
fun SpeedTestScreen(
    viewModel: SpeedTestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val config by viewModel.config.collectAsState()
    val isExpertMode by viewModel.isExpertMode.collectAsState()
    val serverAddress by viewModel.serverAddress.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val recentServers by viewModel.recentServers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BrandHeader(isExpertMode = isExpertMode)

        Spacer(modifier = Modifier.height(12.dp))

        ServerAddressInput(
            address = serverAddress,
            port = serverPort,
            recentServers = recentServers,
            onAddressChange = { viewModel.updateServerAddress(it) },
            onPortChange = { v ->
                val p = try { v.toInt() } catch (_: Exception) { 5201 }
                viewModel.updateServerPort(p)
            },
            onServerSelect = { viewModel.selectServer(it) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        GaugeCanvas(
            speedMbps = when (val s = state) {
                is SpeedTestState.Running -> s.currentMbps
                is SpeedTestState.Completed -> s.throughputMbps
                else -> 0.0
            },
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${config.direction.toDisplayName()} · ${config.protocol.toDisplayName()} · ${config.ipVersion.toDisplayName()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        AnimatedVisibility(visible = isExpertMode || state is SpeedTestState.Idle) {
            ProtocolDirectionChips(
                protocol = config.protocol,
                direction = config.direction,
                onProtocolChange = { viewModel.updateProtocol(it) },
                onDirectionChange = { viewModel.updateDirection(it) },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilterChip(
                selected = isExpertMode,
                onClick = { viewModel.toggleMode() },
                label = { Text(if (isExpertMode) "专家模式" else "简单模式") },
            )
        }

        AnimatedVisibility(visible = isExpertMode) {
            ExpertParamsPanel(config = config, onConfigChange = { viewModel.updateConfig(it) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is SpeedTestState.Idle -> {
                StartTestButton(onClick = { viewModel.startTest() })
            }

            is SpeedTestState.Preparing -> {
                StatusBlock("准备中...")
            }

            is SpeedTestState.Running -> {
                ProgressIndicator(s.progressPercent, s.elapsedSeconds, config.durationSeconds)
                Spacer(modifier = Modifier.height(8.dp))
                CancelButton(onClick = { viewModel.cancelTest() })
            }

            is SpeedTestState.Cancelling -> {
                StatusBlock("取消中...")
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
                Text(
                    text = "测试已取消",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                StartTestButton(onClick = { viewModel.resetToIdle() })
            }
        }
    }
}

@Composable
private fun BrandHeader(isExpertMode: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("INetSpeed", style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (isExpertMode) "网络测速 · 专家模式" else "网络测速 · 简单模式",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Text(
                text = "在线",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ServerAddressInput(
    address: String,
    port: Int,
    recentServers: List<com.ikuai.inetspeed.core.data.model.Server>,
    onAddressChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onServerSelect: (com.ikuai.inetspeed.core.data.model.Server) -> Unit,
) {
    var showDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("服务器地址", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("输入服务器地址") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "选择服务器",
                            modifier = Modifier.clickable { showDropdown = !showDropdown },
                        )
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = port.toString(),
                    onValueChange = { onPortChange(try { it.toInt() } catch (_: Exception) { 5201 }) },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("端口") },
                )
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                if (recentServers.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("暂无历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = { showDropdown = false },
                    )
                } else {
                    recentServers.take(10).forEach { server ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(server.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${server.address}:${server.port}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                onServerSelect(server)
                                showDropdown = false
                            },
                        )
                    }
                }
            }
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("专家参数", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = config.durationSeconds.toString(),
                onValueChange = { v ->
                    val n = try { v.toInt() } catch (_: Exception) { return@OutlinedTextField }
                    if (n in 1..3600) onConfigChange(config.copy(durationSeconds = n))
                },
                label = { Text("测试时长 (秒)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = config.parallelStreams.toString(),
                onValueChange = { v ->
                    val n = try { v.toInt() } catch (_: Exception) { return@OutlinedTextField }
                    if (n in 1..32) onConfigChange(config.copy(parallelStreams = n))
                },
                label = { Text("并发线程数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = config.udpBandwidth ?: "",
                onValueChange = { onConfigChange(config.copy(udpBandwidth = it.ifBlank { null })) },
                label = { Text("UDP 带宽 (留空为不限)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("如 100M") },
            )

            OutlinedTextField(
                value = config.windowSize ?: "",
                onValueChange = { onConfigChange(config.copy(windowSize = it.ifBlank { null })) },
                label = { Text("窗口大小 (留空为默认)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("如 256K") },
            )

            OutlinedTextField(
                value = config.bufferLength ?: "",
                onValueChange = { onConfigChange(config.copy(bufferLength = it.ifBlank { null })) },
                label = { Text("缓冲区长度 (留空为默认)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("如 128K") },
            )
        }
    }
}

@Composable
private fun ProgressIndicator(percent: Int, elapsed: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(progress = { percent / 100f })
        Text(
            text = "${elapsed}s / ${total}s",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StatusBlock(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator()
        Text(text, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun StartTestButton(onClick: () -> Unit, label: String = "开始测试") {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
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
                text = "${formatSpeed(state.throughputMbps)} Mbps",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ErrorInfo(errorCode: ErrorCode, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(errorCode.userMessage, style = MaterialTheme.typography.bodyMedium)
                if (message.isNotBlank() && message != errorCode.userMessage) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
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
