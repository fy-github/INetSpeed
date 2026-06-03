package com.ikuai.inetspeed.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.network.ping.PingService
import com.ikuai.inetspeed.core.network.traceroute.TracerouteService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ping", "Traceroute", "网络信息")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("工具") })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> PingTab(viewModel)
                1 -> TracerouteTab(viewModel)
                2 -> NetworkInfoTab(viewModel)
            }
        }
    }
}

@Composable
private fun PingTab(viewModel: ToolsViewModel) {
    val results by viewModel.pingResults.collectAsState()
    val isPinging by viewModel.isPinging.collectAsState()
    var target by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("目标地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.startPing(target) },
                enabled = !isPinging && target.isNotBlank(),
            ) { Text("开始 Ping") }

            if (isPinging) {
                OutlinedButton(onClick = { viewModel.stopPing() }) {
                    Text("停止")
                }
                CircularProgressIndicator(modifier = Modifier.padding(start = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (results.isNotEmpty()) {
            val reachable = results.filter { it.reachable }
            val avg = reachable.mapNotNull { it.avgLatencyMs }.average().takeIf { !it.isNaN() }
            val loss = ((results.size - reachable.size) * 100.0) / results.size

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    MetricItem("发送", "${results.size}")
                    MetricItem("接收", "${reachable.size}")
                    MetricItem("丢包", "${loss.toInt()}%")
                    MetricItem("平均", avg?.let { "${it.toInt()}ms" } ?: "--")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn {
            items(results.reversed()) { result ->
                PingResultRow(result)
            }
        }
    }
}

@Composable
private fun PingResultRow(result: PingService.PingResult) {
    val reachable = result.reachable
    val avgLatency = result.avgLatencyMs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = if (reachable) "✓" else "✗",
            color = if (reachable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = avgLatency?.let { "${it.toInt()}ms" } ?: "超时",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TracerouteTab(viewModel: ToolsViewModel) {
    val hops by viewModel.tracerouteHops.collectAsState()
    val isTracing by viewModel.isTracing.collectAsState()
    var target by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("目标地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.startTraceroute(target) },
            enabled = !isTracing && target.isNotBlank(),
        ) {
            Text("开始 Traceroute")
        }

        if (isTracing) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(hops) { hop ->
                TracerouteHopRow(hop)
            }
        }
    }
}

@Composable
private fun TracerouteHopRow(hop: TracerouteService.TracerouteHop) {
    val hopNumber = hop.hopNumber
    val host = hop.host
    val latency = hop.latencyMs
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = "$hopNumber",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = host,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = latency?.let { "${it.toInt()}ms" } ?: "*",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun NetworkInfoTab(viewModel: ToolsViewModel) {
    val info by viewModel.networkInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNetworkInfo()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        if (info != null) {
            val n = info!!
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("网络类型", n.networkType)
                    InfoRow("IPv4", n.ipv4 ?: "不可用")
                    InfoRow("IPv6", n.ipv6 ?: "不可用")
                    InfoRow("网关", n.gateway ?: "不可用")
                    InfoRow("DNS", n.dns ?: "不可用")
                    if (n.ssid != null) {
                        InfoRow("WiFi 名称", n.ssid!!)
                        InfoRow("信号强度", "${n.signalStrength} dBm")
                        InfoRow("连接速度", "${n.linkSpeed} Mbps")
                    }
                    InfoRow("MAC 地址", n.macAddress ?: "不可用")
                }
            }
        } else {
            Button(onClick = { viewModel.loadNetworkInfo() }) {
                Text("获取网络信息")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
