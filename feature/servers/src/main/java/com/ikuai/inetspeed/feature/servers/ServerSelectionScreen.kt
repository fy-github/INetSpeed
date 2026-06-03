package com.ikuai.inetspeed.feature.servers

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.Server

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSelectionScreen(
    onServerSelected: (Server) -> Unit,
    onNavigateToAdd: () -> Unit,
    viewModel: ServerViewModel = hiltViewModel(),
) {
    val servers by viewModel.servers.collectAsState()
    val recommended by viewModel.recommendedServer.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshRecommendation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择服务器") },
                actions = {
                    IconButton(onClick = { viewModel.refreshRecommendation() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新推荐")
                    }
                    IconButton(onClick = { viewModel.startDiscovery() }) {
                        Icon(Icons.Default.Search, contentDescription = "局域网发现")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "添加服务器")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 推荐服务器
            recommended?.let { server ->
                item {
                    Text(
                        text = "推荐",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    ServerCard(
                        server = server,
                        isRecommended = true,
                        onClick = { onServerSelected(server) },
                        onFavorite = { viewModel.toggleFavorite(server) },
                    )
                }
            }

            // 内置服务器
            val builtIn = servers.filter { it.isBuiltIn && it.id != recommended?.id }
            if (builtIn.isNotEmpty()) {
                item {
                    Text(
                        text = "内置服务器",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(builtIn) { server ->
                    ServerCard(
                        server = server,
                        onClick = { onServerSelected(server) },
                        onFavorite = { viewModel.toggleFavorite(server) },
                    )
                }
            }

            // 自定义服务器
            val custom = servers.filter { !it.isBuiltIn }
            if (custom.isNotEmpty()) {
                item {
                    Text(
                        text = "自定义服务器",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                items(custom) { server ->
                    ServerCard(
                        server = server,
                        onClick = { onServerSelected(server) },
                        onFavorite = { viewModel.toggleFavorite(server) },
                        onDelete = { viewModel.deleteServer(server) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: Server,
    isRecommended: Boolean = false,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isRecommended) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.padding(end = 8.dp)) {
                Text("●", color = if (isRecommended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(server.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${server.address}:${server.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (server.lastLatencyMs != null) {
                    Text(
                        text = "延迟: ${server.lastLatencyMs.toInt()}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (server.region != null) {
                    Text(
                        text = server.region,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (server.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (server.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
