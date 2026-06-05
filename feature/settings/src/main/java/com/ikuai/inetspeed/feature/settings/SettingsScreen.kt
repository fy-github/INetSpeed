package com.ikuai.inetspeed.feature.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
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
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
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
                subtitle = "Configuration Matrix · 主题 / 同步 / iperf3",
                status = themeMode.label(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CockpitMetricTile("主题", themeMode.label(), Modifier.weight(1f))
                CockpitMetricTile("同步", syncState.label(), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                CockpitMetricTile("iperf3", iperfVersion?.version ?: "--", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }

            CockpitPanel(title = "外观", overline = "Theme") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("主题模式", style = MaterialTheme.typography.titleSmall)
                }
                CockpitSegmentedControl(
                    options = ThemeChoice.entries.map { it.label() },
                    selectedIndex = ThemeChoice.entries.indexOf(themeMode),
                    onSelected = { viewModel.setThemeMode(ThemeChoice.entries[it]) },
                )
            }

            SettingsSection("网络", "Servers") {
                SettingsItem(
                    icon = Icons.Default.Dns,
                    title = "服务器管理",
                    subtitle = "管理内置、自定义和局域网服务器",
                    onClick = onNavigateToServers,
                )
            }

            SettingsSection("数据", "Sync & Backup") {
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "云端同步",
                    subtitle = syncState.detailLabel(),
                    onClick = { viewModel.triggerSync() },
                )
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "数据导入/导出",
                    subtitle = "备份和恢复测试记录",
                    onClick = { showExportDialog = true },
                )
            }

            SettingsSection("iperf3", "Runtime") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "版本信息",
                    subtitle = iperfVersion?.let { "v${it.version}" } ?: "加载中...",
                    onClick = { viewModel.loadIperfVersion() },
                )
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "检查更新",
                    subtitle = "当前渠道: Google Play",
                    onClick = { },
                )
            }

            SettingsSection("关于", "Meta") {
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

            Text(
                text = viewModel.getDeviceInfo(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("数据导入/导出") },
            text = { Text("此功能将在后续版本中支持。当前版本的测试数据已自动保存在本地数据库中。") },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("知道了")
                }
            },
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    overline: String,
    content: @Composable () -> Unit,
) {
    CockpitPanel(title = title, overline = overline) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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

private fun SyncEngine.SyncState.detailLabel(): String = when (this) {
    SyncEngine.SyncState.Idle -> "点击同步"
    SyncEngine.SyncState.NotLoggedIn -> "未登录"
    SyncEngine.SyncState.Syncing -> "同步中..."
    is SyncEngine.SyncState.Success -> "已同步"
    is SyncEngine.SyncState.Error -> "同步失败: $message"
}
