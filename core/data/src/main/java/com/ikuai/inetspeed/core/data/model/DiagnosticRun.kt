package com.ikuai.inetspeed.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_runs")
data class DiagnosticRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val toolType: String,
    val target: String,
    val hops: String? = null,
    val avgLatencyMs: Double? = null,
    val minLatencyMs: Double? = null,
    val maxLatencyMs: Double? = null,
    val packetLossPercent: Double? = null,
    val networkType: String? = null,
    val localIp: String? = null,
    val gateway: String? = null,
    val dns: String? = null,
    val rawOutputPath: String? = null,
    val isSynced: Boolean = false,
)
