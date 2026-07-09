package com.fcbyk.slide.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fcbyk.slide.data.api.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionStatus {
    Connected,
    Connecting,
    Disconnected,
}

data class TouchpadUiState(
    val serverUrl: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.Connecting,
    val latency: Long = 0,
    val isLoggedIn: Boolean = true,
)

class TouchpadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var initialized = false

    fun init(serverUrl: String) {
        if (initialized) return
        initialized = true
        _uiState.value = _uiState.value.copy(
            serverUrl = serverUrl,
            connectionStatus = ConnectionStatus.Connecting,
        )
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                val serverUrl = _uiState.value.serverUrl
                if (serverUrl.isEmpty()) {
                    delay(3_000)
                    continue
                }

                val result = ApiClient.checkAuth(serverUrl)
                _uiState.value = _uiState.value.copy(
                    connectionStatus = if (result.success) ConnectionStatus.Connected
                    else ConnectionStatus.Disconnected,
                    latency = if (result.success) result.latency else 0,
                )

                delay(3_000)
            }
        }
    }

    /** 触摸板点击 → 发送鼠标点击 */
    fun sendClick() {
        viewModelScope.launch {
            ApiClient.mouseClick(_uiState.value.serverUrl)
        }
    }

    /** 下一页 */
    fun nextSlide() {
        viewModelScope.launch {
            ApiClient.nextSlide(_uiState.value.serverUrl)
        }
    }

    /** 上一页 */
    fun prevSlide() {
        viewModelScope.launch {
            ApiClient.prevSlide(_uiState.value.serverUrl)
        }
    }

    /** 退出登录 */
    fun logout() {
        pollJob?.cancel()
        viewModelScope.launch {
            ApiClient.logout(_uiState.value.serverUrl)
            _uiState.value = _uiState.value.copy(isLoggedIn = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
