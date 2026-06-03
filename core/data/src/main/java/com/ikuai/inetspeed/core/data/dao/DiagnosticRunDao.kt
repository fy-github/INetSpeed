package com.ikuai.inetspeed.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ikuai.inetspeed.core.data.model.DiagnosticRun
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticRunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: DiagnosticRun): Long

    @Delete
    suspend fun delete(run: DiagnosticRun)

    @Query("SELECT * FROM diagnostic_runs ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<DiagnosticRun>>

    @Query("SELECT * FROM diagnostic_runs WHERE toolType = :type ORDER BY timestamp DESC")
    suspend fun getByType(type: String): List<DiagnosticRun>

    @Query("SELECT * FROM diagnostic_runs WHERE id = :id")
    suspend fun getById(id: Long): DiagnosticRun?
}
