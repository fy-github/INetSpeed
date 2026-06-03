package com.ikuai.inetspeed.feature.report

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ikuai.inetspeed.core.data.model.Report
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val reports by viewModel.reports.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val selectedMeasurements by viewModel.selectedMeasurements.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("报告") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            // 导出按钮
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "生成报告",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    Text(
                        text = "基于最近 ${selectedMeasurements.size} 条测试记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.exportPdf() },
                            enabled = exportState !is ReportViewModel.ExportState.Exporting,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Text(" PDF", modifier = Modifier.padding(start = 4.dp))
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportCsv() },
                            enabled = exportState !is ReportViewModel.ExportState.Exporting,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null)
                            Text(" CSV", modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    // 导出状态
                    when (val state = exportState) {
                        is ReportViewModel.ExportState.Exporting -> {
                            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                                Text("导出中...")
                            }
                        }
                        is ReportViewModel.ExportState.Success -> {
                            Text(
                                text = "${state.format} 导出成功: ${state.file.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        is ReportViewModel.ExportState.Error -> {
                            Text(
                                text = "导出失败: ${state.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        is ReportViewModel.ExportState.Idle -> {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 历史报告列表
            Text(
                text = "历史报告",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (reports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无报告", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(reports) { report ->
                        ReportCard(
                            report = report,
                            onDelete = { viewModel.deleteReport(report) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: Report,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, fontWeight = FontWeight.Medium)
                Text(
                    text = "${report.format.uppercase()} · ${formatDate(report.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = File(report.filePath).let { "${it.length() / 1024}KB" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
