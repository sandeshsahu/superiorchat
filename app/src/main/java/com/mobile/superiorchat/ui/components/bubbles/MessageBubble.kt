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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.mobile.superiorchat.ui.components.popups.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.entity.MessageStatus
import com.mobile.superiorchat.theme.DividerColor
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.*
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
import com.mobile.superiorchat.utils.FileUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult
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

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

// ──────────────────────────────────────────────────────────────
// Markdown-aware text renderer
// Supports: **bold**, __italic__, `monospace`, ~~strikethrough~~, ||spoiler||, <u>underline</u>, ```code blocks```
// ──────────────────────────────────────────────────────────────
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    isFromMe: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    pressedCodeRange: IntRange? = null,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null
) {
    val codeBg = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                 else SurfaceContainerHighest
    val codePressBg = if (isFromMe) MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.40f)
                      else PrimaryLight.copy(alpha = 0.40f)
    val spoilerBg = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)

    val annotated = remember(text, codeBg, codePressBg, spoilerBg, pressedCodeRange) { 
        parseMarkdown(text, codeBg, codePressBg, spoilerBg, pressedCodeRange) 
    }

    Text(
        text = annotated,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
        onTextLayout = { onTextLayout?.invoke(it) }
    )
}

private data class StyleState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrike: Boolean = false,
    val isUnderline: Boolean = false,
    val isCode: Boolean = false,
    val isPre: Boolean = false,
    val isSpoiler: Boolean = false
)

private data class FormattedSegment(
    val text: String,
    val style: StyleState
)

private enum class MatchType {
    PRE, CODE, SPOILER, UNDERLINE, BOLD, ITALIC, STRIKE
}

private data class MatchResultData(
    val start: Int,
    val end: Int,
    val inner: String,
    val type: MatchType
)

private val REGEX_PRE = Regex("""```(?:[a-zA-Z0-9_-]+)?\n?([\s\S]*?)```""")
private val REGEX_INLINE_CODE = Regex("""`([^`\n]+)`""")
private val REGEX_SPOILER = Regex("""\|\|([\s\S]+?)\|\|""")
private val REGEX_UNDERLINE = Regex("""<u>([\s\S]+?)</u>""", RegexOption.IGNORE_CASE)
private val REGEX_BOLD_DOUBLE = Regex("""\*\*([^\*\n]+?)\*\*""")
private val REGEX_ITALIC_DOUBLE = Regex("""__([^_\n]+?)__""")
private val REGEX_STRIKE_DOUBLE = Regex("""~~([^~\n]+?)~~""")
private val REGEX_BOLD_SINGLE = Regex("""(?<=\s|^|[^\w*])\*(?!\s)([^*\n]+?)(?<!\s)\*(?=\s|$|[^\w*])""")
private val REGEX_ITALIC_SINGLE = Regex("""(?<=\s|^|[^\w_])_(?!\s)([^_\n]+?)(?<!\s)_(?=\s|$|[^\w_])""")
private val REGEX_STRIKE_SINGLE = Regex("""(?<=\s|^|[^\w~])~(?!\s)([^~\n]+?)(?<!\s)~(?=\s|$|[^\w~])""")

private fun findEarliestMatch(text: String): MatchResultData? {
    val matches = mutableListOf<MatchResultData>()
    
    REGEX_PRE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.PRE)) }
    REGEX_INLINE_CODE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.CODE)) }
    REGEX_SPOILER.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.SPOILER)) }
    REGEX_UNDERLINE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.UNDERLINE)) }
    REGEX_BOLD_DOUBLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.BOLD)) }
    REGEX_ITALIC_DOUBLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.ITALIC)) }
    REGEX_STRIKE_DOUBLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.STRIKE)) }
    REGEX_BOLD_SINGLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.BOLD)) }
    REGEX_ITALIC_SINGLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.ITALIC)) }
    REGEX_STRIKE_SINGLE.find(text)?.let { matches.add(MatchResultData(it.range.first, it.range.last + 1, it.groupValues[1], MatchType.STRIKE)) }

    return matches.minByOrNull { it.start }
}

