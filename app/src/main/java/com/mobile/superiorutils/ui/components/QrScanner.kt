package com.mobile.superiorutils.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.app.Activity
import androidx.core.app.ActivityCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.mobile.superiorutils.theme.Primary
import com.mobile.superiorutils.utils.QrManager
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanner(
    onDismiss: () -> Unit,
    onSuccess: (botToken: String, chatId: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) 
    }
    
    var showCameraSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                showCameraSettingsDialog = true
            } else {
                Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
                onDismiss()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    var showCustomGallery by remember { mutableStateOf(false) }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            QrManager.processUri(uri, context, onSuccess)
        }
    }
    
    val legacyPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            QrManager.processUri(uri, context, onSuccess)
        }
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || 
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }

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
                Toast.makeText(context, "Storage permission is required to select QR codes", Toast.LENGTH_LONG).show()
                showCustomGallery = false
            }
        }
    }

    if (showCameraSettingsDialog) {
        ActionDialog(
            title = "Permission Required",
            message = "You have permanently denied Camera access. To scan QR codes, please enable it in Settings.",
            confirmText = "Go to Settings",
            onConfirm = {
                showCameraSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                onDismiss()
            },
            onDismiss = {
                showCameraSettingsDialog = false
                onDismiss()
            }
        )
    }

    if (showStorageSettingsDialog) {
        ActionDialog(
            title = "Permission Required",
            message = "You have permanently denied Storage access. To select QR codes from gallery, please enable it in Settings.",
            confirmText = "Go to Settings",
            onConfirm = {
                showStorageSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
                showCustomGallery = false
            },
            onDismiss = {
                showStorageSettingsDialog = false
                showCustomGallery = false
            }
        )
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
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            
                        val executor = Executors.newSingleThreadExecutor()
                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                            QrManager.processImageProxy(imageProxy, onSuccess)
                        }
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Scanner Overlay
            ScannerOverlay(modifier = Modifier.fillMaxSize())
            
            // Top Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            if (hasStoragePermission) {
                                legacyPhotoPickerLauncher.launch("image/*")
                            } else {
                                storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }
            }
            
            // Bottom Instructions
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Text(
                    "Align QR Code within the frame to scan",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        } // Added closing brace for Dialog
    }
}



@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()
        val cornerLength = 32.dp.toPx()
        
        val boxSize = size.minDimension * 0.7f
        val left = (size.width - boxSize) / 2
        val top = (size.height - boxSize) / 2
        val right = left + boxSize
        val bottom = top + boxSize
        
        val color = Primary
        
        // Darken outside
        val path = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left, top, right, bottom,
                    androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            )
            fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
        }
        drawPath(path, Color.Black.copy(alpha = 0.5f))
        
        // Draw corners
        val sw = strokeWidth
        
        // Top Left
        drawLine(color, Offset(left, top + cornerLength), Offset(left, top), strokeWidth = sw)
        drawLine(color, Offset(left, top), Offset(left + cornerLength, top), strokeWidth = sw)
        
        // Top Right
        drawLine(color, Offset(right - cornerLength, top), Offset(right, top), strokeWidth = sw)
        drawLine(color, Offset(right, top), Offset(right, top + cornerLength), strokeWidth = sw)
        
        // Bottom Left
        drawLine(color, Offset(left, bottom - cornerLength), Offset(left, bottom), strokeWidth = sw)
        drawLine(color, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth = sw)
        
        // Bottom Right
        drawLine(color, Offset(right - cornerLength, bottom), Offset(right, bottom), strokeWidth = sw)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth = sw)
    }
}


