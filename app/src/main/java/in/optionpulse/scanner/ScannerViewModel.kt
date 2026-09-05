package com.optionpulse.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val loading: Boolean = true,
    val status: MarketStatus = MarketStatus(),
    val signals: List<Signal> = emptyList(),
    val selected: Signal? = null,
    val callsOnly: Boolean = false,
    val error: String? = null
)

class ScannerViewModel(private val repository: ScannerRepository = LiveScannerRepository()) : ViewModel() {
    private val _ui = MutableStateFlow(ScannerUiState())
    val ui: StateFlow<ScannerUiState> = _ui.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        runCatching { repository.marketStatus() to repository.signals() }
            .onSuccess { (status, signals) -> _ui.value = _ui.value.copy(loading = false, status = status, signals = signals) }
            .onFailure { error -> _ui.value = _ui.value.copy(loading = false, status = MarketStatus(connected = false, scanned = 0), signals = emptyList(), error = error.message ?: "Backend unavailable") }
    }
    fun select(signal: Signal?) { _ui.value = _ui.value.copy(selected = signal) }
    fun toggleCalls() { _ui.value = _ui.value.copy(callsOnly = !_ui.value.callsOnly) }
}
