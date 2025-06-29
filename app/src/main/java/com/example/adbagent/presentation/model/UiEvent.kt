package com.example.adbagent.presentation.model

sealed class UiEvent {
    data class ShowToast(val msg: String) : UiEvent()
    data class CopyText(val text: String) : UiEvent()
}
