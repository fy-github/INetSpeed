package com.ikuai.inetspeed.core.sync.engine

import android.util.Log
import com.google.gson.Gson
import com.ikuai.inetspeed.core.sync.api.SyncApi
import com.ikuai.inetspeed.core.sync.auth.AuthManager
import com.ikuai.inetspeed.core.data.dao.ServerDao
import com.ikuai.inetspeed.core.data.dao.TestMeasurementDao
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncEngine @Inject constructor(
    private val syncApi: SyncApi,
    private val authManager: AuthManager,
    private val measurementDao: TestMeasurementDao,
    private val serverDao: ServerDao,
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var lastSyncAt: Long = 0

    suspend fun sync() {
        if (!authManager.isLoggedIn.value) {
            _syncState.value = SyncState.NotLoggedIn
            return
        }

        _syncState.value = SyncState.Syncing

        try {
            // Push local changes
            pushChanges()

            // Pull remote changes
            pullChanges()

            lastSyncAt = System.currentTimeMillis()
            _syncState.value = SyncState.Success(lastSyncAt)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun syncWithRetry(maxRetries: Int = 3) {
        var attempt = 0
        while (attempt < maxRetries) {
            sync()
            if (_syncState.value is SyncState.Success) return
            attempt++
            if (attempt < maxRetries) {
                delay(1000L * attempt) // Exponential backoff
            }
        }
    }

    private suspend fun pushChanges() {
        val unsynced = measurementDao.getUnsynced()
        if (unsynced.isEmpty()) return

        val request = SyncApi.PushRequest(
            lastSyncAt = lastSyncAt,
            measurements = unsynced,
            servers = emptyList(),
            deletedIds = SyncApi.DeletedIds(emptyList(), emptyList()),
        )

        val response = syncApi.push(request)
        if (response.isSuccessful) {
            val body = response.body()!!
            unsynced.forEach { m ->
                measurementDao.update(m.copy(isSynced = true))
            }
            body.conflicts.forEach { conflict ->
                handleConflict(conflict)
            }
        } else {
            throw Exception("Push failed: ${response.code()} ${response.message()}")
        }
    }

    private suspend fun pullChanges() {
        val request = SyncApi.PullRequest(
            since = lastSyncAt,
            types = listOf("measurements", "servers"),
        )

        val response = syncApi.pull(request)
        if (response.isSuccessful) {
            val body = response.body()!!
            body.measurements.forEach { m ->
                val existing = measurementDao.getById(m.id)
                if (existing == null) {
                    measurementDao.insert(m.copy(isSynced = true))
                } else {
                    measurementDao.update(m.copy(isSynced = true))
                }
            }
            body.servers.forEach { s ->
                val existing = serverDao.getById(s.id)
                if (existing == null) {
                    serverDao.insert(s)
                } else {
                    serverDao.update(s)
                }
            }
            body.deletedIds.measurements.forEach { id ->
                measurementDao.deleteById(id)
            }
            body.deletedIds.servers.forEach { id ->
                serverDao.deleteById(id)
            }
        } else {
            throw Exception("Pull failed: ${response.code()} ${response.message()}")
        }
    }

    private val gson = Gson()

    private suspend fun handleConflict(conflict: SyncApi.Conflict) {
        when (conflict.resolution) {
            "server_wins" -> {
                conflict.serverVersion?.let { serverData ->
                    when (conflict.type) {
                        "measurement" -> {
                            val local = measurementDao.getById(conflict.localId)
                            if (local != null) {
                                val serverMeasurement = gson.fromJson(
                                    gson.toJson(serverData), TestMeasurement::class.java
                                )
                                measurementDao.update(serverMeasurement.copy(isSynced = true))
                                Log.i("SyncEngine", "Applied server version for measurement#${conflict.localId}")
                            }
                        }
                        "server" -> {
                            val local = serverDao.getById(conflict.localId)
                            if (local != null) {
                                val serverServer = gson.fromJson(
                                    gson.toJson(serverData), com.ikuai.inetspeed.core.data.model.Server::class.java
                                )
                                serverDao.update(serverServer)
                                Log.i("SyncEngine", "Applied server version for server#${conflict.localId}")
                            }
                        }
                    }
                } ?: Log.w("SyncEngine", "server_wins but no serverVersion data for ${conflict.type}#${conflict.localId}")
            }
            "local_wins" -> {
                Log.i("SyncEngine", "Conflict resolved: local wins for ${conflict.type}#${conflict.localId}")
            }
            "delete" -> {
                when (conflict.type) {
                    "measurement" -> measurementDao.deleteById(conflict.localId)
                    "server" -> serverDao.deleteById(conflict.localId)
                }
                Log.i("SyncEngine", "Conflict resolved: deleted ${conflict.type}#${conflict.localId}")
            }
            else -> {
                Log.w("SyncEngine", "Unknown conflict resolution: ${conflict.resolution} for ${conflict.type}#${conflict.localId}")
            }
        }
    }

    sealed class SyncState {
        data object Idle : SyncState()
        data object NotLoggedIn : SyncState()
        data object Syncing : SyncState()
        data class Success(val syncedAt: Long) : SyncState()
        data class Error(val message: String) : SyncState()
    }
}
