package com.ikuai.inetspeed.core.iperf3.model

import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.IpVersion
import com.ikuai.inetspeed.core.data.model.Protocol

data class IperfRequest(
    val testId: String,
    val host: String,
    val port: Int = 5201,
    val protocol: Protocol = Protocol.TCP,
    val direction: Direction = Direction.FORWARD,
    val ipVersion: IpVersion = IpVersion.IPV4,
    val durationSeconds: Int = 10,
    val parallelStreams: Int = 1,
    val udpBandwidth: String? = null,
    val windowSize: String? = null,
    val bufferLength: String? = null,
    val useJson: Boolean = false,
)

data class IperfVersion(
    val version: String,
    val rawOutput: String,
)

data class BinaryValidationResult(
    val isValid: Boolean,
    val exists: Boolean,
    val isExecutable: Boolean,
    val sizeMatches: Boolean,
    val hashMatches: Boolean,
    val error: String? = null,
)

sealed class IperfEvent {
    data class Interval(val data: com.ikuai.inetspeed.core.iperf3.parser.SpeedInterval) : IperfEvent()
    data class Completed(val result: com.ikuai.inetspeed.core.data.model.TestMeasurement) : IperfEvent()
    data class Failed(val error: com.ikuai.inetspeed.core.data.error.INetSpeedException) : IperfEvent()
    data class Progress(val percent: Int, val currentMbps: Double) : IperfEvent()
}

data class SctpCapability(
    val supported: Boolean,
    val errorMessage: String? = null,
)
