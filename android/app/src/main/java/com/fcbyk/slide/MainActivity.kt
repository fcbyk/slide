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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fcbyk.slide.ui.screen.LoginScreen
import com.fcbyk.slide.ui.screen.QrScannerScreen
import com.fcbyk.slide.ui.screen.TouchpadScreen
import com.fcbyk.slide.ui.theme.SlideTheme
import com.fcbyk.slide.viewmodel.LoginViewModel

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
            val viewModel: LoginViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            val scanning by showScannerState

            SlideTheme(darkTheme = state.isDarkMode) {
                Crossfade(targetState = scanning to state.isLoggedIn) { (scanMode, loggedIn) ->
                    when {
                        scanMode -> QrScannerScreen(
                            onResult = { url -> handleScanResult(url, viewModel) },
                            onClose = { showScanner = false }
                        )
                        loggedIn -> TouchpadScreen(serverUrl = state.serverUrl)
                        else -> LoginScreen(
                            state = state,
                            onServerUrlChange = { viewModel.setServerUrl(it) },
                            onPasswordChange = { viewModel.setPassword(it) },
                            onLogin = { viewModel.login() },
                            onScanClick = { requestCamera() },
                            onToggleDarkMode = { viewModel.toggleDarkMode() }
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
