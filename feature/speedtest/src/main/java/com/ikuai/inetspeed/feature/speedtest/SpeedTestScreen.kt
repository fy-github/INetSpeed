package com.ikuai.inetspeed.feature.speedtest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.IpVersion
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.designsystem.components.CockpitActionButton
import com.ikuai.inetspeed.core.designsystem.components.CockpitCurve
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitKeyValueRow
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import com.ikuai.inetspeed.core.designsystem.components.CockpitSegmentedControl
import com.ikuai.inetspeed.core.designsystem.components.CockpitStatusPill
import com.ikuai.inetspeed.core.designsystem.components.CockpitTextField
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestConfig
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestState
import java.util.Locale
import kotlin.math.sin

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
    val cliOutput by viewModel.cliOutput.collectAsState()
    var expertInputMode by remember { mutableStateOf(ExpertInputMode.CUSTOM) }
    var cliBuffer by remember(serverAddress, serverPort) {
        mutableStateOf("iperf3 -c $serverAddress -p $serverPort -t ${config.durationSeconds} -i 1")
    }

    LaunchedEffect(cliOutput) {
        if (cliOutput.isNotBlank()) cliBuffer = cliOutput
    }

    val active = state.isActive()
    val realtimeModel = SpeedTestUiRules.realtimeModel(config.protocol, state)

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "INetSpeed",
                subtitle = if (isExpertMode) "专业网络测试 · 参数会话" else "快速网络测试 · 实时遥测",
                status = state.statusText(),
            )

            CockpitSegmentedControl(
                options = listOf("简单模式", "专业模式"),
                selectedIndex = if (isExpertMode) 1 else 0,
                onSelected = { index ->
                    if ((index == 1) != isExpertMode) viewModel.toggleMode()
                },
            )

            if (isExpertMode) {
                ExpertModeContent(
                    state = state,
                    config = config,
                    inputMode = expertInputMode,
                    cliBuffer = cliBuffer,
                    serverAddress = serverAddress,
                    serverPort = serverPort,
                    recentServers = recentServers,
                    onInputModeChange = { expertInputMode = it },
                    onCliBufferChange = { if (!active) cliBuffer = it },
                    onConfigChange = viewModel::updateConfig,
                    onProtocolChange = viewModel::updateProtocol,
                    onDirectionChange = viewModel::updateDirection,
                    onAddressChange = viewModel::updateServerAddress,
                    onPortChange = viewModel::updateServerPort,
                    onServerSelect = viewModel::selectServer,
                    onAction = {
                        if (active) {
                            viewModel.cancelTest()
                        } else if (expertInputMode == ExpertInputMode.CLI) {
                            viewModel.startCliCommand(cliBuffer)
                        } else {
                            viewModel.startTest()
                        }
                    },
                )
            } else {
                SimpleModeContent(
                    state = state,
                    config = config,
                    model = realtimeModel,
                    serverAddress = serverAddress,
                    serverPort = serverPort,
                    recentServers = recentServers,
                    onProtocolChange = viewModel::updateProtocol,
                    onAddressChange = viewModel::updateServerAddress,
                    onPortChange = viewModel::updateServerPort,
                    onServerSelect = viewModel::selectServer,
                    onAction = {
                        if (active) viewModel.cancelTest() else viewModel.startTest()
                    },
                )
            }
        }
    }
}

@Composable
private fun SimpleModeContent(
    state: SpeedTestState,
    config: SpeedTestConfig,
    model: RealtimeTestModel,
    serverAddress: String,
    serverPort: Int,
    recentServers: List<Server>,
    onProtocolChange: (Protocol) -> Unit,
    onAddressChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onServerSelect: (Server) -> Unit,
    onAction: () -> Unit,
) {
    ServerAddressPanel(serverAddress, serverPort, recentServers, onAddressChange, onPortChange, onServerSelect)
    ProtocolControl(config.protocol, onProtocolChange)
    CockpitActionButton(
        text = primaryActionLabel(state, model.actionLabel),
        onClick = onAction,
        enabled = serverAddress.isNotBlank(),
        destructive = state.isActive(),
    )
    RealtimeMetrics(protocol = config.protocol, state = state, progressPercent = model.progressPercent)
    CurveStack(protocol = config.protocol, state = state, curves = model.curves, hasLiveData = model.hasLiveData)
    StateMessage(state)
}

