package com.mobile.superiorchat.ui.components.bubbles

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.entity.MessageStatus
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.ui.ChatViewModel
import com.mobile.superiorchat.ui.components.popups.ActionDialog
import com.mobile.superiorchat.utils.AppLog
import com.mobile.superiorchat.utils.FileUtils
import com.mobile.superiorchat.utils.LogCategory
import com.mobile.superiorchat.utils.LogLevel
import java.io.File
import java.util.Locale

@Composable
fun DocumentBubble(
    message: MessageNode,
    viewModel: ChatViewModel,
    progress: Float,
    textColor: Color
) {
    val context = LocalContext.current
    var showApkInstallDialog by remember { mutableStateOf(false) }

    if (showApkInstallDialog) {
        ActionDialog(
            title = "Installation Permission Required",
            message = "To install this app, you need to allow SuperiorChat to install unknown apps.",
            icon = Icons.Filled.Warning,
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

    val isUploading = message.isFromMe && message.status == MessageStatus.SENDING
    val isQueued = message.isFromMe && message.status == MessageStatus.QUEUED
    val isFailed = message.status == MessageStatus.FAILED
    val isDownloading = !message.isFromMe && message.status == MessageStatus.SENDING

    val resolvedFile = com.mobile.superiorchat.media.LocalDirs.resolveFile(context, message.mediaLocalPath)
    if (resolvedFile != null && resolvedFile.exists()) {
        val file = resolvedFile
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
                .clickable(enabled = !isUploading && !isQueued) {
                    if (isFailed) {
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
            if (isQueued) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = textColor,
                        strokeWidth = 2.dp
                    )
                }
            } else if (isUploading) {
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
            } else if (isFailed) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry Document Upload",
                    tint = textColor,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = FileUtils.resolveFileIcon(displayName),
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
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                val ext = displayName.substringAfterLast(".", "").uppercase(Locale.ROOT)
                val extPrefix = if (ext.isNotEmpty()) "$ext • " else ""
                Text(
                    text = if (isQueued) {
                        "${extPrefix}Preparing..."
                    } else if (isUploading) {
                        val uploaded = (progress * totalSize).toLong()
                        "${extPrefix}${FileUtils.formatFileSize(uploaded)} / ${FileUtils.formatFileSize(totalSize)}"
                    } else if (isFailed) {
                        "${extPrefix}Upload failed. Tap to retry."
                    } else {
                        "${extPrefix}${FileUtils.formatFileSize(totalSize)}"
                    },
                    color = if (isQueued) PrimaryLight else textColor.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        val displayName = message.mediaFileName ?: "Document"
        val totalSize = message.mediaFileSize ?: 0L
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (message.isFromMe) Color.White.copy(alpha = 0.1f) else SurfaceLevel2)
                .clickable(enabled = !isDownloading && !isQueued) { if (!isDownloading && !isQueued) viewModel.retryDownload(message) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isQueued) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = textColor,
                        strokeWidth = 2.dp
                    )
                }
            } else if (isDownloading) {
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
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                val ext = displayName.substringAfterLast(".", "").uppercase(Locale.ROOT)
                val extPrefix = if (ext.isNotEmpty()) "$ext • " else ""
                Text(
                    text = if (isQueued) {
                        "${extPrefix}Preparing..."
                    } else if (isDownloading && totalSize > 0L) {
                        val downloaded = (progress * totalSize).toLong()
                        "${extPrefix}${FileUtils.formatFileSize(downloaded)} / ${FileUtils.formatFileSize(totalSize)}"
                    } else if (isFailed) {
                        "${extPrefix}Failed"
                    } else if (totalSize > 0L) {
                        "${extPrefix}${FileUtils.formatFileSize(totalSize)}"
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
}
