package com.ikuai.inetspeed.feature.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.designsystem.components.CockpitActionButton
import com.ikuai.inetspeed.core.designsystem.components.CockpitCurve
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitListItemSurface
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import com.ikuai.inetspeed.core.designsystem.components.CockpitTextField

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var target by remember { mutableStateOf("gateway.local") }
    var tcpPort by remember { mutableStateOf("5201") }
    val tabs = listOf("PING", "TCPING", "TRACE", "INFO")
    val pingResults by viewModel.pingResults.collectAsState()
    val tcpResults by viewModel.tcpPingResults.collectAsState()
    val hops by viewModel.tracerouteHops.collectAsState()
    val networkInfo by viewModel.networkInfo.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    val isTcpPinging by viewModel.isTcpPinging.collectAsState()
    val isTracing by viewModel.isTracing.collectAsState()
    val active = isPinging || isTcpPinging || isTracing
    val reachablePing = pingResults.filter { it.reachable }
    val reachableTcp = tcpResults.filter { it.reachable }
    val latencyValues = when (selectedTab) {
        1 -> reachableTcp.mapNotNull { it.latencyMs?.toFloat() }
        2 -> hops.mapNotNull { it.latencyMs?.toFloat() }
        else -> reachablePing.mapNotNull { it.avgLatencyMs?.toFloat() }
    }
    val avgLatency = latencyValues.average().takeIf { !it.isNaN() }
    val loss = when (selectedTab) {
        0 -> if (pingResults.isEmpty()) 0.0 else ((pingResults.size - reachablePing.size) * 100.0) / pingResults.size
        1 -> if (tcpResults.isEmpty()) 0.0 else ((tcpResults.size - reachableTcp.size) * 100.0) / tcpResults.size
        else -> 0.0
    }

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "工具",
                subtitle = "DIAGNOSTIC CONSOLE",
                status = tabs[selectedTab],
            )
            CockpitPanel(title = "Target", overline = tabs[selectedTab]) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CockpitTextField(
                        value = target,
                        onValueChange = { target = it },
                        modifier = Modifier.weight(1f),
                        label = null,
                        placeholder = "gateway.local",
                        fieldHeight = 46.dp,
                    )
                    if (selectedTab == 1) {
                        CockpitTextField(
                            value = tcpPort,
                            onValueChange = { tcpPort = it },
                            modifier = Modifier.width(76.dp),
                            label = null,
                            placeholder = "5201",
                            fieldHeight = 46.dp,
                        )
                    }
                }
            }
            ToolDeck(selectedTab = selectedTab, onSelected = { selectedTab = it })
            CockpitCurve(
                title = "Diagnostic waveform",
                valueLabel = avgLatency?.let { "${it.toInt()} ms avg" } ?: if (selectedTab == 3) (networkInfo?.networkType ?: "等待数据") else "等待数据",
                samples = latencySamples(latencyValues),
                height = 82.dp,
                yAxisUnit = if (selectedTab == 3) "Mbps" else "ms",
                color = if (selectedTab == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitMetricTile("RTT", avgLatency?.let { "${it.toInt()}ms" } ?: "--", Modifier.weight(1f))
                CockpitMetricTile("TTL", if (selectedTab == 2) hops.size.toString() else "--", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                CockpitMetricTile("LOSS", "${loss.toInt()}%", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }
            CockpitActionButton(
                text = if (active) "停止诊断" else "开始诊断",
                onClick = {
                    when (selectedTab) {
                        0 -> if (isPinging) viewModel.stopPing() else viewModel.startPing(target)
                        1 -> if (isTcpPinging) viewModel.stopTcpPing() else viewModel.startTcpPing(target, tcpPort.toIntOrNull() ?: 5201)
                        2 -> if (!isTracing) viewModel.startTraceroute(target)
                        3 -> viewModel.loadNetworkInfo()
                    }
                },
                enabled = target.isNotBlank() && (selectedTab != 2 || !isTracing),
                destructive = active,
            )
        }
    }
}

@Composable
private fun ToolDeck(
    selectedTab: Int,
    onSelected: (Int) -> Unit,
) {
    CockpitPanel(title = "工具矩阵", overline = "Probe Modules") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolCard("Ping", "LATENCY PROBE", selectedTab == 0, { onSelected(0) }, Modifier.weight(1f))
            ToolCard("Traceroute", "ROUTE PATH", selectedTab == 2, { onSelected(2) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ToolCard("Tcping", "PORT PROBE", selectedTab == 1, { onSelected(1) }, Modifier.weight(1f))
            ToolCard("网络信息", "INTERFACE MAP", selectedTab == 3, { onSelected(3) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CockpitListItemSurface(modifier = modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            CockpitDot(if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

private fun latencySamples(values: List<Float>): List<Float> {
    if (values.isEmpty()) return listOf(0.32f, 0.40f, 0.36f, 0.48f, 0.42f, 0.54f, 0.45f, 0.50f)
    return values.takeLast(16).map { it.coerceAtLeast(1f) }
}
