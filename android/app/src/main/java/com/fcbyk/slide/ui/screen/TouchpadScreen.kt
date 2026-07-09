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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fcbyk.slide.ui.theme.Green
import com.fcbyk.slide.ui.theme.Primary
import com.fcbyk.slide.ui.theme.Red
import com.fcbyk.slide.viewmodel.ConnectionStatus
import com.fcbyk.slide.viewmodel.TouchpadUiState

@Composable
fun TouchpadScreen(
    state: TouchpadUiState,
    onLogout: () -> Unit,
    onSendClick: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            onLogout = onLogout,
        )

        // ==================== 触摸板区域 ====================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TouchpadArea(onClick = onSendClick)
        }

        // ==================== 底部按钮 ====================
        BottomControls(
            onPrev = onPrev,
            onNext = onNext,
        )
    }
}

// ==================== 顶部栏 ====================
@Composable
private fun HeaderBar(
    connectionStatus: ConnectionStatus,
    latency: Long,
    onLogout: () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

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

        // 右侧：退出登录按钮
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { onLogout() },
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

// ==================== 触摸板区域 ====================
@Composable
private fun TouchpadArea(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 图标提示
            Text(
                text = "👆",
                fontSize = 40.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "点击此处发送鼠标点击",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "点击电脑鼠标左键",
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
                        .background(Green),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "触控区域",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                )
            }
        }
    }
}

// ==================== 底部控制按钮 ====================
@Composable
private fun BottomControls(
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
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
            onClick = onPrev,
            modifier = Modifier.weight(1f),
        )

        // 下一页
        ControlButton(
            text = "下一页",
            subText = "NEXT SLIDE",
            isPrimary = true,
            onClick = onNext,
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
