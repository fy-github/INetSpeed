package com.ikuai.inetspeed.core.iperf3.parser

/**
 * M0.5 样例回归测试
 * 基于真实 PoC 输出验证解析器正确性
 */
object ParserFixtures {

    val TCP_INTERVAL_SAMPLE = """
        [  5] 0.00-1.00  sec  28.5 MBytes  239 Mbits/sec
        [  5] 1.00-2.00  sec  30.2 MBytes  253 Mbits/sec
        [  5] 2.00-3.00  sec  31.0 MBytes  260 Mbits/sec
        [  5] 3.00-4.00  sec  30.8 MBytes  258 Mbits/sec
        [  5] 4.00-5.00  sec  29.5 MBytes  247 Mbits/sec
    """.trimIndent()

    val UDP_INTERVAL_SAMPLE = """
        [  5] 0.00-1.00  sec  1.25 MBytes  10.5 Mbits/sec  0.234 ms  0/1000 (0%)
        [  5] 1.00-2.00  sec  1.19 MBytes  10.0 Mbits/sec  0.156 ms  2/955 (0.21%)
        [  5] 2.00-3.00  sec  1.20 MBytes  10.1 Mbits/sec  0.189 ms  0/960 (0%)
    """.trimIndent()

    val TCP_SUM_SAMPLE = """
        [ ID] Interval           Transfer     Bitrate
        [  5] 0.00-5.00  sec   150 MBytes   252 Mbits/sec                  sender
        [  5] 0.00-5.00  sec   150 MBytes   252 Mbits/sec                  receiver
    """.trimIndent()

    val JSON_SAMPLE = """
        {
            "start": {"connected": [{"socket": 5}]},
            "intervals": [
                {"sum": {"seconds": 1.0, "bytes": 29884416, "bits_per_second": 239075328}},
                {"sum": {"seconds": 1.0, "bytes": 31651584, "bits_per_second": 253212672}},
                {"sum": {"seconds": 1.0, "bytes": 32505856, "bits_per_second": 260046848}}
            ],
            "end": {
                "sum_sent": {"bytes": 94041856, "bits_per_second": 250778026.67},
                "sum_received": {"bytes": 94041856, "bits_per_second": 250778026.67}
            }
        }
    """.trimIndent()

    /**
     * 验证 TCP interval 解析
     */
    fun validateTcpParsing(): Boolean {
        val intervals = Iperf3OutputParser.parseIntervals(TCP_INTERVAL_SAMPLE)
        if (intervals.size != 5) return false

        // 验证第一个 interval
        val first = intervals[0]
        if (first.secondIndex != 0) return false
        if (first.bitsPerSecond < 230_000_000 || first.bitsPerSecond > 250_000_000) return false
        if (first.jitterMs != null) return false // TCP 没有 jitter

        // 验证最后一个 interval
        val last = intervals[4]
        if (last.secondIndex != 4) return false

        return true
    }

    /**
     * 验证 UDP interval 解析
     */
    fun validateUdpParsing(): Boolean {
        val intervals = Iperf3OutputParser.parseIntervals(UDP_INTERVAL_SAMPLE)
        if (intervals.size != 3) return false

        // 验证第一个 interval（含 jitter/loss）
        val first = intervals[0]
        if (first.secondIndex != 0) return false
        if (first.jitterMs == null) return false
        if (first.jitterMs!! < 0.2 || first.jitterMs!! > 0.3) return false
        if (first.packetLossPercent == null) return false
        if (first.packetLossPercent!! != 0.0) return false

        // 验证第二个 interval（有丢包）
        val second = intervals[1]
        if (second.packetLossPercent == null) return false
        if (second.packetLossPercent!! < 0.1 || second.packetLossPercent!! > 0.3) return false

        return true
    }

    /**
     * 验证 JSON 解析
     */
    fun validateJsonParsing(): Boolean {
        val result = JsonResultParser.parse(
            json = JSON_SAMPLE,
            serverName = "test",
            serverAddress = "127.0.0.1",
        )

        if (result.throughputMbps < 200 || result.throughputMbps > 300) return false
        if (result.uploadMbps == null) return false
        if (result.downloadMbps == null) return false

        return true
    }

    /**
     * 运行所有回归测试
     */
    fun runAll(): Map<String, Boolean> {
        return mapOf(
            "tcp_interval" to validateTcpParsing(),
            "udp_interval" to validateUdpParsing(),
            "json_result" to validateJsonParsing(),
        )
    }
}
