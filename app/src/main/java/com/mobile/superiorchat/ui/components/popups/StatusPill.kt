package com.mobile.superiorchat.ui.components.popups

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobile.superiorchat.core.ActiveTransfer
import com.mobile.superiorchat.core.StatusFlow
import com.mobile.superiorchat.core.SyncState
import com.mobile.superiorchat.media.MediaSync
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.core.call.CallManager
import com.mobile.superiorchat.core.call.CallState
import java.io.File

/**
 * UI Configuration for the StatusPill, completely decoupling state mapping from the composable rendering.
 */
private data class SyncUIConfig(
    val bgColor: Color,
    val textColor: Color,
    val icon: ImageVector? = null,
    val isLoading: Boolean = false,
    val isTransfer: Boolean = false,
    val canExpand: Boolean = false
)

/**
 * Exhaustive mapping of all SyncState values to their respective UI configurations.
 * If a new state is added to SyncState, the compiler will require this when-block to be updated.
 */
private fun SyncState.toUIConfig(hasUploads: Boolean): SyncUIConfig {
    return when (this) {
        SyncState.IDLE -> SyncUIConfig(
            bgColor = SurfaceLevel1,
            textColor = Color.White
        )
        SyncState.SYNCING_PROFILE,
        SyncState.SYNCING_MESSAGES -> SyncUIConfig(
            bgColor = PrimaryLight,
            textColor = Color(0xFF1000A9),
            isLoading = true
        )
        SyncState.TRANSFERRING -> SyncUIConfig(
            bgColor = Color(0xFF1E1E24),
            textColor = Color.White,
            icon = if (hasUploads) Icons.Filled.FileUpload else Icons.Filled.FileDownload,
            isTransfer = true,
            canExpand = true
        )
        SyncState.SUCCESS -> SyncUIConfig(
            bgColor = PrimaryLight,
            textColor = Color(0xFF1000A9),
            icon = Icons.Filled.CheckCircle
        )
        SyncState.ERROR,
        SyncState.OFFLINE,
        SyncState.AUTH_ERROR -> SyncUIConfig(
            bgColor = Color(0xFF690005),
            textColor = Color.White,
            icon = Icons.Filled.Error
        )
    }
}

@Composable
fun StatusPill(
    modifier: Modifier = Modifier,
    isCallMinimized: Boolean = false,
    onRestoreCall: () -> Unit = {}
) {
    val syncState by StatusFlow.syncState.collectAsState()
    val syncMessage by StatusFlow.syncMessage.collectAsState()
    val activeTransfers by StatusFlow.activeTransfers.collectAsState()
    val overallProgress by StatusFlow.overallProgress.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val callState by CallManager.callState.collectAsState()
    val callDuration by CallManager.callDuration.collectAsState()
    
    val isCallActive = callState == CallState.ACTIVE || callState == CallState.CONNECTING
    val showCallUi = isCallActive && isCallMinimized
    val showSyncUi = syncState != SyncState.IDLE && !showCallUi // Hide sync if call pill is showing
    
    val isVisible = showCallUi || showSyncUi

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it - 50 }) + fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)),
        exit = slideOutVertically(targetOffsetY = { -it - 50 }) + fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)),
        modifier = modifier
    ) {
        val uiConfig = if (showSyncUi) {
            syncState.toUIConfig(hasUploads = activeTransfers.any { it.isUpload })
        } else {
            SyncState.IDLE.toUIConfig(false)
        }

        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = if (showCallUi) Color.Black else uiConfig.bgColor,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = if (showCallUi) Color.White.copy(alpha = 0.15f) 
                            else if (showSyncUi && uiConfig.isTransfer) PrimaryLight.copy(alpha = 0.4f) 
                            else Color.Transparent,
                    shape = RoundedCornerShape(percent = 50)
                )
                .animateContentSize(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                .clickable(enabled = showCallUi || (showSyncUi && uiConfig.canExpand)) {
                    if (showCallUi) onRestoreCall()
                    else isExpanded = !isExpanded
                },
            shadowElevation = 12.dp
        ) {
            AnimatedContent(
                targetState = showCallUi,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "pill_content"
            ) { isCall ->
                if (isCall) {
                    // Beautiful Call UI
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "pulse"
                        )
                        
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = null,
                            tint = if (callState == CallState.ACTIVE) Color(0xFF34D399).copy(alpha = pulseAlpha) else Color(0xFFFBBF24).copy(alpha = pulseAlpha),
                            modifier = Modifier.size(16.dp)
                        )
                        
                        val text = if (callState == CallState.CONNECTING) "Connecting" else {
                            val mins = (callDuration / 60).toString().padStart(2, '0')
                            val secs = (callDuration % 60).toString().padStart(2, '0')
                            "$mins:$secs"
                        }
                        
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.widthIn(min = 42.dp)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30)) // Apple-like red
                                .clickable { CallManager.endCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    // Sync UI
                    Column(
                        modifier = if (uiConfig.isTransfer) {
                            Modifier
                                .widthIn(min = 220.dp, max = 340.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        } else {
                            Modifier
                                .wrapContentWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        }
                    ) {
                        Row(
                            modifier = if (uiConfig.isTransfer) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiConfig.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = uiConfig.textColor,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else if (uiConfig.icon != null) {
                                Icon(
                                    imageVector = uiConfig.icon,
                                    contentDescription = null,
                                    tint = if (uiConfig.isTransfer) PrimaryLight else uiConfig.textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
        
                            Text(
                                text = syncMessage ?: "",
                                color = uiConfig.textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = if (uiConfig.isTransfer) Modifier.weight(1f) else Modifier.wrapContentWidth()
                            )
        
                            if (uiConfig.canExpand) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand transfers",
                                    tint = PrimaryLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
        
                        if (uiConfig.isTransfer) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = overallProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = PrimaryLight,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    if (isExpanded && activeTransfers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        val mediaCount = activeTransfers.count { it.mediaType == "photo" || it.mediaType == "video" }
                        val docCount = activeTransfers.count { it.mediaType == "document" }
                        val audioCount = activeTransfers.count { it.mediaType == "audio" || it.mediaType == "voice" }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (mediaCount > 0) CategoryPill("$mediaCount Media", Color(0xFF4CAF50))
                            if (docCount > 0) CategoryPill("$docCount Document${if (docCount > 1) "s" else ""}", Color(0xFF2196F3))
                            if (audioCount > 0) CategoryPill("$audioCount Audio", Color(0xFFFF9800))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeTransfers.forEach { transfer ->
                                ActiveTransferItemRow(
                                    transfer = transfer,
                                    onCancel = {
                                        MediaSync.cancelTransfer(context, transfer.messageId)
                                    }
                                )
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

@Composable
private fun CategoryPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun ActiveTransferItemRow(
    transfer: ActiveTransfer,
    onCancel: () -> Unit
) {
    val progress by transfer.progressFlow.collectAsState()
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val hasLocalFile = !transfer.localPath.isNullOrBlank() && File(transfer.localPath).exists()
        val isMedia = transfer.mediaType == "photo" || transfer.mediaType == "video"

        if (isMedia && hasLocalFile) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(transfer.localPath!!))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        } else {
            val icon = when (transfer.mediaType) {
                "photo" -> Icons.Default.Image
                "video" -> Icons.Default.Videocam
                "voice", "audio" -> Icons.Default.Audiotrack
                else -> Icons.Default.InsertDriveFile
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transfer.fileName ?: if (transfer.isUpload) "Uploading..." else "Downloading...",
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = PrimaryLight,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel transfer",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
