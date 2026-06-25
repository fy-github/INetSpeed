package com.ikuai.inetspeed.core.data.repository

import com.ikuai.inetspeed.core.data.dao.ReportDao
import com.ikuai.inetspeed.core.data.model.Report
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val reportDao: ReportDao,
) {
    fun getAllFlow(): Flow<List<Report>> = reportDao.getAllFlow()

    suspend fun getAll(): List<Report> = reportDao.getAll()

    suspend fun getById(id: Long): Report? = reportDao.getById(id)

    suspend fun insert(report: Report): Long = reportDao.insert(report)

    suspend fun delete(report: Report) = reportDao.delete(report)

    suspend fun deleteById(id: Long) = reportDao.deleteById(id)
}
