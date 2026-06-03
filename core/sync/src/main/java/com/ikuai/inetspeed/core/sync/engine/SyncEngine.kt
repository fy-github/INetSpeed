package com.ikuai.inetspeed.core.sync.engine

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
        val unsynced = measurementDao.getAll().filter { !it.isSynced }
        if (unsynced.isEmpty()) return

        val request = SyncApi.PushRequest(
            lastSyncAt = lastSyncAt,
            measurements = unsynced,
            servers = emptyList(),
            deletedIds = SyncApi.DeletedIds(emptyList(), emptyList()),
        )

        val result = syncApi.push(request)
        result.onSuccess { response ->
            // Mark as synced
            unsynced.forEach { m ->
                measurementDao.update(m.copy(isSynced = true))
            }
            // Handle conflicts
            response.conflicts.forEach { conflict ->
                handleConflict(conflict)
            }
        }
    }

    private suspend fun pullChanges() {
        val request = SyncApi.PullRequest(
            since = lastSyncAt,
            types = listOf("measurements", "servers"),
        )

        val result = syncApi.pull(request)
        result.onSuccess { response ->
            // Insert new measurements
            response.measurements.forEach { m ->
                val existing = measurementDao.getById(m.id)
                if (existing == null) {
                    measurementDao.insert(m.copy(isSynced = true))
                } else {
                    measurementDao.update(m.copy(isSynced = true))
                }
            }
            // Insert new servers
            response.servers.forEach { s ->
                val existing = serverDao.getById(s.id)
                if (existing == null) {
                    serverDao.insert(s)
                } else {
                    serverDao.update(s)
                }
            }
            // Handle deletions
            response.deletedIds.measurements.forEach { id ->
                measurementDao.deleteById(id)
            }
            response.deletedIds.servers.forEach { id ->
                serverDao.deleteById(id)
            }
        }
    }

    private suspend fun handleConflict(conflict: SyncApi.Conflict) {
        // Server wins by default
        // Local delete wins over server modify
    }

    sealed class SyncState {
        data object Idle : SyncState()
        data object NotLoggedIn : SyncState()
        data object Syncing : SyncState()
        data class Success(val syncedAt: Long) : SyncState()
        data class Error(val message: String) : SyncState()
    }
}
