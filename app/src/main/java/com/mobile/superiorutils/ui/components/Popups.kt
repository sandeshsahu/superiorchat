package com.mobile.superiorutils.ui.components

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import com.mobile.superiorutils.data.entity.MessageNode
import com.mobile.superiorutils.theme.*
import com.mobile.superiorutils.theme.PrimaryLight
@Composable
fun ErrorDialog(
    title: String = "Error",
    message: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Background)
                .border(1.dp, PrimaryLight.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFB4AB), // Material Error color
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryLight.copy(alpha = 0.2f),
                        contentColor = PrimaryLight
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(text = "OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ActionDialog(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = PrimaryLight,
    confirmText: String,
    dismissText: String = "Dismiss",
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(Background)
                .border(1.dp, PrimaryLight.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = dismissText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontWeight = FontWeight.SemiBold)
                    }
                    if (neutralText != null && onNeutral != null) {
                        TextButton(onClick = onNeutral) {
                            Text(text = neutralText, color = PrimaryLight, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryLight.copy(alpha = 0.2f),
                            contentColor = PrimaryLight
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(text = confirmText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GlobalDialogHandler(
    dialogState: com.mobile.superiorutils.ui.GlobalDialogState?,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    when (dialogState) {
        is com.mobile.superiorutils.ui.GlobalDialogState.PermissionPermanentlyDenied -> {
            ActionDialog(
                title = "Permission Permanently Denied",
                message = "This permission has been permanently denied. Please enable it in the App Settings.",
                confirmText = "Go to Settings",
                onConfirm = {
                    context.startActivity(dialogState.intent)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorutils.ui.GlobalDialogState.PartialMediaAccessPermanentlyDenied -> {
            ActionDialog(
                title = "Media Access Denied",
                message = "You have previously denied full access to your media. To allow full access or select more photos, please go to Settings.",
                confirmText = "Go to Settings",
                dismissText = "Not Now",
                onConfirm = {
                    onDismiss()
                    dialogState.onGoToSettings()
                },
                onDismiss = {
                    onDismiss()
                    dialogState.onContinue()
                }
            )
        }
        is com.mobile.superiorutils.ui.GlobalDialogState.ManageStorageRequired -> {
            ActionDialog(
                title = "All Files Access Required",
                message = "The file explorer requires full access to your device storage to view and attach documents.",
                confirmText = "Open Settings",
                onConfirm = {
                    context.startActivity(dialogState.intent)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorutils.ui.GlobalDialogState.PartialMediaAccess -> {
            ActionDialog(
                title = "Limited Access Granted",
                message = "You have granted limited access to your media. Would you like to grant full access so you can easily select any photo?",
                confirmText = "Grant Full Access",
                dismissText = "Not Now",
                onConfirm = {
                    onDismiss()
                    dialogState.onUpgrade()
                },
                onDismiss = {
                    onDismiss()
                    dialogState.onContinue()
                }
            )
        }
        is com.mobile.superiorutils.ui.GlobalDialogState.CameraPermissionRationale -> {
            ActionDialog(
                title = "Camera Permission",
                message = "We need access to your camera to take photos.",
                confirmText = "Agree",
                dismissText = "Cancel",
                onConfirm = {
                    onDismiss()
                    dialogState.onConfirm()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorutils.ui.GlobalDialogState.MicrophonePermissionRationale -> {
            ActionDialog(
                title = "Microphone Permission",
                message = "We need access to your microphone to record voice messages.",
                confirmText = "Agree",
                dismissText = "Cancel",
                onConfirm = {
                    onDismiss()
                    dialogState.onConfirm()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorutils.ui.GlobalDialogState.StoragePermissionRationale -> {
            ActionDialog(
                title = "Storage Permission",
                message = "We need access to your device storage to view and attach documents.",
                confirmText = "Agree",
                dismissText = "Cancel",
                onConfirm = {
                    onDismiss()
                    dialogState.onConfirm()
                },
                onDismiss = onDismiss
            )
        }
        null -> { /* No active dialog */ }
    }
}

@Composable
fun BlurredPopup(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Standard scrim
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DividerColor)
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    content()
                }
            }
        }
    }
}


@Composable
fun AddManuallyPopup(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var botToken by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }

    val isTokenValid = botToken.isBlank() || com.mobile.superiorutils.utils.Validator.isValidBotToken(botToken.trim())
    val isChatIdValid = chatId.isBlank() || com.mobile.superiorutils.utils.Validator.isValidChatId(chatId.trim())
    val canSave = botToken.isNotBlank() && chatId.isNotBlank() && isTokenValid && isChatIdValid

    BlurredPopup(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Add Manually",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bot Token Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isTokenValid) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bot Token", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isTokenValid,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chat ID Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isChatIdValid) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        placeholder = { Text("e.g. -1001234567890", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isChatIdValid,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (canSave) {
                        onSave(botToken.trim(), chatId.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isTokenValid || !isChatIdValid) ErrorRed else Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = botToken.isNotBlank() && chatId.isNotBlank()
            ) {
                Text(
                    text = if (!isTokenValid || !isChatIdValid) "Credentials invalid" else "Save Credentials",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}



@Composable
fun EditManuallyPopup(
    initialToken: String,
    initialChatId: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var botToken by remember { mutableStateOf(initialToken) }
    var chatId by remember { mutableStateOf(initialChatId) }
    var tokenVisible by remember { mutableStateOf(false) }

    val isTokenValid = botToken.isBlank() || com.mobile.superiorutils.utils.Validator.isValidBotToken(botToken.trim())
    val isChatIdValid = chatId.isBlank() || com.mobile.superiorutils.utils.Validator.isValidChatId(chatId.trim())
    val canSave = botToken.isNotBlank() && chatId.isNotBlank() && isTokenValid && isChatIdValid

    BlurredPopup(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Edit Manually",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bot Token Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isTokenValid) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bot Token", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isTokenValid,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Chat ID Field
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isChatIdValid) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        placeholder = { Text("e.g. -1001234567890", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Primary,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isChatIdValid,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (canSave) {
                        onSave(botToken.trim(), chatId.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isTokenValid || !isChatIdValid) ErrorRed else Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = botToken.isNotBlank() && chatId.isNotBlank()
            ) {
                Text(
                    text = if (!isTokenValid || !isChatIdValid) "Credentials invalid" else "Save Credentials",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

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
    onPinClick: () -> Unit = {}
) {
    val expanded = expandedProvider()
    val currentReactions = message.reactions
        ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    
    var isRendered by remember { mutableStateOf(expanded) }

    if (expanded && !isRendered) {
        isRendered = true
    }

    LaunchedEffect(expanded) {
        if (!expanded && isRendered) {
            kotlinx.coroutines.delay(80)
            isRendered = false
        }
    }

    if (isRendered) {
        androidx.compose.ui.window.Popup(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.PopupProperties(focusable = true)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = expanded,
                enter = androidx.compose.animation.scaleIn(
                    initialScale = 0.85f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f),
                    animationSpec = androidx.compose.animation.core.tween(80, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(80)),
                exit = androidx.compose.animation.scaleOut(
                    targetScale = 0.85f,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(80))
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
    var deleteForEveryone by remember { androidx.compose.runtime.mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(20.dp)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(top = 22.dp, start = 22.dp, end = 22.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Delete message?",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Are you sure you want to delete this message?",
                    color = Color(0xFFAAAAAA),
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                if (targetUserName != null && targetUserName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { deleteForEveryone = !deleteForEveryone },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = deleteForEveryone,
                            onCheckedChange = { deleteForEveryone = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = PrimaryLight,
                                uncheckedColor = Color(0xFFAAAAAA),
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Also delete for $targetUserName",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Fallback if we don't have a name
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { deleteForEveryone = !deleteForEveryone },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = deleteForEveryone,
                            onCheckedChange = { deleteForEveryone = it },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = PrimaryLight,
                                uncheckedColor = Color(0xFFAAAAAA),
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Also delete for everyone",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = InfoBlue,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                    TextButton(
                        onClick = {
                            if (deleteForEveryone) onConfirmDeleteForEveryone() else onConfirmDeleteForMe()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Delete",
                            color = ErrorRed,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
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
