package com.mobile.superiorutils.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobile.superiorutils.theme.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.nativeCanvas
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.AttachFile
import java.io.File
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.LogLevel
import com.mobile.superiorutils.utils.AppLog
import com.mobile.superiorutils.ui.components.ChatBubble
import com.mobile.superiorutils.ui.components.AttachmentOption
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                // To keep it simple, we copy the URI to a temp file and enqueue an upload
                viewModel.sendMedia(context, it, "photo")
            }
        }
    )

    var showAttachmentMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                viewModel.currentCameraUri?.let { uri ->
                    viewModel.sendMedia(context, uri, "photo")
                }
            }
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = viewModel.createCameraUri(context)
            cameraLauncher.launch(uri)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingAudio(context)
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadRecentImages(context)
        }
        showAttachmentMenu = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Chat Area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = true
        ) {
            if (messages.isEmpty()) {
                item {
                    Text("No messages yet. Send a message to start!", color = Color(0xFFC7C4D7), modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            } else {
                items(
                    items = messages.reversed(),
                    key = { it.messageId }
                ) { msg ->
                    ChatBubble(msg, viewModel)
                }
            }
        }

        // Input Area - Glass panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f), Color.Black)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val isRecording = viewModel.isRecordingAudio
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showAttachmentMenu,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(SurfaceLevel2.copy(alpha = 0.95f))
                            .border(1.dp, DividerColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                            .padding(top = 16.dp, bottom = 8.dp)
                    ) {
                        val images = viewModel.recentImages
                        if (images.isNotEmpty()) {
                            androidx.compose.foundation.lazy.LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(images) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Recent Photo",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { 
                                                showAttachmentMenu = false
                                                viewModel.sendMedia(context, uri, "photo") 
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.HorizontalDivider(color = DividerColor.copy(alpha = 0.3f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            AttachmentOption(icon = Icons.Default.Image, title = "Gallery", color = PrimaryLight) {
                                showAttachmentMenu = false
                                photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            AttachmentOption(icon = Icons.Default.CameraAlt, title = "Camera", color = Color(0xFF00C853)) {
                                showAttachmentMenu = false
                                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    cameraLauncher.launch(viewModel.createCameraUri(context))
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9999.dp))
                        .background(SurfaceLevel2.copy(alpha = 0.6f))
                        .border(1.dp, DividerColor, RoundedCornerShape(9999.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                IconButton(onClick = { 
                    if (!showAttachmentMenu) {
                        val perm = if (android.os.Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                        if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                            viewModel.loadRecentImages(context)
                            showAttachmentMenu = true
                        } else {
                            storagePermissionLauncher.launch(perm)
                        }
                    } else {
                        showAttachmentMenu = false
                    }
                }) {
                    val rotation by animateFloatAsState(targetValue = if (showAttachmentMenu) 45f else 0f)
                    Icon(Icons.Default.Add, contentDescription = "Attach Media", tint = Color(0xFFC7C4D7), modifier = Modifier.rotate(rotation))
                }
                
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                viewModel.recordingDurationSec / 60,
                                viewModel.recordingDurationSec % 60
                            ),
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Slide left / release to cancel",
                            color = Color(0xFFC7C4D7).copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFE2E2E2)),
                        decorationBox = { innerTextField ->
                            if (messageText.isEmpty()) {
                                Text("Message...", color = Color(0xFFC7C4D7).copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    )
                }
                
                val micScale by animateFloatAsState(targetValue = if (isRecording) 1.5f else 1f, label = "mic_scale")
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isRecording) Color.Red else PrimaryLight)
                        .scale(micScale)
                        .pointerInput(messageText.isNotBlank()) {
                            detectTapGestures(
                                onTap = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendMessage(messageText)
                                        messageText = ""
                                    }
                                },
                                onPress = {
                                    if (messageText.isBlank()) {
                                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            viewModel.startRecordingAudio(context)
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            return@detectTapGestures
                                        }
                                        
                                        val success = tryAwaitRelease()
                                        viewModel.stopRecordingAudio(context, cancel = !success)
                                    }
                                }
                            )
                        }
                ) {
                    val icon = if (messageText.isBlank()) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send
                    Icon(icon, contentDescription = "Send or Mic", tint = if (isRecording) Color.White else Color(0xFF1000A9), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
}


