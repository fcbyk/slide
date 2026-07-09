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
    val isDragMode: Boolean = false,
    val isStealthMode: Boolean = false,
)

class TouchpadViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TouchpadUiState())
    val uiState: StateFlow<TouchpadUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var initialized = false

    /** 移动累积器，用于批量发送鼠标移动事件 */
    private var moveAccDx = 0f
    private var moveAccDy = 0f
    private var flushJob: Job? = null

    private val mouseSensitivity = 1.5f

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

    /** 切换拖拽模式 */
    fun toggleDragMode() {
        _uiState.value = _uiState.value.copy(isDragMode = !_uiState.value.isDragMode)
    }

    /** 进入息屏模式（纯黑触摸板，音量键翻页，通知栏退出） */
    fun enterStealthMode() {
        _uiState.value = _uiState.value.copy(isStealthMode = true)
    }

    /** 退出息屏模式 */
    fun exitStealthMode() {
        _uiState.value = _uiState.value.copy(isStealthMode = false)
    }

    // ==================== 鼠标操作 ====================

    /** 触摸板点击 → 发送鼠标点击 */
    fun sendClick() {
        viewModelScope.launch {
            ApiClient.mouseClick(_uiState.value.serverUrl)
        }
    }

    /** 鼠标移动（累积并批量发送） */
    fun accumulateMove(dx: Float, dy: Float) {
        moveAccDx += dx
        moveAccDy += dy
    }

    /** 启动移动刷新协程（持续运行，模拟 requestAnimationFrame） */
    fun startFlush() {
        if (flushJob?.isActive == true) return
        flushJob = viewModelScope.launch {
            while (true) {
                delay(16) // ~60fps
                val dx = moveAccDx
                val dy = moveAccDy
                if (dx != 0f || dy != 0f) {
                    moveAccDx = 0f
                    moveAccDy = 0f
                    val serverUrl = _uiState.value.serverUrl
                    val dxFinal = dx * mouseSensitivity
                    val dyFinal = dy * mouseSensitivity
                    // 异步发送，不阻塞刷新循环
                    launch {
                        ApiClient.mouseMove(serverUrl, dxFinal, dyFinal)
                    }
                }
            }
        }
    }

    /** 停止移动刷新并发送剩余累积量 */
    fun stopFlush() {
        flushJob?.cancel()
        flushJob = null
        val dx = moveAccDx
        val dy = moveAccDy
        moveAccDx = 0f
        moveAccDy = 0f
        if (dx != 0f || dy != 0f) {
            viewModelScope.launch {
                ApiClient.mouseMove(
                    _uiState.value.serverUrl,
                    dx * mouseSensitivity,
                    dy * mouseSensitivity
                )
            }
        }
    }

    /** 鼠标按下 */
    fun sendMouseDown() {
        viewModelScope.launch {
            ApiClient.mouseDown(_uiState.value.serverUrl)
        }
    }

    /** 鼠标释放 */
    fun sendMouseUp() {
        viewModelScope.launch {
            ApiClient.mouseUp(_uiState.value.serverUrl)
        }
    }

    /** 鼠标右键 */
    fun sendRightClick() {
        viewModelScope.launch {
            ApiClient.mouseRightClick(_uiState.value.serverUrl)
        }
    }

    /** 鼠标滚轮 */
    fun sendScroll(dx: Float, dy: Float) {
        viewModelScope.launch {
            ApiClient.mouseScroll(_uiState.value.serverUrl, dx, dy)
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
        stopFlush()
        viewModelScope.launch {
            ApiClient.logout(_uiState.value.serverUrl)
            _uiState.value = _uiState.value.copy(isLoggedIn = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
        stopFlush()
    }
}
