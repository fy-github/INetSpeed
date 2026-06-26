package com.ikuai.inetspeed.core.network.traceroute

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
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
        val hostRegex = Regex("^[a-zA-Z0-9.:-]+$")
        if (!hostRegex.matches(host)) {
            emit(TracerouteHop(1, "*", null, false))
            return@flow
        }
        for (ttl in 1..maxHops) {
            currentCoroutineContext().ensureActive()
            val hop = traceHop(host, ttl)
            emit(hop)
            if (hop.reachable) break
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun traceHop(host: String, ttl: Int): TracerouteHop {
        return withContext(Dispatchers.IO) {
            try {
                ensureActive()
                val cmd = arrayOf("ping", "-c", "1", "-W", "3", "-t", ttl.toString(), host)
                val process = ProcessBuilder(*cmd).redirectErrorStream(true).start()
                try {
                    // 使用轮询读取，每200ms检查一次取消状态
                    val output = StringBuilder()
                    val reader = process.inputStream.bufferedReader()
                    val startTime = System.currentTimeMillis()
                    val timeoutMs = 8000L // 最大等待8秒

                    while (true) {
                        ensureActive()
                        if (reader.ready()) {
                            val line = reader.readLine() ?: break
                            output.appendLine(line)
                        } else if (System.currentTimeMillis() - startTime > timeoutMs) {
                            break
                        } else {
                            Thread.sleep(200)
                        }
                    }

                    val completed = process.waitFor(1, java.util.concurrent.TimeUnit.SECONDS)
                    if (!completed) {
                        process.destroyForcibly()
                    }

                    val outputStr = output.toString()
                    val latencyMatch = Regex("time=([\\d.]+)").find(outputStr)
                    val latency = latencyMatch?.groupValues?.get(1)?.toDoubleOrNull()

                    val hostMatch = Regex("from ([\\d.a-fA-F:]+)").find(outputStr)
                    val resolvedHost = hostMatch?.groupValues?.get(1) ?: "*"

                    TracerouteHop(
                        hopNumber = ttl,
                        host = resolvedHost,
                        latencyMs = latency,
                        reachable = resolvedHost == host || outputStr.contains("bytes from"),
                    )
                } finally {
                    process.destroyForcibly()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                TracerouteHop(ttl, "*", null, false)
            }
        }
    }
}
