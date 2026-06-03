package com.ikuai.inetspeed.core.data.repository

import com.ikuai.inetspeed.core.data.dao.ServerDao
import com.ikuai.inetspeed.core.data.dao.TestMeasurementDao
import com.ikuai.inetspeed.core.data.file.RawOutputManager
import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestRepository @Inject constructor(
    private val measurementDao: TestMeasurementDao,
    private val rawOutputManager: RawOutputManager,
) {
    fun getAllFlow(): Flow<List<TestMeasurement>> = measurementDao.getAllFlow()

    suspend fun getAll(): List<TestMeasurement> = measurementDao.getAll()

    suspend fun getById(id: Long): TestMeasurement? = measurementDao.getById(id)

    suspend fun getRecent(limit: Int): List<TestMeasurement> = measurementDao.getRecent(limit)

    suspend fun getFiltered(
        serverId: Long? = null,
        protocol: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
    ): List<TestMeasurement> = measurementDao.getFiltered(serverId, protocol, startTime, endTime)

    suspend fun insert(measurement: TestMeasurement): Long = measurementDao.insert(measurement)

    suspend fun delete(measurement: TestMeasurement) {
        measurement.rawOutputPath?.let { rawOutputManager.delete(it) }
        measurementDao.delete(measurement)
    }

    suspend fun deleteById(id: Long) {
        val measurement = measurementDao.getById(id)
        measurement?.rawOutputPath?.let { rawOutputManager.delete(it) }
        measurementDao.deleteById(id)
    }

    suspend fun getCount(): Int = measurementDao.getCount()
}

@Singleton
class ServerRepository @Inject constructor(
    private val serverDao: ServerDao,
) {
    fun getAllFlow(): Flow<List<Server>> = serverDao.getAllFlow()

    suspend fun getAll(): List<Server> = serverDao.getAll()

    suspend fun getById(id: Long): Server? = serverDao.getById(id)

    suspend fun getBuiltIn(): List<Server> = serverDao.getBuiltIn()

    suspend fun getCustom(): List<Server> = serverDao.getCustom()

    suspend fun search(query: String): List<Server> = serverDao.search(query)

    suspend fun insert(server: Server): Long = serverDao.insert(server)

    suspend fun update(server: Server) = serverDao.update(server)

    suspend fun delete(server: Server) = serverDao.delete(server)

    suspend fun updateLatency(id: Long, latency: Double) {
        serverDao.updateLatency(id, latency, System.currentTimeMillis())
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        serverDao.updateFavorite(id, isFavorite)
    }

    suspend fun initBuiltInServers() {
        if (serverDao.getCount() == 0) {
            val builtIn = listOf(
                Server(name = "北京节点", address = "bj-iperf.ikuai.com", port = 5201, isBuiltIn = true, region = "华北"),
                Server(name = "上海节点", address = "sh-iperf.ikuai.com", port = 5201, isBuiltIn = true, region = "华东"),
                Server(name = "香港节点", address = "hk-iperf.ikuai.com", port = 5201, isBuiltIn = true, region = "港澳"),
                Server(name = "新加坡节点", address = "sg-iperf.ikuai.com", port = 5201, isBuiltIn = true, region = "东南亚"),
            )
            serverDao.insertAll(builtIn)
        }
    }
}
