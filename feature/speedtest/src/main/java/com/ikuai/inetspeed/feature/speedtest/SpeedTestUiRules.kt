package com.ikuai.inetspeed.feature.speedtest

import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestState

enum class TestCurve {
    THROUGHPUT,
    LATENCY,
    JITTER,
    PACKET_LOSS,
}

enum class ExpertInputMode {
    CUSTOM,
    CLI,
}

data class RealtimeTestModel(
    val curves: List<TestCurve>,
    val hasLiveData: Boolean,
    val actionLabel: String,
    val progressPercent: Int,
)

data class ExpertPanels(
    val showServerAddress: Boolean,
    val showParameters: Boolean,
    val showCli: Boolean,
    val curves: List<TestCurve>,
)

object SpeedTestUiRules {
    fun curvesFor(protocol: Protocol): List<TestCurve> = when (protocol) {
        Protocol.UDP -> listOf(
            TestCurve.THROUGHPUT,
            TestCurve.LATENCY,
            TestCurve.JITTER,
            TestCurve.PACKET_LOSS,
        )
        Protocol.TCP,
        Protocol.SCTP,
        -> listOf(TestCurve.THROUGHPUT)
    }

    fun realtimeModel(
        protocol: Protocol,
        state: SpeedTestState,
    ): RealtimeTestModel {
        val isActive = state is SpeedTestState.Preparing ||
            state is SpeedTestState.Running ||
            state is SpeedTestState.Cancelling
        return RealtimeTestModel(
            curves = curvesFor(protocol),
            hasLiveData = state is SpeedTestState.Running || state is SpeedTestState.Completed,
            actionLabel = if (isActive) "停止测试" else "开始测试",
            progressPercent = when (state) {
                is SpeedTestState.Running -> state.progressPercent
                is SpeedTestState.Completed -> 100
                else -> 0
            },
        )
    }

    fun expertPanels(
        mode: ExpertInputMode,
        protocol: Protocol,
    ): ExpertPanels = when (mode) {
        ExpertInputMode.CUSTOM -> ExpertPanels(
            showServerAddress = true,
            showParameters = true,
            showCli = false,
            curves = curvesFor(protocol),
        )
        ExpertInputMode.CLI -> ExpertPanels(
            showServerAddress = false,
            showParameters = false,
            showCli = true,
            curves = emptyList(),
        )
    }
}
