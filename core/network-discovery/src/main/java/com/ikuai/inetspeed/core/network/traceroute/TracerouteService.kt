package com.ikuai.inetspeed.core.network.traceroute

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TracerouteService @Inject constructor() {

    data class TracerouteHop(
        val hopNumber: Int,
        val host: String,
        val latencyMs: Double?,
        val reachable: Boolean,
    )

    /**
     * 执行 traceroute
     * Android 没有原生 traceroute，使用 ping -t TTL 逐跳探测
     */
    fun trace(host: String, maxHops: Int = 30): Flow<TracerouteHop> = flow {
        for (ttl in 1..maxHops) {
            val hop = traceHop(host, ttl)
            emit(hop)
            if (hop.reachable) break
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun traceHop(host: String, ttl: Int): TracerouteHop {
        return try {
            val cmd = arrayOf("ping", "-c", "1", "-W", "3", "-t", ttl.toString(), host)
            val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)

            val latencyMatch = Regex("time=([\\d.]+)").find(output)
            val latency = latencyMatch?.groupValues?.get(1)?.toDoubleOrNull()

            val hostMatch = Regex("from ([\\d.a-fA-F:]+)").find(output)
            val resolvedHost = hostMatch?.groupValues?.get(1) ?: "*"

            TracerouteHop(
                hopNumber = ttl,
                host = resolvedHost,
                latencyMs = latency,
                reachable = resolvedHost == host || output.contains("bytes from"),
            )
        } catch (e: Exception) {
            TracerouteHop(ttl, "*", null, false)
        }
    }
}
