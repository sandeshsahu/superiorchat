package com.mobile.superiorchat.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.mobile.superiorchat.theme.Primary
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.Success
import com.mobile.superiorchat.ui.components.media.GalleryGrid
import com.mobile.superiorchat.utils.QrManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ─── State Machine ────────────────────────────────────────────────────────────

sealed class ScannerState {
    object Scanning : ScannerState()
    object Detected : ScannerState()
    object Processing : ScannerState()
    data class Error(val message: String) : ScannerState()
    data class PinRequired(val rawPayload: String) : ScannerState()
    object Success : ScannerState()
}

// ─── Main Composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanner(
    onDismiss: () -> Unit,
    onSuccess: (com.mobile.superiorchat.utils.QrConfigData) -> Unit,
    onShowGlobalDialog: (com.mobile.superiorchat.ui.GlobalDialogState) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Scanning) }
    var showCustomGallery by remember { mutableStateOf(false) }
    var isGalleryLoading by remember { mutableStateOf(false) }

    // Camera controls
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }

    // Executor — created once, shut down on disposal
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        QrManager.resetState()
        onDispose {
            executor.shutdownNow()
        }
    }

    // No LaunchedEffect needed for Success — onSuccess is called directly
    // from the camera analyzer and gallery callbacks which are already on Main thread.

    val permissionHandler = com.mobile.superiorchat.utils.rememberPermissionHandler(onShowGlobalDialog)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isGalleryLoading = true
            QrManager.processUri(
                uri = uri,
                context = context,
                onSuccess = { data ->
                    isGalleryLoading = false
                    scannerState = ScannerState.Success
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSuccess(data)
                },
                onError = { err ->
                    isGalleryLoading = false
                    if (err is QrManager.PinRequiredException) {
                        scannerState = ScannerState.PinRequired(err.rawPayload)
                    } else {
                        scannerState = ScannerState.Error(err.message ?: "Invalid QR code.")
                    }
                }
            )
        }
    }

    val launchGallery = {
        permissionHandler.requestStorageForMedia {
            showCustomGallery = true
        }
    }

    val openGallery = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            launchGallery()
        }
    }

    if (hasCameraPermission) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    // Pinch to zoom
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val newZoom = (zoomRatio * zoom).coerceIn(1f, 6f)
                            zoomRatio = newZoom
                            cameraControl?.setZoomRatio(newZoom)
                        }
                    }
            ) {
                // ── Camera Preview ─────────────────────────────────────────
                val cameraProviderRef = remember { ProcessCameraProvider.getInstance(context) }

                DisposableEffect(lifecycleOwner) {
                    onDispose {
                        try { cameraProviderRef.get().unbindAll() } catch (_: Exception) {}
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        cameraProviderRef.addListener({
                            try {
                                val cameraProvider = cameraProviderRef.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setTargetRotation(Surface.ROTATION_0)
                                    .build()

                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    if (scannerState !is ScannerState.Scanning) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    QrManager.processImageProxy(
                                        imageProxy = imageProxy,
                                        onSuccess = { data ->
                                            scannerState = ScannerState.Success
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSuccess(data)
                                        },
                                        onError = { err ->
                                            if (err is QrManager.PinRequiredException) {
                                                scannerState = ScannerState.PinRequired(err.rawPayload)
                                            } else {
                                                scannerState = ScannerState.Error(err.message ?: "Invalid QR code.")
                                            }
                                        }
                                    )
                                }

                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = camera.cameraControl
                                cameraInfo = camera.cameraInfo
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // ── Scanning Overlay ───────────────────────────────────────
                ScannerOverlay(
                    modifier = Modifier.fillMaxSize(),
                    state = scannerState
                )

                // ── Top Bar ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close
                    GlassButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }

                    // Center label
                    Text(
                        "Scan QR Code",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Right side: Torch + Gallery
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Torch (only if supported)
                        val hasTorch = cameraInfo?.hasFlashUnit() == true
                        if (hasTorch) {
                            GlassButton(onClick = {
                                isTorchOn = !isTorchOn
                                cameraControl?.enableTorch(isTorchOn)
                            }) {
                                Icon(
                                    if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                    contentDescription = "Torch",
                                    tint = if (isTorchOn) Color.Yellow else Color.White
                                )
                            }
                        }

                        // Gallery
                        GlassButton(onClick = openGallery) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                        }
                    }
                }

                // ── Bottom State Area ──────────────────────────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 40.dp)
                ) {
                    AnimatedContent(
                        targetState = scannerState,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                        },
                        label = "ScannerStateContent"
                    ) { state ->
                        when (state) {
                            is ScannerState.Scanning -> {
                                HintPill("Align the QR code within the frame")
                            }
                            is ScannerState.PinRequired -> {
                                HintPill("Action Required", color = com.mobile.superiorchat.theme.WarningAmber.copy(alpha = 0.85f))
                            }
                            is ScannerState.Detected -> {
                                HintPill("QR code detected…", color = Success.copy(alpha = 0.85f))
                            }
                            is ScannerState.Processing -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 20.dp, vertical = 14.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryLight
                                    )
                                    Text("Verifying credentials…", color = Color.White, fontSize = 14.sp)
                                }
                            }
                            is ScannerState.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFB00020).copy(alpha = 0.85f))
                                            .padding(horizontal = 20.dp, vertical = 14.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            state.message,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            QrManager.resetState()
                                            scannerState = ScannerState.Scanning
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = PrimaryLight
                                        )
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Tap to retry", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            is ScannerState.Success -> {
                                HintPill("✓  Configured successfully!", color = Success.copy(alpha = 0.9f))
                            }
                        }
                    }
                }

                // ── Gallery Loading Overlay ────────────────────────────────
                if (isGalleryLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = PrimaryLight, strokeWidth = 3.dp)
                            Text("Scanning image…", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                if (scannerState is ScannerState.PinRequired) {
                    val payload = (scannerState as ScannerState.PinRequired).rawPayload
                    var pinError by remember { mutableStateOf<String?>(null) }
                    
                    com.mobile.superiorchat.ui.components.popups.PinEntryDialog(
                        errorMessage = pinError,
                        onDismiss = { 
                            QrManager.resetState()
                            scannerState = ScannerState.Scanning 
                        },
                        onSubmit = { pin ->
                            val decrypted = com.mobile.superiorchat.utils.Security.decryptAESWithPin(payload, pin)
                            val result = QrManager.parseDecryptedJson(decrypted)
                            result.onSuccess { data ->
                                scannerState = ScannerState.Success
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSuccess(data)
                            }.onFailure { err ->
                                pinError = "Incorrect PIN or Invalid Config."
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Custom Gallery Dialog ─────────────────────────────────────────────
    if (showCustomGallery) {
        Dialog(
            onDismissRequest = { showCustomGallery = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            GalleryGrid(
                maxSelection = 1,
                showVideos = false,
                onDismiss = { showCustomGallery = false },
                onMediaSelected = { items, _ ->
                    if (items.isNotEmpty()) {
                        isGalleryLoading = true
                        showCustomGallery = false
                        QrManager.processUri(
                            uri = items.first().uri,
                            context = context,
                            onSuccess = { data ->
                                isGalleryLoading = false
                                scannerState = ScannerState.Success
                                onSuccess(data)
                            },
                            onError = { err ->
                                isGalleryLoading = false
                                if (err is QrManager.PinRequiredException) {
                                    scannerState = ScannerState.PinRequired(err.rawPayload)
                                } else {
                                    scannerState = ScannerState.Error(err.message ?: "Invalid QR code.")
                                }
                            }
                        )
                        true
                    } else false
                },
                onCameraClick = { showCustomGallery = false }
            )
        }
    }
}

// ─── Helper Composables ───────────────────────────────────────────────────────

@Composable
private fun GlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
    ) {
        content()
    }
}

@Composable
private fun HintPill(text: String, color: Color = Color.Black.copy(alpha = 0.70f)) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(color)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

// ─── Scanner Overlay ──────────────────────────────────────────────────────────

@Composable
fun ScannerOverlay(modifier: Modifier = Modifier, state: ScannerState = ScannerState.Scanning) {

    // Corner color based on state
    val cornerColor by animateColorAsState(
        targetValue = when (state) {
            is ScannerState.Success, is ScannerState.Detected -> Success
            is ScannerState.Error -> Color(0xFFCF6679)
            else -> Primary
        },
        animationSpec = tween(300),
        label = "CornerColor"
    )

    // Animated scan line
    val scanLineAnim = rememberInfiniteTransition(label = "ScanLine")
    val scanLineProgress by scanLineAnim.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScanLineProgress"
    )

    // Frame flash on detection
    val flashAlpha by animateFloatAsState(
        targetValue = when (state) {
            is ScannerState.Success -> 0.3f
            is ScannerState.Detected -> 0.15f
            else -> 0f
        },
        animationSpec = tween(250),
        label = "FlashAlpha"
    )

    val flashColor = when (state) {
        is ScannerState.Success -> Success
        else -> Success
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 36.dp.toPx()
        val boxSize = size.minDimension * 0.68f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2.2f  // slightly above center, more natural
        val right = left + boxSize
        val bottom = top + boxSize
        val cornerRadius = 16.dp.toPx()

        // ── Dark scrim outside the box ─────────────────────────────────────
        val scrimPath = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left, top, right, bottom,
                    androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            )
            fillType = PathFillType.EvenOdd
        }
        drawPath(scrimPath, Color.Black.copy(alpha = 0.62f))

        // ── Success/detected flash inside box ──────────────────────────────
        if (flashAlpha > 0f) {
            drawRoundRect(
                color = flashColor.copy(alpha = flashAlpha),
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )
        }

        // ── Corner brackets ────────────────────────────────────────────────
        val sw = strokeWidth
        val c = cornerColor

        // Top-left
        drawLine(c, Offset(left, top + cornerLength), Offset(left, top + cornerRadius), strokeWidth = sw)
        drawLine(c, Offset(left + cornerRadius, top), Offset(left + cornerLength, top), strokeWidth = sw)

        // Top-right
        drawLine(c, Offset(right - cornerLength, top), Offset(right - cornerRadius, top), strokeWidth = sw)
        drawLine(c, Offset(right, top + cornerRadius), Offset(right, top + cornerLength), strokeWidth = sw)

        // Bottom-left
        drawLine(c, Offset(left, bottom - cornerLength), Offset(left, bottom - cornerRadius), strokeWidth = sw)
        drawLine(c, Offset(left + cornerRadius, bottom), Offset(left + cornerLength, bottom), strokeWidth = sw)

        // Bottom-right
        drawLine(c, Offset(right - cornerLength, bottom), Offset(right - cornerRadius, bottom), strokeWidth = sw)
        drawLine(c, Offset(right, bottom - cornerLength), Offset(right, bottom - cornerRadius), strokeWidth = sw)

        // ── Animated scan line (only when scanning) ────────────────────────
        if (state is ScannerState.Scanning) {
            val lineY = top + (boxSize * scanLineProgress)
            val gradientAlpha = 0.85f

            // Glow line — draw twice for bloom effect
            drawLine(
                color = Primary.copy(alpha = gradientAlpha * 0.3f),
                start = Offset(left + 2.dp.toPx(), lineY),
                end = Offset(right - 2.dp.toPx(), lineY),
                strokeWidth = 8.dp.toPx()
            )
            drawLine(
                color = Primary.copy(alpha = gradientAlpha),
                start = Offset(left + 2.dp.toPx(), lineY),
                end = Offset(right - 2.dp.toPx(), lineY),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