@Composable
private fun ExpertModeContent(
    state: SpeedTestState,
    config: SpeedTestConfig,
    inputMode: ExpertInputMode,
    cliBuffer: String,
    serverAddress: String,
    serverPort: Int,
    recentServers: List<Server>,
    onInputModeChange: (ExpertInputMode) -> Unit,
    onCliBufferChange: (String) -> Unit,
    onConfigChange: (SpeedTestConfig) -> Unit,
    onProtocolChange: (Protocol) -> Unit,
    onDirectionChange: (Direction) -> Unit,
    onAddressChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onServerSelect: (Server) -> Unit,
    onAction: () -> Unit,
) {
    val panels = SpeedTestUiRules.expertPanels(inputMode, config.protocol)
    CockpitSegmentedControl(
        options = listOf("CUSTOM", "CLI"),
        selectedIndex = if (inputMode == ExpertInputMode.CUSTOM) 0 else 1,
        onSelected = { onInputModeChange(if (it == 0) ExpertInputMode.CUSTOM else ExpertInputMode.CLI) },
    )

    if (panels.showCli) {
        CliConsolePanel(
            value = cliBuffer,
            onValueChange = onCliBufferChange,
            running = state.isActive(),
        )
        CockpitActionButton(
            text = if (state.isActive()) "停止测试" else "启动会话",
            onClick = onAction,
            enabled = cliBuffer.isNotBlank(),
            destructive = state.isActive(),
        )
        StateMessage(state)
        return
    }

    if (panels.showServerAddress) {
        ServerAddressPanel(serverAddress, serverPort, recentServers, onAddressChange, onPortChange, onServerSelect)
    }
    if (panels.showParameters) {
        ExpertParamsPanel(
            config = config,
            onConfigChange = onConfigChange,
            onProtocolChange = onProtocolChange,
            onDirectionChange = onDirectionChange,
        )
    }
    CockpitActionButton(
        text = if (state.isActive()) "停止测试" else "启动会话",
        onClick = onAction,
        enabled = serverAddress.isNotBlank(),
        destructive = state.isActive(),
    )
    RealtimeMetrics(protocol = config.protocol, state = state, progressPercent = SpeedTestUiRules.realtimeModel(config.protocol, state).progressPercent)
    CurveStack(protocol = config.protocol, state = state, curves = panels.curves, hasLiveData = SpeedTestUiRules.realtimeModel(config.protocol, state).hasLiveData)
    StateMessage(state)
}

