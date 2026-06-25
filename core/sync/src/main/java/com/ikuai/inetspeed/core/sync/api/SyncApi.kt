package com.ikuai.inetspeed.core.sync.api

import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApi {

    @POST("sync/push")
    suspend fun push(@Body request: PushRequest): Response<PushResponse>

    @POST("sync/pull")
    suspend fun pull(@Body request: PullRequest): Response<PullResponse>

    @POST("sync/upload-report")
    suspend fun uploadReport(
        @Query("reportId") reportId: Long,
        @Body filePath: String,
    ): Response<String>

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
}
