package com.ikuai.inetspeed.core.iperf3.command

import com.ikuai.inetspeed.core.data.error.ErrorCode
import com.ikuai.inetspeed.core.data.error.INetSpeedException
import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.IpVersion
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.core.iperf3.model.IperfRequest

object CommandBuilder {

    /**
     * 构建 iperf3 命令行参数
     * @return 参数列表（不含 iperf3 二进制路径）
     * @throws INetSpeedException 如果参数不合法
     */
    fun build(request: IperfRequest): List<String> {
        validate(request)

        val args = mutableListOf<String>()

        // 基础参数
        args.addAll(listOf("-c", request.host))
        args.addAll(listOf("-p", request.port.toString()))
        args.addAll(listOf("-t", request.durationSeconds.toString()))
        args.addAll(listOf("-i", "1"))

        // 协议
        when (request.protocol) {
            Protocol.UDP -> args.add("--udp")
            Protocol.SCTP -> args.add("--sctp")
            Protocol.TCP -> { /* 默认 */ }
        }

        // 方向
        if (request.direction == Direction.REVERSE) {
            args.add("-R")
        }

        // IPv6
        if (request.ipVersion == IpVersion.IPV6) {
            args.add("-6")
        }

        // 并发线程
        if (request.parallelStreams > 1) {
            args.addAll(listOf("-P", request.parallelStreams.toString()))
        }

        // UDP 目标带宽
        if (request.protocol == Protocol.UDP && request.udpBandwidth != null) {
            args.addAll(listOf("-b", request.udpBandwidth))
        }

        // 窗口大小
        if (request.windowSize != null) {
            args.addAll(listOf("-w", request.windowSize))
        }

        // 缓冲区长度
        if (request.bufferLength != null) {
            args.addAll(listOf("-l", request.bufferLength))
        }

        // JSON 输出
        if (request.useJson) {
            args.add("-J")
        }

        // 强制使用 json-stream 模式实现实时输出（iperf3 在 pipe 模式下会块缓冲）
        args.add("--json-stream")

        return args
    }

    /**
     * 构建版本查询命令
     */
    fun buildVersionQuery(): List<String> = listOf("--version")

    /**
     * 构建 SCTP 能力检测命令
     */
    fun buildSctpCheck(host: String, port: Int = 5201): List<String> {
        return listOf("-c", host, "-p", port.toString(), "-t", "1", "--sctp")
    }

    fun parseCliInput(command: String): List<String> {
        val tokens = splitCommand(command.trim())
        if (tokens.isEmpty()) {
            throw INetSpeedException(ErrorCode.PARAM_INVALID, mapOf("field" to "command", "reason" to "empty"))
        }
        val args = if (tokens.first().substringAfterLast('/').substringAfterLast('\\') == "iperf3") {
            tokens.drop(1)
        } else {
            tokens
        }
        if (args.isEmpty()) {
            throw INetSpeedException(ErrorCode.PARAM_INVALID, mapOf("field" to "command", "reason" to "missing arguments"))
        }
        return args
    }

    /**
     * 校验请求参数
     */
    private fun validate(request: IperfRequest) {
        if (request.host.isBlank()) {
            throw INetSpeedException(ErrorCode.PARAM_INVALID, mapOf("field" to "host", "reason" to "empty"))
        }
        if (request.port !in 1..65535) {
            throw INetSpeedException(ErrorCode.PARAM_INVALID, mapOf("field" to "port", "value" to request.port.toString()))
        }
        if (request.durationSeconds < 1 || request.durationSeconds > 3600) {
            throw INetSpeedException(ErrorCode.PARAM_INVALID, mapOf("field" to "duration", "value" to request.durationSeconds.toString()))
        }
        if (request.parallelStreams < 1 || request.parallelStreams > 32) {
            throw INetSpeedException(ErrorCode.PARAM_INVALID, mapOf("field" to "parallelStreams", "value" to request.parallelStreams.toString()))
        }
    }

    private fun splitCommand(command: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false
        for (char in command) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }
                char == '\\' -> escaped = true
                quote != null && char == quote -> quote = null
                quote == null && (char == '"' || char == '\'') -> quote = char
                quote == null && char.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
        }
        if (escaped) current.append('\\')
        if (current.isNotEmpty()) tokens.add(current.toString())
        return tokens
    }
}
