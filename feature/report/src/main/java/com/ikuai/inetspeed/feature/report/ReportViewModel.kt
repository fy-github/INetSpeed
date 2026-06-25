package com.ikuai.inetspeed.feature.report

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.repository.ReportRepository
import com.ikuai.inetspeed.core.data.export.ReportExporter
import com.ikuai.inetspeed.core.data.model.Report
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.data.dao.PrivacySettingsDao
import com.ikuai.inetspeed.core.data.model.PrivacySettings
import com.ikuai.inetspeed.core.data.repository.TestRepository
import com.ikuai.inetspeed.core.privacy.DataMasker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val testRepository: TestRepository,
    private val reportRepository: ReportRepository,
    private val reportExporter: ReportExporter,
    private val dataMasker: DataMasker,
    private val privacySettingsDao: PrivacySettingsDao,
) : ViewModel() {

    val reports = reportRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMeasurements = MutableStateFlow<List<TestMeasurement>>(emptyList())
    val selectedMeasurements: StateFlow<List<TestMeasurement>> = _selectedMeasurements.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    init {
        viewModelScope.launch {
            val recent = testRepository.getRecent(10)
            _selectedMeasurements.value = recent
        }
    }

    fun exportCsv(title: String = "INetSpeed测试报告") {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val settings = privacySettingsDao.get() ?: PrivacySettings()
                val maskedMeasurements = _selectedMeasurements.value.map { m ->
                    m.copy(
                        serverName = if (!settings.includeDomainInExport) dataMasker.maskDomain(m.serverName) else m.serverName,
                        serverAddress = if (!settings.includeIpInExport) dataMasker.maskIpv4(m.serverAddress) else m.serverAddress,
                    )
                }
                val file = reportExporter.exportCsv(maskedMeasurements, title)
                saveReportRecord(title, "csv", file.absolutePath)
                _exportState.value = ExportState.Success(file, "CSV")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "导出失败")
            }
        }
    }

    fun exportPdf(title: String = "INetSpeed测试报告") {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val settings = privacySettingsDao.get() ?: PrivacySettings()
                val maskedMeasurements = _selectedMeasurements.value.map { m ->
                    m.copy(
                        serverName = if (!settings.includeDomainInExport) dataMasker.maskDomain(m.serverName) else m.serverName,
                        serverAddress = if (!settings.includeIpInExport) dataMasker.maskIpv4(m.serverAddress) else m.serverAddress,
                    )
                }
                val file = reportExporter.exportPdf(maskedMeasurements, title)
                saveReportRecord(title, "pdf", file.absolutePath)
                _exportState.value = ExportState.Success(file, "PDF")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "导出失败")
            }
        }
    }

    fun exportPng(bitmap: Bitmap, title: String = "INetSpeed测试报告") {
        viewModelScope.launch {
            _exportState.value = ExportState.Exporting
            try {
                val dir = reportExporter.exportDir
                val file = File(dir, "${title}_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                saveReportRecord(title, "png", file.absolutePath)
                _exportState.value = ExportState.Success(file, "PNG")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "导出失败")
            }
        }
    }

    fun deleteReport(report: Report) {
        viewModelScope.launch {
            File(report.filePath).delete()
            reportRepository.delete(report)
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    private suspend fun saveReportRecord(title: String, format: String, filePath: String) {
        val ids = _selectedMeasurements.value.map { it.id }
        reportRepository.insert(
            Report(
                createdAt = System.currentTimeMillis(),
                title = title,
                measurementIds = ids.joinToString(","),
                format = format,
                filePath = filePath,
            )
        )
    }

    sealed class ExportState {
        data object Idle : ExportState()
        data object Exporting : ExportState()
        data class Success(val file: File, val format: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }
}
