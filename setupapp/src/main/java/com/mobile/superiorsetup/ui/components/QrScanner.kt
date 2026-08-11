package com.mobile.superiorsetup.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.app.Activity
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mobile.superiorsetup.core.QrConfigData
import com.mobile.superiorsetup.core.QrManager
import com.mobile.superiorsetup.theme.Primary
import com.mobile.superiorsetup.theme.PrimaryLight
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

private val ScanSuccess = Color(0xFF4CAF50)

// ─── Main Composable ──────────────────────────────────────────────────────────

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanner(
    onDismiss: () -> Unit,
    onSuccess: (QrConfigData) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showCameraRationaleDialog by remember { mutableStateOf(!hasCameraPermission) }
    var showCameraSettingsDialog by remember { mutableStateOf(false) }

    var scannerState by remember { mutableStateOf<ScannerState>(ScannerState.Scanning) }
    var showCustomGallery by remember { mutableStateOf(false) }
    var isGalleryLoading by remember { mutableStateOf(false) }

    // Camera controls for torch and zoom
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }

    // Executor — created once, shut down on disposal
    val executor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        QrManager.resetState()
        onDispose { executor.shutdownNow() }
    }

    // Permission launchers
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                showCameraSettingsDialog = true
            } else {
                onDismiss()
            }
        }
    }

    // Photo picker (Android 13+)
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

    // Storage permission (pre-Android 13 gallery fallback)
    var hasStoragePermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showStorageRationaleDialog by remember { mutableStateOf(false) }
    var showStorageSettingsDialog by remember { mutableStateOf(false) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showStorageSettingsDialog = true
            } else {
                showCustomGallery = false
            }
        }
    }

    val openGallery = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            showCustomGallery = true
        }
    }

    // ── Permission Dialogs ────────────────────────────────────────────────
    if (showCameraRationaleDialog) {
        ActionDialog(
            title = "Camera Permission",
            message = "Superior Setup needs access to your camera to scan the configuration QR code.",
            confirmText = "Continue",
            dismissText = "Cancel",
            onConfirm = {
                showCameraRationaleDialog = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showCameraRationaleDialog = false; onDismiss() }
        )
    }

    if (showCameraSettingsDialog) {
        ActionDialog(
            title = "Permission Required",
            message = "You have permanently denied Camera access. To scan QR codes, please enable it in Settings.",
            confirmText = "Go to Settings",
            onConfirm = {
                showCameraSettingsDialog = false
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                })
                onDismiss()
            },
            onDismiss = { showCameraSettingsDialog = false; onDismiss() }
        )
    }

    if (showStorageRationaleDialog) {
        ActionDialog(
            title = "Storage Permission",
            message = "Superior Setup needs access to your storage to read QR code images from your gallery.",
            confirmText = "Continue",
            dismissText = "Cancel",
            onConfirm = {
                showStorageRationaleDialog = false
                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            },
            onDismiss = { showStorageRationaleDialog = false; showCustomGallery = false }
        )
    }

    if (showStorageSettingsDialog) {
        ActionDialog(
            title = "Permission Required",
            message = "You have permanently denied Storage access. To select QR codes from gallery, please enable it in Settings.",
            confirmText = "Go to Settings",
            onConfirm = {
                showStorageSettingsDialog = false
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                })
                showCustomGallery = false
            },
            onDismiss = { showStorageSettingsDialog = false; showCustomGallery = false }
        )
    }

    // ── Custom Gallery (pre-Android 13) ────────────────────────────────────
    if (showCustomGallery) {
        Dialog(
            onDismissRequest = { showCustomGallery = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            if (hasStoragePermission) {
                GalleryGrid(
                    onDismiss = { showCustomGallery = false },
                    onImageSelected = { uri ->
                        showCustomGallery = false
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
                                if (err is com.mobile.superiorsetup.core.QrManager.PinRequiredException) {
                                    scannerState = ScannerState.PinRequired(err.rawPayload)
                                } else {
                                    scannerState = ScannerState.Error(err.message ?: "Invalid QR code.")
                                }
                            }
                        )
                    }
                )
            } else {
                LaunchedEffect(Unit) { showStorageRationaleDialog = true }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryLight)
                }
            }
        }
        return
    }

    // ── Main Camera Scanner Dialog ─────────────────────────────────────────
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
                SetupScannerOverlay(modifier = Modifier.fillMaxSize(), state = scannerState)

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
                    SetupGlassButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Text("Scan Configuration QR", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val hasTorch = cameraInfo?.hasFlashUnit() == true
                        if (hasTorch) {
                            SetupGlassButton(onClick = {
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
                        SetupGlassButton(onClick = openGallery) {
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
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                        label = "ScannerStateContent"
                    ) { state ->
                        when (state) {
                            is ScannerState.Scanning ->
                                SetupHintPill("Align the QR code within the frame")
                            is ScannerState.PinRequired ->
                                SetupHintPill("Action Required", color = com.mobile.superiorsetup.theme.WarningAmber.copy(alpha = 0.85f))
                            is ScannerState.Detected ->
                                SetupHintPill("QR code detected…", color = ScanSuccess.copy(alpha = 0.85f))
                            is ScannerState.Processing ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .padding(horizontal = 20.dp, vertical = 14.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryLight)
                                    Text("Verifying credentials…", color = Color.White, fontSize = 14.sp)
                                }
                            is ScannerState.Error ->
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
                                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text(state.message, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                                    }
                                    TextButton(
                                        onClick = { QrManager.resetState(); scannerState = ScannerState.Scanning },
                                        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryLight)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Tap to retry", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            is ScannerState.Success ->
                                SetupHintPill("✓  Configured successfully!", color = ScanSuccess.copy(alpha = 0.9f))
                        }
                    }
                }

                // ── Gallery Loading Overlay ────────────────────────────────
                if (isGalleryLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(color = PrimaryLight, strokeWidth = 3.dp)
                            Text("Scanning image…", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                if (scannerState is ScannerState.PinRequired) {
                    val payload = (scannerState as ScannerState.PinRequired).rawPayload
                    var pinError by remember { mutableStateOf<String?>(null) }
                    
                    com.mobile.superiorsetup.ui.components.PinEntryDialog(
                        errorMessage = pinError,
                        onDismiss = { 
                            QrManager.resetState()
                            scannerState = ScannerState.Scanning 
                        },
                        onSubmit = { pin ->
                            val decrypted = com.mobile.superiorsetup.core.Security.decryptAESWithPin(payload, pin)
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
}

// ─── Helper Composables ───────────────────────────────────────────────────────

@Composable
private fun SetupGlassButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
    ) { content() }
}

@Composable
private fun SetupHintPill(text: String, color: Color = Color.Black.copy(alpha = 0.70f)) {
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
fun SetupScannerOverlay(modifier: Modifier = Modifier, state: ScannerState = ScannerState.Scanning) {

    val cornerColor by animateColorAsState(
        targetValue = when (state) {
            is ScannerState.Success, is ScannerState.Detected -> ScanSuccess
            is ScannerState.Error -> Color(0xFFCF6679)
            else -> Primary
        },
        animationSpec = tween(300),
        label = "CornerColor"
    )

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

    val flashAlpha by animateFloatAsState(
        targetValue = when (state) {
            is ScannerState.Success -> 0.3f
            is ScannerState.Detected -> 0.15f
            else -> 0f
        },
        animationSpec = tween(250),
        label = "FlashAlpha"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 36.dp.toPx()
        val boxSize = size.minDimension * 0.68f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2.2f
        val right = left + boxSize
        val bottom = top + boxSize
        val cornerRadius = 16.dp.toPx()

        // Dark scrim outside box
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

        // Success/detected flash
        if (flashAlpha > 0f) {
            drawRoundRect(
                color = ScanSuccess.copy(alpha = flashAlpha),
                topLeft = Offset(left, top),
                size = Size(boxSize, boxSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )
        }

        // Corner brackets
        val sw = strokeWidth
        val c = cornerColor
        drawLine(c, Offset(left, top + cornerLength), Offset(left, top + cornerRadius), strokeWidth = sw)
        drawLine(c, Offset(left + cornerRadius, top), Offset(left + cornerLength, top), strokeWidth = sw)
        drawLine(c, Offset(right - cornerLength, top), Offset(right - cornerRadius, top), strokeWidth = sw)
        drawLine(c, Offset(right, top + cornerRadius), Offset(right, top + cornerLength), strokeWidth = sw)
        drawLine(c, Offset(left, bottom - cornerLength), Offset(left, bottom - cornerRadius), strokeWidth = sw)
        drawLine(c, Offset(left + cornerRadius, bottom), Offset(left + cornerLength, bottom), strokeWidth = sw)
        drawLine(c, Offset(right - cornerLength, bottom), Offset(right - cornerRadius, bottom), strokeWidth = sw)
        drawLine(c, Offset(right, bottom - cornerLength), Offset(right, bottom - cornerRadius), strokeWidth = sw)

        // Animated scan line (Scanning state only)
        if (state is ScannerState.Scanning) {
            val lineY = top + (boxSize * scanLineProgress)
            drawLine(color = Primary.copy(alpha = 0.25f), start = Offset(left + 2.dp.toPx(), lineY), end = Offset(right - 2.dp.toPx(), lineY), strokeWidth = 8.dp.toPx())
            drawLine(color = Primary.copy(alpha = 0.85f), start = Offset(left + 2.dp.toPx(), lineY), end = Offset(right - 2.dp.toPx(), lineY), strokeWidth = 2.dp.toPx())
        }
    }
}
