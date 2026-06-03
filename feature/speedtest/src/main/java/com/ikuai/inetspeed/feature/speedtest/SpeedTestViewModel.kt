package com.ikuai.inetspeed.feature.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.error.ErrorCode
import com.ikuai.inetspeed.core.data.error.INetSpeedException
import com.ikuai.inetspeed.core.data.model.Direction
import com.ikuai.inetspeed.core.data.model.IpVersion
import com.ikuai.inetspeed.core.data.model.Protocol
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.data.repository.TestRepository
import com.ikuai.inetspeed.core.iperf3.Iperf3Runner
import com.ikuai.inetspeed.core.iperf3.model.IperfEvent
import com.ikuai.inetspeed.core.iperf3.model.IperfRequest
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestConfig
import com.ikuai.inetspeed.feature.speedtest.state.SpeedTestState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SpeedTestViewModel @Inject constructor(
    private val iperf3Runner: Iperf3Runner,
    private val testRepository: TestRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    private val _config = MutableStateFlow(SpeedTestConfig())
    val config: StateFlow<SpeedTestConfig> = _config.asStateFlow()

    private val _isExpertMode = MutableStateFlow(false)
    val isExpertMode: StateFlow<Boolean> = _isExpertMode.asStateFlow()

    private var currentJob: Job? = null
    private var currentTestId: String? = null

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

    fun startTest(serverAddress: String, serverPort: Int = 5201, serverName: String = "") {
        if (_state.value is SpeedTestState.Running || _state.value is SpeedTestState.Preparing) {
            return
        }

        val testId = UUID.randomUUID().toString()
        currentTestId = testId

        _state.value = SpeedTestState.Preparing

        val request = IperfRequest(
            testId = testId,
            host = serverAddress,
            port = serverPort,
            protocol = _config.value.protocol,
            direction = _config.value.direction,
            ipVersion = _config.value.ipVersion,
            durationSeconds = _config.value.durationSeconds,
            parallelStreams = _config.value.parallelStreams,
            udpBandwidth = _config.value.udpBandwidth,
            windowSize = _config.value.windowSize,
            bufferLength = _config.value.bufferLength,
        )

        currentJob = viewModelScope.launch {
            var lastMbps = 0.0
            var lastLatency: Double? = null
            var lastJitter: Double? = null
            var lastPacketLoss: Double? = null
            var elapsed = 0

            iperf3Runner.run(request).collect { event ->
                when (event) {
                    is IperfEvent.Interval -> {
                        lastMbps = event.data.megabitsPerSecond
                        if (event.data.isUdp) {
                            lastJitter = event.data.jitterMs
                            lastPacketLoss = event.data.packetLossPercent
                        }
                        elapsed++
                        _state.value = SpeedTestState.Running(
                            currentMbps = lastMbps,
                            latencyMs = lastLatency,
                            jitterMs = lastJitter,
                            packetLossPercent = lastPacketLoss,
                            progressPercent = (elapsed * 100) / _config.value.durationSeconds,
                            elapsedSeconds = elapsed,
                        )
                    }
                    is IperfEvent.Progress -> {
                        _state.update {
                            if (it is SpeedTestState.Running) {
                                it.copy(progressPercent = event.percent)
                            } else it
                        }
                    }
                    is IperfEvent.Completed -> {
                        saveResult(event.result, serverName, serverAddress, serverPort)
                        _state.value = SpeedTestState.Completed(
                            throughputMbps = event.result.throughputMbps,
                            latencyMs = event.result.latencyMs,
                            jitterMs = event.result.jitterMs,
                            packetLossPercent = event.result.packetLossPercent,
                        )
                    }
                    is IperfEvent.Failed -> {
                        _state.value = SpeedTestState.Failed(
                            errorCode = event.error.errorCode,
                            message = event.error.message ?: "Unknown error",
                        )
                    }
                }
            }
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
                    message = "Cancel failed: ${e.message}",
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
    }

    private fun saveResult(
        measurement: TestMeasurement,
        serverName: String,
        serverAddress: String,
        serverPort: Int,
    ) {
        viewModelScope.launch {
            try {
                val updated = measurement.copy(
                    serverName = serverName,
                    serverAddress = serverAddress,
                    serverPort = serverPort,
                )
                testRepository.insert(updated)
            } catch (_: Exception) {
                // 保存失败不影响 UI
            }
        }
    }
}
