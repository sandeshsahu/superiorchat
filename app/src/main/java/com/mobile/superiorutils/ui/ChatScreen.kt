package com.mobile.superiorutils.ui

import com.mobile.superiorutils.ui.components.ChatInputBox
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.mobile.superiorutils.ui.components.ErrorDialog
import com.mobile.superiorutils.ui.components.bounceClick
import com.mobile.superiorutils.ui.components.glow
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageLimit by viewModel.messageLimit.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var activeFullScreenMediaPath by remember { mutableStateOf<String?>(null) }
    var activeFullScreenMediaType by remember { mutableStateOf<String?>(null) }
    var currentPickerMode by remember { mutableStateOf(PickerMode.NONE) }

    val isOnline by viewModel.isOnline.collectAsState()
    val isTelegramApiReachable by viewModel.isTelegramApiReachable.collectAsState()
    val isRetrying = viewModel.isRetryingConnection
    val hasConnectionError = !isOnline || !isTelegramApiReachable

    // Auto-scroll: only jump to bottom when a single new message arrives,
    // NOT when pagination loads a batch of older messages.
    var previousMessageCount by remember { mutableIntStateOf(messages.size) }
    LaunchedEffect(messages.size) {
        val delta = messages.size - previousMessageCount
        if (messages.isNotEmpty() && delta == 1) {
            listState.requestScrollToItem(0)
        }
        previousMessageCount = messages.size
    }

    // Pagination: load older messages when user scrolls near the top
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && messages.isNotEmpty() && lastIndex >= messages.size - 5) {
                    viewModel.loadMoreMessages()
                }
            }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                viewModel.sendMedia(context, it, "photo")
            }
        }
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                viewModel.sendMedia(context, uri, "document")
            }
        }
    )

    var showAttachmentMenu by remember { mutableStateOf(false) }
    
    @OptIn(ExperimentalLayoutApi::class)
    val isKeyboardVisible = WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && showAttachmentMenu) {
            showAttachmentMenu = false
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                viewModel.currentCameraUri?.let { uri ->
                    viewModel.sendMedia(context, uri, "photo")
                    currentPickerMode = PickerMode.NONE
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

    val onMediaClickRemembered = remember {
        { path: String, type: String ->
            activeFullScreenMediaPath = path
            activeFullScreenMediaType = type
        }
    }

    val onRecentImageClickRemembered = remember(context) {
        { uri: Uri ->
            showAttachmentMenu = false
            viewModel.sendMedia(context, uri, "photo")
            Unit
        }
    }

    val onGalleryClickRemembered = remember {
        {
            showAttachmentMenu = false
            currentPickerMode = PickerMode.GALLERY
        }
    }

    val onCameraClickRemembered = remember(context) {
        {
            showAttachmentMenu = false
            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                cameraLauncher.launch(viewModel.createCameraUri(context))
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val onFileClickRemembered = remember {
        {
            showAttachmentMenu = false
            currentPickerMode = PickerMode.FILES
        }
    }

    val onCloseClickRemembered = remember {
        {
            showAttachmentMenu = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            // Chat Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .glow(color = Primary, radius = 40f, dx = 0f, dy = 0f)
                                        .background(Primary.copy(alpha = 0.1f), CircleShape)
                                        .border(1.dp, Primary.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Secure Chat",
                                        tint = PrimaryLight,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "No messages yet",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Send a message to start the conversation.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(
                            items = messages.reversed(),
                            key = { it.messageId }
                        ) { msg ->
                            MessageBubble(
                                message = msg,
                                viewModel = viewModel,
                                onMediaClick = onMediaClickRemembered
                            )
                        }
                        
                        if (messages.size >= messageLimit) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showAttachmentMenu,
                    enter = fadeIn(animationSpec = tween(150)),
                    exit = fadeOut(animationSpec = tween(150)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                showAttachmentMenu = false
                            }
                    )
                }
            }

            // Input Area - Glass panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f), Color.Black)
                        )
                    )
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* Consume clicks to prevent backdrop dismissal */ }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                val isRecording = viewModel.isRecordingAudio
                Column(modifier = Modifier.fillMaxWidth()) {
                    AttachMenu(
                        visible = showAttachmentMenu,
                        recentImages = viewModel.recentImages,
                        onRecentImageClick = onRecentImageClickRemembered,
                        onGalleryClick = onGalleryClickRemembered,
                        onCameraClick = onCameraClickRemembered,
                        onFileClick = onFileClickRemembered,
                        onCloseClick = onCloseClickRemembered
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
                                    .bounceClick(scaleDown = 0.95f) { if (!isRetrying) viewModel.retryConnection(context) }
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
                                        text = "Connection lost. Tap to retry",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            ChatInputBox(
                                viewModel = viewModel,
                                showAttachmentMenu = showAttachmentMenu,
                                onAttachmentMenuChange = { showAttachmentMenu = it },
                                storagePermissionLauncher = storagePermissionLauncher,
                                audioPermissionLauncher = audioPermissionLauncher
                            )
                        }
                    }
                }
            }
        }

        MediaViewer(
            mediaPath = activeFullScreenMediaPath,
            mediaType = activeFullScreenMediaType,
            onDismiss = {
                activeFullScreenMediaPath = null
                activeFullScreenMediaType = null
            }
        )

        viewModel.errorPopupMessage?.let { errorMessage ->
            ErrorDialog(
                message = errorMessage,
                onDismiss = { viewModel.errorPopupMessage = null }
            )
        }
    }

    MediaPicker(
        visible = currentPickerMode != PickerMode.NONE,
        initialTab = if (currentPickerMode == PickerMode.FILES) PickerTab.FILES else PickerTab.GALLERY,
        onDismiss = { currentPickerMode = PickerMode.NONE },
        viewModel = viewModel,
        onMediaSelected = { uris ->
            var allSuccess = true
            uris.forEach { uri ->
                val success = viewModel.sendMedia(context, uri, "photo")
                if (!success) allSuccess = false
            }
            if (allSuccess) currentPickerMode = PickerMode.NONE
            allSuccess
        },
        onFilesSelected = { files ->
            var allSuccess = true
            files.forEach { file ->
                val success = viewModel.sendMedia(context, Uri.fromFile(file), "document")
                if (!success) allSuccess = false
            }
            if (allSuccess) currentPickerMode = PickerMode.NONE
            allSuccess
        },
        onCameraClick = {
            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                cameraLauncher.launch(viewModel.createCameraUri(context))
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        onSystemPickerClick = {
            filePickerLauncher.launch("*/*")
        }
    )
}

enum class PickerMode { NONE, GALLERY, FILES }

