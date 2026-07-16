package com.mobile.superiorutils.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.entity.MessageStatus
import com.mobile.superiorutils.theme.DividerColor
import com.mobile.superiorutils.theme.PrimaryLight
import com.mobile.superiorutils.theme.InfoBlue
import com.mobile.superiorutils.theme.SurfaceLevel1
import com.mobile.superiorutils.theme.SurfaceLevel2
import com.mobile.superiorutils.ui.ChatViewModel
import com.mobile.superiorutils.utils.AppLog
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.LogLevel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mobile.superiorutils.media.MediaSync
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.animateContentSize

// ──────────────────────────────────────────────────────────────
// Markdown-aware text renderer
// Supports: **bold**, __italic__, `monospace`, ~~strikethrough~~
// ──────────────────────────────────────────────────────────────
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default
) {
    val annotated = remember(text) { parseMarkdown(text) }
    Text(text = annotated, color = color, modifier = modifier, style = style)
}

/** Lightweight inline-markdown parser — no external libs needed. */
private fun parseMarkdown(raw: String): AnnotatedString = buildAnnotatedString {
    val patterns = listOf(
        Pair(Regex("""\*\*(.+?)\*\*"""), "bold"),
        Pair(Regex("""\*(.+?)\*"""), "bold"),
        Pair(Regex("""__(.+?)__"""), "italic"),
        Pair(Regex("""_(.+?)_"""), "italic"),
        Pair(Regex("""`(.+?)`"""), "mono"),
        Pair(Regex("""~~(.+?)~~"""), "strike")
    )
    var cursor = 0
    data class Token(val start: Int, val end: Int, val inner: String, val type: String)
    val tokens = mutableListOf<Token>()
    for ((regex, type) in patterns) {
        for (m in regex.findAll(raw)) {
            tokens.add(Token(m.range.first, m.range.last + 1, m.groupValues[1], type))
        }
    }
    tokens.sortBy { it.start }
    // Remove overlapping tokens
    val clean = mutableListOf<Token>()
    var lastEnd = 0
    for (t in tokens) {
        if (t.start >= lastEnd) { clean.add(t); lastEnd = t.end }
    }
    for (t in clean) {
        if (t.start > cursor) append(raw.substring(cursor, t.start))
        val span = when (t.type) {
            "bold" -> SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
            "mono" -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color.White.copy(alpha = 0.1f))
            "strike" -> SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            else -> SpanStyle()
        }
        withStyle(span) { append(t.inner) }
        cursor = t.end
    }
    if (cursor < raw.length) append(raw.substring(cursor))
}

// ──────────────────────────────────────────────────────────────
// Emoji reaction quick-tray (shown on double-tap)
// ──────────────────────────────────────────────────────────────
val QUICK_REACTIONS = listOf("👍", "❤️", "🤣", "😱", "😢", "🔥")

