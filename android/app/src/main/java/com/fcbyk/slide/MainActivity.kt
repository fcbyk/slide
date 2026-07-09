package com.fcbyk.slide

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fcbyk.slide.ui.screen.LoginScreen
import com.fcbyk.slide.ui.screen.QrScannerScreen
import com.fcbyk.slide.ui.screen.TouchpadScreen
import com.fcbyk.slide.ui.theme.SlideTheme
import com.fcbyk.slide.viewmodel.LoginViewModel
import com.fcbyk.slide.viewmodel.TouchpadViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val CHANNEL_ID = "slide_stealth"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_EXIT_STEALTH = "com.fcbyk.slide.EXIT_STEALTH"
    }

    private val showScannerState = mutableStateOf(false)
    private var showScanner: Boolean
        get() = showScannerState.value
        set(v) { showScannerState.value = v }

    /** 音量键回调，仅登录后生效，未设置时走系统默认行为 */
    private var onVolumeKey: ((keyCode: Int) -> Unit)? = null

    /** 息屏模式退出回调（由通知栏退出按钮触发） */
    private var onRequestExitStealth: (() -> Unit)? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showScanner = true }

    /** 通知权限请求（Android 13+），允许即能显示息屏退出通知 */
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 不管用户是否允许，息屏模式都会进入 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

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

                            // 注册音量键翻页
                            DisposableEffect(Unit) {
                                onVolumeKey = { keyCode ->
                                    when (keyCode) {
                                        KeyEvent.KEYCODE_VOLUME_UP -> touchpadViewModel.prevSlide()
                                        KeyEvent.KEYCODE_VOLUME_DOWN -> touchpadViewModel.nextSlide()
                                    }
                                }
                                onDispose { onVolumeKey = null }
                            }

                            // 注册通知栏退出回调
                            DisposableEffect(Unit) {
                                onRequestExitStealth = { touchpadViewModel.exitStealthMode() }
                                onDispose { onRequestExitStealth = null }
                            }

                            // 监听退出登录
                            LaunchedEffect(touchpadState.isLoggedIn) {
                                if (!touchpadState.isLoggedIn) {
                                    loginViewModel.resetLoginState()
                                }
                            }

                            // 息屏模式：调暗屏幕、保持唤醒、隐藏系统栏、显示通知
                            DisposableEffect(touchpadState.isStealthMode) {
                                if (touchpadState.isStealthMode) {
                                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    val attrs = window.attributes
                                    attrs.screenBrightness = 0.01f
                                    window.attributes = attrs
                                    WindowCompat.setDecorFitsSystemWindows(window, false)
                                    WindowInsetsControllerCompat(window, window.decorView).apply {
                                        hide(WindowInsetsCompat.Type.systemBars())
                                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                    }
                                    showStealthNotification()
                                }
                                onDispose {
                                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                    val attrs = window.attributes
                                    attrs.screenBrightness = -1f
                                    window.attributes = attrs
                                    WindowCompat.setDecorFitsSystemWindows(window, true)
                                    WindowInsetsControllerCompat(window, window.decorView).apply {
                                        show(WindowInsetsCompat.Type.systemBars())
                                    }
                                    dismissStealthNotification()
                                }
                            }

                            TouchpadScreen(
                                state = touchpadState,
                                onLogout = { touchpadViewModel.logout() },
                                onSendClick = { touchpadViewModel.sendClick() },
                                onNext = { touchpadViewModel.nextSlide() },
                                onPrev = { touchpadViewModel.prevSlide() },
                                onToggleDragMode = { touchpadViewModel.toggleDragMode() },
                                onEnterStealthMode = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            this@MainActivity,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    touchpadViewModel.enterStealthMode()
                                },
                                onAccumulateMove = { dx, dy -> touchpadViewModel.accumulateMove(dx, dy) },
                                onStartFlush = { touchpadViewModel.startFlush() },
                                onStopFlush = { touchpadViewModel.stopFlush() },
                                onMouseDown = { touchpadViewModel.sendMouseDown() },
                                onMouseUp = { touchpadViewModel.sendMouseUp() },
                                onRightClick = { touchpadViewModel.sendRightClick() },
                                onScroll = { dx, dy -> touchpadViewModel.sendScroll(dx, dy) },
                                isDarkMode = loginState.isDarkMode,
                                onToggleDarkMode = { loginViewModel.toggleDarkMode() },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_EXIT_STEALTH) {
            onRequestExitStealth?.invoke()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = onVolumeKey
        if (handler != null && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    handler(KeyEvent.KEYCODE_VOLUME_UP)
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    handler(KeyEvent.KEYCODE_VOLUME_DOWN)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ==================== 息屏通知 ====================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "全屏触摸板",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "全屏触摸板运行状态" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun showStealthNotification() {
        val exitIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_EXIT_STEALTH
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val exitPendingIntent = PendingIntent.getActivity(
            this, 0, exitIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("全屏触摸板运行中")
            .setContentText("音量键翻页 · 点击通知或下拉退出")
            .setContentIntent(exitPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "退出", exitPendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun dismissStealthNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(NOTIFICATION_ID)
    }

    // ==================== 其他 ====================

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
