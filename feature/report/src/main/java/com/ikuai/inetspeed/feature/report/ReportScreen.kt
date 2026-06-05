package com.ikuai.inetspeed.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.Report
import com.ikuai.inetspeed.core.designsystem.components.CockpitActionButton
import com.ikuai.inetspeed.core.designsystem.components.CockpitDot
import com.ikuai.inetspeed.core.designsystem.components.CockpitHeader
import com.ikuai.inetspeed.core.designsystem.components.CockpitKeyValueRow
import com.ikuai.inetspeed.core.designsystem.components.CockpitMetricTile
import com.ikuai.inetspeed.core.designsystem.components.CockpitPanel
import com.ikuai.inetspeed.core.designsystem.components.CockpitScreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val reports by viewModel.reports.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val selectedMeasurements by viewModel.selectedMeasurements.collectAsState()
    val exporting = exportState is ReportViewModel.ExportState.Exporting

    CockpitScreen {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CockpitHeader(
                title = "报告",
                subtitle = "Report Orbit · PDF / CSV 导出与归档",
                status = exportState.label(),
            )

            CockpitPanel(title = "生成报告", overline = "Export") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CockpitMetricTile("样本", selectedMeasurements.size.toString(), Modifier.weight(1f))
                    CockpitMetricTile("报告", reports.size.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                    CockpitMetricTile("状态", exportState.label(), Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CockpitActionButton(
                        text = "导出 PDF",
                        onClick = { viewModel.exportPdf() },
                        enabled = !exporting,
                        modifier = Modifier.weight(1f),
                    )
                    CockpitActionButton(
                        text = "导出 CSV",
                        onClick = { viewModel.exportCsv() },
                        enabled = !exporting,
                        modifier = Modifier.weight(1f),
                    )
                }
                ExportStateBlock(exportState)
            }

            CockpitPanel(
                modifier = Modifier.weight(1f),
                title = "历史报告",
                overline = "Archive",
            ) {
                if (reports.isEmpty()) {
                    Text("暂无报告", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(reports, key = { it.id }) { report ->
                            ReportRow(
                                report = report,
                                onDelete = { viewModel.deleteReport(report) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportStateBlock(state: ReportViewModel.ExportState) {
    when (state) {
        is ReportViewModel.ExportState.Exporting -> {
            CockpitKeyValueRow("导出状态", "导出中...")
        }
        is ReportViewModel.ExportState.Success -> {
            CockpitKeyValueRow("导出成功", "${state.format} · ${state.file.name}")
        }
        is ReportViewModel.ExportState.Error -> {
            Text("导出失败: ${state.message}", color = MaterialTheme.colorScheme.error)
        }
        ReportViewModel.ExportState.Idle -> {
            CockpitKeyValueRow("导出状态", "待命")
        }
    }
}

@Composable
private fun ReportRow(
    report: Report,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CockpitDot(if (report.format.lowercase() == "pdf") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                report.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${report.format.uppercase()} · ${formatDate(report.createdAt)} · ${fileSize(report.filePath)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun ReportViewModel.ExportState.label(): String = when (this) {
    ReportViewModel.ExportState.Idle -> "待命"
    ReportViewModel.ExportState.Exporting -> "导出中"
    is ReportViewModel.ExportState.Success -> "完成"
    is ReportViewModel.ExportState.Error -> "失败"
}

private fun fileSize(path: String): String {
    val file = File(path)
    if (!file.exists()) return "0KB"
    return "${file.length() / 1024}KB"
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
