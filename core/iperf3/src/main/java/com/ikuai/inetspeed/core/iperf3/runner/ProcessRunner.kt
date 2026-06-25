package com.ikuai.inetspeed.core.iperf3.runner

import com.ikuai.inetspeed.core.data.error.ErrorCode
import com.ikuai.inetspeed.core.data.error.INetSpeedException
import com.ikuai.inetspeed.core.iperf3.Iperf3Runner
import com.ikuai.inetspeed.core.iperf3.binary.BinaryInstaller
import com.ikuai.inetspeed.core.iperf3.command.CommandBuilder
import com.ikuai.inetspeed.core.iperf3.model.BinaryValidationResult
import com.ikuai.inetspeed.core.iperf3.model.IperfEvent
import com.ikuai.inetspeed.core.iperf3.model.IperfRequest
import com.ikuai.inetspeed.core.iperf3.model.IperfVersion
import com.ikuai.inetspeed.core.iperf3.model.SctpCapability
import com.ikuai.inetspeed.core.iperf3.parser.Iperf3OutputParser
import com.ikuai.inetspeed.core.iperf3.parser.JsonResultParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessRunner @Inject constructor(
    private val binaryInstaller: BinaryInstaller,
) : Iperf3Runner {

    private val activeProcesses = ConcurrentHashMap<String, Process>()

    override fun run(request: IperfRequest): Flow<IperfEvent> = flow {
        // 校验二进制
        val validation = validateBinary()
        if (!validation.isValid) {
            emit(IperfEvent.Failed(INetSpeedException(ErrorCode.BINARY_MISSING, mapOf("error" to (validation.error ?: "unknown")))))
            return@flow
        }

        // 构建命令
        val args = try {
            CommandBuilder.build(request)
        } catch (e: INetSpeedException) {
            emit(IperfEvent.Failed(e))
            return@flow
        }

        val cmd = listOf(binaryInstaller.getBinaryPath()) + args
        val rawLines = ArrayDeque<String>(200) // 限制缓冲区大小为 200 行
        var finalThroughputMbps = 0.0
        var intervalsParsed = 0

        try {
            val processBuilder = createProcessBuilder(cmd)

            val process = processBuilder.start()
            activeProcesses[request.testId] = process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val timeoutMs = (request.durationSeconds + 15) * 1000L
            val startTime = System.currentTimeMillis()

            // 启动 watchdog 线程监控进程超时
            val watchdog = Thread {
                try {
                    Thread.sleep(timeoutMs)
                    if (process.isAlive) {
                        process.destroyForcibly()
                        activeProcesses.remove(request.testId)
                    }
                } catch (_: InterruptedException) {
                    // 线程被中断，正常退出
                }
            }
            watchdog.isDaemon = true
            watchdog.start()

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (rawLines.size >= 200) rawLines.removeFirst()
                rawLines.addLast(currentLine)

                // 解析输出：优先尝试 JSON（--json-stream 模式），回退到文本
                if (currentLine.trimStart().startsWith("{")) {
                    try {
                        val json = org.json.JSONObject(currentLine)
                        val event = json.optString("event", "")
                        val data = json.optJSONObject("data")

                        when (event) {
                            "interval" -> {
                                val sum = data?.optJSONObject("sum")
                                if (sum != null) {
                                    val bps = sum.optDouble("bits_per_second", 0.0)
                                    val mbps = bps / 1_000_000.0
                                    val retransmits = sum.optInt("retransmits", 0)
                                    val omitted = sum.optBoolean("omitted", false)
                                    if (!omitted) {
                                        val secondIndex = intervalsParsed++
                                        val percent = ((secondIndex + 1) * 100) / request.durationSeconds
                                        val speedInterval = com.ikuai.inetspeed.core.iperf3.parser.SpeedInterval(
                                            streamId = 0,
                                            secondIndex = secondIndex,
                                            bitsPerSecond = bps,
                                            retransmits = retransmits,
                                            jitterMs = null,
                                            packetLossPercent = null,
                                            rawLine = currentLine,
                                        )
                                        emit(IperfEvent.Interval(speedInterval))
                                        emit(IperfEvent.Progress(percent.coerceAtMost(100), mbps))
                                    }
                                }
                            }
                            "end" -> {
                                val isSender = request.direction == com.ikuai.inetspeed.core.data.model.Direction.FORWARD
                                val sumKey = if (isSender) "sum_sent" else "sum_received"
                                val sum = data?.optJSONObject(sumKey)
                                val bps = sum?.optDouble("bits_per_second", 0.0) ?: 0.0
                                finalThroughputMbps = bps / 1_000_000.0
                            }
                        }
                    } catch (_: org.json.JSONException) {
                        // 非 JSON 行，忽略
                    }
                } else if (!request.useJson) {
                    // 文本模式回退
                    val interval = com.ikuai.inetspeed.core.iperf3.parser.Iperf3OutputParser.parseIntervalLine(currentLine)
                    if (interval != null) {
                        val percent = ((interval.secondIndex + 1) * 100) / request.durationSeconds
                        emit(IperfEvent.Interval(interval))
                        emit(IperfEvent.Progress(percent.coerceAtMost(100), interval.megabitsPerSecond))
                    }
                }
            }

            // 停止 watchdog 线程
            watchdog.interrupt()
            val completedInTime = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
            activeProcesses.remove(request.testId)

            if (!completedInTime) {
                process.destroyForcibly()
                val errorOutput = rawLines.joinToString("\n")
                val errorCode = if (request.protocol == com.ikuai.inetspeed.core.data.model.Protocol.UDP) {
                    ErrorCode.SERVER_UNREACHABLE
                } else {
                    ErrorCode.TEST_TIMEOUT
                }
                emit(IperfEvent.Failed(INetSpeedException(errorCode, mapOf("output" to errorOutput.take(500)))))
                return@flow
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                val errorOutput = rawLines.joinToString("\n")
                val errorCode = classifyError(errorOutput, exitCode)
                val diagnosticInfo = mutableMapOf<String, String>()
                diagnosticInfo["exitCode"] = exitCode.toString()
                diagnosticInfo["output"] = errorOutput.take(500)

                // 为 UDP 测试添加特定的错误消息
                if (request.protocol == com.ikuai.inetspeed.core.data.model.Protocol.UDP) {
                    diagnosticInfo["error"] = "UDP 连接超时，请检查服务器是否支持 UDP 或网络是否阻止 UDP 流量"
                }

                android.util.Log.e("ProcessRunner", "iperf3 failed: exitCode=$exitCode, errorCode=$errorCode, output=${errorOutput.take(300)}")
                emit(IperfEvent.Failed(INetSpeedException(errorCode, diagnosticInfo)))
                return@flow
            }

            // 解析最终结果
            val rawOutput = rawLines.joinToString("\n")
            val result = if (finalThroughputMbps > 0.0) {
                // --json-stream 模式：使用从 "end" JSON 解析的吞吐量
                com.ikuai.inetspeed.core.data.model.TestMeasurement(
                    timestamp = System.currentTimeMillis(),
                    serverId = 0,
                    serverName = "",
                    serverAddress = request.host,
                    serverPort = request.port,
                    protocol = request.protocol.value,
                    direction = request.direction.value,
                    ipVersion = request.ipVersion.value,
                    durationSeconds = request.durationSeconds,
                    parallelStreams = request.parallelStreams,
                    throughputMbps = finalThroughputMbps,
                    status = com.ikuai.inetspeed.core.data.model.TestStatus.COMPLETED.value,
                )
            } else if (request.useJson) {
                JsonResultParser.parse(
                    json = rawOutput,
                    serverId = 0,
                    serverName = "",
                    serverAddress = request.host,
                    serverPort = request.port,
                    protocol = request.protocol,
                    direction = request.direction,
                    ipVersion = request.ipVersion,
                    durationSeconds = request.durationSeconds,
                    parallelStreams = request.parallelStreams,
                )
            } else {
                // 从 interval 输出构建简单结果
                val intervals = Iperf3OutputParser.parseIntervals(rawOutput)
                val avgMbps = if (intervals.isNotEmpty()) {
                    intervals.map { it.megabitsPerSecond }.average()
                } else 0.0

                com.ikuai.inetspeed.core.data.model.TestMeasurement(
                    timestamp = System.currentTimeMillis(),
                    serverId = 0,
                    serverName = "",
                    serverAddress = request.host,
                    serverPort = request.port,
                    protocol = request.protocol.value,
                    direction = request.direction.value,
                    ipVersion = request.ipVersion.value,
                    durationSeconds = request.durationSeconds,
                    parallelStreams = request.parallelStreams,
                    throughputMbps = avgMbps,
                    status = com.ikuai.inetspeed.core.data.model.TestStatus.COMPLETED.value,
                )
            }

            emit(IperfEvent.Completed(result))

        } catch (e: Exception) {
            activeProcesses.remove(request.testId)
            val errorCode = when {
                e is java.io.IOException && e.message?.contains("Permission denied") == true -> ErrorCode.PERMISSION_DENIED
                e is java.io.IOException && e.message?.contains("error=13") == true -> ErrorCode.PERMISSION_DENIED
                else -> ErrorCode.UNKNOWN
            }
            android.util.Log.e("ProcessRunner", "Exception: ${e.javaClass.simpleName}: ${e.message}", e)
            emit(IperfEvent.Failed(INetSpeedException(errorCode, cause = e)))
        }
    }.flowOn(Dispatchers.IO)

    override fun runCli(testId: String, command: String): Flow<String> = flow {
        val validation = validateBinary()
        if (!validation.isValid) {
            emit("ERROR: ${validation.error ?: "Binary not available"}")
            return@flow
        }

        val args = try {
            CommandBuilder.parseCliInput(command)
        } catch (e: INetSpeedException) {
            emit("ERROR: ${e.message ?: e.errorCode.code}")
            return@flow
        }

        val cmd = listOf(binaryInstaller.getBinaryPath()) + args
        try {
            emit("$ ${listOf("iperf3").plus(args).joinToString(" ")}")
            val process = createProcessBuilder(cmd).start()
            activeProcesses[testId] = process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val timeoutMs = (extractDurationSeconds(args) + 30) * 1000L
            val startTime = System.currentTimeMillis()

            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    process.destroyForcibly()
                    activeProcesses.remove(testId)
                    emit("ERROR: command timed out")
                    return@flow
                }
            }

            val completed = process.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            activeProcesses.remove(testId)
            if (!completed) {
                process.destroyForcibly()
                emit("ERROR: command timed out")
                return@flow
            }
            emit("[exit ${process.exitValue()}]")
        } catch (e: Exception) {
            activeProcesses.remove(testId)
            emit("ERROR: ${e.message ?: e.javaClass.simpleName}")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun cancel(testId: String) {
        withContext(Dispatchers.IO) {
            val process = activeProcesses.remove(testId) ?: return@withContext
            try {
                process.destroy()
                // 等待 5 秒，如果还没退出则强杀
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
            } catch (e: Exception) {
                try { process.destroyForcibly() } catch (e2: Exception) {
                    android.util.Log.w("ProcessRunner", "Failed to force destroy process", e2)
                }
            }
        }
    }

    override suspend fun version(): IperfVersion = withContext(Dispatchers.IO) {
        val validation = validateBinary()
        if (!validation.isValid) {
            return@withContext IperfVersion("unknown", "Binary not available")
        }

        try {
            val cmd = listOf(binaryInstaller.getBinaryPath()) + CommandBuilder.buildVersionQuery()
            val process = createProcessBuilder(cmd).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)

            val version = output.lines().firstOrNull { it.contains("iperf") }
                ?.replace(Regex("[^\\d.]"), "") ?: "unknown"

            IperfVersion(version, output.trim())
        } catch (e: Exception) {
            IperfVersion("unknown", "Error: ${e.message}")
        }
    }

    override suspend fun validateBinary(): BinaryValidationResult = withContext(Dispatchers.IO) {
        // 如果未安装，尝试安装
        if (!binaryInstaller.isInstalled()) {
            binaryInstaller.install()
        }
        binaryInstaller.validate()
    }

    override suspend fun detectSctp(): SctpCapability = withContext(Dispatchers.IO) {
        val validation = validateBinary()
        if (!validation.isValid) {
            return@withContext SctpCapability(false, "Binary not available")
        }

        try {
            val cmd = listOf(binaryInstaller.getBinaryPath()) + CommandBuilder.buildSctpCheck("127.0.0.1")
            val process = createProcessBuilder(cmd).start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)

            if (exitCode && process.exitValue() == 0) {
                SctpCapability(true)
            } else {
                val errorMsg = output.lines().firstOrNull { it.contains("error", ignoreCase = true) } ?: output.take(200)
                SctpCapability(false, errorMsg)
            }
        } catch (e: Exception) {
            SctpCapability(false, "Detection failed: ${e.message}")
        }
    }

    internal fun classifyError(output: String, exitCode: Int): ErrorCode {
        val lower = output.lowercase()
        return when {
            lower.contains("unable to create a new stream") -> ErrorCode.BINARY_UNSUPPORTED
            lower.contains("permission denied") -> ErrorCode.PERMISSION_DENIED
            lower.contains("connection refused") || lower.contains("connect failed") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("unable to connect") || lower.contains("no route") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("server is busy") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("unable to read from stream") || lower.contains("try again") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("udp") && lower.contains("timeout") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("udp") && lower.contains("unreachable") -> ErrorCode.SERVER_UNREACHABLE
            output.contains("UDP") && output.contains("超时") -> ErrorCode.SERVER_UNREACHABLE
            output.contains("UDP") && output.contains("不可达") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("protocol") && lower.contains("not supported") -> ErrorCode.PROTOCOL_UNSUPPORTED
            lower.contains("sctp") && lower.contains("not supported") -> ErrorCode.PROTOCOL_UNSUPPORTED
            lower.contains("timeout") || lower.contains("timed out") -> ErrorCode.TEST_TIMEOUT
            lower.contains("error") -> ErrorCode.UNKNOWN
            exitCode != 0 -> ErrorCode.UNKNOWN
            else -> ErrorCode.UNKNOWN
        }
    }

    private fun createProcessBuilder(cmd: List<String>): ProcessBuilder {
        return ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .apply {
                environment()["ANDROID_NO_USE_FWMARK_CLIENT"] = "1"
            }
    }

    private fun extractDurationSeconds(args: List<String>): Int {
        val optionIndex = args.indexOfFirst { it == "-t" || it == "--time" }
        return args.getOrNull(optionIndex + 1)?.toIntOrNull()?.coerceIn(1, 3600) ?: 10
    }
}
