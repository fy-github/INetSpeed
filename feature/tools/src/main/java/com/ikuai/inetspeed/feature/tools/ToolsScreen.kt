package com.ikuai.inetspeed.feature.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.ikuai.inetspeed.core.network.info.NetworkInfoService.NetworkInfo
import com.ikuai.inetspeed.core.network.traceroute.TracerouteService.TracerouteHop

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
    val active = isPinging || isTcpPinging
    val reachablePing = pingResults.filter { it.reachable }
    val reachableTcp = tcpResults.filter { it.reachable }
    val latencyValues = when (selectedTab) {
        1 -> reachableTcp.mapNotNull { it.latencyMs?.toFloat() }
        else -> reachablePing.mapNotNull { it.avgLatencyMs?.toFloat() }
    }
    val avgLatency = latencyValues.average().takeIf { !it.isNaN() }
    val loss = when (selectedTab) {
        0 -> if (pingResults.isEmpty()) 0.0 else ((pingResults.size - reachablePing.size) * 100.0) / pingResults.size
        1 -> if (tcpResults.isEmpty()) 0.0 else ((tcpResults.size - reachableTcp.size) * 100.0) / tcpResults.size
        else -> 0.0
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3) viewModel.loadNetworkInfo()
    }

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "工具",
                subtitle = "DIAGNOSTIC CONSOLE",
                status = tabs[selectedTab],
            )
            if (selectedTab != 3) {
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
            }
            ToolDeck(
                selectedTab = selectedTab,
                onSelected = {
                    selectedTab = it
                    if (it == 3) viewModel.loadNetworkInfo()
                },
            )
            when (selectedTab) {
                2 -> TraceRouteMode(
                    target = target,
                    hops = hops,
                    isTracing = isTracing,
                    onStart = { viewModel.startTraceroute(target) },
                )
                3 -> NetworkInfoPanel(networkInfo = networkInfo)
                else -> ProbeWaveformMode(
                    selectedTab = selectedTab,
                    avgLatency = avgLatency,
                    latencyValues = latencyValues,
                    loss = loss,
                    active = active,
                    target = target,
                    onAction = {
                        when (selectedTab) {
                            0 -> if (isPinging) viewModel.stopPing() else viewModel.startPing(target)
                            1 -> if (isTcpPinging) viewModel.stopTcpPing() else viewModel.startTcpPing(target, tcpPort.toIntOrNull() ?: 5201)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ProbeWaveformMode(
    selectedTab: Int,
    avgLatency: Double?,
    latencyValues: List<Float>,
    loss: Double,
    active: Boolean,
    target: String,
    onAction: () -> Unit,
) {
    CockpitCurve(
        title = "Diagnostic waveform",
        valueLabel = avgLatency?.let { "${it.toInt()} ms avg" } ?: "等待数据",
        samples = latencySamples(latencyValues),
        height = 82.dp,
        yAxisUnit = "ms",
        color = if (selectedTab == 1) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CockpitMetricTile("RTT", avgLatency?.let { "${it.toInt()}ms" } ?: "--", Modifier.weight(1f))
        CockpitMetricTile("TTL", "--", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
        CockpitMetricTile("LOSS", "${loss.toInt()}%", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
    }
    CockpitActionButton(
        text = if (active) "停止诊断" else "开始诊断",
        onClick = onAction,
        enabled = target.isNotBlank(),
        destructive = active,
    )
}

@Composable
private fun TraceRouteMode(
    target: String,
    hops: List<TracerouteHop>,
    isTracing: Boolean,
    onStart: () -> Unit,
) {
    CockpitActionButton(
        text = if (isTracing) "路径跟踪中" else "开始诊断",
        onClick = onStart,
        enabled = target.isNotBlank() && !isTracing,
    )
    TraceRoutePathPanel(hops = hops, target = target, isTracing = isTracing)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        CockpitMetricTile("HOPS", hops.size.toString(), Modifier.weight(1f))
        CockpitMetricTile("LAST", hops.lastOrNull()?.latencyMs?.let { "${it.toInt()}ms" } ?: "--", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
        CockpitMetricTile("STATE", if (isTracing) "RUN" else if (hops.isEmpty()) "WAIT" else "DONE", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun TraceRoutePathPanel(
    hops: List<TracerouteHop>,
    target: String,
    isTracing: Boolean,
) {
    val visibleHops = if (hops.isEmpty()) {
        List(5) { index -> TracerouteHop(index + 1, if (index == 4) target else "*", null, false) }
    } else {
        hops.take(8)
    }
    CockpitPanel(title = "路径跟踪图", overline = "Route Path") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isTracing) "正在探测跳点" else if (hops.isEmpty()) "等待跟踪" else "路径完成",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = target,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RouteMap(visibleHops = visibleHops, hasData = hops.isNotEmpty())
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            visibleHops.take(5).forEach { hop ->
                HopRow(hop = hop, pending = hops.isEmpty())
            }
        }
    }
}

@Composable
private fun RouteMap(
    visibleHops: List<TracerouteHop>,
    hasData: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val muted = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val left = 20.dp.toPx()
            val right = size.width - 20.dp.toPx()
            drawLine(
                muted.copy(alpha = 0.18f),
                Offset(left, centerY),
                Offset(right, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            val count = visibleHops.size.coerceAtLeast(1)
            val step = if (count == 1) 0f else (right - left) / (count - 1)
            visibleHops.forEachIndexed { index, hop ->
                val x = left + step * index
                val reached = hasData && hop.host != "*"
                val color = if (reached) {
                    if (hop.reachable) secondary else primary
                } else {
                    muted.copy(alpha = 0.36f)
                }
                drawCircle(color.copy(alpha = 0.16f), radius = 12.dp.toPx(), center = Offset(x, centerY))
                drawCircle(color, radius = 5.dp.toPx(), center = Offset(x, centerY))
                drawCircle(color.copy(alpha = 0.62f), radius = 12.dp.toPx(), center = Offset(x, centerY), style = Stroke(width = 1.dp.toPx()))
            }
        }
    }
}

@Composable
private fun HopRow(
    hop: TracerouteHop,
    pending: Boolean,
) {
    CockpitListItemSurface {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = hop.hopNumber.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (pending) "等待跳点" else hop.host,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (hop.reachable) "目标已到达" else "中继节点",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Text(
                text = hop.latencyMs?.let { "${it.toInt()}ms" } ?: "--",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NetworkInfoPanel(networkInfo: NetworkInfo?) {
    CockpitPanel(title = "设备网络信息", overline = "Interface Map") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CockpitMetricTile("TYPE", networkInfo?.networkType ?: "--", Modifier.weight(1f))
            CockpitMetricTile("LINK", networkInfo?.linkSpeed?.let { "${it}M" } ?: "--", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
            CockpitMetricTile("RSSI", networkInfo?.signalStrength?.let { "${it}dBm" } ?: "--", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
        }
        NetworkInfoRow("SSID", networkInfo?.ssid ?: "--")
        NetworkInfoRow("IPv4", networkInfo?.ipv4 ?: "--")
        NetworkInfoRow("IPv6", networkInfo?.ipv6 ?: "--")
        NetworkInfoRow("Gateway", networkInfo?.gateway ?: "--")
        NetworkInfoRow("DNS", networkInfo?.dns ?: "--")
        NetworkInfoRow("MAC", networkInfo?.macAddress ?: "--")
    }
}

@Composable
private fun NetworkInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(76.dp),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

private fun latencySamples(values: List<Float>): List<Float> {
    if (values.isEmpty()) return listOf(0.32f, 0.40f, 0.36f, 0.48f, 0.42f, 0.54f, 0.45f, 0.50f)
    return values.takeLast(16).map { it.coerceAtLeast(1f) }
}
