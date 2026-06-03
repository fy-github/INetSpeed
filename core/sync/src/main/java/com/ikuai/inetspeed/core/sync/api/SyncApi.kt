package com.ikuai.inetspeed.core.sync.api

import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncApi @Inject constructor(
    private val authManager: com.ikuai.inetspeed.core.sync.auth.AuthManager,
) {
    // TODO: Replace with real Retrofit interface
    // This is a placeholder implementation for M8 architecture

    data class PushRequest(
        val lastSyncAt: Long,
        val measurements: List<TestMeasurement>,
        val servers: List<Server>,
        val deletedIds: DeletedIds,
    )

    data class DeletedIds(
        val measurements: List<Long>,
        val servers: List<Long>,
    )

    data class PushResponse(
        val syncedAt: Long,
        val conflicts: List<Conflict>,
        val errors: List<String>,
    )

    data class PullRequest(
        val since: Long,
        val types: List<String>,
    )

    data class PullResponse(
        val syncedAt: Long,
        val measurements: List<TestMeasurement>,
        val servers: List<Server>,
        val deletedIds: DeletedIds,
    )

    data class Conflict(
        val type: String,
        val localId: Long,
        val serverVersion: Map<String, Any>?,
        val resolution: String,
    )

    suspend fun push(request: PushRequest): Result<PushResponse> {
        // TODO: Implement real API call
        return Result.failure(NotImplementedError("Sync API not configured"))
    }

    suspend fun pull(request: PullRequest): Result<PullResponse> {
        // TODO: Implement real API call
        return Result.failure(NotImplementedError("Sync API not configured"))
    }

    suspend fun uploadReport(reportId: Long, filePath: String): Result<String> {
        // TODO: Implement real API call
        return Result.failure(NotImplementedError("Sync API not configured"))
    }
}
