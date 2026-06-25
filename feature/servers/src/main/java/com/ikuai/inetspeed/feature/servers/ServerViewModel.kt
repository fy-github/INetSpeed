package com.ikuai.inetspeed.feature.servers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.model.Server
import com.ikuai.inetspeed.core.data.repository.ServerRepository
import com.ikuai.inetspeed.core.network.discovery.MdnsDiscoveryService
import com.ikuai.inetspeed.core.network.ping.PingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val pingService: PingService,
    private val mdnsDiscoveryService: MdnsDiscoveryService,
) : ViewModel() {

    val servers: StateFlow<List<Server>> = serverRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recommendedServer = MutableStateFlow<Server?>(null)
    val recommendedServer: StateFlow<Server?> = _recommendedServer.asStateFlow()

    private val _discoveredServers = MutableStateFlow<List<MdnsDiscoveryService.DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<MdnsDiscoveryService.DiscoveredServer>> = _discoveredServers.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _editServer = MutableStateFlow<Server?>(null)
    val editServer: StateFlow<Server?> = _editServer.asStateFlow()

    fun refreshRecommendation() {
        viewModelScope.launch {
            val builtIn = serverRepository.getBuiltIn()
            if (builtIn.isEmpty()) return@launch

            val results = pingService.pingAndSort(builtIn.map { it.address })
            val best = results.firstOrNull { it.reachable }

            if (best != null) {
                val server = builtIn.firstOrNull { it.address == best.host }
                val latency = best.avgLatencyMs
                if (server != null && latency != null) {
                    serverRepository.updateLatency(server.id, latency)
                    _recommendedServer.value = server.copy(lastLatencyMs = latency)
                }
            }

            // 更新所有服务器的延迟
            results.forEach { result ->
                val avgLatency = result.avgLatencyMs
                if (result.reachable && avgLatency != null) {
                    val server = builtIn.firstOrNull { it.address == result.host }
                    if (server != null) {
                        serverRepository.updateLatency(server.id, avgLatency)
                    }
                }
            }
        }
    }

    fun startDiscovery() {
        _isDiscovering.value = true
        viewModelScope.launch {
            mdnsDiscoveryService.discover().collect { server ->
                _discoveredServers.value = _discoveredServers.value + server
            }
        }
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
    }

    fun addDiscoveredServer(discovered: MdnsDiscoveryService.DiscoveredServer) {
        viewModelScope.launch {
            serverRepository.insert(
                Server(
                    name = discovered.name,
                    address = discovered.host,
                    port = discovered.port,
                    isBuiltIn = false,
                    region = "局域网",
                )
            )
        }
    }

    fun addCustomServer(name: String, address: String, port: Int) {
        viewModelScope.launch {
            serverRepository.insert(
                Server(
                    name = name,
                    address = address,
                    port = port,
                    isBuiltIn = false,
                )
            )
        }
    }

    fun updateServer(server: Server) {
        viewModelScope.launch {
            serverRepository.update(server)
        }
    }

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            serverRepository.delete(server)
        }
    }

    fun toggleFavorite(server: Server) {
        viewModelScope.launch {
            serverRepository.updateFavorite(server.id, !server.isFavorite)
        }
    }

    fun selectEditServer(server: Server?) {
        _editServer.value = server
    }
}
