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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
) {
    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            CockpitHeader(title = "隐私政策")

            CockpitPanel(overline = "数据收集") {
                Text(
                    text = "INetSpeed 仅收集必要的网络测试数据，包括：\n\n" +
                        "• 测试结果（吞吐量、延迟、丢包率）\n" +
                        "• 服务器信息（地址、端口）\n" +
                        "• 网络类型（WiFi/移动数据）\n\n" +
                        "所有数据均存储在您的设备本地，不会自动上传至任何服务器。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            CockpitPanel(overline = "数据使用") {
                Text(
                    text = "收集的数据仅用于：\n\n" +
                        "• 显示测试历史记录\n" +
                        "• 生成测试报告\n" +
                        "• 提供网络诊断信息\n\n" +
                        "我们不会将您的数据用于广告投放或用户画像。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            CockpitPanel(overline = "数据共享") {
                Text(
                    text = "除非获得您的明确同意，我们不会与第三方共享您的个人数据。\n\n" +
                        "导出报告功能生成的文件仅存储在您的设备上，由您自行决定是否分享。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            CockpitPanel(overline = "数据安全") {
                Text(
                    text = "我们采取以下措施保护您的数据：\n\n" +
                        "• 应用数据存储在私有目录\n" +
                        "• 禁用应用备份（allowBackup=false）\n" +
                        "• 敏感信息加密存储\n" +
                        "• 网络传输使用 HTTPS",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            CockpitPanel(overline = "权限使用") {
                Text(
                    text = "本应用请求以下权限：\n\n" +
                        "• INTERNET - 网络测速\n" +
                        "• ACCESS_NETWORK_STATE - 检测网络状态\n" +
                        "• ACCESS_WIFI_STATE - 获取 WiFi 信息\n" +
                        "• FOREGROUND_SERVICE - 后台测速\n" +
                        "• POST_NOTIFICATIONS - 测速通知\n\n" +
                        "这些权限仅用于核心功能，不会用于其他目的。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            CockpitPanel(overline = "联系我们") {
                Text(
                    text = "如果您对本隐私政策有任何疑问，请通过以下方式联系我们：\n\n" +
                        "GitHub: https://github.com/fy-github/INetSpeed\n\n" +
                        "最后更新：2026 年 6 月",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
