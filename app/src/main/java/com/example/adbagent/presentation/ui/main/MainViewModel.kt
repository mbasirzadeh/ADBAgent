package com.example.adbagent.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adbagent.data.datasource.local.datastore.DataStore
import com.example.adbagent.domain.usecase.ConfigureAdbUseCase
import com.example.adbagent.presentation.model.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val repo: DataStore,
    private val configurator: ConfigureAdbUseCase
) : ViewModel() {

    val rsa = repo.rsa.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val port = repo.port.stateIn(viewModelScope, SharingStarted.Eagerly, 5555)
    val isADBEnable = configurator.adbEnabledFlow().stateIn(viewModelScope, SharingStarted.Eagerly,false)

    private val wifiIp: StateFlow<String?> = configurator.wifiIpFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = configurator.getWifiIp()
        )

    val endpoint: StateFlow<String> = combine(wifiIp, port) { ip, p ->
        ip?.let { "$it:$p" } ?: "No Wi‑Fi IP"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    fun savePort(input: Int?) = viewModelScope.launch {
        val p = input ?: return@launch _uiEvent.emit(UiEvent.ShowToast("Port cannot be empty"))
        if (p !in 1..65_535) {
            _uiEvent.emit(UiEvent.ShowToast("Port must be 1‑65535"))
            return@launch
        }
        repo.savePort(p)
        _uiEvent.emit(UiEvent.ShowToast("Port saved"))
    }

    fun saveRSAKey(key: String) = viewModelScope.launch {
        if (key.isBlank()) {
            _uiEvent.emit(UiEvent.ShowToast("RSA key cannot be empty"))
            return@launch
        }
        // Persist first so that future launches pick it up even if root fails now
        repo.saveRSA(key)
        if (configurator.addKeyIfPossible(key)) {
            _uiEvent.emit(UiEvent.ShowToast("RSA key saved"))
        } else {
            _uiEvent.emit(UiEvent.ShowToast("Cannot add RSA key (need root)"))
        }
    }

    fun startAdb() = viewModelScope.launch {
        val success = configurator.startAdb()
        if (success) {
            _uiEvent.emit(UiEvent.CopyText(endpoint.value))
            delay(100)
            _uiEvent.emit(UiEvent.ShowToast("ADB ready on ${endpoint.value} (copied)"))
        } else {
            _uiEvent.emit(UiEvent.ShowToast("Failed to start ADB"))
        }
    }

}