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
        val rawLines = mutableListOf<String>()

        try {
            val processBuilder = ProcessBuilder(cmd)
                .redirectErrorStream(true)

            val process = processBuilder.start()
            activeProcesses[request.testId] = process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                rawLines.add(currentLine)

                // 解析 interval 输出
                if (!request.useJson) {
                    val interval = Iperf3OutputParser.parseIntervalLine(currentLine)
                    if (interval != null) {
                        val percent = ((interval.secondIndex + 1) * 100) / request.durationSeconds
                        emit(IperfEvent.Interval(interval))
                        emit(IperfEvent.Progress(percent.coerceAtMost(100), interval.megabitsPerSecond))
                    }
                }
            }

            val exitCode = process.waitFor()
            activeProcesses.remove(request.testId)

            if (exitCode != 0) {
                val errorOutput = rawLines.joinToString("\n")
                val errorCode = classifyError(errorOutput, exitCode)
                emit(IperfEvent.Failed(INetSpeedException(errorCode, mapOf("exitCode" to exitCode.toString(), "output" to errorOutput.take(500)))))
                return@flow
            }

            // 解析最终结果
            val rawOutput = rawLines.joinToString("\n")
            val result = if (request.useJson) {
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
            emit(IperfEvent.Failed(INetSpeedException(ErrorCode.UNKNOWN, cause = e)))
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
            } catch (_: Exception) {
                try { process.destroyForcibly() } catch (_: Exception) {}
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
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
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
            val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
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

    private fun classifyError(output: String, exitCode: Int): ErrorCode {
        val lower = output.lowercase()
        return when {
            lower.contains("connection refused") || lower.contains("connect failed") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("unable to connect") || lower.contains("no route") -> ErrorCode.SERVER_UNREACHABLE
            lower.contains("port") && lower.contains("error") -> ErrorCode.PORT_BLOCKED
            lower.contains("protocol") && lower.contains("not supported") -> ErrorCode.PROTOCOL_UNSUPPORTED
            lower.contains("sctp") && lower.contains("not supported") -> ErrorCode.PROTOCOL_UNSUPPORTED
            lower.contains("timeout") -> ErrorCode.TEST_TIMEOUT
            lower.contains("permission") -> ErrorCode.PERMISSION_DENIED
            exitCode != 0 -> ErrorCode.UNKNOWN
            else -> ErrorCode.UNKNOWN
        }
    }
}
