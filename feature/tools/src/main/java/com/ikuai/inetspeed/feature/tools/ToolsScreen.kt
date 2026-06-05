package com.ikuai.inetspeed.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.designsystem.components.CockpitActionButton
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitKeyValueRow
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import com.ikuai.inetspeed.core.designsystem.components.CockpitSegmentedControl
import com.ikuai.inetspeed.core.network.ping.PingService
import com.ikuai.inetspeed.core.network.ping.TcpPingService
import com.ikuai.inetspeed.core.network.traceroute.TracerouteService

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("PING", "TCPING", "TRACE", "INFO")

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "网络工具",
                subtitle = "诊断控制台 · 连通性 / 路由 / 链路信息",
                status = tabs[selectedTab],
            )
            CockpitSegmentedControl(
                options = tabs,
                selectedIndex = selectedTab,
                onSelected = { selectedTab = it },
            )
            when (selectedTab) {
                0 -> PingTab(viewModel, Modifier.weight(1f))
                1 -> TcpPingTab(viewModel, Modifier.weight(1f))
                2 -> TracerouteTab(viewModel, Modifier.weight(1f))
                3 -> NetworkInfoTab(viewModel, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PingTab(viewModel: ToolsViewModel, modifier: Modifier = Modifier) {
    val results by viewModel.pingResults.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    var target by remember { mutableStateOf("") }
    val reachable = results.filter { it.reachable }
    val avg = reachable.mapNotNull { it.avgLatencyMs }.average().takeIf { !it.isNaN() }
    val loss = if (results.isEmpty()) 0.0 else ((results.size - reachable.size) * 100.0) / results.size

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolCommandPanel(
            title = "ICMP Ping",
            target = target,
            onTargetChange = { target = it },
            action = if (isPinging) "停止" else "开始 Ping",
            running = isPinging,
            enabled = target.isNotBlank(),
            onAction = { if (isPinging) viewModel.stopPing() else viewModel.startPing(target) },
        )
        MetricStrip(
            listOf(
                "发送" to results.size.toString(),
                "接收" to reachable.size.toString(),
                "丢包" to "${loss.toInt()}%",
                "平均" to (avg?.let { "${it.toInt()}ms" } ?: "--"),
            ),
        )
        ResultConsole(modifier = Modifier.weight(1f), empty = "等待 Ping 数据", isEmpty = results.isEmpty()) {
            items(results.reversed()) { PingResultRow(it) }
        }
    }
}

@Composable
private fun TcpPingTab(viewModel: ToolsViewModel, modifier: Modifier = Modifier) {
    val results by viewModel.tcpPingResults.collectAsState()
    val isTcpPinging by viewModel.isTcpPinging.collectAsState()
    var target by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5201") }
    val reachable = results.filter { it.reachable }
    val avg = reachable.mapNotNull { it.latencyMs }.average().takeIf { !it.isNaN() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitPanel(title = "TCP Port Probe", overline = "Socket") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("目标地址") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("端口") },
                    singleLine = true,
                    modifier = Modifier.width(96.dp),
                )
            }
            CockpitActionButton(
                text = if (isTcpPinging) "停止" else "开始 Tcping",
                onClick = {
                    if (isTcpPinging) viewModel.stopTcpPing() else viewModel.startTcpPing(target, port.toIntOrNull() ?: 5201)
                },
                enabled = target.isNotBlank(),
                destructive = isTcpPinging,
            )
        }
        MetricStrip(
            listOf(
                "发送" to results.size.toString(),
                "连通" to reachable.size.toString(),
                "失败" to (results.size - reachable.size).toString(),
                "延迟" to (avg?.let { "${it.toInt()}ms" } ?: "--"),
            ),
        )
        ResultConsole(modifier = Modifier.weight(1f), empty = "等待 Tcping 数据", isEmpty = results.isEmpty()) {
            items(results.reversed()) { TcpPingResultRow(it) }
        }
    }
}

