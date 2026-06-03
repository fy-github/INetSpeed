package com.ikuai.inetspeed.core.iperf3.di

import com.ikuai.inetspeed.core.iperf3.Iperf3Runner
import com.ikuai.inetspeed.core.iperf3.runner.ProcessRunner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class Iperf3Module {

    @Binds
    @Singleton
    abstract fun bindIperf3Runner(impl: ProcessRunner): Iperf3Runner
}
