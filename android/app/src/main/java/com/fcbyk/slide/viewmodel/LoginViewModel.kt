package com.fcbyk.slide.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fcbyk.slide.data.api.ApiClient
import com.fcbyk.slide.data.api.LoginResult
import com.fcbyk.slide.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isDarkMode: Boolean = false,
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.isDarkMode.collect { dark ->
                _uiState.value = _uiState.value.copy(isDarkMode = dark)
            }
        }
        viewModelScope.launch {
            prefs.serverUrl.collect { url ->
                if (url.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(serverUrl = url)
                }
            }
        }
    }

    fun setServerUrl(url: String) {
        val trimmed = url.trim()
        _uiState.value = _uiState.value.copy(serverUrl = trimmed, errorMessage = null)
    }

    fun setPassword(pw: String) {
        _uiState.value = _uiState.value.copy(password = pw, errorMessage = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return

        var url = state.serverUrl.trim()
        if (url.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "请输入服务器地址")
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        val pw = state.password.trim()
        if (pw.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "请输入密码")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null, serverUrl = url)

        viewModelScope.launch {
            prefs.setServerUrl(url)
            when (val result = ApiClient.login(url, pw)) {
                is LoginResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                }
                is LoginResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val newValue = !_uiState.value.isDarkMode
            prefs.setDarkMode(newValue)
        }
    }

    /** 扫码自动登录 */
    fun autoLoginFromQr(serverUrl: String, token: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, serverUrl = serverUrl)

        viewModelScope.launch {
            prefs.setServerUrl(serverUrl)
            when (val result = ApiClient.autoLogin(serverUrl, token)) {
                is LoginResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true
                    )
                }
                is LoginResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}
