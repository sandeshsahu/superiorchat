package com.mobile.superiorutils.ui.components


import android.Manifest
import com.mobile.superiorutils.ui.ChatViewModel
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobile.superiorutils.theme.*
import com.mobile.superiorutils.ui.components.AttachMenu
import com.mobile.superiorutils.ui.components.MessageBubble
import com.mobile.superiorutils.ui.components.MediaViewer
import com.mobile.superiorutils.ui.components.MediaPicker
import com.mobile.superiorutils.ui.components.PickerTab
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBox(
    viewModel: ChatViewModel,
    showAttachmentMenu: Boolean,
    onAttachmentMenuChange: (Boolean) -> Unit,
    onRequestStoragePermission: (String) -> Unit,
    onRequestAudioPermission: (String) -> Unit,
    hasConnectionError: Boolean = false,
    isBotTokenInvalid: Boolean = false,
    isRetrying: Boolean = false,
    onRetryConnection: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val isRecording = viewModel.isRecordingAudio
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val density = LocalDensity.current

    // ── Swipe-to-cancel tracking ──
    var swipeDragX by remember { mutableFloatStateOf(0f) }
    val cancelThresholdPx = with(density) { 80.dp.toPx() }
    val isCancelZone = swipeDragX < -cancelThresholdPx

    // ── Glow modifier for mic button is now handled by Modifier.glow ──

    // ── Animation values ──
    val buttonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "mic_scale"
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) MaterialTheme.colorScheme.error else PrimaryLight,
        animationSpec = tween(300),
        label = "mic_color"
    )
    val buttonIconTint by animateColorAsState(
        targetValue = if (isRecording) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = tween(300),
        label = "mic_icon_tint"
    )

    // ── Pulsing glow ring for recording state ──
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    AnimatedContent(
        targetState = hasConnectionError,
        transitionSpec = {
            (slideInVertically(initialOffsetY = { it }) + fadeIn())
                .togetherWith(slideOutVertically(targetOffsetY = { -it }) + fadeOut())
        },
        label = "input_area_transition"
    ) { connectionError ->
        if (connectionError) {
            // Offline / Connection retry panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(PrimaryLight.copy(alpha = 0.1f))
                    .border(1.dp, PrimaryLight.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .clickable { 
                        if (isBotTokenInvalid) {
                            onNavigateToSettings()
                        } else if (!isRetrying) {
                            onRetryConnection()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        color = ErrorRed,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Connecting...",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Offline",
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isBotTokenInvalid) "Invalid Bot Token - Check Settings" else "Connection lost. Tap to retry",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ═══════════════════════════════════════
                //  ATTACH BUTTON (Stitch style)
                // ═══════════════════════════════════════
                if (!isRecording) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                if (!showAttachmentMenu) {
                                    val hasFullImages = if (android.os.Build.VERSION.SDK_INT >= 33) {
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    } else {
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    }
                                    val hasPartialImages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    } else false
                                    val isExternalStorageManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        android.os.Environment.isExternalStorageManager()
                                    } else false

                                    if (hasFullImages || hasPartialImages || isExternalStorageManager) {
                                        viewModel.loadRecentImages(context)
                                        onAttachmentMenuChange(true)
                                    } else {
                                        onRequestStoragePermission(Manifest.permission.READ_EXTERNAL_STORAGE) // The parameter is ignored in ChatScreen now
                                    }
                                } else {
                                    onAttachmentMenuChange(false)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val rotation by animateFloatAsState(targetValue = if (showAttachmentMenu) 45f else 0f, label = "attach_rotate")
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Attach Media",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                    }
                }

                // ═══════════════════════════════════════
                //  TEXT FIELD PILL (or Recording UI)
                // ═══════════════════════════════════════
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(9999.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isRecording) {
                        // ── Recording state: waveform bars + duration ──
                        RecordingIndicator(
                            durationSec = viewModel.recordingDurationSec,
                            infiniteTransition = infiniteTransition,
                            isCancelZone = isCancelZone
                        )
                    } else {
                        // ── Normal state: text input ──
                        BasicTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        onAttachmentMenuChange(false)
                                    }
                                },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (messageText.isEmpty()) {
                                        Text(
                                            "Message",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // ═══════════════════════════════════════
                //  MIC / SEND BUTTON with Glow
                // ═══════════════════════════════════════
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp)
                ) {
                    // Pulsing glow ring behind button (only during recording)
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(glowScale)
                                .glow(
                                    color = Color(0xCCEF4444),
                                    radius = 60f,
                                    dx = 0f,
                                    dy = 0f,
                                    shapeColor = MaterialTheme.colorScheme.error.copy(alpha = glowAlpha)
                                )
                        )
                    }

                    // Main button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(buttonScale)
                            .glow(
                                color = if (isRecording) Color(0xCCFF6B6B) else Color(0x99C0C1FF),
                                radius = if (isRecording) 50f else 40f,
                                dy = 8f,
                                shapeColor = if (isRecording) Color(0xFFFF6B6B) else Color(0xFFC0C1FF)
                            )
                            .clip(CircleShape)
                            .background(buttonColor)
                            .pointerInput(messageText.isNotBlank()) {
                                if (messageText.isNotBlank()) {
                                    detectTapGestures(
                                        onTap = {
                                            viewModel.sendMessage(messageText)
                                            messageText = ""
                                        }
                                    )
                                } else {
                                    awaitPointerEventScope {
                                        var startX = 0f
                                        var startTimeMs = 0L
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull()
                                            if (change != null) {
                                                if (change.pressed && !change.previousPressed) {
                                                    // ACTION_DOWN — start recording
                                                    startX = change.position.x
                                                    startTimeMs = System.currentTimeMillis()
                                                    swipeDragX = 0f
                                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    if (hasPermission) {
                                                        viewModel.startRecordingAudio(context)
                                                    } else {
                                                        onRequestAudioPermission(Manifest.permission.RECORD_AUDIO)
                                                    }
                                                } else if (change.pressed) {
                                                    // DRAG — track horizontal swipe
                                                    swipeDragX = change.position.x - startX
                                                } else if (!change.pressed && change.previousPressed) {
                                                    // ACTION_UP — send or cancel based on swipe distance and duration
                                                    val durationMs = System.currentTimeMillis() - startTimeMs
                                                    val isMisclick = durationMs < 1000 // Cancel if under 1 second
                                                    val shouldCancel = swipeDragX < -cancelThresholdPx || isMisclick
                                                    viewModel.stopRecordingAudio(context, cancel = shouldCancel)
                                                    swipeDragX = 0f
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        val icon = if (messageText.isBlank()) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send
                        Icon(
                            icon,
                            contentDescription = if (messageText.isBlank()) "Record" else "Send",
                            tint = buttonIconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Recording Indicator — animated waveform + duration
// ═══════════════════════════════════════════════════════════

@Composable
private fun RecordingIndicator(
    durationSec: Int,
    infiniteTransition: InfiniteTransition,
    isCancelZone: Boolean
) {
    val density = LocalContext.current.resources.displayMetrics.density

    // Animated waveform bar heights
    val barAnimations = (0 until 24).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + (index * 37 % 200),
                    easing = EaseInOutSine
                ),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(index * 60)
            ),
            label = "bar_$index"
        )
    }

    // Pulsing red dot
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    // Smooth color transition for cancel zone
    val indicatorColor by animateColorAsState(
        targetValue = if (isCancelZone) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.error,
        animationSpec = tween(200),
        label = "cancel_color"
    )
    val hintTextAlpha by animateFloatAsState(
        targetValue = if (isCancelZone) 1f else 0.4f,
        animationSpec = tween(200),
        label = "hint_alpha"
    )
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red pulsing dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(indicatorColor.copy(alpha = dotAlpha))
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Duration text
        Text(
            text = String.format(
                Locale.getDefault(),
                "%d:%02d",
                durationSec / 60,
                durationSec % 60
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.width(16.dp))

        if (isCancelZone) {
            // Cancel zone: show cancel message prominently
            Text(
                text = "← Release to cancel",
                color = Color(0xFFFF6B6B).copy(alpha = hintTextAlpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Normal recording: animated waveform bars
            val errorColor = MaterialTheme.colorScheme.error
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
            ) {
                val barWidth = 3f * density
                val gap = 2.5f * density
                val totalBarWidth = barWidth + gap
                val barsToShow = minOf(24, (size.width / totalBarWidth).toInt())
                val centerY = size.height / 2f
                val maxBarHeight = size.height * 0.85f

                for (i in 0 until barsToShow) {
                    val heightFactor = barAnimations[i % barAnimations.size].value
                    val barHeight = maxBarHeight * heightFactor
                    val x = i * totalBarWidth
                    val color = errorColor.copy(alpha = 0.5f + heightFactor * 0.5f)

                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(x, centerY - barHeight / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Swipe hint
            Text(
                text = "← Swipe to cancel",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
