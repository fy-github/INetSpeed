package com.ikuai.inetspeed.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ikuai.inetspeed.core.data.model.TestMeasurement
import com.ikuai.inetspeed.core.data.repository.TestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val testRepository: TestRepository,
) : ViewModel() {

    enum class TimeRange(val label: String, val days: Int) {
        WEEK("7天", 7),
        MONTH("30天", 30),
        ALL("全部", 0),
    }

    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    private val _filterProtocol = MutableStateFlow<String?>(null)
    val filterProtocol: StateFlow<String?> = _filterProtocol.asStateFlow()

    private val _filterServerId = MutableStateFlow<Long?>(null)
    val filterServerId: StateFlow<Long?> = _filterServerId.asStateFlow()

    private val allMeasurements = testRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMeasurements: StateFlow<List<TestMeasurement>> = combine(
        allMeasurements,
        _timeRange,
        _filterProtocol,
        _filterServerId,
    ) { measurements, range, protocol, serverId ->
        val cutoff = if (range.days > 0) {
            System.currentTimeMillis() - range.days * 24 * 60 * 60 * 1000L
        } else 0L

        measurements.filter { m ->
            m.timestamp >= cutoff
                && (protocol == null || m.protocol == protocol)
                && (serverId == null || m.serverId == serverId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    fun setFilterProtocol(protocol: String?) {
        _filterProtocol.value = protocol
    }

    fun setFilterServerId(serverId: Long?) {
        _filterServerId.value = serverId
    }

    fun deleteMeasurement(measurement: TestMeasurement) {
        viewModelScope.launch {
            testRepository.delete(measurement)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            testRepository.deleteById(id)
        }
    }
}
