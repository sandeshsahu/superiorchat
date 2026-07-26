package com.mobile.superiorchat.ui.components.popups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.data.entity.MessageNode
import com.mobile.superiorchat.theme.*

@Composable
fun MessageContextMenu(
    expandedProvider: () -> Boolean,
    message: MessageNode,
    sortedEmojis: List<String>,
    onDismiss: () -> Unit,
    onReact: (String) -> Unit,
    onReplyClick: () -> Unit,
    onCopyClick: () -> Unit,
    onEditClick: () -> Unit,
    onSelectClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isPinned: Boolean = false,
    onPinClick: () -> Unit = {},
    onSaveClick: (() -> Unit)? = null
) {
    val expanded = expandedProvider()
    val currentReactions = com.mobile.superiorchat.data.entity.ReactionData.parse(message.reactions).me
    
    val transitionState = remember { androidx.compose.animation.core.MutableTransitionState(expanded) }
    transitionState.targetState = expanded

    if (transitionState.currentState || transitionState.targetState) {
        androidx.compose.ui.window.Popup(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visibleState = transitionState,
                enter = androidx.compose.animation.scaleIn(
                    initialScale = 0.85f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f),
                    animationSpec = androidx.compose.animation.core.tween(120, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(120)),
                exit = androidx.compose.animation.scaleOut(
                    targetScale = 0.85f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f),
                    animationSpec = androidx.compose.animation.core.tween(100)
                ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100))
            ) {
                Column(
                    modifier = Modifier.widthIn(min = 200.dp, max = 260.dp)
                ) {
                    // ── Emoji quick-react strip (Detached Pill) ──
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortedEmojis.forEachIndexed { index, emoji ->
                        val isSelected = currentReactions.contains(emoji)
                        val isLastUsed = index == 0
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> PrimaryLight.copy(alpha = 0.28f)
                                        isLastUsed -> Color.White.copy(alpha = 0.08f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onReact(emoji); onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Actions menu (Detached Block) ──
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .widthIn(min = 150.dp, max = 200.dp)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
            ) {
                Column {
                    // Reply
                    ContextMenuItem(
                        text = "Reply",
                        icon = Icons.AutoMirrored.Filled.Reply,
                        onClick = { onReplyClick(); onDismiss() }
                    )

                    // Copy
                    ContextMenuItem(
                        text = "Copy",
                        icon = Icons.Filled.ContentCopy,
                        onClick = { onCopyClick(); onDismiss() }
                    )

                    // Save
                    if (onSaveClick != null) {
                        ContextMenuItem(
                            text = "Save",
                            icon = Icons.Filled.Download,
                            onClick = { onSaveClick(); onDismiss() }
                        )
                    }

                    // Edit (Only for own text messages)
                    if (message.isFromMe && !message.text.isNullOrBlank()) {
                        ContextMenuItem(
                            text = "Edit",
                            icon = Icons.Filled.Edit,
                            onClick = { onEditClick(); onDismiss() }
                        )
                    }

                    // Select
                    ContextMenuItem(
                        text = "Select",
                        icon = Icons.Filled.CheckBox,
                        onClick = { onSelectClick(); onDismiss() }
                    )

                    // Pin
                    ContextMenuItem(
                        text = if (isPinned) "Unpin" else "Pin",
                        icon = Icons.Filled.Lock,
                        onClick = { onPinClick(); onDismiss() }
                    )

                    // Delete
                    ContextMenuItem(
                        text = "Delete",
                        icon = Icons.Filled.Delete,
                        textColor = ErrorRed,
                        iconColor = ErrorRed,
                        onClick = { onDeleteClick(); onDismiss() }
                    )
                }
            }
        }
    }
    }
}
}

@Composable
fun ContextMenuItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color = Color.White,
    iconColor: Color = Color(0xFF8E8E93),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun DeleteWarningDialog(
    onDismiss: () -> Unit,
    targetUserName: String? = null,
    onConfirmDeleteForEveryone: () -> Unit,
    onConfirmDeleteForMe: () -> Unit
) {
    var deleteForEveryone by remember { mutableStateOf(false) }
    
    BaseAppDialog(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = ErrorRed,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Delete message?",
                style = MaterialTheme.typography.titleLarge,
                color = ErrorRed,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Are you sure you want to delete this message?",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { deleteForEveryone = !deleteForEveryone },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = deleteForEveryone,
                onCheckedChange = { deleteForEveryone = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryLight,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = Color.Black
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (targetUserName != null && targetUserName.isNotBlank()) "Also delete for $targetUserName" else "Also delete for everyone",
                color = TextPrimary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    if (deleteForEveryone) onConfirmDeleteForEveryone() else onConfirmDeleteForMe()
                },
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.15f),
                    contentColor = ErrorRed
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Delete",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun SelectionActionBar(
    selectedCount: Int,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A1A),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel / Close Button
            IconButton(onClick = onCancelSelection) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Cancel selection",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Selection count — center-weighted
            Text(
                text = "$selectedCount selected",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )

            // Delete action
            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete selected",
                    tint = if (selectedCount > 0) ErrorRed else ErrorRed.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

val QUICK_REACTIONS = listOf("👍", "❤️", "🤣", "😱", "😢", "🔥")

@Composable
fun EmojiReactionTray(
    message: com.mobile.superiorchat.data.entity.MessageNode,
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
            val currentReactions = com.mobile.superiorchat.data.entity.ReactionData.parse(message.reactions).me
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
                                if (isSelected) com.mobile.superiorchat.theme.PrimaryLight.copy(alpha = 0.25f)
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
fun ClearChatWarningDialog(
    onDismiss: () -> Unit,
    onConfirmClear: (Boolean) -> Unit
) {
    var deleteMedia by remember { mutableStateOf(false) }

    BaseAppDialog(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Warning",
                tint = ErrorRed,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Clear Chat History",
                style = MaterialTheme.typography.titleLarge,
                color = ErrorRed,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This will permanently delete all messages from your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = ErrorRed.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Note: Due to Telegram limitations, messages will not be deleted for the other person.",
                color = ErrorRed.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { deleteMedia = !deleteMedia },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = deleteMedia,
                onCheckedChange = { deleteMedia = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryLight,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = Color.Black
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Also delete media from device",
                color = TextPrimary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    onConfirmClear(deleteMedia)
                    onDismiss()
                },
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.15f),
                    contentColor = ErrorRed
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "Clear",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
