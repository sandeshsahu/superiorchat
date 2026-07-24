package com.mobile.superiorchat.ui.components.popups

import androidx.compose.animation.*
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
    modifier: Modifier = Modifier
) {
    val syncState by StatusFlow.syncState.collectAsState()
    val syncMessage by StatusFlow.syncMessage.collectAsState()
    val activeTransfers by StatusFlow.activeTransfers.collectAsState()
    val overallProgress by StatusFlow.overallProgress.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AnimatedVisibility(
        visible = syncState != SyncState.IDLE,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        val uiConfig = syncState.toUIConfig(hasUploads = activeTransfers.any { it.isUpload })

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = uiConfig.bgColor,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    color = if (uiConfig.isTransfer) PrimaryLight.copy(alpha = 0.4f) else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
                .animateContentSize()
                .clickable(enabled = uiConfig.canExpand) {
                    isExpanded = !isExpanded
                },
            shadowElevation = 6.dp
        ) {
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
