package com.ikuai.inetspeed.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val route: String,
    val iconSemantic: String,
) {
    SPEEDTEST(
        selectedIcon = Icons.Filled.Timeline,
        unselectedIcon = Icons.Outlined.Timeline,
        label = "测速",
        route = "speedtest",
        iconSemantic = "waveform",
    ),
    TOOLS(
        selectedIcon = Icons.Filled.Computer,
        unselectedIcon = Icons.Outlined.Computer,
        label = "工具",
        route = "tools",
        iconSemantic = "monitor",
    ),
    HISTORY(
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        label = "历史",
        route = "history",
        iconSemantic = "bars",
    ),
    REPORT(
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        label = "报告",
        route = "report",
        iconSemantic = "document",
    ),
    SETTINGS(
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        label = "设置",
        route = "settings",
        iconSemantic = "gear",
    ),
}
