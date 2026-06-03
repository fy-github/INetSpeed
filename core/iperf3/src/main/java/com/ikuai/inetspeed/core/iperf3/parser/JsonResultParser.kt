package com.ikuai.inetspeed.core.iperf3.parser

import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.IpVersion
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.data.model.TestStatus
import org.json.JSONObject

/**
 * iperf3 JSON 输出解析器
 * 解析 -J 参数的 JSON 最终结果
 */
object JsonResultParser {

    /**
     * 解析 JSON 输出为 TestMeasurement
     */
    fun parse(
        json: String,
        testId: Long = 0,
        serverId: Long = 0,
        serverName: String = "",
        serverAddress: String = "",
        serverPort: Int = 5201,
        protocol: Protocol = Protocol.TCP,
        direction: Direction = Direction.FORWARD,
        ipVersion: IpVersion = IpVersion.IPV4,
        durationSeconds: Int = 0,
        parallelStreams: Int = 1,
        rawOutputPath: String? = null,
    ): TestMeasurement {
        val root = JSONObject(json)

        val end = root.optJSONObject("end")
        val sumSent = end?.optJSONObject("sum_sent")
        val sumReceived = end?.optJSONObject("sum_received")

        // 根据方向确定主指标
        val throughputBps = when (direction) {
            Direction.FORWARD -> sumSent?.optDouble("bits_per_second", 0.0) ?: 0.0
            Direction.REVERSE -> sumReceived?.optDouble("bits_per_second", 0.0) ?: 0.0
        }
        val throughputMbps = throughputBps / 1_000_000

        // UDP 特有字段
        val jitterMs = sumSent?.optDouble("jitter_ms", 0.0)
        val packetLossPercent = sumSent?.optDouble("lost_percent", 0.0)

        // TCP 重传
        val retransmits = sumSent?.optInt("retransmits", 0)

        return TestMeasurement(
            timestamp = System.currentTimeMillis(),
            serverId = serverId,
            serverName = serverName,
            serverAddress = serverAddress,
            serverPort = serverPort,
            protocol = protocol.value,
            direction = direction.value,
            ipVersion = ipVersion.value,
            durationSeconds = durationSeconds,
            parallelStreams = parallelStreams,
            throughputMbps = throughputMbps,
            uploadMbps = sumSent?.optDouble("bits_per_second", 0.0)?.div(1_000_000),
            downloadMbps = sumReceived?.optDouble("bits_per_second", 0.0)?.div(1_000_000),
            jitterMs = if (protocol == Protocol.UDP) jitterMs else null,
            packetLossPercent = if (protocol == Protocol.UDP) packetLossPercent else null,
            retransmits = if (protocol == Protocol.TCP) retransmits else null,
            rawOutputPath = rawOutputPath,
            status = TestStatus.COMPLETED.value,
        )
    }

    /**
     * 解析 JSON 中的 interval 数据
     */
    fun parseIntervals(json: String): List<SpeedInterval> {
        val root = JSONObject(json)
        val intervals = root.optJSONArray("intervals") ?: return emptyList()

        val result = mutableListOf<SpeedInterval>()
        for (i in 0 until intervals.length()) {
            val interval = intervals.getJSONObject(i)
            val sum = interval.optJSONObject("sum") ?: continue

            val secondIndex = i
            val bitsPerSecond = sum.optDouble("bits_per_second", 0.0)
            val retransmits = sum.optInt("retransmits", -1).takeIf { it >= 0 }

            result.add(
                SpeedInterval(
                    streamId = 0,
                    secondIndex = secondIndex,
                    bitsPerSecond = bitsPerSecond,
                    retransmits = retransmits,
                    jitterMs = null,
                    packetLossPercent = null,
                    rawLine = interval.toString(),
                )
            )
        }
        return result
    }
}
