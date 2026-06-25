package com.ikuai.inetspeed.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // TestMeasurement indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_measurements_timestamp` ON `test_measurements` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_measurements_serverId` ON `test_measurements` (`serverId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_measurements_protocol` ON `test_measurements` (`protocol`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_measurements_isSynced` ON `test_measurements` (`isSynced`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_test_measurements_serverId_protocol_timestamp` ON `test_measurements` (`serverId`, `protocol`, `timestamp`)")
        // Server indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_servers_address` ON `servers` (`address`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_servers_isBuiltIn` ON `servers` (`isBuiltIn`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_servers_isFavorite` ON `servers` (`isFavorite`)")
        // ToolRecord indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tool_records_timestamp` ON `tool_records` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tool_records_toolType` ON `tool_records` (`toolType`)")
        // DiagnosticRun indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diagnostic_runs_timestamp` ON `diagnostic_runs` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diagnostic_runs_toolType` ON `diagnostic_runs` (`toolType`)")
        // Report indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reports_createdAt` ON `reports` (`createdAt`)")
    }
}
