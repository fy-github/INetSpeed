package com.ikuai.inetspeed.core.data.repository

import com.ikuai.inetspeed.core.data.dao.DiagnosticRunDao
import com.ikuai.inetspeed.core.data.dao.ToolRecordDao
import com.ikuai.inetspeed.core.data.model.DiagnosticRun
import com.ikuai.inetspeed.core.data.model.ToolRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolsRepository @Inject constructor(
    private val toolRecordDao: ToolRecordDao,
    private val diagnosticRunDao: DiagnosticRunDao,
) {
    fun getAllToolRecordsFlow(): Flow<List<ToolRecord>> = toolRecordDao.getAllFlow()

    suspend fun insertToolRecord(record: ToolRecord): Long = toolRecordDao.insert(record)

    suspend fun deleteToolRecord(record: ToolRecord) = toolRecordDao.delete(record)

    fun getAllDiagnosticRunsFlow(): Flow<List<DiagnosticRun>> = diagnosticRunDao.getAllFlow()

    suspend fun insertDiagnosticRun(run: DiagnosticRun): Long = diagnosticRunDao.insert(run)

    suspend fun deleteDiagnosticRun(run: DiagnosticRun) = diagnosticRunDao.delete(run)
}
