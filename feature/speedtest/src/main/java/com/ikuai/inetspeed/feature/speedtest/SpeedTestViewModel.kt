package com.ikuai.inetspeed.feature.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.error.ErrorCode
import com.ikuai.inetspeed.core.data.error.INetSpeedException
import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.IpVersion
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.data.repository.ServerRepository
import com.ikuai.inetspeed.core.data.repository.TestRepository
import com.ikuai.inetspeed.core.iperf3.Iperf3Runner
import com.ikuai.inetspeed.core.iperf3.model.IperfEvent
import com.ikuai.inetspeed.core.iperf3.model.IperfRequest
import com.ikuai.inetspeed.core.network.ping.PingService
import com.ikuai.inetspeed.core.network.ping.TcpPingService
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestConfig
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val iperf3Runner: Iperf3Runner,
    private val testRepository: TestRepository,
    private val serverRepository: ServerRepository,
    private val pingService: PingService,
    private val tcpPingService: TcpPingService,
) : ViewModel() {

    private val _state = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    private val _config = MutableStateFlow(SpeedTestConfig())
    val config: StateFlow<SpeedTestConfig> = _config.asStateFlow()

    private val _isExpertMode = MutableStateFlow(false)
    val isExpertMode: StateFlow<Boolean> = _isExpertMode.asStateFlow()

    private val _serverAddress = MutableStateFlow("")
    val serverAddress: StateFlow<String> = _serverAddress.asStateFlow()

    private val _serverPort = MutableStateFlow(5201)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _cliOutput = MutableStateFlow("")
    val cliOutput: StateFlow<String> = _cliOutput.asStateFlow()

    val recentServers: StateFlow<List<Server>> = serverRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentJob: Job? = null
    private var currentTestId: String? = null

    init {
        viewModelScope.launch {
            serverRepository.initBuiltInServers()
        }
    }

    fun toggleMode() {
        _isExpertMode.update { !it }
        _config.update { it.copy(isExpertMode = _isExpertMode.value) }
    }

    fun updateConfig(config: SpeedTestConfig) {
        _config.value = config
    }

    fun updateProtocol(protocol: Protocol) {
        _config.update { it.copy(protocol = protocol) }
    }

    fun updateDirection(direction: Direction) {
        _config.update { it.copy(direction = direction) }
    }

    fun updateServerAddress(address: String) {
        _serverAddress.value = address
    }

    fun updateServerPort(port: Int) {
        _serverPort.value = port
    }

    fun selectServer(server: Server) {
        _serverAddress.value = server.address
        _serverPort.value = server.port
    }

    fun startTest() {
        val address = _serverAddress.value.trim()
        val port = _serverPort.value
        if (address.isBlank()) return
        if (_state.value is SpeedTestState.Running || _state.value is SpeedTestState.Preparing) return

        val testId = UUID.randomUUID().toString()
        currentTestId = testId
        _state.value = SpeedTestState.Preparing

        currentJob = viewModelScope.launch {
            android.util.Log.d("SpeedTestVM", "startTest launched, state=Preparing")
            // 前置检测：ping → tcping → 任一通则继续
            val pingResult = pingService.ping(address, count = 2)
            if (!pingResult.reachable) {
                val tcpResult = tcpPingService.ping(address, port, timeoutMs = 3000)
                if (!tcpResult.reachable) {
                    _state.value = SpeedTestState.Failed(
                        errorCode = ErrorCode.SERVER_UNREACHABLE,
                        message = "Ping 和 TCP($port) 均不可达，请检查服务器地址和端口",
                    )
                    return@launch
                }
            }

            val request = IperfRequest(
                testId = testId,
                host = address,
                port = port,
                protocol = _config.value.protocol,
                direction = _config.value.direction,
                ipVersion = _config.value.ipVersion,
                durationSeconds = _config.value.durationSeconds,
                parallelStreams = _config.value.parallelStreams,
                udpBandwidth = if (_config.value.protocol == Protocol.UDP && _config.value.udpBandwidth.isNullOrBlank()) "10M" else _config.value.udpBandwidth,
                windowSize = _config.value.windowSize,
                bufferLength = _config.value.bufferLength,
            )

            var lastMbps = 0.0
            var lastLatency: Double? = null
            var lastJitter: Double? = null
            var lastPacketLoss: Double? = null
            var elapsed = 0

            iperf3Runner.run(request).collect { event ->
                android.util.Log.d("SpeedTestVM", "Event: $event")
                when (event) {
                    is IperfEvent.Interval -> {
                        // Interval 只更新 UDP 指标，不更新 currentMbps
                        // currentMbps 由 Progress 事件统一负责
                        if (event.data.isUdp) {
                            lastJitter = event.data.jitterMs
                            lastPacketLoss = event.data.packetLossPercent
                        }
                        elapsed++
                        _state.update {
                            if (it is SpeedTestState.Running) {
                                it.copy(
                                    latencyMs = lastLatency,
                                    jitterMs = lastJitter,
                                    packetLossPercent = lastPacketLoss,
                                    elapsedSeconds = elapsed,
                                )
                            } else it
                        }
                    }

                    is IperfEvent.Progress -> {
                        lastMbps = event.currentMbps
                        _state.update {
                            if (it is SpeedTestState.Running) {
                                it.copy(
                                    currentMbps = event.currentMbps,
                                    progressPercent = event.percent,
                                )
                            } else {
                                // 首个 Progress 事件时创建 Running 状态
                                SpeedTestState.Running(
                                    currentMbps = event.currentMbps,
                                    latencyMs = lastLatency,
                                    jitterMs = lastJitter,
                                    packetLossPercent = lastPacketLoss,
                                    progressPercent = event.percent,
                                    elapsedSeconds = elapsed,
                                )
                            }
                        }
                    }

                    is IperfEvent.Completed -> {
                        saveServerRecord(address, port)
                        saveResult(event.result, address, port)
                        _state.value = SpeedTestState.Completed(
                            throughputMbps = event.result.throughputMbps,
                            latencyMs = event.result.latencyMs,
                            jitterMs = event.result.jitterMs,
                            packetLossPercent = event.result.packetLossPercent,
                        )
                    }

                    is IperfEvent.Failed -> {
                        val errorMsg = event.error.cause?.message ?: event.error.message ?: "未知错误"
                        android.util.Log.d("SpeedTestVM", "Setting state to Failed: ${event.error.errorCode} - $errorMsg")
                        _state.value = SpeedTestState.Failed(
                            errorCode = event.error.errorCode,
                            message = errorMsg,
                        )
                        android.util.Log.d("SpeedTestVM", "State set to Failed: ${_state.value}")
                    }
                }
            }
        }
    }

    fun startCliCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) return
        if (_state.value is SpeedTestState.Running || _state.value is SpeedTestState.Preparing) return

        val testId = UUID.randomUUID().toString()
        currentTestId = testId
        _state.value = SpeedTestState.Preparing
        // 追加分隔线，不清空旧输出
        val separator = if (_cliOutput.value.isBlank()) "" else "\n${"─".repeat(40)}\n"
        _cliOutput.value = _cliOutput.value + separator

        currentJob = viewModelScope.launch {
            val lines = mutableListOf<String>()
            iperf3Runner.runCli(testId, trimmed).collect { line ->
                lines.add(line)
                _cliOutput.value = _cliOutput.value + line + "\n"
                _state.value = SpeedTestState.Running(
                    progressPercent = 0,
                    elapsedSeconds = lines.size,
                )
            }
            currentTestId = null
            currentJob = null
            _state.value = SpeedTestState.Completed(
                throughputMbps = 0.0,
                latencyMs = null,
                jitterMs = null,
                packetLossPercent = null,
            )
        }
    }

    fun cancelTest() {
        val testId = currentTestId ?: return
        _state.value = SpeedTestState.Cancelling

        viewModelScope.launch {
            try {
                iperf3Runner.cancel(testId)
                _state.value = SpeedTestState.Cancelled
            } catch (e: Exception) {
                _state.value = SpeedTestState.Failed(
                    errorCode = ErrorCode.UNKNOWN,
                    message = "取消失败: ${e.message}",
                )
            } finally {
                currentTestId = null
                currentJob?.cancel()
                currentJob = null
            }
        }
    }

    fun resetToIdle() {
        currentJob?.cancel()
        currentJob = null
        currentTestId = null
        _state.value = SpeedTestState.Idle
        _cliOutput.value = ""
    }

    private fun saveServerRecord(address: String, port: Int) {
        viewModelScope.launch {
            try {
                val existing = serverRepository.getAll().find {
                    it.address == address && it.port == port
                }
                if (existing == null) {
                    serverRepository.insert(
                        Server(
                            name = address,
                            address = address,
                            port = port,
                            isBuiltIn = false,
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun saveResult(
        measurement: TestMeasurement,
        serverAddress: String,
        serverPort: Int,
    ) {
        viewModelScope.launch {
            try {
                val updated = measurement.copy(
                    serverName = serverAddress,
                    serverAddress = serverAddress,
                    serverPort = serverPort,
                )
                testRepository.insert(updated)
            } catch (_: Exception) {}
        }
    }
}
