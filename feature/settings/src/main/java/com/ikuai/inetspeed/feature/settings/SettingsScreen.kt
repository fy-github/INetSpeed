package com.ikuai.inetspeed.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.prefs.ThemeChoice
import com.ikuai.inetspeed.core.designsystem.components.CockpitActionButton
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitListItemSurface
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import com.ikuai.inetspeed.core.designsystem.components.CockpitSegmentedControl
import com.ikuai.inetspeed.core.sync.engine.SyncEngine

@Composable
fun SettingsScreen(
    onNavigateToServers: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val iperfVersion by viewModel.iperfVersion.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "设置",
                subtitle = "SYSTEM MATRIX",
                status = themeMode.label(),
            )
            CockpitPanel(title = "Theme", overline = "System Matrix") {
                CockpitSegmentedControl(
                    options = ThemeChoice.entries.map { it.label() },
                    selectedIndex = ThemeChoice.entries.indexOf(themeMode),
                    onSelected = { viewModel.setThemeMode(ThemeChoice.entries[it]) },
                )
            }
            CockpitPanel(title = "配置矩阵", overline = "Runtime Profile") {
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "默认服务器",
                    subtitle = "IPERF.IKUAI.LOCAL:5201",
                    onClick = onNavigateToServers,
                )
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "默认协议",
                    subtitle = "TCP · 下载 · IPv4",
                    onClick = { viewModel.loadIperfVersion() },
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "专业模式参数",
                    subtitle = "时长 30s · 并发 8 · JSON ON",
                    onClick = { viewModel.loadIperfVersion() },
                )
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "数据保留",
                    subtitle = "保留 90 天历史和报告输出",
                    onClick = { showExportDialog = true },
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "开源许可",
                    subtitle = "第三方组件许可证",
                    onClick = onNavigateToLicenses,
                )
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "关于 INetSpeed",
                    subtitle = viewModel.getAppVersion(),
                    onClick = onNavigateToAbout,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitMetricTile("SYNC", syncState.label(), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                CockpitMetricTile("ADS", "OFF", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }
            CockpitActionButton(text = "保存配置", onClick = { viewModel.loadIperfVersion() })
            Text(
                text = "iperf3 ${iperfVersion?.version ?: "--"} · ${viewModel.getDeviceInfo()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("数据导入/导出") },
            text = { Text("当前版本的测试数据会自动保存在本地数据库中。") },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("知道了")
                }
            },
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    CockpitListItemSurface(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CockpitDot(MaterialTheme.colorScheme.primary)
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ThemeChoice.label(): String = when (this) {
    ThemeChoice.SYSTEM -> "跟随系统"
    ThemeChoice.LIGHT -> "浅色"
    ThemeChoice.DARK -> "深色"
}

private fun SyncEngine.SyncState.label(): String = when (this) {
    SyncEngine.SyncState.Idle -> "待命"
    SyncEngine.SyncState.NotLoggedIn -> "未登录"
    SyncEngine.SyncState.Syncing -> "同步中"
    is SyncEngine.SyncState.Success -> "完成"
    is SyncEngine.SyncState.Error -> "失败"
}
