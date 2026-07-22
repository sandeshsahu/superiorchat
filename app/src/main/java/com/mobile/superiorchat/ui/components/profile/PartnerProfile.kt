package com.mobile.superiorchat.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.theme.TextPrimary
import com.mobile.superiorchat.theme.TextSecondary
import java.io.File

import androidx.compose.foundation.clickable
import com.mobile.superiorchat.data.entity.UserProfile

@Composable
fun PartnerProfile(
    userProfile: UserProfile?,
    onImageClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val prefs = AppGraph.prefs
    val profilePath = userProfile?.profilePhotoPath ?: ""
    val title = userProfile?.title?.ifEmpty { "Unknown" } ?: "Unknown"
    val username = userProfile?.username ?: ""
    val type = userProfile?.type ?: "private"
    val chatId = userProfile?.chatId ?: prefs.chatId
    val bio = userProfile?.bio ?: ""
    val inviteLink = userProfile?.inviteLink ?: ""
    val hasProtectedContent = userProfile?.hasProtectedContent ?: false
    val isForum = userProfile?.isForum ?: false

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceLevel1)
                .border(1.dp, SurfaceLevel2, RoundedCornerShape(24.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Close button top-right
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(SurfaceLevel2, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Profile Image or Fallback
                if (profilePath.isNotEmpty() && File(profilePath).exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(profilePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, PrimaryLight, CircleShape)
                            .clickable { onImageClick(profilePath) },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(SurfaceLevel2)
                            .border(2.dp, PrimaryLight.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (type == "group" || type == "supergroup") {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Group",
                                tint = PrimaryLight,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            // Initials
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
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Username
                if (username.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@$username",
                        color = PrimaryLight,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (bio.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = bio,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Details Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceLevel2)
                        .padding(16.dp)
                ) {
                    DetailRow(label = "Chat ID", value = chatId)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "Type", value = type.replaceFirstChar { it.uppercase() }.ifEmpty { "Private" })
                    
                    if (isForum) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailRow(label = "Forum", value = "Yes")
                    }
                    if (hasProtectedContent) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailRow(label = "Protected Content", value = "Yes")
                    }
                    if (inviteLink.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailRow(label = "Invite Link", value = "Available")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
