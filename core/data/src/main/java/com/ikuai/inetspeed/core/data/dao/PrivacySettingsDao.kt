package com.ikuai.inetspeed.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ikuai.inetspeed.core.data.model.PrivacySettings
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivacySettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: PrivacySettings)

    @Query("SELECT * FROM privacy_settings WHERE id = 1")
    suspend fun get(): PrivacySettings?

    @Query("SELECT * FROM privacy_settings WHERE id = 1")
    fun getFlow(): Flow<PrivacySettings?>

    @Query("""
        UPDATE privacy_settings SET
            cloudSyncEnabled = :cloudSyncEnabled,
            includeIpInExport = :includeIpInExport,
            includeDomainInExport = :includeDomainInExport,
            includeRouteInExport = :includeRouteInExport,
            includeRawOutput = :includeRawOutput,
            anonymizePublicIp = :anonymizePublicIp,
            macAddressDisplayMode = :macAddressDisplayMode
        WHERE id = 1
    """)
    suspend fun update(
        cloudSyncEnabled: Boolean,
        includeIpInExport: Boolean,
        includeDomainInExport: Boolean,
        includeRouteInExport: Boolean,
        includeRawOutput: Boolean,
        anonymizePublicIp: Boolean,
        macAddressDisplayMode: String,
    )
}
