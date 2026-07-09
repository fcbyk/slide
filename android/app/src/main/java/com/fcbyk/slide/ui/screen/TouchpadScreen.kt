package com.fcbyk.slide.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fcbyk.slide.ui.theme.Green
import com.fcbyk.slide.ui.theme.Primary
import com.fcbyk.slide.ui.theme.Red
import com.fcbyk.slide.viewmodel.ConnectionStatus
import com.fcbyk.slide.viewmodel.TouchpadUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 手势识别常量，与网页端对齐 */
private const val CLICK_MAX_DURATION = 200L
private const val DOUBLE_TAP_INTERVAL = 200L
private const val LONG_PRESS_THRESHOLD = 300L
private const val MOVE_THRESHOLD = 0.5f
private const val SCROLL_MULTIPLIER = 20f

@Composable
fun TouchpadScreen(
    state: TouchpadUiState,
    onLogout: () -> Unit,
    onSendClick: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onToggleDragMode: () -> Unit,
    onEnterStealthMode: () -> Unit,
    onAccumulateMove: (Float, Float) -> Unit,
    onStartFlush: () -> Unit,
    onStopFlush: () -> Unit,
    onMouseDown: () -> Unit,
    onMouseUp: () -> Unit,
    onRightClick: () -> Unit,
    onScroll: (Float, Float) -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (state.isStealthMode) {
        // ==================== 息屏模式：纯黑全屏触摸板 ====================
        TouchpadArea(
            isDragMode = state.isDragMode,
            onClick = onSendClick,
            onAccumulateMove = onAccumulateMove,
            onStartFlush = onStartFlush,
            onStopFlush = onStopFlush,
            onMouseDown = onMouseDown,
            onMouseUp = onMouseUp,
            onRightClick = onRightClick,
            onScroll = onScroll,
            isStealth = true,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
        ) {
            // ==================== 顶部栏 ====================
            HeaderBar(
                connectionStatus = state.connectionStatus,
                latency = state.latency,
                isDragMode = state.isDragMode,
                isDarkMode = isDarkMode,
                onToggleDragMode = onToggleDragMode,
                onToggleDarkMode = onToggleDarkMode,
                onEnterStealthMode = onEnterStealthMode,
                onLogout = onLogout,
            )

            // ==================== 触摸板区域 ====================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                TouchpadArea(
                    isDragMode = state.isDragMode,
                    onClick = onSendClick,
                    onAccumulateMove = onAccumulateMove,
                    onStartFlush = onStartFlush,
                    onStopFlush = onStopFlush,
                    onMouseDown = onMouseDown,
                    onMouseUp = onMouseUp,
                    onRightClick = onRightClick,
                    onScroll = onScroll,
                    isStealth = false,
                )
            }

            // ==================== 底部按钮 ====================
            BottomControls(
                onPrev = onPrev,
                onNext = onNext,
            )
        }
    }
}

// ==================== 顶部栏 ====================
@Composable
private fun HeaderBar(
    connectionStatus: ConnectionStatus,
    latency: Long,
    isDragMode: Boolean,
    isDarkMode: Boolean,
    onToggleDragMode: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onEnterStealthMode: () -> Unit,
    onLogout: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val haptic = LocalHapticFeedback.current
    val vibrate: () -> Unit = remember { { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧：连接状态
        StatusIndicator(
            status = connectionStatus,
            latency = latency,
        )

        // 右侧按钮组
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 拖拽模式切换
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDragMode) Primary.copy(alpha = 0.15f) else surfaceColor
                    )
                    .border(
                        1.dp,
                        if (isDragMode) Primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { vibrate(); onToggleDragMode() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✋",
                    fontSize = 16.sp,
                )
            }

            // 息屏模式
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { vibrate(); onEnterStealthMode() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🌑",
                    fontSize = 16.sp,
                )
            }

            // 夜间模式切换
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { vibrate(); onToggleDarkMode() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isDarkMode) "☀️" else "🌙",
                    fontSize = 16.sp,
                )
            }

            // 退出登录按钮
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { vibrate(); onLogout() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "退出登录",
                    tint = Red,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ==================== 状态指示器 ====================
@Composable
private fun StatusIndicator(
    status: ConnectionStatus,
    latency: Long,
) {
    val dotColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.Connected -> Green
            ConnectionStatus.Connecting -> Color(0xFFF59E0B)
            ConnectionStatus.Disconnected -> Red
        },
        label = "dotColor",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    val textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    val bgColor = if (MaterialTheme.colorScheme.background == Color(0xFF000000))
        Color.White.copy(alpha = 0.08f)
    else Color.Black.copy(alpha = 0.04f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 圆点
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (status == ConnectionStatus.Connecting) dotColor.copy(alpha = pulseAlpha)
                    else dotColor
                ),
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 状态文字
        Text(
            text = when (status) {
                ConnectionStatus.Connected -> "已连接"
                ConnectionStatus.Connecting -> "连接中..."
                ConnectionStatus.Disconnected -> "未连接"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )

        // 延迟
        if (status == ConnectionStatus.Connected && latency > 0) {
            Text(
                text = "  ${latency}ms",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Green.copy(alpha = 0.8f),
            )
        }
    }
}

