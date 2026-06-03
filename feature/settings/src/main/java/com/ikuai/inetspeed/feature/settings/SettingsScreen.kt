package com.ikuai.inetspeed.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Server
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.sync.engine.SyncEngine

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("设置") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // 外观
            SettingsSection(title = "外观") {
                ThemeSetting(
                    currentMode = themeMode,
                    onModeChange = { viewModel.setThemeMode(it) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 服务器
            SettingsSection(title = "服务器") {
                SettingsItem(
                    icon = Icons.Default.Server,
                    title = "服务器管理",
                    subtitle = "管理内置、自定义和局域网服务器",
                    onClick = onNavigateToServers,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 数据
            SettingsSection(title = "数据") {
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "云端同步",
                    subtitle = when (syncState) {
                        is SyncEngine.SyncState.NotLoggedIn -> "未登录"
                        is SyncEngine.SyncState.Syncing -> "同步中..."
                        is SyncEngine.SyncState.Success -> "已同步"
                        is SyncEngine.SyncState.Error -> "同步失败"
                        else -> "点击同步"
                    },
                    onClick = { viewModel.triggerSync() },
                )
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "数据导入/导出",
                    subtitle = "备份和恢复测试记录",
                    onClick = { /* TODO */ },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // iperf3
            SettingsSection(title = "iperf3") {
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
                    onClick = { /* TODO: 按渠道拆分 */ },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 关于
            SettingsSection(title = "关于") {
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

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = viewModel.getDeviceInfo(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(4.dp)) {
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
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSetting(
    currentMode: SettingsViewModel.ThemeMode,
    onModeChange: (SettingsViewModel.ThemeMode) -> Unit,
) {
    Column(modifier = Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ColorLens,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text("主题", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SettingsViewModel.ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SettingsViewModel.ThemeMode.entries.size),
                ) {
                    Text(
                        text = when (mode) {
                            SettingsViewModel.ThemeMode.SYSTEM -> "跟随系统"
                            SettingsViewModel.ThemeMode.LIGHT -> "浅色"
                            SettingsViewModel.ThemeMode.DARK -> "深色"
                        },
                    )
                }
            }
        }
    }
}
