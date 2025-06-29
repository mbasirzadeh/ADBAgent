package com.example.adbagent.presentation.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adbagent.data.datasource.local.datastore.DataStore
import com.example.adbagent.domain.usecase.ConfigureAdbUseCase
import com.example.adbagent.presentation.model.UiEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repo: DataStore,
    private val configurator: ConfigureAdbUseCase
) : ViewModel() {
    val rsa = repo.rsa.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val port = repo.port.stateIn(viewModelScope, SharingStarted.Eagerly, 5555)
    var endpoint by mutableStateOf("")
        private set

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    fun save(rsa: String, port: Int) = viewModelScope.launch {
        repo.saveRSA(rsa)
        repo.savePort(port)
    }

    fun configure() = viewModelScope.launch {
        configurator().onSuccess { ep ->
            endpoint = ep
            _uiEvent.emit(UiEvent.CopyText(ep))
            delay(100)
            _uiEvent.emit(UiEvent.ShowToast("ADB ready on $ep (copied)"))
        }.onFailure { e ->
            _uiEvent.emit(UiEvent.ShowToast(e.message ?: "error"))
        }
    }
}