package com.ikuai.inetspeed.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ikuai.inetspeed.core.data.model.ToolRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ToolRecord): Long

    @Delete
    suspend fun delete(record: ToolRecord)

    @Query("DELETE FROM tool_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM tool_records ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<ToolRecord>>

    @Query("SELECT * FROM tool_records ORDER BY timestamp DESC")
    suspend fun getAll(): List<ToolRecord>

    @Query("SELECT * FROM tool_records WHERE toolType = :type ORDER BY timestamp DESC")
    suspend fun getByType(type: String): List<ToolRecord>

    @Query("SELECT * FROM tool_records WHERE id = :id")
    suspend fun getById(id: Long): ToolRecord?

    @Query("DELETE FROM tool_records")
    suspend fun deleteAll()
}
