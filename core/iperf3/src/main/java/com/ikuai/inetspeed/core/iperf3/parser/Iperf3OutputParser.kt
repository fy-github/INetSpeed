package com.ikuai.inetspeed.core.iperf3.parser

import java.util.regex.Pattern

/**
 * iperf3 interval 输出解析器
 * 基于 M0.5 PoC 验证的正则和逻辑
 */
object Iperf3OutputParser {

    // TCP interval: [  5] 0.00-1.00  sec  28.5 MBytes  239 Mbits/sec
    private val TCP_INTERVAL_PATTERN = Pattern.compile(
        """\[\s*(\d+)]\s+(\d+\.\d+)-(\d+\.\d+)\s+sec\s+([\d.]+)\s+(\w+)\s+([\d.]+)\s+(\w+/sec)"""
    )

    // UDP interval: [  5] 0.00-1.00  sec  1.25 MBytes  10.5 Mbits/sec  0.234 ms  0/1000 (0%)
    private val UDP_INTERVAL_PATTERN = Pattern.compile(
        """\[\s*(\d+)]\s+(\d+\.\d+)-(\d+\.\d+)\s+sec\s+([\d.]+)\s+(\w+)\s+([\d.]+)\s+(\w+/sec)\s+([\d.]+)\s+ms\s+(\d+)/(\d+)\s+\(([\d.]+)%\)"""
    )

    /**
     * 解析单行 interval 输出
     * @return SpeedInterval 或 null（非 interval 行）
     */
    fun parseIntervalLine(line: String): SpeedInterval? {
        // 先尝试 UDP 格式（更长，包含 jitter/loss）
        val udpMatcher = UDP_INTERVAL_PATTERN.matcher(line)
        if (udpMatcher.matches()) {
            return SpeedInterval(
                streamId = udpMatcher.group(1)!!.toInt(),
                secondIndex = udpMatcher.group(2)!!.toDouble().toInt(),
                bitsPerSecond = toBitsPerSecond(
                    udpMatcher.group(6)!!.toDouble(),
                    udpMatcher.group(7)!!
                ),
                retransmits = null,
                jitterMs = udpMatcher.group(8)!!.toDouble(),
                packetLossPercent = udpMatcher.group(11)!!.toDouble(),
                rawLine = line,
            )
        }

        // 尝试 TCP 格式
        val tcpMatcher = TCP_INTERVAL_PATTERN.matcher(line)
        if (tcpMatcher.matches()) {
            return SpeedInterval(
                streamId = tcpMatcher.group(1)!!.toInt(),
                secondIndex = tcpMatcher.group(2)!!.toDouble().toInt(),
                bitsPerSecond = toBitsPerSecond(
                    tcpMatcher.group(6)!!.toDouble(),
                    tcpMatcher.group(7)!!
                ),
                retransmits = null,
                jitterMs = null,
                packetLossPercent = null,
                rawLine = line,
            )
        }

        return null
    }

    /**
     * 解析所有输出行，提取 interval 列表
     */
    fun parseIntervals(output: String): List<SpeedInterval> {
        return output.lines()
            .mapNotNull { parseIntervalLine(it) }
    }

    /**
     * 将带宽值转换为 bits/sec
     */
    private fun toBitsPerSecond(value: Double, unit: String): Double {
        val normalizedUnit = unit.lowercase().replace("/sec", "")
        val multiplier = when (normalizedUnit) {
            "bits" -> 1.0
            "kbits" -> 1_000.0
            "mbits" -> 1_000_000.0
            "gbits" -> 1_000_000_000.0
            "bytes" -> 8.0
            "kbytes" -> 8_000.0
            "mbytes" -> 8_000_000.0
            "gbytes" -> 8_000_000_000.0
            else -> 1.0
        }
        return value * multiplier
    }
}

/**
 * iperf3 interval 输出数据
 */
data class SpeedInterval(
    val streamId: Int,
    val secondIndex: Int,
    val bitsPerSecond: Double,
    val retransmits: Int?,
    val jitterMs: Double?,
    val packetLossPercent: Double?,
    val rawLine: String,
) {
    val megabitsPerSecond: Double get() = bitsPerSecond / 1_000_000
    val isUdp: Boolean get() = jitterMs != null
}
