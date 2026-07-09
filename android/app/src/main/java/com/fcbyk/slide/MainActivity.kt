package com.fcbyk.slide

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fcbyk.slide.ui.screen.LoginScreen
import com.fcbyk.slide.ui.screen.QrScannerScreen
import com.fcbyk.slide.ui.screen.TouchpadScreen
import com.fcbyk.slide.ui.theme.SlideTheme
import com.fcbyk.slide.viewmodel.LoginViewModel
import com.fcbyk.slide.viewmodel.TouchpadViewModel

class MainActivity : ComponentActivity() {

    private val showScannerState = mutableStateOf(false)
    private var showScanner: Boolean
        get() = showScannerState.value
        set(v) { showScannerState.value = v }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showScanner = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val loginViewModel: LoginViewModel = viewModel()
            val loginState by loginViewModel.uiState.collectAsState()
            val scanning by showScannerState

            SlideTheme(darkTheme = loginState.isDarkMode) {
                Crossfade(
                    targetState = scanning to loginState.isLoggedIn,
                    modifier = Modifier.fillMaxSize()
                ) { (scanMode, loggedIn) ->
                    when {
                        scanMode -> QrScannerScreen(
                            onResult = { url -> handleScanResult(url, loginViewModel) },
                            onClose = { showScanner = false }
                        )
                        loggedIn -> {
                            val touchpadViewModel: TouchpadViewModel = viewModel()
                            val touchpadState by touchpadViewModel.uiState.collectAsState()

                            LaunchedEffect(loginState.serverUrl) {
                                touchpadViewModel.init(loginState.serverUrl)
                            }

                            // 监听退出登录
                            LaunchedEffect(touchpadState.isLoggedIn) {
                                if (!touchpadState.isLoggedIn) {
                                    loginViewModel.resetLoginState()
                                }
                            }

                            TouchpadScreen(
                                state = touchpadState,
                                onLogout = { touchpadViewModel.logout() },
                                onSendClick = { touchpadViewModel.sendClick() },
                                onNext = { touchpadViewModel.nextSlide() },
                                onPrev = { touchpadViewModel.prevSlide() },
                            )
                        }
                        else -> LoginScreen(
                            state = loginState,
                            onServerUrlChange = { loginViewModel.setServerUrl(it) },
                            onPasswordChange = { loginViewModel.setPassword(it) },
                            onLogin = { loginViewModel.login() },
                            onScanClick = { requestCamera() },
                            onToggleDarkMode = { loginViewModel.toggleDarkMode() }
                        )
                    }
                }
            }
        }
    }

    private fun requestCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED -> showScanner = true
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun handleScanResult(rawUrl: String, viewModel: LoginViewModel) {
        showScanner = false
        try {
            val uri = Uri.parse(rawUrl)
            val host = uri.host ?: return
            val port = if (uri.port > 0 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            val baseUrl = "${uri.scheme}://$host$port"
            val token = uri.getQueryParameter("token")

            if (token != null) {
                // 有 token，直接自动登录
                viewModel.autoLoginFromQr(baseUrl, token)
            } else {
                // 没有 token，只填入地址
                viewModel.setServerUrl(baseUrl)
            }
        } catch (_: Exception) {}
    }
}
