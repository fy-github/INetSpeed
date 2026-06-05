package com.ikuai.inetspeed.feature.speedtest

import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTestUiRulesTest {

    @Test
    fun tcpModeShowsOnlyThroughputCurve() {
        assertEquals(
            listOf(TestCurve.THROUGHPUT),
            SpeedTestUiRules.curvesFor(Protocol.TCP),
        )
    }

    @Test
    fun udpModeShowsThroughputLatencyJitterAndLossCurves() {
        assertEquals(
            listOf(
                TestCurve.THROUGHPUT,
                TestCurve.LATENCY,
                TestCurve.JITTER,
                TestCurve.PACKET_LOSS,
            ),
            SpeedTestUiRules.curvesFor(Protocol.UDP),
        )
    }

    @Test
    fun simpleIdleStateHasNoDataAndUsesStartAction() {
        val model = SpeedTestUiRules.realtimeModel(
            protocol = Protocol.TCP,
            state = SpeedTestState.Idle,
        )

        assertFalse(model.hasLiveData)
        assertEquals("开始测试", model.actionLabel)
    }

    @Test
    fun simpleRunningStateHasDataAndUsesStopAction() {
        val model = SpeedTestUiRules.realtimeModel(
            protocol = Protocol.UDP,
            state = SpeedTestState.Running(
                currentMbps = 88.4,
                latencyMs = 12.0,
                jitterMs = 2.1,
                packetLossPercent = 0.4,
                progressPercent = 42,
                elapsedSeconds = 4,
            ),
        )

        assertTrue(model.hasLiveData)
        assertEquals("停止测试", model.actionLabel)
        assertEquals(42, model.progressPercent)
    }

    @Test
    fun expertCustomShowsServerParametersAndCurves() {
        val panels = SpeedTestUiRules.expertPanels(
            mode = ExpertInputMode.CUSTOM,
            protocol = Protocol.UDP,
        )

        assertTrue(panels.showServerAddress)
        assertTrue(panels.showParameters)
        assertFalse(panels.showCli)
        assertEquals(4, panels.curves.size)
    }

    @Test
    fun expertCliOnlyShowsCliConsole() {
        val panels = SpeedTestUiRules.expertPanels(
            mode = ExpertInputMode.CLI,
            protocol = Protocol.UDP,
        )

        assertFalse(panels.showServerAddress)
        assertFalse(panels.showParameters)
        assertTrue(panels.showCli)
        assertTrue(panels.curves.isEmpty())
    }
}
