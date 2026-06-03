package com.ikuai.inetspeed.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tool_records")
data class ToolRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val toolType: String,
    val target: String,
    val durationMs: Long? = null,
    val resultSummary: String? = null,
    val rawOutputPath: String? = null,
    val status: String,
    val errorCode: String? = null,
    val isSynced: Boolean = false,
)
