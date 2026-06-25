package com.ikuai.inetspeed.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import kotlinx.coroutines.flow.Flow

@Dao
interface TestMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: TestMeasurement): Long

    @Update
    suspend fun update(measurement: TestMeasurement)

    @Delete
    suspend fun delete(measurement: TestMeasurement)

    @Query("DELETE FROM test_measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM test_measurements ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TestMeasurement>>

    @Query("SELECT * FROM test_measurements ORDER BY timestamp DESC")
    suspend fun getAll(): List<TestMeasurement>

    @Query("SELECT * FROM test_measurements WHERE id = :id")
    suspend fun getById(id: Long): TestMeasurement?

    @Query("SELECT * FROM test_measurements WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<TestMeasurement?>

    @Query("SELECT * FROM test_measurements WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    suspend fun getByTimeRange(start: Long, end: Long): List<TestMeasurement>

    @Query("SELECT * FROM test_measurements WHERE serverId = :serverId ORDER BY timestamp DESC")
    suspend fun getByServerId(serverId: Long): List<TestMeasurement>

    @Query("SELECT * FROM test_measurements WHERE protocol = :protocol ORDER BY timestamp DESC")
    suspend fun getByProtocol(protocol: String): List<TestMeasurement>

    @Query("""
        SELECT * FROM test_measurements 
        WHERE (:serverId IS NULL OR serverId = :serverId)
          AND (:protocol IS NULL OR protocol = :protocol)
          AND (:startTime IS NULL OR timestamp >= :startTime)
          AND (:endTime IS NULL OR timestamp <= :endTime)
        ORDER BY timestamp DESC
    """)
    suspend fun getFiltered(
        serverId: Long? = null,
        protocol: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
    ): List<TestMeasurement>

    @Query("SELECT * FROM test_measurements ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TestMeasurement>

    @Query("SELECT COUNT(*) FROM test_measurements")
    suspend fun getCount(): Int

    @Query("DELETE FROM test_measurements")
    suspend fun deleteAll()

    @Query("SELECT * FROM test_measurements WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TestMeasurement>
}
