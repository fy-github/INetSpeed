package com.ikuai.inetspeed.core.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "test_measurements",
    indices = [
        Index("timestamp"),
        Index("serverId"),
        Index("protocol"),
        Index("isSynced"),
    ],
)
data class TestMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val serverId: Long,
    val serverName: String,
    val serverAddress: String,
    val serverPort: Int,
    val protocol: String,
    val direction: String,
    val ipVersion: String,
    val durationSeconds: Int,
    val parallelStreams: Int,
    val throughputMbps: Double,
    val uploadMbps: Double? = null,
    val downloadMbps: Double? = null,
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val packetLossPercent: Double? = null,
    val retransmits: Int? = null,
    val rawOutputPath: String? = null,
    val status: String,
    val errorCode: String? = null,
    val isSynced: Boolean = false,
)