private fun parseMarkdownSegments(text: String, currentStyle: StyleState = StyleState()): List<FormattedSegment> {
    if (text.isEmpty()) return emptyList()

    val earliest = findEarliestMatch(text) ?: return listOf(FormattedSegment(text, currentStyle))

    val result = mutableListOf<FormattedSegment>()

    // Text before the match
    if (earliest.start > 0) {
        result.addAll(parseMarkdownSegments(text.substring(0, earliest.start), currentStyle))
    }

    // Process matched inner content
    when (earliest.type) {
        MatchType.PRE -> {
            result.add(FormattedSegment(earliest.inner, currentStyle.copy(isPre = true)))
        }
        MatchType.CODE -> {
            result.add(FormattedSegment(earliest.inner, currentStyle.copy(isCode = true)))
        }
        MatchType.SPOILER -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isSpoiler = true)))
        }
        MatchType.UNDERLINE -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isUnderline = true)))
        }
        MatchType.BOLD -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isBold = true)))
        }
        MatchType.ITALIC -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isItalic = true)))
        }
        MatchType.STRIKE -> {
            result.addAll(parseMarkdownSegments(earliest.inner, currentStyle.copy(isStrike = true)))
        }
    }

    // Text after the match
    if (earliest.end < text.length) {
        result.addAll(parseMarkdownSegments(text.substring(earliest.end), currentStyle))
    }

    return result
}

