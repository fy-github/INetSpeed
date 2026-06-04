package com.ikuai.inetspeed.core.network.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PingService @Inject constructor() {

    data class PingResult(
        val host: String,
        val reachable: Boolean,
        val avgLatencyMs: Double?,
        val minLatencyMs: Double?,
        val maxLatencyMs: Double?,
        val packetLossPercent: Double?,
    )

    /**
     * Ping 单个主机
     */
    suspend fun ping(host: String, count: Int = 3): PingResult = withContext(Dispatchers.IO) {
        try {
            val cmd = arrayOf("ping", "-c", count.toString(), "-W", "3", host)
            val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val rawOutput = StringBuilder()
            val reader = Thread {
                try {
                    process.inputStream.bufferedReader().forEachLine { line ->
                        rawOutput.appendLine(line)
                    }
                } catch (_: Exception) {}
            }
            reader.start()

            val completed = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
            }
            reader.join(2000)

            parsePingOutput(host, rawOutput.toString())
        } catch (_: Exception) {
            PingResult(host, reachable = false, null, null, null, null)
        }
    }

    /**
     * 并发 Ping 多个主机，返回按延迟排序的结果
     */
    suspend fun pingAndSort(hosts: List<String>): List<PingResult> = withContext(Dispatchers.IO) {
        hosts.map { host ->
            async { ping(host) }
        }.awaitAll().sortedBy { it.avgLatencyMs ?: Double.MAX_VALUE }
    }

    private fun parsePingOutput(host: String, output: String): PingResult {
        val lossMatch = Regex("(\\d+)% packet loss").find(output)
        val packetLoss = lossMatch?.groupValues?.get(1)?.toDoubleOrNull()

        val rttMatch = Regex("rtt min/avg/max/mdev = ([\\d.]+)/([\\d.]+)/([\\d.]+)/([\\d.]+)").find(output)
        val min = rttMatch?.groupValues?.get(1)?.toDoubleOrNull()
        val avg = rttMatch?.groupValues?.get(2)?.toDoubleOrNull()
        val max = rttMatch?.groupValues?.get(3)?.toDoubleOrNull()

        return PingResult(
            host = host,
            reachable = avg != null && (packetLoss == null || packetLoss < 100),
            avgLatencyMs = avg,
            minLatencyMs = min,
            maxLatencyMs = max,
            packetLossPercent = packetLoss,
        )
    }
}
