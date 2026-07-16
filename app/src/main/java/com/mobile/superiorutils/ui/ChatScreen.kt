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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.Reply
import com.mobile.superiorutils.ui.components.ScrollEvent
import com.mobile.superiorutils.ui.components.MessageContextMenu
import com.mobile.superiorutils.ui.components.DeleteWarningDialog
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.repository.LocalMediaItem
import com.mobile.superiorutils.data.repository.LocalFileItem
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.mobile.superiorutils.ui.components.ActionDialog
import com.mobile.superiorutils.ui.components.TargetProfileDialog
import com.mobile.superiorutils.ui.components.bounceClick
import com.mobile.superiorutils.ui.components.glow
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onShowGlobalDialog: (com.mobile.superiorutils.ui.GlobalDialogState) -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val messageLimit by viewModel.messageLimit.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var activeFullScreenMediaPath by remember { mutableStateOf<String?>(null) }
    var activeFullScreenMediaType by remember { mutableStateOf<String?>(null) }
    var currentPickerMode by remember { mutableStateOf(PickerMode.NONE) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showUserInfoDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedMessageForAction by remember { androidx.compose.runtime.mutableStateOf<MessageNode?>(null) }
    var messageToDelete by remember { androidx.compose.runtime.mutableStateOf<MessageNode?>(null) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val isKeyboardVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && showAttachmentMenu) {
            showAttachmentMenu = false
        }
    }

    val isOnline by viewModel.isOnline.collectAsState()
    val isTelegramApiReachable by viewModel.isTelegramApiReachable.collectAsState()
    val isBotTokenInvalid by viewModel.isBotTokenInvalid.collectAsState()
    val isRetrying = viewModel.isRetryingConnection
    val hasConnectionError = !isOnline || !isTelegramApiReachable || isBotTokenInvalid

    val activeConversationId = remember(messages) { messages.firstOrNull()?.conversationId }
    val coroutineScope = rememberCoroutineScope()
    var shouldScrollToBottomOnStart by remember(activeConversationId) { mutableStateOf(true) }
    var shouldScrollToBottom by remember { mutableStateOf(false) }
    val isScrolledUp by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 4 }
    }

    // Initial scroll when conversation changes or first loads
    if (shouldScrollToBottomOnStart && messages.isNotEmpty()) {
        SideEffect {
            coroutineScope.launch {
                listState.scrollToItem(0)
                shouldScrollToBottomOnStart = false
            }
        }
    }

    // Scroll to bottom after message list recomposes
    if (shouldScrollToBottom) {
        SideEffect {
            coroutineScope.launch {
                listState.scrollToItem(0)
                shouldScrollToBottom = false
            }
        }
    }

    // Event-driven Auto-scroll (subsequent events)
    LaunchedEffect(Unit) {
        viewModel.scrollEvents.collect { event ->
            when (event) {
                is ScrollEvent.NewMessageInserted -> {
                    val wasAtBottom = listState.firstVisibleItemIndex <= 2
                    if (event.isFromMe || wasAtBottom) {
                        shouldScrollToBottom = true
                        viewModel.hasUnreadMessages = false
                    } else {
                        viewModel.hasUnreadMessages = true
                    }
                }
                is ScrollEvent.JumpToBottomRequested -> {
                    listState.scrollToItem(0)
                    viewModel.hasUnreadMessages = false
                }
                is ScrollEvent.OlderMessagesLoaded -> {
                    // Do nothing, Compose naturally maintains scroll position for prepended items
                }
            }
        }
    }

    // Monitor manual scrolls to reset unread indicator
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (index <= 1 && viewModel.hasUnreadMessages) {
                    viewModel.hasUnreadMessages = false
                }
            }
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
        } else {
            val activity = context as? android.app.Activity
            if (activity != null && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                onShowGlobalDialog(
                    com.mobile.superiorutils.ui.GlobalDialogState.PermissionPermanentlyDenied(
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    )
                )
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecordingAudio(context)
        } else {
            val activity = context as? android.app.Activity
            if (activity != null && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)) {
                onShowGlobalDialog(
                    com.mobile.superiorutils.ui.GlobalDialogState.PermissionPermanentlyDenied(
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    )
                )
            }
        }
    }

    val storageLauncherRef = remember { androidx.compose.runtime.mutableStateOf<androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val imagesGranted = results[Manifest.permission.READ_MEDIA_IMAGES] == true
        val videoGranted = results[Manifest.permission.READ_MEDIA_VIDEO] == true
        val storageGranted = results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        val partialGranted = results[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true

        if (imagesGranted || storageGranted) {
            viewModel.loadRecentImages(context)
            showAttachmentMenu = true
        } else if (partialGranted) {
            viewModel.loadRecentImages(context)
            val activity = context as? android.app.Activity
            val fullPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (activity != null && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, fullPerm)) {
                onShowGlobalDialog(
                    com.mobile.superiorutils.ui.GlobalDialogState.PartialMediaAccessPermanentlyDenied(
                        onContinue = { /* Do nothing, just continue */ },
                        onGoToSettings = {
                            context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        }
                    )
                )
            } else {
                onShowGlobalDialog(com.mobile.superiorutils.ui.GlobalDialogState.PartialMediaAccess(
                    onContinue = { /* Do nothing, just continue */ },
                    onUpgrade = {
                        val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        } else if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        storageLauncherRef.value?.launch(perms)
                    }
                ))
            }
        } else {
            val activity = context as? android.app.Activity
            val permsToCheck = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            } else if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (activity != null && !permsToCheck.any { perm -> androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, perm) }) {
                onShowGlobalDialog(
                    com.mobile.superiorutils.ui.GlobalDialogState.PermissionPermanentlyDenied(
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    )
                )
            }
        }
    }
    
    storageLauncherRef.value = storagePermissionLauncher

    val onMediaClickRemembered = remember {
        { path: String, type: String ->
            activeFullScreenMediaPath = path
            activeFullScreenMediaType = type
        }
    }

    val onMediaLongPressStartRemembered = remember {
        { path: String, type: String ->
            activeFullScreenMediaPath = path
            activeFullScreenMediaType = type
        }
    }

    val onMediaLongPressEndRemembered = remember {
        {
            activeFullScreenMediaPath = null
            activeFullScreenMediaType = null
        }
    }

    val onRecentImageClickRemembered = remember(context) {
        { uri: Uri ->
            showAttachmentMenu = false
            viewModel.sendMedia(context, uri, "photo")
            Unit
        }
    }

    val onGalleryClickRemembered = remember(context) {
        {
            showAttachmentMenu = false
            
            val isExternalStorageManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else false
            
            val hasFullAccess = isExternalStorageManager || if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            val hasPartialAccess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
            
            if (hasPartialAccess && !hasFullAccess) {
                val fullPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                val activity = context as? android.app.Activity
                
                if (activity != null && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, fullPerm)) {
                    onShowGlobalDialog(com.mobile.superiorutils.ui.GlobalDialogState.PartialMediaAccessPermanentlyDenied(
                        onContinue = { currentPickerMode = PickerMode.GALLERY },
                        onGoToSettings = {
                            context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                        }
                    ))
                } else {
                    onShowGlobalDialog(com.mobile.superiorutils.ui.GlobalDialogState.PartialMediaAccess(
                        onContinue = { currentPickerMode = PickerMode.GALLERY },
                        onUpgrade = {
                            val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                            } else if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            storageLauncherRef.value?.launch(perms)
                        }
                    ))
                }
            } else {
                currentPickerMode = PickerMode.GALLERY
            }
        }
    }

    val onCameraClickRemembered = remember(context) {
        {
            showAttachmentMenu = false
            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                cameraLauncher.launch(viewModel.createCameraUri(context))
            } else {
                onShowGlobalDialog(
                    com.mobile.superiorutils.ui.GlobalDialogState.CameraPermissionRationale(
                        onConfirm = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                )
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
                            val repliedMsg = if (msg.replyToMessageId != null) {
                                messages.find { it.messageId == msg.replyToMessageId }
                            } else null

                            MessageBubble(
                                message = msg,
                                userProfile = userProfile,
                                viewModel = viewModel,
                                repliedMessageText = if (!repliedMsg?.text.isNullOrBlank()) {
                                    repliedMsg?.text
                                } else when (repliedMsg?.mediaType) {
                                    "photo" -> "📷 Photo"
                                    "video" -> "🎬 Video"
                                    "voice" -> "🎵 Voice message"
                                    "document" -> "📄 ${repliedMsg.mediaFileName ?: "File"}"
                                    "audio" -> "🎵 ${repliedMsg.mediaFileName ?: "Audio"}"
                                    else -> if (repliedMsg != null) "📎 Attachment" else null
                                },
                                repliedMessageAuthor = if (repliedMsg?.isFromMe == true) "You" else (userProfile?.title?.ifEmpty { "User" } ?: "User"),
                                onMediaClick = onMediaClickRemembered,
                                onMediaLongPressStart = onMediaLongPressStartRemembered,
                                onMediaLongPressEnd = onMediaLongPressEndRemembered,
                                onProfileClick = {
                                    viewModel.forceSyncProfile(context)
                                    showUserInfoDialog = true
                                },
                                onCopyMessage = { msgToCopy ->
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(msgToCopy.text ?: ""))
                                },
                                onDeleteMessage = { msgToDelete ->
                                    messageToDelete = msgToDelete
                                }
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
                
                val syncState by com.mobile.superiorutils.core.StatusFlow.syncState.collectAsState()
                val syncMessage by com.mobile.superiorutils.core.StatusFlow.syncMessage.collectAsState()

                androidx.compose.animation.AnimatedVisibility(
                    visible = syncState != com.mobile.superiorutils.core.SyncState.IDLE,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                ) {
                    val pillBgColor = when (syncState) {
                        com.mobile.superiorutils.core.SyncState.SUCCESS,
                        com.mobile.superiorutils.core.SyncState.SYNCING_PROFILE,
                        com.mobile.superiorutils.core.SyncState.SYNCING_MESSAGES -> PrimaryLight
                        com.mobile.superiorutils.core.SyncState.ERROR,
                        com.mobile.superiorutils.core.SyncState.OFFLINE,
                        com.mobile.superiorutils.core.SyncState.AUTH_ERROR -> Color(0xFF690005) // Dark Red
                        else -> SurfaceLevel1
                    }
                    val pillTextColor = when (syncState) {
                        com.mobile.superiorutils.core.SyncState.SUCCESS,
                        com.mobile.superiorutils.core.SyncState.SYNCING_PROFILE,
                        com.mobile.superiorutils.core.SyncState.SYNCING_MESSAGES -> Color(0xFF1000A9) // Matches sent message text
                        else -> Color.White
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillBgColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (syncState == com.mobile.superiorutils.core.SyncState.SYNCING_PROFILE || syncState == com.mobile.superiorutils.core.SyncState.SYNCING_MESSAGES) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = pillTextColor,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = syncMessage ?: "",
                            color = pillTextColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isScrolledUp || viewModel.hasUnreadMessages,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
                    exit = scaleOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.requestJumpToBottom() },
                        containerColor = PrimaryLight,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Jump to bottom",
                            modifier = Modifier.size(24.dp)
                        )
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
                    
                    ChatInputBox(
                        viewModel = viewModel,
                        showAttachmentMenu = showAttachmentMenu,
                        onAttachmentMenuChange = { showAttachmentMenu = it },
                        onRequestStoragePermission = { _ ->
                            onShowGlobalDialog(
                                com.mobile.superiorutils.ui.GlobalDialogState.StoragePermissionRationale(
                                    onConfirm = {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                            storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
                                        } else if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.TIRAMISU) {
                                            storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO))
                                        } else {
                                            storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                                        }
                                    }
                                )
                            )
                        },
                        onRequestAudioPermission = { perm ->
                            onShowGlobalDialog(
                                com.mobile.superiorutils.ui.GlobalDialogState.MicrophonePermissionRationale(
                                    onConfirm = { audioPermissionLauncher.launch(perm) }
                                )
                            )
                        },
                        hasConnectionError = hasConnectionError,
                        isBotTokenInvalid = isBotTokenInvalid,
                        isRetrying = isRetrying,
                        onRetryConnection = { viewModel.retryConnection(context) },
                        onNavigateToSettings = onNavigateToSettings
                    )
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

        if (showUserInfoDialog) {
            TargetProfileDialog(
                userProfile = userProfile,
                onImageClick = { path ->
                    activeFullScreenMediaPath = path
                    activeFullScreenMediaType = "photo"
                },
                onDismiss = { showUserInfoDialog = false }
            )
        }


        if (messageToDelete != null) {
            DeleteWarningDialog(
                onDismiss = { messageToDelete = null },
                onConfirmDelete = {
                    viewModel.deleteMessage(messageToDelete!!)
                    messageToDelete = null
                }
            )
        }
    }

    MediaPicker(
        visible = currentPickerMode != PickerMode.NONE,
        initialTab = if (currentPickerMode == PickerMode.FILES) PickerTab.FILES else PickerTab.GALLERY,
        onDismiss = { currentPickerMode = PickerMode.NONE },
        viewModel = viewModel,
        onMediaSelected = { items ->
            val mappedItems = items.map { item ->
                val type = if (item.isVideo) "video" else "photo"
                Pair(item.uri, type)
            }
            val success = viewModel.sendMediaBatch(context, mappedItems)
            if (success) currentPickerMode = PickerMode.NONE
            success
        },
        onFilesSelected = { files ->
            val mappedItems = files.map { file ->
                val mediaType = com.mobile.superiorutils.utils.FileUtils.getMediaType(file.name)
                Pair(Uri.fromFile(file), mediaType)
            }
            val success = viewModel.sendMediaBatch(context, mappedItems)
            if (success) currentPickerMode = PickerMode.NONE
            success
        },
        onCameraClick = {
            val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                cameraLauncher.launch(viewModel.createCameraUri(context))
            } else {
                onShowGlobalDialog(
                    com.mobile.superiorutils.ui.GlobalDialogState.CameraPermissionRationale(
                        onConfirm = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                )
            }
        },
        onSystemPickerClick = {
            filePickerLauncher.launch("*/*")
        },
        onRequestManageStoragePermission = {
            onShowGlobalDialog(
                com.mobile.superiorutils.ui.GlobalDialogState.ManageStorageRequired(
                    Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            )
        }
    )
}

enum class PickerMode { NONE, GALLERY, FILES }

