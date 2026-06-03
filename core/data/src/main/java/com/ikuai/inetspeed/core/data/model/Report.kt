package com.ikuai.inetspeed.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val title: String,
    val measurementIds: String,
    val format: String,
    val filePath: String,
    val isSynced: Boolean = false,
)