@Composable
fun EmojiReactionTray(
    message: MessageNode,
    onReact: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Popup(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true)
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.8f),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
        ) {
            val currentReactions = message.reactions
                ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1E1E2E))
                    .border(1.dp, Color(0xFF3A3A4E), RoundedCornerShape(28.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QUICK_REACTIONS.forEach { emoji ->
                    val isSelected = currentReactions.contains(emoji)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) PrimaryLight.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .clickable { onReact(emoji); onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageNode,
    userProfile: com.mobile.superiorutils.data.entity.UserProfile?,
    viewModel: ChatViewModel,
    onMediaClick: (String, String) -> Unit,
    onMediaLongPressStart: (String, String) -> Unit = { _, _ -> },
    onMediaLongPressEnd: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMessageLongPress: (MessageNode) -> Unit = {},
    onCopyMessage: (MessageNode) -> Unit = {},
    onDeleteMessage: (MessageNode) -> Unit = {},
    repliedMessageText: String? = null,
    repliedMessageAuthor: String? = null
) {
    val progress by MediaSync.getProgress(message.messageId).collectAsState()
    val context = LocalContext.current
    var showApkInstallDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showContextMenu by remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showApkInstallDialog) {
        ActionDialog(
            title = "Installation Permission Required",
            message = "To install this app, you need to allow SuperiorChat to install unknown apps.",
            confirmText = "Settings",
            onConfirm = {
                showApkInstallDialog = false
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            },
            onDismiss = { showApkInstallDialog = false }
        )
    }

    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isFromMe) PrimaryLight else SurfaceLevel1
    val textColor = if (message.isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val shape = if (message.isFromMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }
    
    val glowModifier = if (message.isFromMe) {
        Modifier.glow(color = Color(0xFFC0C1FF), radius = 30f, dy = 10f, cornerRadius = 20.dp)
    } else {
        Modifier
    }

    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = sdf.format(Date(message.timestamp))

    val verticalPadding = if (message.mediaType == "voice" || message.mediaType == "audio") 6.dp else 10.dp

    var swipeOffsetX by remember { androidx.compose.runtime.mutableStateOf(0f) }
    val animatedSwipeOffsetX by androidx.compose.animation.core.animateFloatAsState(targetValue = swipeOffsetX)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (message.isFromMe) {
                            if (swipeOffsetX < -50f) {
                                viewModel.setReplyingToMessage(message)
                            }
                        } else {
                            if (swipeOffsetX > 50f) {
                                viewModel.setReplyingToMessage(message)
                            }
                        }
                        swipeOffsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (message.isFromMe) {
                            if (dragAmount < 0 || swipeOffsetX < 0) { // Allow left swipe
                                swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-60f, 0f)
                            }
                        } else {
                            if (dragAmount > 0 || swipeOffsetX > 0) { // Allow right swipe
                                swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(0f, 60f)
                            }
                        }
                    }
                )
            },
        contentAlignment = alignment
    ) {
        if (message.isFromMe && animatedSwipeOffsetX < -20f) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Reply",
                tint = PrimaryLight.copy(alpha = ((-animatedSwipeOffsetX) / 60f).coerceIn(0f, 1f)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    .size(28.dp)
            )
        } else if (!message.isFromMe && animatedSwipeOffsetX > 20f) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Reply,
                contentDescription = "Reply",
                tint = PrimaryLight.copy(alpha = (animatedSwipeOffsetX / 60f).coerceIn(0f, 1f)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(28.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .offset(x = animatedSwipeOffsetX.dp)
        ) {
            if (!message.isFromMe) {
                val profilePath = userProfile?.profilePhotoPath ?: ""
                val title = userProfile?.title?.ifEmpty { "Unknown" } ?: "Unknown"
                
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SurfaceLevel2)
                        .border(1.dp, PrimaryLight.copy(alpha = 0.3f), CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePath.isNotEmpty() && File(profilePath).exists()) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(File(profilePath))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initials = try {
                            title.trim().split(Regex("\\s+")).take(2).mapNotNull { 
                                if (it.isNotEmpty()) {
                                    val cp = it.codePointAt(0)
                                    String(Character.toChars(cp)).uppercase()
                                } else null
                            }.joinToString("").take(4)
                        } catch (e: Exception) {
                            "?"
                        }
                        Text(
                            text = initials.ifEmpty { "?" },
                            color = PrimaryLight,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Column(
                horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
                modifier = Modifier.wrapContentWidth()
            ) {
            Box(
                modifier = Modifier
                    .then(glowModifier)
                    .clip(shape)
                    .background(bgColor)
                    .widthIn(min = 60.dp, max = 260.dp)
                    .then(if (!message.isFromMe) Modifier.border(1.dp, DividerColor, shape) else Modifier)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                // Single tap: open combined emoji + actions popup
                                showContextMenu = true
                            },
                            onDoubleTap = {
                                // Double tap: quick-react with last used emoji (fallback 👍)
                                val emoji = viewModel.lastUsedEmoji ?: "👍"
                                viewModel.sendReaction(message, emoji)
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = verticalPadding)
            ) {
                MessageContextMenu(
                    expandedProvider = { showContextMenu },
                    message = message,
                    sortedEmojis = viewModel.sortedEmojis,
                    onDismiss = { showContextMenu = false },
                    onReact = { emoji -> viewModel.sendReaction(message, emoji) },
                    onReplyClick = {
                        viewModel.setReplyingToMessage(message)
                    },
                    onCopyClick = {
                        onCopyMessage(message)
                    },
                    onEditClick = {
                        viewModel.setEditingMessage(message)
                    },
                    onDeleteClick = {
                        onDeleteMessage(message)
                    }
                )
                Column {
                    if (message.replyToMessageId != null) {
                        // Reply stub — compact Telegram-style
                        Row(
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (message.isFromMe) SurfaceLevel2.copy(alpha = 0.65f)
                                    else PrimaryLight.copy(alpha = 0.12f)
                                )
                                .padding(start = if (message.isFromMe) 0.dp else 3.dp)
                                .background(
                                    if (message.isFromMe) Color.Transparent
                                    else SurfaceLevel2
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = repliedMessageAuthor ?: "Message",
                                    color = if (message.isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = repliedMessageText?.takeIf { it.isNotBlank() } ?: "📎 Attachment",
                                    color = if (message.isFromMe) Color.White else textColor.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (message.mediaType == "photo" && message.mediaLocalPath != null) {
                            val isUploading = message.isFromMe && (message.status == MessageStatus.SENDING || message.status == MessageStatus.QUEUED)
                            val isFailedUpload = message.isFromMe && message.status == MessageStatus.FAILED
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data(File(message.mediaLocalPath))
                                        .size(600)
                                        .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                        .build(),
                                    contentDescription = "Photo",
                                    modifier = Modifier
                                        .heightIn(max = 300.dp)
                                        .pointerInput(isUploading) {
                                            if (isUploading) return@pointerInput
                                            detectTapGestures(
                                                onPress = {
                                                    val job = CoroutineScope(Dispatchers.Main).launch {
                                                        delay(150)
                                                        onMediaLongPressStart(message.mediaLocalPath, "photo")
                                                    }
                                                    val success = tryAwaitRelease()
                                                    job.cancel()
                                                    onMediaLongPressEnd()
                                                    if (success && !job.isCompleted) {
                                                        if (isFailedUpload) {
                                                            viewModel.retryMessage(message)
                                                        } else {
                                                            onMediaClick(message.mediaLocalPath, "photo")
                                                        }
                                                    }
                                                }
                                            )
                                        },
                                    contentScale = ContentScale.Crop
                                )
                                if (isUploading || isFailedUpload) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isFailedUpload) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry Upload", tint = Color.White, modifier = Modifier.size(36.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Upload failed. Tap to retry", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                            }
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(
                                                    progress = { if (progress > 0f) progress else 0f },
                                                    modifier = Modifier.size(40.dp),
                                                    color = Color.White,
                                                    strokeWidth = 3.dp
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Cancel Upload",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clickable { viewModel.cancelTransfer(message) }
                                                )
                                            }
                                        }
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    val totalSize = message.mediaFileSize ?: java.io.File(message.mediaLocalPath).length()
                                    val sizeText = if (totalSize > 0) " • ${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}" else ""
                                    val label = "IMAGE"
                                    Text(if (isUploading) "UPLOADING$sizeText" else "$label$sizeText", color = Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                    } else if (message.mediaType == "photo") {
                        val isDownloading = message.status == MessageStatus.SENDING
                        val isFailed = message.status == MessageStatus.FAILED
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Gray.copy(alpha = 0.3f))
                                .clickable(enabled = !isDownloading) { if (!isDownloading) viewModel.retryDownload(message) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloading) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { if (progress > 0f) progress else 0f },
                                        modifier = Modifier.size(40.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Download",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.cancelTransfer(message) }
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(imageVector = if (isFailed) Icons.Default.Refresh else Icons.Default.ArrowDownward, contentDescription = "Download Photo", tint = Color.White, modifier = Modifier.size(36.dp))
                                    if (isFailed) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Download failed", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val totalSize = message.mediaFileSize ?: 0L
                                val sizeText = if (totalSize > 0) " • ${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}" else ""
                                Text("IMAGE$sizeText", color = Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (message.mediaType == "voice" || message.mediaType == "audio") {
                        val progress by com.mobile.superiorutils.media.MediaSync.getProgress(message.messageId).collectAsState(initial = 0f)
                        AudioBubble(
                            mediaLocalPath = message.mediaLocalPath,
                            mediaUrl = message.mediaUrl,
                            mediaType = message.mediaType,
                            status = message.status,
                            isFromMe = message.isFromMe,
                            progress = progress,
                            onDownloadClick = { viewModel.retryDownload(message) },
                            onCancelClick = { viewModel.cancelTransfer(message) }
                        )
                        if (!message.text.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else if (message.mediaType == "video" && message.mediaLocalPath != null) {
                        val isUploading = message.isFromMe && (message.status == MessageStatus.SENDING || message.status == MessageStatus.QUEUED)
                        val isFailedUpload = message.isFromMe && message.status == MessageStatus.FAILED
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .pointerInput(isUploading) {
                                    if (isUploading) return@pointerInput
                                    detectTapGestures(
                                        onPress = {
                                            val job = CoroutineScope(Dispatchers.Main).launch {
                                                delay(150)
                                                onMediaLongPressStart(message.mediaLocalPath, "video")
                                            }
                                            val success = tryAwaitRelease()
                                            job.cancel()
                                            onMediaLongPressEnd()
                                            if (success && !job.isCompleted) {
                                                if (isFailedUpload) {
                                                    viewModel.retryMessage(message)
                                                } else {
                                                    onMediaClick(message.mediaLocalPath, "video")
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(File(message.mediaLocalPath))
                                    .videoFrameMillis(1000)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Video Thumbnail",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Dimming overlay over the thumbnail
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

                            if (!isUploading && !isFailedUpload) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else if (isFailedUpload) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Retry Video Upload",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Failed. Tap to retry", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { if (progress > 0f) progress else 0f },
                                        modifier = Modifier.size(40.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Upload",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.cancelTransfer(message) }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val totalSize = message.mediaFileSize ?: java.io.File(message.mediaLocalPath).length()
                                val sizeText = if (totalSize > 0) " • ${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}" else ""
                                Text(if (isUploading) "UPLOADING$sizeText" else "VIDEO$sizeText", color = Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (message.mediaType == "video") {
                        val isDownloading = message.status == MessageStatus.SENDING
                        val isFailed = message.status == MessageStatus.FAILED
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Gray.copy(alpha = 0.2f))
                                .clickable(enabled = !isDownloading) { if (!isDownloading) viewModel.retryDownload(message) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDownloading) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { if (progress > 0f) progress else 0f },
                                        modifier = Modifier.size(40.dp),
                                        color = Color.White,
                                        strokeWidth = 3.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Download",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.cancelTransfer(message) }
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFailed) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                                            contentDescription = "Download Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    if (isFailed) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Failed. Tap to retry", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val totalSize = message.mediaFileSize ?: 0L
                                val sizeText = if (totalSize > 0) " • ${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}" else ""
                                Text("VIDEO$sizeText", color = Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (message.mediaType == "document" && message.mediaLocalPath != null) {
                        val file = File(message.mediaLocalPath)
                        val isUploading = message.isFromMe && (message.status == MessageStatus.SENDING || message.status == MessageStatus.QUEUED)
                        val isFailedUpload = message.isFromMe && message.status == MessageStatus.FAILED
                        // Use stored original name, or strip timestamp prefix from local file name
                        val displayName = message.mediaFileName ?: if (file.name.matches(Regex("^-?\\d+_.+"))) {
                            file.name.substringAfter("_")
                        } else {
                            file.name
                        }
                        val totalSize = message.mediaFileSize ?: file.length()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (message.isFromMe) Color.White.copy(alpha = 0.1f) else SurfaceLevel2)
                                .clickable(enabled = !isUploading) {
                                    if (isFailedUpload) {
                                        viewModel.retryMessage(message)
                                    } else {
                                        if (file.extension.equals("apk", ignoreCase = true) &&
                                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                                            !context.packageManager.canRequestPackageInstalls()
                                        ) {
                                            showApkInstallDialog = true
                                            return@clickable
                                        }
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    file
                                                )
                                                val ext = file.extension.lowercase(Locale.ROOT)
                                                val mimeType = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
                                                setDataAndType(uri, mimeType)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            AppLog.log(LogCategory.SYSTEM, "Failed to open document: ${e.message}", LogLevel.ERROR)
                                        }
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isUploading) {
                                // Cancel button overlaid on progress circle
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { if (progress > 0f) progress else 0f },
                                        modifier = Modifier.size(40.dp),
                                        color = textColor,
                                        trackColor = textColor.copy(alpha = 0.15f),
                                        strokeWidth = 3.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Upload",
                                        tint = textColor,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.cancelTransfer(message) }
                                    )
                                }
                            } else if (isFailedUpload) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry Document Upload",
                                    tint = textColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = com.mobile.superiorutils.utils.FileUtils.resolveFileIcon(displayName),
                                    contentDescription = "Document File",
                                    tint = textColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                val ext = displayName.substringAfterLast(".", "").uppercase(Locale.ROOT)
                                val extPrefix = if (ext.isNotEmpty()) "$ext • " else ""
                                
                                Text(
                                    text = if (isUploading) {
                                        val uploaded = (progress * totalSize).toLong()
                                        "${extPrefix}${com.mobile.superiorutils.utils.FileUtils.formatFileSize(uploaded)} / ${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}"
                                    } else if (isFailedUpload) {
                                        "${extPrefix}Upload failed. Tap to retry."
                                    } else {
                                        "${extPrefix}${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}"
                                    },
                                    color = textColor.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (message.mediaType == "document") {
                        val isDownloading = message.status == MessageStatus.SENDING
                        val isFailed = message.status == MessageStatus.FAILED
                        val displayName = message.mediaFileName ?: "Document"
                        val totalSize = message.mediaFileSize ?: 0L
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (message.isFromMe) Color.White.copy(alpha = 0.1f) else SurfaceLevel2)
                                .clickable(enabled = !isDownloading) { if (!isDownloading) viewModel.retryDownload(message) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDownloading) {
                                // Telegram-style: progress circle with X cancel button
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { if (progress > 0f) progress else 0f },
                                        modifier = Modifier.size(40.dp),
                                        color = PrimaryLight,
                                        trackColor = Color.White.copy(alpha = 0.15f),
                                        strokeWidth = 3.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Download",
                                        tint = textColor,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.cancelTransfer(message) }
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isFailed) Icons.Default.Refresh else Icons.Default.ArrowDownward,
                                    contentDescription = "Download Document",
                                    tint = textColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    fontSize = 13.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                val ext = displayName.substringAfterLast(".", "").uppercase(Locale.ROOT)
                                val extPrefix = if (ext.isNotEmpty()) "$ext • " else ""
                                
                                Text(
                                    text = if (isDownloading && totalSize > 0L) {
                                        val downloaded = (progress * totalSize).toLong()
                                        "${extPrefix}${com.mobile.superiorutils.utils.FileUtils.formatFileSize(downloaded)} / ${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}"
                                    } else if (isFailed) {
                                        "${extPrefix}Failed"
                                    } else if (totalSize > 0L) {
                                        "${extPrefix}${com.mobile.superiorutils.utils.FileUtils.formatFileSize(totalSize)}"
                                    } else {
                                        "${extPrefix}Tap to download"
                                    },
                                    color = textColor.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (!message.text.isNullOrEmpty()) {
                        MarkdownText(text = message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
                    }
                    // Reaction pill badges
                    val reactionList = message.reactions
                        ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                    androidx.compose.animation.AnimatedVisibility(
                        visible = reactionList.isNotEmpty(),
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.wrapContentWidth().animateContentSize()
                            ) {
                                reactionList.forEach { emoji ->
                                    val state = remember(emoji) { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visibleState = state,
                                        enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn()
                                    ) {
                                        val pillBgColor = if (message.isFromMe) Color.Black.copy(alpha = 0.2f) else PrimaryLight.copy(alpha = 0.18f)
                                        val pillBorderColor = if (message.isFromMe) Color.Black.copy(alpha = 0.1f) else PrimaryLight.copy(alpha = 0.35f)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(pillBgColor)
                                                .border(1.dp, pillBorderColor, RoundedCornerShape(12.dp))
                                                .clickable { viewModel.sendReaction(message, emoji) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(text = emoji, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 4.dp, start = if (message.isFromMe) 0.dp else 6.dp, end = if (message.isFromMe) 6.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (message.isEdited) {
                    Text(text = "edited", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), modifier = Modifier.padding(end = 4.dp))
                }
                Text(text = timeString, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                if (message.isFromMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.status) {
                        MessageStatus.SENDING -> Icon(imageVector = Icons.Outlined.Schedule, contentDescription = "Sending", tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        MessageStatus.SENT -> Icon(imageVector = Icons.Default.Done, contentDescription = "Sent", tint = PrimaryLight, modifier = Modifier.size(16.dp))
                        MessageStatus.QUEUED -> Icon(imageVector = Icons.Outlined.Schedule, contentDescription = "Queued", tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(12.dp))
                        MessageStatus.FAILED -> {
                            if (message.mediaType == null) {
                                IconButton(
                                    onClick = { viewModel.retryMessage(message) },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                                }
                            } else {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Failed", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
        }
    }
}
