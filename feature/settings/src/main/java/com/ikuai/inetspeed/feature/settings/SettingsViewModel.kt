package com.ikuai.inetspeed.feature.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.iperf3.Iperf3Runner
import com.ikuai.inetspeed.core.iperf3.model.IperfVersion
import com.ikuai.inetspeed.core.sync.engine.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val iperf3Runner: Iperf3Runner,
    private val syncEngine: SyncEngine,
) : ViewModel() {

    private val _iperfVersion = MutableStateFlow<IperfVersion?>(null)
    val iperfVersion: StateFlow<IperfVersion?> = _iperfVersion.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    val syncState = syncEngine.syncState

    init {
        loadIperfVersion()
    }

    fun loadIperfVersion() {
        viewModelScope.launch {
            _iperfVersion.value = iperf3Runner.version()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncEngine.sync()
        }
    }

    fun getAppVersion(): String {
        return "1.0.0"
    }

    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}"
    }

    enum class ThemeMode {
        SYSTEM, LIGHT, DARK
    }
}
