package com.fcbyk.slide.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fcbyk.slide.ui.theme.Primary
import com.fcbyk.slide.ui.theme.Red
import com.fcbyk.slide.viewmodel.LoginUiState
import kotlin.math.roundToInt

@Composable
fun LoginScreen(
    state: LoginUiState,
    onServerUrlChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onScanClick: () -> Unit,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            // 顶栏 - 暗色模式
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onToggleDarkMode) {
                    Icon(
                        imageVector = if (state.isDarkMode) Icons.Filled.LightMode
                        else Icons.Filled.DarkMode,
                        contentDescription = "切换暗色模式",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // 标题
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SLIDE",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "准备好控制了吗？请输入访问密码",
                    fontSize = 12.sp,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // 服务器地址 —— 始终可见
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = onServerUrlChange,
                    placeholder = { Text("服务器地址 例：192.168.1.100", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)) },
                    leadingIcon = { Icon(Icons.Filled.Language, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onScanClick) {
                            Icon(
                                Icons.Filled.QrCodeScanner,
                                contentDescription = "扫描二维码",
                                tint = Primary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 密码 —— 始终可见
                OutlinedTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    placeholder = { Text("访问密码", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)) },
                    leadingIcon = { Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onLogin()
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 错误提示
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = state.errorMessage, color = Red, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }

            // 底部滑块
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .navigationBarsPadding()
            ) {
                SlideToConfirm(
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onConfirm = onLogin
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SlideToConfirm(
    isLoading: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit
) {
    val thumbSize = 52.dp
    val trackPadding = 6.dp
    val density = LocalDensity.current
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val paddingPx = with(density) { trackPadding.toPx() * 2 }

    var trackWidth by remember { mutableFloatStateOf(0f) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    if (errorMessage != null && dragOffset > 0f) {
        dragOffset = 0f
    }

    val maxDrag = if (trackWidth > 0) trackWidth - thumbSizePx - paddingPx else 0f
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(durationMillis = if (dragOffset == 0f) 500 else 0),
        label = "slider"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(32.dp))
            .onSizeChanged { trackWidth = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "右滑进入控制界面",
            fontSize = 13.sp,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.Center).padding(start = 52.dp)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt().coerceIn(0, maxDrag.roundToInt()), 0) }
                .padding(trackPadding)
                .size(thumbSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(Primary)
                .pointerInput(isLoading) {
                    if (isLoading) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset >= maxDrag * 0.95f) onConfirm()
                            dragOffset = 0f
                        }
                    ) { _, dragAmount ->
                        dragOffset = (dragOffset + dragAmount).coerceIn(0f, maxDrag)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
