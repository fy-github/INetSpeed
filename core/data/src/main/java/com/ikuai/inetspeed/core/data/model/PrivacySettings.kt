package com.ikuai.inetspeed.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "privacy_settings")
data class PrivacySettings(
    @PrimaryKey val id: Int = 1,
    val cloudSyncEnabled: Boolean = false,
    val includeIpInExport: Boolean = true,
    val includeDomainInExport: Boolean = true,
    val includeRouteInExport: Boolean = false,
    val includeRawOutput: Boolean = false,
    val anonymizePublicIp: Boolean = false,
    val macAddressDisplayMode: String = "random_only",
)
