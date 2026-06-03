package com.ikuai.inetspeed.core.ads.di

import com.ikuai.inetspeed.core.ads.AdManager
import com.ikuai.inetspeed.core.ads.NoOpAdManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdModule {

    @Binds
    @Singleton
    abstract fun bindAdManager(impl: NoOpAdManager): AdManager
}
