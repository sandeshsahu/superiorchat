package com.mobile.superiorchat.ui.components.bubbles

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.entity.MessageStatus
import com.mobile.superiorchat.theme.DividerColor
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.InfoBlue
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.ui.ChatViewModel
import com.mobile.superiorchat.ui.components.popups.ActionDialog
import com.mobile.superiorchat.ui.components.glow
import com.mobile.superiorchat.ui.components.popups.MessageContextMenu
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogLevel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.mobile.superiorchat.media.MediaSync
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
@Composable
fun MessageBubble(
    message: MessageNode,
    userProfile: com.mobile.superiorchat.data.entity.UserProfile?,
    viewModel: ChatViewModel,
    onMediaClick: (String, String) -> Unit,
    onMediaLongPressStart: (String, String) -> Unit = { _, _ -> },
    onMediaLongPressEnd: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onMessageLongPress: (MessageNode) -> Unit = {},
    onCopyMessage: (MessageNode) -> Unit = {},
    onDeleteMessage: (MessageNode) -> Unit = {},
    isPinned: Boolean = false,
    onPinClick: (MessageNode) -> Unit = {},
    onSelectMessage: (MessageNode) -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    repliedMessageText: String? = null,
    repliedMessageAuthor: String? = null
) {
    val progress by MediaSync.getProgress(message.messageId).collectAsState()
    val context = LocalContext.current
    var showApkInstallDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current

    if (message.mediaType == "system_pin") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.text ?: "Pinned a message",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        return
    }

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

    val sdf = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = sdf.format(Date(message.timestamp))

    val verticalPadding = if (message.mediaType == "voice" || message.mediaType == "audio") 6.dp else 10.dp

    var swipeOffsetX by remember { androidx.compose.runtime.mutableStateOf(0f) }
    val animatedSwipeOffsetX by androidx.compose.animation.core.animateFloatAsState(targetValue = swipeOffsetX)
    val currentMessageState = androidx.compose.runtime.rememberUpdatedState(message)

    val selectionBgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryLight.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(150),
        label = "selectionBg"
    )

    // Animate checkbox offset: slides from off-screen left (-40dp) to visible (8dp from left)
    val checkboxOffsetX by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelectionMode) 8.dp else (-40).dp,
        animationSpec = tween(180),
        label = "checkboxOffset"
    )

    // Animate content shift: pushes received messages to the right so they don't overlap the checkbox
    val contentPaddingStart by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelectionMode && !message.isFromMe) 36.dp else 0.dp,
        animationSpec = tween(180),
        label = "contentPadding"
    )

    val currentViewConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
    val customViewConfig = remember(currentViewConfig) {
        object : androidx.compose.ui.platform.ViewConfiguration by currentViewConfig {
            override val longPressTimeoutMillis: Long
                get() = 250L // Snappier long press!
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalViewConfiguration provides customViewConfig
    ) {
        Box(
            modifier = Modifier
            .padding(horizontal = 4.dp, vertical = 2.dp) // Margin around the selected item
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)) // Round the background corners
            .background(selectionBgColor)
            .padding(vertical = 4.dp) // Inner padding
            // Outer Box handles swipe-to-reply gestures
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (!isSelectionMode) {
                            if (message.isFromMe) {
                                if (swipeOffsetX < -50f) viewModel.setReplyingToMessage(message)
                            } else {
                                if (swipeOffsetX > 50f) viewModel.setReplyingToMessage(message)
                            }
                        }
                        swipeOffsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        if (!isSelectionMode) {
                            if (message.isFromMe) {
                                if (dragAmount < 0 || swipeOffsetX < 0)
                                    swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(-60f, 0f)
                            } else {
                                if (dragAmount > 0 || swipeOffsetX > 0)
                                    swipeOffsetX = (swipeOffsetX + dragAmount).coerceIn(0f, 60f)
                            }
                        }
                    }
                )
            }
            // Taps on the empty area outside the bubble trigger selection or open the popup instantly
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onTap = {
                        if (isSelectionMode) {
                            onSelectMessage(currentMessageState.value)
                        } else {
                            if (viewModel.activePopupMessageId != null) viewModel.hideContextMenu()
                        }
                    },
                    onLongPress = {
                        if (!isSelectionMode) {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            viewModel.showContextMenu(currentMessageState.value.messageId)
                        }
                    }
                )
            },
        contentAlignment = alignment  // ← This is what aligns sent right / received left
    ) {
        // Swipe-to-reply hint icons (only shown when not in selection mode)
        if (!isSelectionMode) {
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
        }

        // Selection checkbox — positioned at the left edge, slides in/out via offset animation.
        // Using Box.align() keeps it as an overlay that does NOT affect the bubble's alignment.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = checkboxOffsetX)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) PrimaryLight else Color.Transparent)
                .border(2.dp, if (isSelected) PrimaryLight else Color(0xFF8E8E93), CircleShape)
                .clickable(enabled = isSelectionMode) { onSelectMessage(currentMessageState.value) },
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // The actual message bubble row — offset by checkbox width when in selection mode
        // so it doesn't overlap the checkbox. Uses padding instead of weight to preserve widthIn.
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .offset(x = animatedSwipeOffsetX.dp)
                .padding(start = contentPaddingStart)
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
                    .pointerInput(isSelectionMode) {
                        detectTapGestures(
                            onTap = {
                                if (isSelectionMode) {
                                    // In selection mode: toggle selection
                                    onSelectMessage(currentMessageState.value)
                                } else {
                                    // Tap outside clears open popups (if any)
                                    if (viewModel.activePopupMessageId != null) viewModel.hideContextMenu()
                                }
                            },
                            onDoubleTap = if (isSelectionMode) null else { _ ->
                                // Double tap: if already reacted, undo that specific reaction.
                                val msg = currentMessageState.value
                                val currentReactions = msg.reactions?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                                val emojiToToggle = if (!currentReactions.isNullOrEmpty()) {
                                    currentReactions.first()
                                } else {
                                    viewModel.lastUsedEmoji ?: "👍"
                                }
                                viewModel.sendReaction(msg, emojiToToggle)
                            },
                            onLongPress = {
                                if (!isSelectionMode) {
                                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                    viewModel.showContextMenu(message.messageId)
                                }
                            }
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = verticalPadding)
            ) {
                MessageContextMenu(
                    expandedProvider = { viewModel.activePopupMessageId == message.messageId },
                    message = message,
                    sortedEmojis = viewModel.sortedEmojis,
                    onDismiss = { viewModel.hideContextMenu() },
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
                    onSelectClick = {
                        viewModel.enterSelectionMode(message)
                    },
                    onDeleteClick = {
                        onDeleteMessage(message)
                    },
                    isPinned = isPinned,
                    onPinClick = {
                        onPinClick(message)
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

                    if (message.mediaType == "photo" || message.mediaType == "video") {
                        com.mobile.superiorchat.ui.components.bubbles.MediaBubble(
                            message = message,
                            viewModel = viewModel,
                            progress = progress,
                            onMediaClick = onMediaClick,
                            onMediaLongPressStart = onMediaLongPressStart,
                            onMediaLongPressEnd = onMediaLongPressEnd
                        )
                    } else if (message.mediaType == "voice" || message.mediaType == "audio") {
                        com.mobile.superiorchat.ui.components.bubbles.AudioBubble(
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
                    } else if (message.mediaType == "document") {
                        com.mobile.superiorchat.ui.components.bubbles.DocumentBubble(
                            message = message,
                            viewModel = viewModel,
                            progress = progress,
                            textColor = textColor
                        )
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
        } // end Column
        } // end inner bubble Row
        } // end outer Box
    } // end CompositionLocalProvider
} // end MessageBubble
