package com.ikuai.inetspeed.feature.servers

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.designsystem.components.CockpitActionButton
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitListItemSurface
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import com.ikuai.inetspeed.core.designsystem.components.CockpitTextField

@Composable
fun ServerSelectionScreen(
    onServerSelected: (Server) -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: ServerViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsState()
    val recommended by viewModel.recommendedServer.collectAsState()
    var showAddPanel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshRecommendation()
    }

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "选择服务器",
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { viewModel.refreshRecommendation() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新推荐", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.startDiscovery() }) {
                            Icon(Icons.Default.Search, contentDescription = "局域网发现", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitMetricTile("总数", servers.size.toString(), Modifier.weight(1f))
                CockpitMetricTile("自定义", servers.count { !it.isBuiltIn }.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                CockpitMetricTile("内置", servers.count { it.isBuiltIn }.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }

            CockpitActionButton(
                text = if (showAddPanel) "收起添加" else "添加节点",
                onClick = {
                    showAddPanel = !showAddPanel
                    onNavigateToAdd()
                },
            )

            if (showAddPanel) {
                AddServerPanel(
                    onAdd = { name, address, port ->
                        viewModel.addCustomServer(name, address, port)
                        showAddPanel = false
                    },
                )
            }

            CockpitPanel(
                modifier = Modifier.weight(1f),
            ) {
                ServerList(
                    servers = servers,
                    recommended = recommended,
                    onServerSelected = onServerSelected,
                    onFavorite = viewModel::toggleFavorite,
                    onDelete = viewModel::deleteServer,
                )
            }
        }
    }
}

@Composable
private fun AddServerPanel(
    onAdd: (String, String, Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5201") }
    val canAdd = address.isNotBlank() && (port.toIntOrNull() ?: 0) in 1..65535

    CockpitPanel(title = "新增服务器", overline = "Custom Node") {
        CockpitTextField(
            value = name,
            onValueChange = { name = it },
            label = "节点名称",
            placeholder = "实验室节点",
        )
        CockpitTextField(
            value = address,
            onValueChange = { address = it },
            label = "服务器地址",
            placeholder = "iperf.example.com",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            CockpitTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.width(96.dp),
                label = "端口",
                placeholder = "5201",
            )
            CockpitActionButton(
                text = "确认添加",
                onClick = {
                    val cleanAddress = address.trim()
                    onAdd(name.ifBlank { cleanAddress }, cleanAddress, port.toIntOrNull() ?: 5201)
                },
                modifier = Modifier.weight(1f),
                enabled = canAdd,
            )
        }
    }
}

@Composable
private fun ServerList(
    servers: List<Server>,
    recommended: Server?,
    onServerSelected: (Server) -> Unit,
    onFavorite: (Server) -> Unit,
    onDelete: (Server) -> Unit,
) {
    if (servers.isEmpty()) {
        EmptyServers()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        recommended?.let { server ->
            item("recommended-${server.id}") {
                ServerRow(
                    server = server,
                    tag = "RECOMMENDED",
                    onClick = { onServerSelected(server) },
                    onFavorite = { onFavorite(server) },
                    onDelete = { onDelete(server) },
                )
            }
        }

        val rest = servers.filter { it.id != recommended?.id }
        items(rest, key = { it.id }) { server ->
            ServerRow(
                server = server,
                tag = if (server.isBuiltIn) "BUILT-IN" else "CUSTOM",
                onClick = { onServerSelected(server) },
                onFavorite = { onFavorite(server) },
                onDelete = { onDelete(server) },
            )
        }
    }
}

@Composable
private fun ServerRow(
    server: Server,
    tag: String,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    CockpitListItemSurface(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CockpitDot(
                when {
                    tag == "RECOMMENDED" -> MaterialTheme.colorScheme.secondary
                    server.isBuiltIn -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.tertiary
                },
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.name.ifBlank { server.address },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                Text(
                    text = "${server.address}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        server.region,
                        server.lastLatencyMs?.let { "${it.toInt()}ms" },
                        if (server.isFavorite) "收藏" else null,
                    ).ifEmpty { listOf("待测") }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (server.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除节点", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EmptyServers() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("暂无服务器节点", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("点击添加节点录入 iperf3 服务器。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
