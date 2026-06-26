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
fun LicensesScreen(
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
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
            CockpitHeader(title = "开源许可")

            CockpitPanel(overline = "AndroidX") {
                LicenseItem("AndroidX Core KTX", "Apache 2.0")
                LicenseItem("AndroidX Activity Compose", "Apache 2.0")
                LicenseItem("AndroidX Lifecycle", "Apache 2.0")
                LicenseItem("AndroidX Navigation", "Apache 2.0")
                LicenseItem("AndroidX Room", "Apache 2.0")
                LicenseItem("AndroidX Security Crypto", "Apache 2.0")
            }

            CockpitPanel(overline = "Jetpack Compose") {
                LicenseItem("Compose UI", "Apache 2.0")
                LicenseItem("Compose Material3", "Apache 2.0")
                LicenseItem("Compose Runtime", "Apache 2.0")
            }

            CockpitPanel(overline = "依赖注入") {
                LicenseItem("Dagger Hilt", "Apache 2.0")
                LicenseItem("Hilt Navigation Compose", "Apache 2.0")
            }

            CockpitPanel(overline = "网络") {
                LicenseItem("Retrofit", "Apache 2.0")
                LicenseItem("OkHttp", "Apache 2.0")
                LicenseItem("Gson", "Apache 2.0")
            }

            CockpitPanel(overline = "异步") {
                LicenseItem("Kotlin Coroutines", "Apache 2.0")
            }

            CockpitPanel(overline = "测试") {
                LicenseItem("JUnit 4", "Eclipse Public License 2.0")
                LicenseItem("MockK", "Apache 2.0")
                LicenseItem("Espresso", "Apache 2.0")
            }

            CockpitPanel(overline = "其他") {
                LicenseItem("iperf3", "BSD 3-Clause")
                LicenseItem("iperf3 Android Binary", "BSD 3-Clause")
            }
        }
    }
}

@Composable
private fun LicenseItem(name: String, license: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = license,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
