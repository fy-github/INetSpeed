package com.ikuai.inetspeed.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ikuai.inetspeed.core.data.dao.DiagnosticRunDao
import com.ikuai.inetspeed.core.data.dao.PrivacySettingsDao
import com.ikuai.inetspeed.core.data.dao.ReportDao
import com.ikuai.inetspeed.core.data.dao.ServerDao
import com.ikuai.inetspeed.core.data.dao.TestMeasurementDao
import com.ikuai.inetspeed.core.data.dao.ToolRecordDao
import com.ikuai.inetspeed.core.data.model.DiagnosticRun
import com.ikuai.inetspeed.core.data.model.PrivacySettings
import com.ikuai.inetspeed.core.data.model.Report
import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.data.model.ToolRecord

@Database(
    entities = [
        TestMeasurement::class,
        Server::class,
        Report::class,
        ToolRecord::class,
        DiagnosticRun::class,
        PrivacySettings::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class INetSpeedDatabase : RoomDatabase() {
    abstract fun testMeasurementDao(): TestMeasurementDao
    abstract fun serverDao(): ServerDao
    abstract fun reportDao(): ReportDao
    abstract fun toolRecordDao(): ToolRecordDao
    abstract fun diagnosticRunDao(): DiagnosticRunDao
    abstract fun privacySettingsDao(): PrivacySettingsDao
}
