package com.mobile.superiorutils.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.data.entity.MessageStatus
import com.mobile.superiorutils.theme.DividerColor
import com.mobile.superiorutils.theme.PrimaryLight
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

@Composable
fun ChatBubble(
    message: MessageNode,
    viewModel: ChatViewModel,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isFromMe) PrimaryLight else SurfaceLevel1
    val textColor = if (message.isFromMe) Color(0xFF1000A9) else Color(0xFFE2E2E2)
    val shape = if (message.isFromMe) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }
    
    val glowModifier = if (message.isFromMe) {
        Modifier.drawBehind {
            drawRoundRect(
                color = PrimaryLight.copy(alpha = 0.5f),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
            )
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#C0C1FF")
                    setShadowLayer(30f, 0f, 10f, android.graphics.Color.parseColor("#80C0C1FF"))
                }
                drawRoundRect(0f, 0f, size.width, size.height, 20.dp.toPx(), 20.dp.toPx(), paint)
            }
        }
    } else {
        Modifier
    }

    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = sdf.format(Date(message.timestamp))

    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .then(glowModifier)
                .clip(shape)
                .background(bgColor)
                .then(if (!message.isFromMe) Modifier.border(1.dp, DividerColor, shape) else Modifier)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.mediaType == "photo" && message.mediaLocalPath != null) {
                AsyncImage(
                    model = File(message.mediaLocalPath),
                    contentDescription = "Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(message.mediaLocalPath) },
                    contentScale = ContentScale.Crop
                )
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
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = if (isFailed) Icons.Default.Refresh else Icons.Default.ArrowDownward, contentDescription = "Download Photo", tint = Color.White, modifier = Modifier.size(36.dp))
                            if (isFailed) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Download failed", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (message.mediaType == "voice" || message.mediaType == "audio") {
                AudioMessage(
                    mediaLocalPath = message.mediaLocalPath,
                    mediaUrl = message.mediaUrl,
                    mediaType = message.mediaType,
                    status = message.status,
                    isFromMe = message.isFromMe,
                    onDownloadClick = { viewModel.retryDownload(message) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else if (message.mediaType == "video" && message.mediaLocalPath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    val file = File(message.mediaLocalPath)
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    setDataAndType(uri, "video/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                AppLog.log(LogCategory.SYSTEM, "Failed to play video: ${e.message}", LogLevel.ERROR)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
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
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("VIDEO", color = Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Downloading...", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
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
                        Text("VIDEO", color = Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (message.mediaType == "document" && message.mediaLocalPath != null) {
                val file = File(message.mediaLocalPath)
                val kbSize = file.length() / 1024
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (message.isFromMe) Color.White.copy(alpha = 0.1f) else SurfaceLevel2)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    setDataAndType(uri, "*/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                AppLog.log(LogCategory.SYSTEM, "Failed to open document: ${e.message}", LogLevel.ERROR)
                            }
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Document File",
                        tint = textColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            color = textColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$kbSize KB",
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (message.mediaType == "document") {
                val isDownloading = message.status == MessageStatus.SENDING
                val isFailed = message.status == MessageStatus.FAILED
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
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = textColor, strokeWidth = 2.dp)
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
                            text = "Document",
                            color = textColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDownloading) "Downloading..." else if (isFailed) "Failed" else "Tap to download",
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (!message.text.isNullOrEmpty()) {
                Text(text = message.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(text = timeString, color = textColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                if (message.isFromMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    when (message.status) {
                        MessageStatus.SENDING -> Icon(imageVector = Icons.Outlined.Schedule, contentDescription = "Sending", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        MessageStatus.SENT -> Icon(imageVector = Icons.Default.Done, contentDescription = "Sent", tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                        MessageStatus.QUEUED -> Icon(imageVector = Icons.Outlined.Schedule, contentDescription = "Queued", tint = textColor.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        MessageStatus.FAILED -> {
                            IconButton(
                                onClick = { viewModel.retryMessage(message) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
