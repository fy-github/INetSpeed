package com.ikuai.inetspeed.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String,
    val port: Int = 5201,
    val isBuiltIn: Boolean,
    val region: String? = null,
    val lastLatencyMs: Double? = null,
    val isFavorite: Boolean = false,
    val lastTestedAt: Long? = null,
)
