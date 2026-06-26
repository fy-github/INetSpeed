package com.ikuai.inetspeed.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    appVersion: String = "1.0.1",
) {
    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            CockpitHeader(title = "关于 INetSpeed")

            CockpitPanel(overline = "应用信息") {
                Text(text = "INetSpeed", style = MaterialTheme.typography.titleLarge)
                Text(text = "版本: $appVersion", style = MaterialTheme.typography.bodyMedium)
                Text(text = "基于 iperf3 协议的网络带宽测试工具", style = MaterialTheme.typography.bodyMedium)
            }

            CockpitPanel(overline = "功能特性") {
                Text(text = "• TCP/UDP 双协议测速", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• 实时带宽曲线", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• Ping / TCP Ping / Traceroute", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• 网络信息查看", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• 测速历史与报告导出", style = MaterialTheme.typography.bodyMedium)
                Text(text = "• 亮色/暗色主题切换", style = MaterialTheme.typography.bodyMedium)
            }

            CockpitPanel(overline = "技术栈") {
                Text(text = "Kotlin + Jetpack Compose", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Hilt + Room + Navigation", style = MaterialTheme.typography.bodyMedium)
                Text(text = "iperf3 (ProcessBuilder)", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
