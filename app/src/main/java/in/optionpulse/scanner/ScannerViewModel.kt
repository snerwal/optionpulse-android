package com.optionpulse.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(val loading:Boolean=true,val status:MarketStatus=MarketStatus(connected=false,scanned=0,alertsToday=0),val signals:List<Signal> = emptyList(),val selected:Signal?=null,val callsOnly:Boolean=false,val error:String?=null)

class ScannerViewModel(application:Application):AndroidViewModel(application){
 private val repository:ScannerRepository=LiveScannerRepository(application)
 private val _ui=MutableStateFlow(ScannerUiState())
 val ui:StateFlow<ScannerUiState> = _ui.asStateFlow()
 init{refresh()}
 fun refresh()=viewModelScope.launch{
  _ui.value=_ui.value.copy(loading=true,error=null)
  runCatching{repository.marketStatus() to repository.signals()}
   .onSuccess{(status,signals)->_ui.value=_ui.value.copy(loading=false,status=status,signals=signals)}
   .onFailure{e->_ui.value=_ui.value.copy(loading=false,error=e.message)}
 }
 fun select(signal:Signal?){_ui.value=_ui.value.copy(selected=signal)}
 fun toggleCalls(){_ui.value=_ui.value.copy(callsOnly=!_ui.value.callsOnly)}
}