@Composable
private fun TracerouteTab(viewModel: ToolsViewModel, modifier: Modifier = Modifier) {
    val hops by viewModel.tracerouteHops.collectAsState()
    val isTracing by viewModel.isTracing.collectAsState()
    var target by remember { mutableStateOf("") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolCommandPanel(
            title = "Traceroute",
            target = target,
            onTargetChange = { target = it },
            action = if (isTracing) "追踪中" else "开始 Traceroute",
            running = isTracing,
            enabled = target.isNotBlank() && !isTracing,
            onAction = { viewModel.startTraceroute(target) },
        )
        MetricStrip(
            listOf(
                "跳数" to hops.size.toString(),
                "目标" to (target.ifBlank { "--" }),
                "状态" to if (isTracing) "运行" else "待命",
            ),
        )
        ResultConsole(modifier = Modifier.weight(1f), empty = "等待路由跳点", isEmpty = hops.isEmpty()) {
            items(hops) { TracerouteHopRow(it) }
        }
    }
}

@Composable
private fun NetworkInfoTab(viewModel: ToolsViewModel, modifier: Modifier = Modifier) {
    val info by viewModel.networkInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNetworkInfo()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CockpitPanel(title = "链路信息", overline = "Interface") {
            if (info == null) {
                CockpitActionButton(text = "获取网络信息", onClick = { viewModel.loadNetworkInfo() })
            } else {
                val n = info!!
                CockpitKeyValueRow("网络类型", n.networkType)
                CockpitKeyValueRow("IPv4", n.ipv4 ?: "不可用")
                CockpitKeyValueRow("IPv6", n.ipv6 ?: "不可用")
                CockpitKeyValueRow("网关", n.gateway ?: "不可用")
                CockpitKeyValueRow("DNS", n.dns ?: "不可用")
                n.ssid?.let {
                    CockpitKeyValueRow("WiFi 名称", it)
                    CockpitKeyValueRow("信号强度", "${n.signalStrength} dBm")
                    CockpitKeyValueRow("连接速度", "${n.linkSpeed} Mbps")
                }
                CockpitKeyValueRow("MAC 地址", n.macAddress ?: "不可用")
            }
        }
    }
}

@Composable
private fun ToolCommandPanel(
    title: String,
    target: String,
    onTargetChange: (String) -> Unit,
    action: String,
    running: Boolean,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    CockpitPanel(title = title, overline = if (running) "Running" else "Ready") {
        OutlinedTextField(
            value = target,
            onValueChange = onTargetChange,
            label = { Text("目标地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        CockpitActionButton(text = action, onClick = onAction, enabled = enabled, destructive = running)
    }
}

@Composable
private fun MetricStrip(metrics: List<Pair<String, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        metrics.forEach { (label, value) ->
            CockpitMetricTile(label = label, value = value, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResultConsole(
    modifier: Modifier = Modifier,
    empty: String,
    isEmpty: Boolean,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    CockpitPanel(modifier = modifier, title = "回显", overline = "Stream") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isEmpty) {
                item { Text(empty, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            content()
        }
    }
}

@Composable
private fun PingResultRow(result: PingService.PingResult) {
    ConsoleRow(
        ok = result.reachable,
        metric = result.avgLatencyMs?.let { "${it.toInt()}ms" } ?: "超时",
        text = result.host,
    )
}

@Composable
private fun TcpPingResultRow(result: TcpPingService.TcpPingResult) {
    ConsoleRow(
        ok = result.reachable,
        metric = result.latencyMs?.let { "${it.toInt()}ms" } ?: "超时",
        text = "${result.host}:${result.port}",
    )
}

@Composable
private fun TracerouteHopRow(hop: TracerouteService.TracerouteHop) {
    ConsoleRow(
        ok = hop.latencyMs != null,
        metric = "#${hop.hopNumber}",
        text = "${hop.host}  ${hop.latencyMs?.let { "${it.toInt()}ms" } ?: "*"}",
    )
}

@Composable
private fun ConsoleRow(ok: Boolean, metric: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CockpitDot(if (ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
        Text(metric, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(58.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
