package com.ikuai.inetspeed.core.data.di

import android.content.Context
import androidx.room.Room
import com.ikuai.inetspeed.core.data.dao.DiagnosticRunDao
import com.ikuai.inetspeed.core.data.dao.PrivacySettingsDao
import com.ikuai.inetspeed.core.data.dao.ReportDao
import com.ikuai.inetspeed.core.data.dao.ServerDao
import com.ikuai.inetspeed.core.data.dao.TestMeasurementDao
import com.ikuai.inetspeed.core.data.dao.ToolRecordDao
import com.ikuai.inetspeed.core.data.db.INetSpeedDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): INetSpeedDatabase {
        return Room.databaseBuilder(
            context,
            INetSpeedDatabase::class.java,
            "inetspeed.db",
        ).build()
    }

    @Provides
    fun provideTestMeasurementDao(db: INetSpeedDatabase): TestMeasurementDao = db.testMeasurementDao()

    @Provides
    fun provideServerDao(db: INetSpeedDatabase): ServerDao = db.serverDao()

    @Provides
    fun provideReportDao(db: INetSpeedDatabase): ReportDao = db.reportDao()

    @Provides
    fun provideToolRecordDao(db: INetSpeedDatabase): ToolRecordDao = db.toolRecordDao()

    @Provides
    fun provideDiagnosticRunDao(db: INetSpeedDatabase): DiagnosticRunDao = db.diagnosticRunDao()

    @Provides
    fun providePrivacySettingsDao(db: INetSpeedDatabase): PrivacySettingsDao = db.privacySettingsDao()
}
