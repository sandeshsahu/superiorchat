package com.mobile.superiorchat.ui.components.bubbles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.data.entity.MessageStatus
import com.mobile.superiorchat.ui.ChatViewModel
import com.mobile.superiorchat.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MediaBubble(
    message: MessageNode,
    viewModel: ChatViewModel,
    progress: Float,
    onMediaClick: (String, String) -> Unit,
    onMediaLongPressStart: (String, String) -> Unit,
    onMediaLongPressEnd: () -> Unit
) {
    val isPhoto = message.mediaType == "photo"
    val isVideo = message.mediaType == "video"
    val mediaLabel = if (isPhoto) "IMAGE" else "VIDEO"
    val isUploading = message.isFromMe && (message.status == MessageStatus.SENDING || message.status == MessageStatus.QUEUED)
    val isFailed = message.status == MessageStatus.FAILED
    val isDownloading = !message.isFromMe && message.status == MessageStatus.SENDING

    if (message.mediaLocalPath != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isPhoto) 300.dp else 180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isVideo) Color.Black.copy(alpha = 0.5f) else Color.Transparent)
                .pointerInput(isUploading) {
                    if (isUploading) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            val job = CoroutineScope(Dispatchers.Main).launch {
                                delay(150)
                                onMediaLongPressStart(message.mediaLocalPath, message.mediaType ?: "")
                            }
                            val success = tryAwaitRelease()
                            job.cancel()
                            onMediaLongPressEnd()
                            if (success && !job.isCompleted) {
                                if (isFailed && message.isFromMe) {
                                    viewModel.retryMessage(message)
                                } else {
                                    onMediaClick(message.mediaLocalPath, message.mediaType ?: "")
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(message.mediaLocalPath))
                    .apply {
                        if (isPhoto) {
                            size(600)
                            bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                        } else {
                            videoFrameMillis(1000)
                        }
                    }
                    .crossfade(true)
                    .build(),
                contentDescription = "$mediaLabel Thumbnail",
                modifier = Modifier
                    .fillMaxSize()
                    .heightIn(max = if (isPhoto) 300.dp else 180.dp),
                contentScale = ContentScale.Crop
            )

            if (isVideo) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }

            if (isUploading || (isFailed && message.isFromMe)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFailed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(36.dp))
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
                }
            } else if (isVideo && !isUploading && !isFailed) {
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
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                val totalSize = message.mediaFileSize ?: File(message.mediaLocalPath).length()
                val sizeText = if (totalSize > 0) " • ${FileUtils.formatFileSize(totalSize)}" else ""
                Text(if (isUploading) "UPLOADING$sizeText" else "$mediaLabel$sizeText", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isPhoto) 150.dp else 180.dp)
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
                    if (isVideo) {
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
                    } else {
                        Icon(imageVector = if (isFailed) Icons.Default.Refresh else Icons.Default.ArrowDownward, contentDescription = "Download Photo", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
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
                val sizeText = if (totalSize > 0) " • ${FileUtils.formatFileSize(totalSize)}" else ""
                Text("$mediaLabel$sizeText", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