/** Robust recursive Telegram-compatible markdown parser with adaptive theme background styling and inline code tagging. */
private fun parseMarkdown(
    raw: String,
    codeBg: Color,
    codePressBg: Color,
    spoilerBg: Color,
    pressedCodeRange: IntRange? = null
): AnnotatedString = buildAnnotatedString {
    if (raw.isEmpty()) return@buildAnnotatedString
    val segments = parseMarkdownSegments(raw)
    var currentOffset = 0
    for (seg in segments) {
        var span = SpanStyle()
        if (seg.style.isBold) {
            span = span.copy(fontWeight = FontWeight.Bold)
        }
        if (seg.style.isItalic) {
            span = span.copy(fontStyle = FontStyle.Italic)
        }
        if (seg.style.isStrike && seg.style.isUnderline) {
            span = span.copy(textDecoration = TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline)))
        } else if (seg.style.isStrike) {
            span = span.copy(textDecoration = TextDecoration.LineThrough)
        } else if (seg.style.isUnderline) {
            span = span.copy(textDecoration = TextDecoration.Underline)
        }
        val isCodeBlock = seg.style.isCode || seg.style.isPre
        val isThisCodePressed = isCodeBlock && pressedCodeRange != null && (currentOffset in pressedCodeRange)
        if (isCodeBlock) {
            span = span.copy(
                fontFamily = FontFamily.Monospace,
                background = if (isThisCodePressed) codePressBg else codeBg
            )
        }
        if (seg.style.isSpoiler) {
            span = span.copy(
                background = spoilerBg
            )
        }
        val startOffset = currentOffset
        withStyle(span) {
            append(seg.text)
        }
        if (isCodeBlock) {
            addStringAnnotation(
                tag = "INLINE_CODE",
                annotation = seg.text,
                start = startOffset,
                end = startOffset + seg.text.length
            )
        }
        currentOffset += seg.text.length
    }
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
    isHighlighted: Boolean = false,
    repliedMessageText: String? = null,
    repliedMessageAuthor: String? = null,
    onNavigateToCallHistory: () -> Unit = {},
    onReplyMessageClick: (Long) -> Unit = {}
) {
    val progress by MediaSync.getProgress(message.messageId).collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showApkInstallDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
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
        ApkInstallPermissionDialog(
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

    if (showSaveDialog) {
        val typeName = when (message.mediaType) {
            "photo" -> "photo"
            "video" -> "video"
            "voice", "audio" -> "audio"
            else -> "file"
        }
        val storagetype = when (message.mediaType) {
            "photo" -> "Pictures"
            "video" -> "Movies"
            "voice", "audio" -> "Music"
            else -> "Downloads"
        }
        SaveMediaConfirmDialog(
            typeName = typeName,
            storageType = storagetype,
            onConfirm = {
                showSaveDialog = false
                val fileToSave = com.mobile.superiorchat.media.LocalDirs.resolveFile(context, message.mediaLocalPath)
                if (fileToSave != null && fileToSave.exists()) {
                    val success = FileUtils.exportMediaToGallery(
                        context,
                        fileToSave,
                        message.mediaType ?: "document",
                        message.mediaFileName
                    )
                    if (success) {
                        com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.SUCCESS, "Saved successfully")
                    } else {
                        com.mobile.superiorchat.core.StatusFlow.reportStatus(com.mobile.superiorchat.core.SyncState.ERROR, "Failed to save")
                    }
                }
            },
            onDismiss = { showSaveDialog = false }
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
        Modifier.glow(color = PrimaryLight, radius = 30f, dy = 10f, cornerRadius = 20.dp)
    } else {
        Modifier
    }

    val sdf = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = sdf.format(Date(message.timestamp))

    if (message.mediaType == "call_event") {
        val fullText = message.text ?: "Call Event"
        val parts = fullText.split(" - ")
        val titleText = parts.getOrNull(0) ?: fullText
        val durationText = parts.getOrNull(1)

        val icon: androidx.compose.ui.graphics.vector.ImageVector
        val iconTint: androidx.compose.ui.graphics.Color

        when {
            titleText.contains("Ended", ignoreCase = true) || titleText.contains("Completed", ignoreCase = true) -> {
                icon = if (message.isFromMe) Icons.Filled.CallMade else Icons.Filled.CallReceived
                iconTint = com.mobile.superiorchat.theme.CallSuccess
            }
            titleText.contains("Missed", ignoreCase = true) -> {
                icon = Icons.Filled.CallMissed
                iconTint = com.mobile.superiorchat.theme.ErrorRed
            }
            titleText.contains("Declined", ignoreCase = true) -> {
                icon = Icons.Filled.CallEnd
                iconTint = com.mobile.superiorchat.theme.WarningAmber
            }
            titleText.contains("Cancelled", ignoreCase = true) || 
            titleText.contains("Unanswered", ignoreCase = true) || 
            titleText.contains("No Answer", ignoreCase = true) -> {
                icon = Icons.Filled.CallMade
                iconTint = com.mobile.superiorchat.theme.WarningAmber
            }
            titleText.contains("Error", ignoreCase = true) || 
            titleText.contains("Failed", ignoreCase = true) -> {
                icon = Icons.Filled.ErrorOutline
                iconTint = com.mobile.superiorchat.theme.ErrorRed
            }
            else -> {
                icon = Icons.Filled.Phone
                iconTint = PrimaryLight
            }
        }
        val naturalColor = Color.White.copy(alpha = 0.6f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable {
                        if (!isSelectionMode) {
                            onNavigateToCallHistory()
                        } else {
                            onSelectMessage(message)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Title line with vector icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = titleText,
                        color = naturalColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Duration line (below title, if present)
                if (!durationText.isNullOrEmpty()) {
                    Text(
                        text = durationText,
                        color = naturalColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Time line (below duration)
                Text(
                    text = timeString,
                    color = naturalColor.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        return
    }

    val verticalPadding = if (message.mediaType == "voice" || message.mediaType == "audio") 6.dp else 10.dp

    var swipeOffsetX by remember { androidx.compose.runtime.mutableStateOf(0f) }
    val animatedSwipeOffsetX by androidx.compose.animation.core.animateFloatAsState(targetValue = swipeOffsetX)
    val currentMessageState = androidx.compose.runtime.rememberUpdatedState(message)

    val primaryColor = MaterialTheme.colorScheme.primary
    val inversePrimaryColor = MaterialTheme.colorScheme.inversePrimary

    val selectionBgColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.15f)
                      else if (isHighlighted) (if (message.isFromMe) inversePrimaryColor.copy(alpha = 0.18f) else primaryColor.copy(alpha = 0.20f))
                      else Color.Transparent,
        animationSpec = tween(200),
        label = "selectionBg"
    )

    val highlightOverlayColor by animateColorAsState(
        targetValue = if (isHighlighted) (if (message.isFromMe) inversePrimaryColor.copy(alpha = 0.25f) else primaryColor.copy(alpha = 0.25f))
                      else Color.Transparent,
        animationSpec = tween(200),
        label = "highlightOverlay"
    )

    var isBubblePressed by remember { mutableStateOf(false) }
    var pressedCodeRange by remember { mutableStateOf<IntRange?>(null) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var bubbleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var textCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val pressOverlayColor by animateColorAsState(
        targetValue = if (isBubblePressed) (if (message.isFromMe) inversePrimaryColor.copy(alpha = 0.15f) else primaryColor.copy(alpha = 0.15f))
                      else Color.Transparent,
        animationSpec = tween(80),
        label = "pressOverlay"
    )

    fun findCodeAnnotationAt(bubbleOffset: androidx.compose.ui.geometry.Offset): AnnotatedString.Range<String>? {
        val bubbleCoords = bubbleCoordinates ?: return null
        val textCoords = textCoordinates ?: return null
        val lr = textLayoutResult ?: return null
        val rawText = message.text ?: return null
        if (rawText.isEmpty() || !bubbleCoords.isAttached || !textCoords.isAttached) return null
        val localOffset = textCoords.localPositionOf(bubbleCoords, bubbleOffset)
        if (localOffset.x < 0 || localOffset.x > textCoords.size.width ||
            localOffset.y < 0 || localOffset.y > textCoords.size.height) {
            return null
        }
        val charIndex = lr.getOffsetForPosition(localOffset)
        val dummyColor = Color.Transparent
        val parsed = parseMarkdown(rawText, dummyColor, dummyColor, dummyColor)
        val allAnnotations = parsed.getStringAnnotations(tag = "INLINE_CODE", start = 0, end = parsed.length)
        val codeAnn = allAnnotations.firstOrNull { charIndex >= it.start && charIndex < it.end } ?: return null

        val glyphBounds = lr.getBoundingBox(charIndex)
        if (localOffset.x < (glyphBounds.left - 2f) || localOffset.x > (glyphBounds.right + 2f) ||
            localOffset.y < (glyphBounds.top - 2f) || localOffset.y > (glyphBounds.bottom + 2f)) {
            return null
        }
        return codeAnn
    }


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
                .border(2.dp, if (isSelected) PrimaryLight else BubbleUnselectedBorder, CircleShape)
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
                    .drawBehind {
                        if (highlightOverlayColor != Color.Transparent) {
                            drawRect(highlightOverlayColor)
                        }
                        if (pressOverlayColor != Color.Transparent) {
                            drawRect(pressOverlayColor)
                        }
                    }
                    .widthIn(min = 60.dp, max = 260.dp)
                    .then(
                        if (isHighlighted) {
                            Modifier.border(2.dp, if (message.isFromMe) inversePrimaryColor else primaryColor, shape)
                        } else if (!message.isFromMe) {
                            Modifier.border(1.dp, DividerColor, shape)
                        } else Modifier
                    )
                    .onGloballyPositioned { bubbleCoordinates = it }
                    .pointerInput(isSelectionMode) {
                        detectTapGestures(
                            onPress = { offset ->
                                if (isSelectionMode) {
                                    isBubblePressed = true
                                } else {
                                    val codeAnn = findCodeAnnotationAt(offset)
                                    if (codeAnn != null) {
                                        pressedCodeRange = codeAnn.start until codeAnn.end
                                    } else {
                                        isBubblePressed = true
                                    }
                                }
                                try {
                                    awaitRelease()
                                } finally {
                                    isBubblePressed = false
                                    pressedCodeRange = null
                                }
                            },
                            onTap = { offset ->
                                if (isSelectionMode) {
                                    // In selection mode: toggle selection
                                    onSelectMessage(currentMessageState.value)
                                } else {
                                    val codeAnn = findCodeAnnotationAt(offset)
                                    if (codeAnn != null) {
                                        clipboardManager.setText(AnnotatedString(codeAnn.item))
                                        com.mobile.superiorchat.core.StatusFlow.reportStatus(
                                            com.mobile.superiorchat.core.SyncState.SUCCESS,
                                            "Text copied to clipboard"
                                        )
                                    } else {
                                        // Tap outside clears open popups (if any)
                                        if (viewModel.activePopupMessageId != null) {
                                            viewModel.hideContextMenu()
                                        }
                                    }
                                }
                            },
                            onDoubleTap = if (isSelectionMode) null else { _ ->
                                // Double tap: if already reacted, undo that specific reaction.
                                val msg = currentMessageState.value
                                val reactionData = com.mobile.superiorchat.data.entity.ReactionData.parse(msg.reactions)
                                val emojiToToggle = if (reactionData.me.isNotEmpty()) {
                                    reactionData.me.first()
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
                    },
                    onSaveClick = if (message.mediaType in listOf("photo", "video", "voice", "audio", "document") && com.mobile.superiorchat.media.LocalDirs.resolveFile(context, message.mediaLocalPath)?.exists() == true) {
                        { showSaveDialog = true }
                    } else null
                )
                Column {
                    if (message.replyToMessageId != null) {
                        // Reply stub — compact Telegram-style
                        Row(
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onReplyMessageClick(message.replyToMessageId) }
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
                                MarkdownText(
                                    text = repliedMessageText?.takeIf { it.isNotBlank() } ?: "📎 Attachment",
                                    color = if (message.isFromMe) Color.White else textColor.copy(alpha = 0.7f),
                                    isFromMe = message.isFromMe,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
                        MarkdownText(
                            text = message.text,
                            color = textColor,
                            modifier = Modifier
                                .padding(top = if (message.mediaType != null) 4.dp else 0.dp)
                                .onGloballyPositioned { textCoordinates = it },
                            isFromMe = message.isFromMe,
                            style = MaterialTheme.typography.bodyMedium,
                            pressedCodeRange = pressedCodeRange,
                            onTextLayout = { textLayoutResult = it }
                        )
                    }



                    // Reaction pill badges
                    val reactionData = com.mobile.superiorchat.data.entity.ReactionData.parse(message.reactions)
                    val allEmojis = reactionData.allReactions()
                    
                    androidx.compose.animation.AnimatedVisibility(
                        visible = allEmojis.isNotEmpty(),
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.wrapContentWidth().animateContentSize()
                            ) {
                                allEmojis.forEach { emoji ->
                                    val count = (if (reactionData.me.contains(emoji)) 1 else 0) + (if (reactionData.peer.contains(emoji)) 1 else 0)
                                    val isMe = reactionData.me.contains(emoji)
                                    val state = remember(emoji) { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
                                    androidx.compose.animation.AnimatedVisibility(
                                        visibleState = state,
                                        enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn()
                                    ) {
                                        val pillBgColor = when {
                                            isMe && message.isFromMe -> textColor.copy(alpha = 0.8f) // Dark inverted pill for strong highlight
                                            isMe && !message.isFromMe -> PrimaryLight.copy(alpha = 0.8f) // Light highlighted pill on dark bubble
                                            !isMe && message.isFromMe -> textColor.copy(alpha = 0.15f) // Subtle dark pill on light bubble
                                            else -> Color.White.copy(alpha = 0.05f) // Subtle light pill on dark bubble
                                        }
                                        val pillBorderColor = when {
                                            isMe && message.isFromMe -> textColor.copy(alpha = 0.9f)
                                            isMe && !message.isFromMe -> PrimaryLight
                                            !isMe && message.isFromMe -> textColor.copy(alpha = 0.2f)
                                            else -> Color.White.copy(alpha = 0.1f)
                                        }
                                        val countColor = when {
                                            isMe && message.isFromMe -> PrimaryLight // Inverted text color inside dark pill
                                            isMe && !message.isFromMe -> SurfaceLevel1
                                            else -> textColor.copy(alpha = 0.8f)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(pillBgColor)
                                                .border(1.dp, pillBorderColor, RoundedCornerShape(12.dp))
                                                .clickable { viewModel.sendReaction(message, emoji) }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(text = emoji, fontSize = 14.sp)
                                                if (count > 1) {
                                                    Text(text = count.toString(), fontSize = 12.sp, color = countColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                                }
                                            }
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
                if (isPinned) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(12.dp)
                    )
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
