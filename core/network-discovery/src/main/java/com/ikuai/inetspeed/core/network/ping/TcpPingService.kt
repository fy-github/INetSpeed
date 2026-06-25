package com.ikuai.inetspeed.core.network.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpPingService @Inject constructor() {

    data class TcpPingResult(
        val host: String,
        val port: Int,
        val reachable: Boolean,
        val latencyMs: Double?,
    )

    suspend fun ping(host: String, port: Int = 5201, timeoutMs: Int = 3000): TcpPingResult =
        withContext(Dispatchers.IO) {
            val socket = Socket()
            try {
                val start = System.currentTimeMillis()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val latency = (System.currentTimeMillis() - start).toDouble()
                TcpPingResult(host, port, reachable = true, latencyMs = latency)
            } catch (_: Exception) {
                TcpPingResult(host, port, reachable = false, latencyMs = null)
            } finally {
                try { socket.close() } catch (e: Exception) {
                    android.util.Log.w("TcpPingService", "Error closing socket", e)
                }
            }
        }
}