// ==================== 触摸板区域（完整多指手势） ====================
@Composable
private fun TouchpadArea(
    isDragMode: Boolean,
    onClick: () -> Unit,
    onAccumulateMove: (Float, Float) -> Unit,
    onStartFlush: () -> Unit,
    onStopFlush: () -> Unit,
    onMouseDown: () -> Unit,
    onMouseUp: () -> Unit,
    onRightClick: () -> Unit,
    onScroll: (Float, Float) -> Unit,
    isStealth: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val vibrate: () -> Unit = remember { { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }

    // 手势状态（在 remember 中保持，不受 recomposition 影响）
    val gestureState = remember {
        GestureState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isStealth) Modifier.background(Color.Black)
                else Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            )
            .pointerInput(isDragMode) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val now = event.changes.firstOrNull()?.uptimeMillis ?: System.currentTimeMillis()

                        // 按 pressed 状态更新活跃指针集合
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                gestureState.activePointers[change.id.value] = change.position
                            } else {
                                gestureState.activePointers.remove(change.id.value)
                            }
                        }

                        val currentCount = gestureState.activePointers.size

                        when (event.type) {
                            PointerEventType.Press -> {
                                // 新的触摸开始，启动移动刷新协程
                                onStartFlush()
                                handlePress(
                                    gestureState, currentCount, now, isDragMode,
                                    onMouseDown, scope
                                )
                            }
                            PointerEventType.Move -> {
                                // 手指移动
                                handleMove(
                                    gestureState, currentCount,
                                    onAccumulateMove, onScroll
                                )
                            }
                            PointerEventType.Release -> {
                                // 触摸结束
                                handleRelease(
                                    gestureState, currentCount, now,
                                    onClick, onStopFlush,
                                    onMouseUp, onRightClick, vibrate, scope
                                )
                            }
                            else -> {}
                        }

                        // 消费所有事件，阻止默认行为
                        event.changes.forEach { if (it.pressed) it.consume() }

                        gestureState.previousCount = currentCount
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!isStealth) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "👆",
                    fontSize = 40.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isDragMode) "拖拽模式已开启"
                    else "滑动移动光标 · 点击左键",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isDragMode) "单指拖拽 · 双指滚动"
                    else "双指点击右键 · 双指滑动滚轮",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                    textAlign = TextAlign.Center,
                )
            }

            // 右下角标签
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isDragMode) Primary else Green),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDragMode) "拖拽模式" else "触控区域",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

// ==================== 手势状态管理 ====================

/** 触摸手势状态，完整对齐网页端 useTouchpad 的状态机 */
private class GestureState {
    val activePointers = mutableMapOf<Long, Offset>()
    var previousCount = 0

    var gestureStartTime = 0L
    var hasMoved = false
    var isDragging = false
    var twoFingerMoved = false

    var lastTapTime = 0L
    var lastTapWasClick = false
    var isSecondTapCandidate = false

    var lastSingleX = 0f
    var lastSingleY = 0f
    var lastTwoFingerY = 0f

    var pendingClickJob: Job? = null
    var longPressJob: Job? = null

    /** 前一帧触摸数量（用于检测手指增减） */
    var fingersBeforeRelease = 0
}

// ==================== 手势处理函数 ====================

private fun handlePress(
    gs: GestureState,
    currentCount: Int,
    now: Long,
    isDragMode: Boolean,
    onMouseDown: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    gs.gestureStartTime = now
    gs.hasMoved = false
    gs.isDragging = false
    gs.twoFingerMoved = false
    gs.isSecondTapCandidate = false
    gs.fingersBeforeRelease = currentCount

    when (currentCount) {
        1 -> {
            val pos = gs.activePointers.values.first()
            gs.lastSingleX = pos.x
            gs.lastSingleY = pos.y

            gs.pendingClickJob?.cancel()
            gs.longPressJob?.cancel()

            if (isDragMode) {
                // 拖拽模式：立刻进入拖拽
                gs.isDragging = true
                onMouseDown()
            } else {
                val isSecondTap = gs.lastTapWasClick &&
                    (now - gs.lastTapTime) <= DOUBLE_TAP_INTERVAL
                gs.isSecondTapCandidate = isSecondTap
                if (isSecondTap) {
                    gs.pendingClickJob?.cancel()
                    gs.longPressJob = scope.launch {
                        delay(LONG_PRESS_THRESHOLD)
                        gs.isDragging = true
                        onMouseDown()
                    }
                }
            }
        }
        2 -> {
            val positions = gs.activePointers.values.toList()
            gs.lastSingleX = (positions[0].x + positions[1].x) / 2f
            gs.lastSingleY = (positions[0].y + positions[1].y) / 2f
            gs.lastTwoFingerY = 0f
        }
    }
}

