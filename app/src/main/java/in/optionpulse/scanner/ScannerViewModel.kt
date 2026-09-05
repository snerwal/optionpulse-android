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
    val callsOnly: Boolean = false
)

class ScannerViewModel(private val repository: ScannerRepository = DemoScannerRepository()) : ViewModel() {
    private val _ui = MutableStateFlow(ScannerUiState())
    val ui: StateFlow<ScannerUiState> = _ui.asStateFlow()
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true)
        _ui.value = _ui.value.copy(loading = false, status = repository.marketStatus(), signals = repository.signals())
    }
    fun select(signal: Signal?) { _ui.value = _ui.value.copy(selected = signal) }
    fun toggleCalls() { _ui.value = _ui.value.copy(callsOnly = !_ui.value.callsOnly) }
}
