package com.ikuai.inetspeed.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.model.DiagnosticRun
import com.ikuai.inetspeed.core.data.model.ToolRecord
import com.ikuai.inetspeed.core.data.repository.ToolsRepository
import com.ikuai.inetspeed.core.data.model.ToolType
import com.ikuai.inetspeed.core.network.info.NetworkInfoService
import com.ikuai.inetspeed.core.network.traceroute.TracerouteService
import com.ikuai.inetspeed.core.network.ping.PingService
import com.ikuai.inetspeed.core.network.ping.TcpPingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val pingService: PingService,
    private val tcpPingService: TcpPingService,
    private val tracerouteService: TracerouteService,
    private val networkInfoService: NetworkInfoService,
    private val toolsRepository: ToolsRepository,
) : ViewModel() {

    // Ping state
    private val _pingResults = MutableStateFlow<List<PingService.PingResult>>(emptyList())
    val pingResults: StateFlow<List<PingService.PingResult>> = _pingResults.asStateFlow()
    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()
    @Volatile
    private var pingJob: Job? = null

    // TcpPing state
    private val _tcpPingResults = MutableStateFlow<List<TcpPingService.TcpPingResult>>(emptyList())
    val tcpPingResults: StateFlow<List<TcpPingService.TcpPingResult>> = _tcpPingResults.asStateFlow()
    private val _isTcpPinging = MutableStateFlow(false)
    val isTcpPinging: StateFlow<Boolean> = _isTcpPinging.asStateFlow()
    @Volatile
    private var tcpPingJob: Job? = null

    // Traceroute state
    private val _tracerouteHops = MutableStateFlow<List<TracerouteService.TracerouteHop>>(emptyList())
    val tracerouteHops: StateFlow<List<TracerouteService.TracerouteHop>> = _tracerouteHops.asStateFlow()
    private val _isTracing = MutableStateFlow(false)
    val isTracing: StateFlow<Boolean> = _isTracing.asStateFlow()
    @Volatile
    private var tracerouteJob: Job? = null

    // Network info state
    private val _networkInfo = MutableStateFlow<NetworkInfoService.NetworkInfo?>(null)
    val networkInfo: StateFlow<NetworkInfoService.NetworkInfo?> = _networkInfo.asStateFlow()

    fun startPing(target: String, count: Int = 10) {
        if (_isPinging.value) return
        _isPinging.value = true
        _pingResults.value = emptyList()

        pingJob = viewModelScope.launch {
            val results = mutableListOf<PingService.PingResult>()
            for (i in 1..count) {
                val result = pingService.ping(target, count = 1)
                results.add(result)
                _pingResults.value = results.toList()
            }
            _isPinging.value = false

            // 保存结果
            savePingResult(target, results)
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        _isPinging.value = false
    }

    fun startTcpPing(target: String, port: Int = 5201, count: Int = 10) {
        if (_isTcpPinging.value) return
        _isTcpPinging.value = true
        _tcpPingResults.value = emptyList()

        tcpPingJob = viewModelScope.launch {
            val results = mutableListOf<TcpPingService.TcpPingResult>()
            for (i in 1..count) {
                val result = tcpPingService.ping(target, port)
                results.add(result)
                _tcpPingResults.value = results.toList()
            }
            _isTcpPinging.value = false
        }
    }

    fun stopTcpPing() {
        tcpPingJob?.cancel()
        _isTcpPinging.value = false
    }

    fun startTraceroute(target: String) {
        if (_isTracing.value) return
        _isTracing.value = true
        _tracerouteHops.value = emptyList()

        tracerouteJob = viewModelScope.launch {
            val hops = mutableListOf<TracerouteService.TracerouteHop>()
            tracerouteService.trace(target).collect { hop ->
                hops.add(hop)
                _tracerouteHops.value = hops.toList()
            }
            _isTracing.value = false

            // 保存结果
            saveTracerouteResult(target, hops)
        }
    }

    fun stopTraceroute() {
        tracerouteJob?.cancel()
        _isTracing.value = false
    }

    fun loadNetworkInfo() {
        viewModelScope.launch {
            _networkInfo.value = networkInfoService.collect()
        }
    }

    private fun savePingResult(target: String, results: List<PingService.PingResult>) {
        viewModelScope.launch {
            if (results.isEmpty()) return@launch
            val reachable = results.filter { it.reachable }
            val avgLatency = reachable.mapNotNull { it.avgLatencyMs }.average()
            val loss = ((results.size - reachable.size) * 100.0) / results.size

            toolsRepository.insertToolRecord(
                ToolRecord(
                    timestamp = System.currentTimeMillis(),
                    toolType = ToolType.PING.value,
                    target = target,
                    resultSummary = "avg=${avgLatency.toInt()}ms loss=${loss.toInt()}%",
                    status = "completed",
                )
            )

            toolsRepository.insertDiagnosticRun(
                DiagnosticRun(
                    timestamp = System.currentTimeMillis(),
                    toolType = ToolType.PING.value,
                    target = target,
                    avgLatencyMs = avgLatency,
                    minLatencyMs = reachable.mapNotNull { it.minLatencyMs }.minOrNull(),
                    maxLatencyMs = reachable.mapNotNull { it.maxLatencyMs }.maxOrNull(),
                    packetLossPercent = loss,
                )
            )
        }
    }

    private fun saveTracerouteResult(target: String, hops: List<TracerouteService.TracerouteHop>) {
        viewModelScope.launch {
            toolsRepository.insertToolRecord(
                ToolRecord(
                    timestamp = System.currentTimeMillis(),
                    toolType = ToolType.TRACEROUTE.value,
                    target = target,
                    resultSummary = "${hops.size} hops",
                    status = "completed",
                )
            )

            toolsRepository.insertDiagnosticRun(
                DiagnosticRun(
                    timestamp = System.currentTimeMillis(),
                    toolType = ToolType.TRACEROUTE.value,
                    target = target,
                    hops = hops.joinToString(",") { "${it.hopNumber}:${it.host}" },
                    avgLatencyMs = hops.mapNotNull { it.latencyMs }.average().takeIf { !it.isNaN() },
                )
            )
        }
    }
}
