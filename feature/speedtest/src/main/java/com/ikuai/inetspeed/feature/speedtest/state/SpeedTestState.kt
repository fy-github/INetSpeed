package com.ikuai.inetspeed.feature.speedtest.state

import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.IpVersion
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.core.data.model.TestStatus
import com.ikuai.inetspeed.core.data.error.ErrorCode

/**
 * 测速状态机
 * 状态转换: Idle → Preparing → Running → Completed/Failed/Cancelled
 *          Running → Cancelling → Cancelled/Failed
 */
sealed class SpeedTestState {
    data object Idle : SpeedTestState()
    data object Preparing : SpeedTestState()
    data class Running(
        val currentMbps: Double = 0.0,
        val latencyMs: Double? = null,
        val jitterMs: Double? = null,
        val packetLossPercent: Double? = null,
        val progressPercent: Int = 0,
        val elapsedSeconds: Int = 0,
    ) : SpeedTestState()
    data object Cancelling : SpeedTestState()
    data class Completed(
        val throughputMbps: Double,
        val latencyMs: Double?,
        val jitterMs: Double?,
        val packetLossPercent: Double?,
    ) : SpeedTestState()
    data class Failed(val errorCode: ErrorCode, val message: String) : SpeedTestState()
    data object Cancelled : SpeedTestState()
}

data class SpeedTestConfig(
    val protocol: Protocol = Protocol.TCP,
    val direction: Direction = Direction.FORWARD,
    val ipVersion: IpVersion = IpVersion.IPV4,
    val durationSeconds: Int = 10,
    val parallelStreams: Int = 1,
    val udpBandwidth: String? = null,
    val windowSize: String? = null,
    val bufferLength: String? = null,
    val isExpertMode: Boolean = false,
)