private fun handleMove(
    gs: GestureState,
    currentCount: Int,
    onAccumulateMove: (Float, Float) -> Unit,
    onScroll: (Float, Float) -> Unit,
) {
    if (currentCount != gs.previousCount) return

    when (currentCount) {
        1 -> {
            val pos = gs.activePointers.values.first()
            val dx = pos.x - gs.lastSingleX
            val dy = pos.y - gs.lastSingleY

            if (abs(dx) > MOVE_THRESHOLD || abs(dy) > MOVE_THRESHOLD) {
                gs.hasMoved = true
                onAccumulateMove(dx, dy)
                gs.lastSingleX = pos.x
                gs.lastSingleY = pos.y
            }
        }
        2 -> {
            val positions = gs.activePointers.values.toList()
            val cx = (positions[0].x + positions[1].x) / 2f
            val cy = (positions[0].y + positions[1].y) / 2f

            if (gs.lastTwoFingerY == 0f) {
                gs.lastTwoFingerY = cy
            } else {
                val dy = cy - gs.lastTwoFingerY
                if (abs(dy) > MOVE_THRESHOLD) {
                    gs.twoFingerMoved = true
                    gs.hasMoved = true
                    onScroll(0f, dy * SCROLL_MULTIPLIER)
                    gs.lastTwoFingerY = cy
                }
            }
            gs.lastSingleX = cx
            gs.lastSingleY = cy
        }
    }
}

private fun handleRelease(
    gs: GestureState,
    currentCount: Int,
    now: Long,
    onClick: () -> Unit,
    onStopFlush: () -> Unit,
    onMouseUp: () -> Unit,
    onRightClick: () -> Unit,
    vibrate: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    // 还有手指在屏幕上（如双指逐个抬起），暂不处理，等待全部抬起
    if (currentCount > 0) return

    val totalFingers = gs.fingersBeforeRelease
    val duration = now - gs.gestureStartTime

    gs.pendingClickJob?.cancel()
    gs.longPressJob?.cancel()

    // 停止移动刷新并发送剩余累积量
    onStopFlush()

    if (gs.isDragging) {
        onMouseUp()
        gs.lastTapWasClick = false
    } else {
        if (!gs.hasMoved && duration < CLICK_MAX_DURATION) {
            when (totalFingers) {
                1 -> {
                    if (gs.isSecondTapCandidate) {
                        // 双击
                        vibrate()
                        onClick()
                        onClick()
                        gs.lastTapTime = now
                        gs.lastTapWasClick = false
                    } else {
                        // 单击 - 延迟 200ms 发送，等待双击判定
                        gs.pendingClickJob = scope.launch {
                            delay(DOUBLE_TAP_INTERVAL)
                            vibrate()
                            onClick()
                        }
                        gs.lastTapTime = now
                        gs.lastTapWasClick = true
                    }
                }
                2 -> {
                    if (!gs.twoFingerMoved) {
                        vibrate()
                        onRightClick()
                        gs.lastTapWasClick = false
                    }
                }
            }
        } else {
            gs.lastTapWasClick = false
        }
    }

    // 重置状态
    gs.hasMoved = false
    gs.isDragging = false
    gs.twoFingerMoved = false
    gs.isSecondTapCandidate = false
    gs.fingersBeforeRelease = 0
    gs.lastTwoFingerY = 0f
}

// ==================== 底部控制按钮 ====================
@Composable
private fun BottomControls(
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val vibrate: () -> Unit = remember { { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 上一页
        ControlButton(
            text = "上一页",
            subText = "PREVIOUS",
            isPrimary = false,
            onClick = { vibrate(); onPrev() },
            modifier = Modifier.weight(1f),
        )

        // 下一页
        ControlButton(
            text = "下一页",
            subText = "NEXT SLIDE",
            isPrimary = true,
            onClick = { vibrate(); onNext() },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ControlButton(
    text: String,
    subText: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isPrimary) Primary else MaterialTheme.colorScheme.surface
    val textColor = if (isPrimary) Color.White else MaterialTheme.colorScheme.onBackground
    val subColor = if (isPrimary)
        Color.White.copy(alpha = 0.6f)
    else
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)

    Column(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .then(
                if (!isPrimary) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                else Modifier.shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = Primary.copy(alpha = 0.3f), spotColor = Primary.copy(alpha = 0.3f))
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
        Text(
            text = subText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = subColor,
        )
    }
}
