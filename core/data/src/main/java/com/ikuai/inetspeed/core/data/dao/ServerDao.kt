package com.ikuai.inetspeed.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ikuai.inetspeed.core.data.model.Server
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: Server): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(servers: List<Server>)

    @Update
    suspend fun update(server: Server)

    @Delete
    suspend fun delete(server: Server)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM servers ORDER BY isFavorite DESC, lastLatencyMs ASC")
    fun getAllFlow(): Flow<List<Server>>

    @Query("SELECT * FROM servers ORDER BY isFavorite DESC, lastLatencyMs ASC")
    suspend fun getAll(): List<Server>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: Long): Server?

    @Query("SELECT * FROM servers WHERE isBuiltIn = 1")
    suspend fun getBuiltIn(): List<Server>

    @Query("SELECT * FROM servers WHERE isBuiltIn = 0")
    suspend fun getCustom(): List<Server>

    @Query("SELECT * FROM servers WHERE isFavorite = 1")
    suspend fun getFavorites(): List<Server>

    @Query("SELECT * FROM servers WHERE address LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<Server>

    @Query("UPDATE servers SET lastLatencyMs = :latency, lastTestedAt = :testedAt WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Double, testedAt: Long)

    @Query("UPDATE servers SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun getCount(): Int
}