@Composable
private fun ServerAddressPanel(
    address: String,
    port: Int,
    recentServers: List<Server>,
    onAddressChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onServerSelect: (Server) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuColor = MaterialTheme.colorScheme.surface
    CockpitPanel(title = "服务器地址") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CockpitTextField(
                        value = address,
                        onValueChange = onAddressChange,
                        modifier = Modifier.weight(1f),
                        label = null,
                        placeholder = "iperf3 server",
                        fieldHeight = 48.dp,
                    )
                    Box {
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)), RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .clickable { expanded = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) {
                            Text(
                                text = "选择",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            containerColor = menuColor,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            properties = PopupProperties(focusable = true),
                            modifier = Modifier
                                .background(menuColor, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)), RoundedCornerShape(8.dp)),
                        ) {
                            if (recentServers.isEmpty()) {
                                DropdownMenuItem(text = { Text("暂无服务器记录") }, onClick = { expanded = false })
                            } else {
                                recentServers.take(8).forEach { server ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                                CockpitDot(MaterialTheme.colorScheme.primary)
                                                Column {
                                                    Text(
                                                        server.name.ifBlank { server.address },
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        "${server.address}:${server.port}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            onServerSelect(server)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            CockpitTextField(
                value = port.toString(),
                onValueChange = { onPortChange(it.toIntOrNull() ?: port) },
                modifier = Modifier.width(78.dp),
                label = null,
                placeholder = "5201",
                fieldHeight = 48.dp,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@Composable
private fun ProtocolControl(
    protocol: Protocol,
    onProtocolChange: (Protocol) -> Unit,
) {
    CockpitSegmentedControl(
        options = listOf("TCP", "UDP"),
        selectedIndex = if (protocol == Protocol.UDP) 1 else 0,
        onSelected = { onProtocolChange(if (it == 0) Protocol.TCP else Protocol.UDP) },
    )
}

@Composable
private fun ExpertParamsPanel(
    config: SpeedTestConfig,
    onConfigChange: (SpeedTestConfig) -> Unit,
    onProtocolChange: (Protocol) -> Unit,
    onDirectionChange: (Direction) -> Unit,
) {
    CockpitPanel(title = "专业参数", overline = "Custom Session") {
        CockpitSegmentedControl(
            options = listOf("TCP", "UDP"),
            selectedIndex = if (config.protocol == Protocol.UDP) 1 else 0,
            onSelected = { onProtocolChange(if (it == 0) Protocol.TCP else Protocol.UDP) },
        )
        CockpitSegmentedControl(
            options = listOf("正向", "反向"),
            selectedIndex = if (config.direction == Direction.FORWARD) 0 else 1,
            onSelected = { onDirectionChange(if (it == 0) Direction.FORWARD else Direction.REVERSE) },
        )
        CockpitSegmentedControl(
            options = listOf("IPv4", "IPv6"),
            selectedIndex = if (config.ipVersion == IpVersion.IPV4) 0 else 1,
            onSelected = { index ->
                onConfigChange(config.copy(ipVersion = if (index == 0) IpVersion.IPV4 else IpVersion.IPV6))
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumericField(
                label = "时长(s)",
                value = config.durationSeconds.toString(),
                modifier = Modifier.weight(1f),
                onValueChange = { it.toIntOrNull()?.takeIf { n -> n in 1..3600 }?.let { n -> onConfigChange(config.copy(durationSeconds = n)) } },
            )
            NumericField(
                label = "并发",
                value = config.parallelStreams.toString(),
                modifier = Modifier.weight(1f),
                onValueChange = { it.toIntOrNull()?.takeIf { n -> n in 1..32 }?.let { n -> onConfigChange(config.copy(parallelStreams = n)) } },
            )
        }
        CockpitTextField(
            value = config.udpBandwidth.orEmpty(),
            onValueChange = { onConfigChange(config.copy(udpBandwidth = it.ifBlank { null })) },
            modifier = Modifier.fillMaxWidth(),
            label = "UDP 带宽",
            placeholder = "100M",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CockpitTextField(
                value = config.windowSize.orEmpty(),
                onValueChange = { onConfigChange(config.copy(windowSize = it.ifBlank { null })) },
                modifier = Modifier.weight(1f),
                label = "窗口",
                placeholder = "256K",
            )
            CockpitTextField(
                value = config.bufferLength.orEmpty(),
                onValueChange = { onConfigChange(config.copy(bufferLength = it.ifBlank { null })) },
                modifier = Modifier.weight(1f),
                label = "缓冲",
                placeholder = "128K",
            )
        }
    }
}

@Composable
private fun NumericField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    CockpitTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@Composable
private fun CliConsolePanel(
    value: String,
    onValueChange: (String) -> Unit,
    running: Boolean,
) {
    CockpitPanel(title = "输入 iperf3 命令") {
        CockpitTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            label = null,
            placeholder = "iperf3 -c 10.10.8.12 -p 5201 -t 30 -i 1",
            singleLine = false,
            minLines = 10,
            maxLines = 16,
        )
    }
}

@Composable
private fun RealtimeMetrics(
    protocol: Protocol,
    state: SpeedTestState,
    progressPercent: Int,
) {
    val throughput = throughputOf(state)
    CockpitPanel(title = "实时数据", overline = protocol.name) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CockpitMetricTile("带宽", "${formatNumber(throughput)} Mbps", modifier = Modifier.weight(1f))
            CockpitMetricTile("进度", "$progressPercent%", modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
            if (protocol == Protocol.TCP) {
                CockpitMetricTile("状态", state.shortText(), modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.tertiary)
            }
        }
        if (protocol == Protocol.UDP) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitMetricTile("时延", latencyOf(state)?.let { "${formatNumber(it)} ms" } ?: "--", modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.tertiary)
                CockpitMetricTile("抖动", jitterOf(state)?.let { "${formatNumber(it)} ms" } ?: "--", modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
                CockpitMetricTile("丢包", lossOf(state)?.let { "${formatNumber(it)}%" } ?: "--", modifier = Modifier.weight(1f), accent = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CurveStack(
    protocol: Protocol,
    state: SpeedTestState,
    curves: List<TestCurve>,
    hasLiveData: Boolean,
) {
    curves.forEach { curve ->
        val value = when (curve) {
            TestCurve.THROUGHPUT -> throughputOf(state)
            TestCurve.LATENCY -> latencyOf(state) ?: 0.0
            TestCurve.JITTER -> jitterOf(state) ?: 0.0
            TestCurve.PACKET_LOSS -> lossOf(state) ?: 0.0
        }
        CockpitCurve(
            title = curve.title(protocol),
            valueLabel = curve.valueLabel(value, hasLiveData),
            samples = curveSamples(value, state.progressForSamples(), hasLiveData),
            color = curve.color(),
            height = if (curve == TestCurve.THROUGHPUT) 78.dp else 48.dp,
            yAxisUnit = curve.unit(),
        )
    }
}

@Composable
private fun StateMessage(state: SpeedTestState) {
    when (state) {
        is SpeedTestState.Failed -> {
            CockpitPanel(title = "错误", overline = state.errorCode.code) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        SpeedTestState.Cancelled -> {
            CockpitPanel(title = "会话已停止", overline = "Cancelled") {
                Text("测试已取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        is SpeedTestState.Completed -> {
            CockpitPanel(title = "会话完成", overline = "Completed") {
                CockpitKeyValueRow("最终吞吐量", "${formatNumber(state.throughputMbps)} Mbps")
            }
        }
        else -> Unit
    }
}

private fun SpeedTestState.isActive(): Boolean =
    this is SpeedTestState.Preparing || this is SpeedTestState.Running || this is SpeedTestState.Cancelling

private fun SpeedTestState.statusText(): String = when (this) {
    SpeedTestState.Idle -> "待测"
    SpeedTestState.Preparing -> "准备中"
    is SpeedTestState.Running -> "运行中"
    SpeedTestState.Cancelling -> "停止中"
    is SpeedTestState.Completed -> "完成"
    is SpeedTestState.Failed -> "异常"
    SpeedTestState.Cancelled -> "已停止"
}

private fun SpeedTestState.shortText(): String = when (this) {
    SpeedTestState.Idle -> "待测"
    SpeedTestState.Preparing -> "准备"
    is SpeedTestState.Running -> "运行"
    SpeedTestState.Cancelling -> "停止"
    is SpeedTestState.Completed -> "完成"
    is SpeedTestState.Failed -> "异常"
    SpeedTestState.Cancelled -> "停止"
}

private fun primaryActionLabel(state: SpeedTestState, defaultLabel: String): String = when (state) {
    is SpeedTestState.Completed -> "重新测试"
    is SpeedTestState.Failed -> "重试"
    else -> defaultLabel
}

private fun throughputOf(state: SpeedTestState): Double = when (state) {
    is SpeedTestState.Running -> state.currentMbps
    is SpeedTestState.Completed -> state.throughputMbps
    else -> 0.0
}

private fun latencyOf(state: SpeedTestState): Double? = when (state) {
    is SpeedTestState.Running -> state.latencyMs
    is SpeedTestState.Completed -> state.latencyMs
    else -> null
}

private fun jitterOf(state: SpeedTestState): Double? = when (state) {
    is SpeedTestState.Running -> state.jitterMs
    is SpeedTestState.Completed -> state.jitterMs
    else -> null
}

private fun lossOf(state: SpeedTestState): Double? = when (state) {
    is SpeedTestState.Running -> state.packetLossPercent
    is SpeedTestState.Completed -> state.packetLossPercent
    else -> null
}

private fun SpeedTestState.progressForSamples(): Int = when (this) {
    is SpeedTestState.Running -> progressPercent
    is SpeedTestState.Completed -> 100
    else -> 0
}

private fun TestCurve.title(protocol: Protocol): String = when (this) {
    TestCurve.THROUGHPUT -> "${protocol.name} 吞吐量曲线"
    TestCurve.LATENCY -> "时延曲线"
    TestCurve.JITTER -> "抖动曲线"
    TestCurve.PACKET_LOSS -> "丢包曲线"
}

@Composable
private fun TestCurve.color(): Color = when (this) {
    TestCurve.THROUGHPUT -> MaterialTheme.colorScheme.primary
    TestCurve.LATENCY -> MaterialTheme.colorScheme.tertiary
    TestCurve.JITTER -> MaterialTheme.colorScheme.secondary
    TestCurve.PACKET_LOSS -> MaterialTheme.colorScheme.error
}

private fun TestCurve.valueLabel(value: Double, hasLiveData: Boolean): String {
    if (!hasLiveData) return "等待数据"
    return when (this) {
        TestCurve.THROUGHPUT -> "${formatNumber(value)} Mbps"
        TestCurve.LATENCY -> "${formatNumber(value)} ms"
        TestCurve.JITTER -> "${formatNumber(value)} ms"
        TestCurve.PACKET_LOSS -> "${formatNumber(value)}%"
    }
}

private fun TestCurve.unit(): String = when (this) {
    TestCurve.THROUGHPUT -> "Mbps"
    TestCurve.LATENCY -> "ms"
    TestCurve.JITTER -> "ms"
    TestCurve.PACKET_LOSS -> "%"
}

private fun curveSamples(value: Double, progress: Int, hasLiveData: Boolean): List<Float> {
    if (!hasLiveData) {
        return listOf(0.22f, 0.22f, 0.22f, 0.22f, 0.22f, 0.22f, 0.22f, 0.22f, 0.22f)
    }
    val base = value.coerceAtLeast(1.0).toFloat()
    return List(12) { index ->
        val wave = sin((index + progress.coerceAtLeast(1)) * 0.78).toFloat()
        val counter = sin((index * 1.17f) + progress * 0.09f).toFloat()
        (base * (0.58f + wave * 0.20f + counter * 0.12f + (index % 3) * 0.045f)).coerceAtLeast(0.1f)
    }
}

private fun formatNumber(value: Double): String = String.format(Locale.US, "%.1f", value)
