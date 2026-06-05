package com.ikuai.inetspeed.core.iperf3.runner

import android.util.Log
import com.ikuai.inetspeed.core.data.error.ErrorCode
import com.ikuai.inetspeed.core.data.error.INetSpeedException
import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.data.model.TestStatus
import com.ikuai.inetspeed.core.iperf3.Iperf3Runner
import com.ikuai.inetspeed.core.iperf3.model.BinaryValidationResult
import com.ikuai.inetspeed.core.iperf3.model.IperfEvent
import com.ikuai.inetspeed.core.iperf3.model.IperfRequest
import com.ikuai.inetspeed.core.iperf3.model.IperfVersion
import com.ikuai.inetspeed.core.iperf3.model.SctpCapability
import com.ikuai.inetspeed.core.iperf3.parser.SpeedInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeIperf3Runner @Inject constructor() : Iperf3Runner {

    private val activeSockets = ConcurrentHashMap<String, Socket>()

    companion object {
        private const val STATE_TEST_START = 1
        private const val STATE_TEST_RUNNING = 2
        private const val STATE_RESULT_REQUEST = 3
        private const val STATE_TEST_END = 4
        private const val STATE_EXCHANGE_RESULTS = 12
        private const val STATE_DISPLAY_RESULTS = 13
        private const val STATE_PARAM_EXCHANGE = 8
        private const val STATE_CREATE_STREAMS = 9
        private const val COOKIE_SIZE = 37
        private const val TAG = "NativeIperf3"
    }

    override fun run(request: IperfRequest): Flow<IperfEvent> = flow {
        if (request.protocol == Protocol.SCTP) {
            emit(IperfEvent.Failed(INetSpeedException(ErrorCode.PROTOCOL_UNSUPPORTED)))
            return@flow
        }

        val intervals = mutableListOf<SpeedInterval>()
        val controlSocket = Socket()
        var dataSocket: Socket? = null
        val eventChannel = Channel<IperfEvent>(Channel.UNLIMITED)
        val cookie = generateCookie()

        try {
            Log.d(TAG, "Connecting to ${request.host}:${request.port}")
            controlSocket.connect(InetSocketAddress(request.host, request.port), 5000)
            controlSocket.soTimeout = (request.durationSeconds + 15) * 1000
            activeSockets[request.testId] = controlSocket

            val ctrlIn = controlSocket.getInputStream()
            val ctrlOut = controlSocket.getOutputStream()

            Log.d(TAG, "Sending cookie (${cookie.size} bytes)")
            ctrlOut.write(cookie)
            ctrlOut.flush()

            Log.d(TAG, "Reading server state")
            val firstState = readExactly(ctrlIn, 1)
                ?: throw INetSpeedException(ErrorCode.SERVER_UNREACHABLE, mapOf("error" to "No server response"))
            val stateVal = firstState[0].toInt() and 0xFF
            Log.d(TAG, "Got state: $stateVal")

            if (stateVal == STATE_PARAM_EXCHANGE) {
                Log.d(TAG, "PARAM_EXCHANGE - sending parameters")
                sendParameters(ctrlOut, request)
                val createState = readExactly(ctrlIn, 1)
                    ?: throw INetSpeedException(ErrorCode.SERVER_UNREACHABLE)
                val cs = createState[0].toInt() and 0xFF
                Log.d(TAG, "State after params: $cs")
                if (cs != STATE_CREATE_STREAMS) {
                    throw INetSpeedException(ErrorCode.SERVER_UNREACHABLE, mapOf("state" to cs.toString()))
                }
            } else if (stateVal != STATE_CREATE_STREAMS) {
                throw INetSpeedException(ErrorCode.SERVER_UNREACHABLE, mapOf("state" to stateVal.toString()))
            }

            Log.d(TAG, "Opening data stream to ${request.host}:${request.port}")
            dataSocket = Socket()
            dataSocket.connect(InetSocketAddress(request.host, request.port), 5000)
            Log.d(TAG, "Data stream connected: ${dataSocket.isConnected}")
            activeSockets[request.testId + "_data"] = dataSocket

            Log.d(TAG, "Sending cookie on data stream (${cookie.size} bytes)")
            val dos = dataSocket.getOutputStream()
            dos.write(cookie)
            dos.flush()
            Log.d(TAG, "Cookie sent on data stream")

            Log.d(TAG, "Waiting for TEST_START from server")
            val testStartState = readExactly(ctrlIn, 1)
                ?: throw INetSpeedException(ErrorCode.SERVER_UNREACHABLE)
            val ts = testStartState[0].toInt() and 0xFF
            Log.d(TAG, "State after data stream: $ts (expected $STATE_TEST_START)")

            if (ts == STATE_TEST_START) {
                Log.d(TAG, "TEST_START received, waiting for TEST_RUNNING")
                val runningState = readExactly(ctrlIn, 1)
                    ?: throw INetSpeedException(ErrorCode.SERVER_UNREACHABLE)
                val rs = runningState[0].toInt() and 0xFF
                Log.d(TAG, "Running state: $rs")
            } else if (ts == 0xFF.toInt()) {
                Log.e(TAG, "ACCESS_DENIED - cookie rejected")
                throw INetSpeedException(ErrorCode.PERMISSION_DENIED)
            } else {
                Log.w(TAG, "Unexpected state: $ts")
            }

            val testThread = if (request.direction == Direction.REVERSE) {
                Thread { runDownload(request, ctrlIn, dataSocket, intervals, eventChannel) }
            } else {
                Thread { runUpload(request, ctrlIn, dataSocket, intervals, eventChannel) }
            }
            testThread.isDaemon = true
            testThread.start()

            for (event in eventChannel) {
                emit(event)
            }
            testThread.join(5000)

            val result = readResults(ctrlIn, request, intervals)
            emit(IperfEvent.Completed(result))

        } catch (e: INetSpeedException) {
            Log.e(TAG, "Error: ${e.errorCode.code}: ${e.message}")
            emit(IperfEvent.Failed(e))
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.javaClass.simpleName}: ${e.message}", e)
            emit(IperfEvent.Failed(INetSpeedException(ErrorCode.UNKNOWN, cause = e)))
        } finally {
            eventChannel.close()
            activeSockets.remove(request.testId)
            activeSockets.remove(request.testId + "_data")
            try { dataSocket?.close() } catch (_: Exception) {}
            try { controlSocket.close() } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    override fun runCli(testId: String, command: String): Flow<String> = flow {
        emit("ERROR: CLI mode requires the process iperf3 runner")
    }

    override suspend fun cancel(testId: String) {
        withContext(Dispatchers.IO) {
            activeSockets.remove(testId)?.close()
            activeSockets.remove(testId + "_data")?.close()
        }
    }

    override suspend fun version(): IperfVersion = withContext(Dispatchers.IO) {
        IperfVersion("3.x-native", "Native Kotlin iperf3 client")
    }

    override suspend fun validateBinary(): BinaryValidationResult = withContext(Dispatchers.IO) {
        BinaryValidationResult(isValid = true, exists = true, isExecutable = true, sizeMatches = true, hashMatches = true)
    }

    override suspend fun detectSctp(): SctpCapability = SctpCapability(false, "Not supported in native mode")

    private fun generateCookie(): ByteArray {
        val hex = "0123456789abcdef"
        val r = Random()
        return ByteArray(COOKIE_SIZE) { hex[r.nextInt(16)].code.toByte() }
    }

    private fun sendParameters(out: OutputStream, request: IperfRequest) {
        val json = JSONObject().apply {
            put("tcp", request.protocol == Protocol.TCP)
            put("omit", 0)
            put("time", request.durationSeconds)
            put("parallel", request.parallelStreams)
            put("len", 131072)
            put("client_version", "3.19")
        }
        sendJsonMessage(out, json)
    }

    private fun runDownload(
        request: IperfRequest,
        ctrlIn: InputStream,
        dataSocket: Socket,
        intervals: MutableList<SpeedInterval>,
        channel: Channel<IperfEvent>,
    ) {
        try {
            val dataIn = dataSocket.getInputStream()
            val buf = ByteArray(65536)
            val running = AtomicBoolean(true)

            val controlThread = Thread { readControlEvents(ctrlIn, request, intervals, running, channel) }
            controlThread.isDaemon = true
            controlThread.start()

            val testEnd = System.currentTimeMillis() + request.durationSeconds * 1000L + 3000L
            while (running.get() && System.currentTimeMillis() < testEnd) {
                try {
                    val n = dataIn.read(buf)
                    if (n == -1) break
                } catch (_: Exception) { break }
            }
            running.set(false)
            controlThread.join(3000)
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}")
        }
    }

    private fun runUpload(
        request: IperfRequest,
        ctrlIn: InputStream,
        dataSocket: Socket,
        intervals: MutableList<SpeedInterval>,
        channel: Channel<IperfEvent>,
    ) {
        try {
            val dataOut = dataSocket.getOutputStream()
            val data = ByteArray(131072)
            val running = AtomicBoolean(true)

            val controlThread = Thread { readControlEvents(ctrlIn, request, intervals, running, channel) }
            controlThread.isDaemon = true
            controlThread.start()

            val testEnd = System.currentTimeMillis() + request.durationSeconds * 1000L + 3000L
            while (running.get() && System.currentTimeMillis() < testEnd) {
                try {
                    dataOut.write(data)
                    dataOut.flush()
                } catch (_: Exception) { break }
            }
            running.set(false)
            controlThread.join(3000)
        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}")
        }
    }

    private fun readControlEvents(
        ctrlIn: InputStream,
        request: IperfRequest,
        intervals: MutableList<SpeedInterval>,
        running: AtomicBoolean,
        channel: Channel<IperfEvent>,
    ) {
        try {
            while (running.get()) {
                val firstByte = readExactly(ctrlIn, 1) ?: break
                val b = firstByte[0].toInt() and 0xFF

                if (b == 0) {
                    val rest = readExactly(ctrlIn, 3) ?: break
                    val size = ((rest[0].toInt() and 0xFF) shl 16) or
                            ((rest[1].toInt() and 0xFF) shl 8) or
                            (rest[2].toInt() and 0xFF)
                    if (size <= 0 || size > 10_000_000) break
                    val body = readExactly(ctrlIn, size) ?: break
                    val json = String(body, Charsets.UTF_8)
                    tryParseIntervals(json, intervals, request, channel)
                    if (json.contains("\"end\"")) {
                        running.set(false)
                        break
                    }
                } else {
                    when (b) {
                        STATE_TEST_START -> Log.d(TAG, "TEST_START")
                        STATE_TEST_RUNNING -> Log.d(TAG, "TEST_RUNNING")
                        STATE_TEST_END -> {
                            running.set(false)
                            break
                        }
                        else -> Log.d(TAG, "State: $b")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Control error: ${e.message}")
        }
    }

    private fun tryParseIntervals(
        json: String,
        intervals: MutableList<SpeedInterval>,
        request: IperfRequest,
        channel: Channel<IperfEvent>,
    ) {
        try {
            val obj = JSONObject(json)
            if (!obj.has("intervals")) return
            val arr = obj.getJSONArray("intervals")
            for (i in 0 until arr.length()) {
                val interval = arr.optJSONObject(i) ?: continue
                val sum = interval.optJSONObject("sum") ?: continue
                val bps = sum.optDouble("bits_per_second", 0.0)
                val retransmits = sum.optInt("retransmits", 0)
                val iv = SpeedInterval(
                    streamId = 0, secondIndex = intervals.size,
                    bitsPerSecond = bps, retransmits = retransmits,
                    jitterMs = null, packetLossPercent = null, rawLine = "",
                )
                intervals.add(iv)
                channel.trySend(IperfEvent.Interval(iv))
                val pct = (intervals.size * 100) / request.durationSeconds
                channel.trySend(IperfEvent.Progress(pct.coerceAtMost(100), iv.megabitsPerSecond))
            }
        } catch (_: Exception) {}
    }

    private fun readResults(
        ctrlIn: InputStream,
        request: IperfRequest,
        intervals: List<SpeedInterval>,
    ): TestMeasurement {
        try {
            while (true) {
                val firstByte = readExactly(ctrlIn, 1) ?: break
                val b = firstByte[0].toInt() and 0xFF
                if (b == 0) {
                    val rest = readExactly(ctrlIn, 3) ?: break
                    val size = ((rest[0].toInt() and 0xFF) shl 16) or
                            ((rest[1].toInt() and 0xFF) shl 8) or
                            (rest[2].toInt() and 0xFF)
                    if (size <= 0 || size > 10_000_000) break
                    val body = readExactly(ctrlIn, size) ?: break
                    val json = String(body, Charsets.UTF_8)
                    if (json.contains("\"end\"")) {
                        return parseEndResult(JSONObject(json), request, intervals)
                    }
                }
            }
        } catch (_: Exception) {}

        val avgMbps = if (intervals.isNotEmpty()) intervals.map { it.megabitsPerSecond }.average() else 0.0
        return makeMeasurement(request, avgMbps, null, null, null)
    }

    private fun parseEndResult(
        json: JSONObject,
        request: IperfRequest,
        intervals: List<SpeedInterval>,
    ): TestMeasurement {
        val end = json.optJSONObject("end") ?: return makeMeasurement(
            request,
            if (intervals.isNotEmpty()) intervals.map { it.megabitsPerSecond }.average() else 0.0,
            null, null, null,
        )
        val isSender = request.direction == Direction.FORWARD
        val sum = end.optJSONObject(if (isSender) "sum_sent" else "sum_received")
            ?: end.optJSONObject("sum")
        val throughputMbps = sum?.optDouble("bits_per_second", 0.0)?.div(1_000_000.0)
            ?: if (intervals.isNotEmpty()) intervals.map { it.megabitsPerSecond }.average() else 0.0
        val retransmits = sum?.optInt("retransmits")
        val streams = end.optJSONObject("streams")
        val stream = streams?.optJSONArray("sender")?.optJSONObject(0)
            ?: streams?.optJSONArray("receiver")?.optJSONObject(0)
        val jitterMs = stream?.optDouble("jitter_ms")
        val packetLoss = stream?.optDouble("lost_percent")
        return makeMeasurement(request, throughputMbps, jitterMs, packetLoss, retransmits)
    }

    private fun makeMeasurement(
        request: IperfRequest,
        throughputMbps: Double,
        jitterMs: Double?,
        packetLoss: Double?,
        retransmits: Int?,
    ) = TestMeasurement(
        timestamp = System.currentTimeMillis(),
        serverId = 0, serverName = "",
        serverAddress = request.host, serverPort = request.port,
        protocol = request.protocol.value, direction = request.direction.value,
        ipVersion = request.ipVersion.value,
        durationSeconds = request.durationSeconds, parallelStreams = request.parallelStreams,
        throughputMbps = throughputMbps, jitterMs = jitterMs,
        packetLossPercent = packetLoss, retransmits = retransmits,
        status = TestStatus.COMPLETED.value,
    )

    private fun sendJsonMessage(out: OutputStream, json: JSONObject) {
        val body = json.toString().toByteArray(Charsets.UTF_8)
        val header = ByteBuffer.allocate(4).putInt(body.size).array()
        out.write(header)
        out.write(body)
        out.flush()
    }

    private fun sendState(out: OutputStream, state: Int) {
        out.write(byteArrayOf(state.toByte()))
        out.flush()
    }

    private fun readMessage(input: InputStream): ByteArray? {
        val first = readExactly(input, 1) ?: return null
        val b = first[0].toInt() and 0xFF
        if (b != 0) return null
        val rest = readExactly(input, 3) ?: return null
        val size = ((rest[0].toInt() and 0xFF) shl 16) or
                ((rest[1].toInt() and 0xFF) shl 8) or
                (rest[2].toInt() and 0xFF)
        if (size <= 0 || size > 10_000_000) return null
        return readExactly(input, size)
    }

    private fun readExactly(input: InputStream, size: Int): ByteArray? {
        val buf = ByteArray(size)
        var off = 0
        while (off < size) {
            val n = input.read(buf, off, size - off)
            if (n == -1) return null
            off += n
        }
        return buf
    }
}
