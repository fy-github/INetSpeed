package com.ikuai.inetspeed.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.dao.ToolRecordDao
import com.ikuai.inetspeed.core.data.dao.DiagnosticRunDao
import com.ikuai.inetspeed.core.data.model.DiagnosticRun
import com.ikuai.inetspeed.core.data.model.ToolRecord
import com.ikuai.inetspeed.core.data.model.ToolType
import com.ikuai.inetspeed.core.network.info.NetworkInfoService
import com.ikuai.inetspeed.core.network.traceroute.TracerouteService
import com.ikuai.inetspeed.core.network.ping.PingService
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
    private val tracerouteService: TracerouteService,
    private val networkInfoService: NetworkInfoService,
    private val toolRecordDao: ToolRecordDao,
    private val diagnosticRunDao: DiagnosticRunDao,
) : ViewModel() {

    // Ping state
    private val _pingResults = MutableStateFlow<List<PingService.PingResult>>(emptyList())
    val pingResults: StateFlow<List<PingService.PingResult>> = _pingResults.asStateFlow()
    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()
    private var pingJob: Job? = null

    // Traceroute state
    private val _tracerouteHops = MutableStateFlow<List<TracerouteService.TracerouteHop>>(emptyList())
    val tracerouteHops: StateFlow<List<TracerouteService.TracerouteHop>> = _tracerouteHops.asStateFlow()
    private val _isTracing = MutableStateFlow(false)
    val isTracing: StateFlow<Boolean> = _isTracing.asStateFlow()

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

            // 淇濆瓨缁撴灉
            savePingResult(target, results)
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        _isPinging.value = false
    }

    fun startTraceroute(target: String) {
        if (_isTracing.value) return
        _isTracing.value = true
        _tracerouteHops.value = emptyList()

        viewModelScope.launch {
            val hops = mutableListOf<TracerouteService.TracerouteHop>()
            tracerouteService.trace(target).collect { hop ->
                hops.add(hop)
                _tracerouteHops.value = hops.toList()
            }
            _isTracing.value = false

            // 淇濆瓨缁撴灉
            saveTracerouteResult(target, hops)
        }
    }

    fun loadNetworkInfo() {
        viewModelScope.launch {
            _networkInfo.value = networkInfoService.collect()
        }
    }

    private fun savePingResult(target: String, results: List<PingService.PingResult>) {
        viewModelScope.launch {
            val reachable = results.filter { it.reachable }
            val avgLatency = reachable.mapNotNull { it.avgLatencyMs }.average()
            val loss = ((results.size - reachable.size) * 100.0) / results.size

            toolRecordDao.insert(
                ToolRecord(
                    timestamp = System.currentTimeMillis(),
                    toolType = ToolType.PING.value,
                    target = target,
                    resultSummary = "avg=${avgLatency.toInt()}ms loss=${loss.toInt()}%",
                    status = "completed",
                )
            )

            diagnosticRunDao.insert(
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
            toolRecordDao.insert(
                ToolRecord(
                    timestamp = System.currentTimeMillis(),
                    toolType = ToolType.TRACEROUTE.value,
                    target = target,
                    resultSummary = "${hops.size} hops",
                    status = "completed",
                )
            )

            diagnosticRunDao.insert(
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
