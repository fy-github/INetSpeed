package com.ikuai.inetspeed.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ikuai.inetspeed.core.data.model.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: Report): Long

    @Delete
    suspend fun delete(report: Report)

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<Report>>

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    suspend fun getAll(): List<Report>

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: Long): Report?
}
