package com.ikuai.inetspeed.feature.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.prefs.ThemeChoice
import com.ikuai.inetspeed.core.data.prefs.ThemePreferences
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
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    private val _iperfVersion = MutableStateFlow<IperfVersion?>(null)
    val iperfVersion: StateFlow<IperfVersion?> = _iperfVersion.asStateFlow()

    private val _themeMode = MutableStateFlow(themePreferences.getTheme())
    val themeMode: StateFlow<ThemeChoice> = _themeMode.asStateFlow()

    val syncState = syncEngine.syncState

    init {
        loadIperfVersion()
        viewModelScope.launch {
            themePreferences.observeTheme().collect { _themeMode.value = it }
        }
    }

    fun loadIperfVersion() {
        viewModelScope.launch {
            try {
                _iperfVersion.value = iperf3Runner.version()
            } catch (_: Exception) {
                _iperfVersion.value = IperfVersion("unknown", "获取失败")
            }
        }
    }

    fun setThemeMode(mode: ThemeChoice) {
        themePreferences.setTheme(mode)
    }

    fun triggerSync() {
        viewModelScope.launch {
            try {
                syncEngine.sync()
            } catch (_: Exception) {}
        }
    }

    fun getAppVersion(): String {
        return "1.0.0"
    }

    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}"
    }
}
