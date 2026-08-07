package com.mobile.superiorchat.ui.components.popups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.bounceClick

@Composable
fun BaseAppDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        isVisible = true
    }

    Dialog(
        onDismissRequest = {
            isVisible = false
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0.65f)
            dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 0.9f, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
            exit = scaleOut(targetScale = 0.9f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(24.dp)),
                color = SurfaceLevel1,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    content = content
                )
            }
        }
    }
}



@Composable
fun parseAnnotatedMessage(text: String, tint: Color = PrimaryLight, isWarning: Boolean = false): AnnotatedString {
    return buildAnnotatedString {
        val regex = "\\*\\*(.*?)\\*\\*|\\*(.*?)\\*".toRegex()
        var lastIndex = 0
        val results = regex.findAll(text)
        
        val highlightColor = if (isWarning) ErrorRed else tint
        val bgColor = if (isWarning) ErrorRed.copy(alpha = 0.15f) else (if (tint == PrimaryLight) SurfaceLevel2 else tint.copy(alpha = 0.15f))
        
        for (match in results) {
            // Append text before the match
            append(text.substring(lastIndex, match.range.first))
            
            // Append the highlighted text
            withStyle(style = SpanStyle(
                color = highlightColor,
                fontWeight = FontWeight.Bold,
                background = bgColor
            )) {
                val matchedText = match.groups[1]?.value ?: match.groups[2]?.value ?: ""
                append(matchedText)
            }
            lastIndex = match.range.last + 1
        }
        // Append remaining text
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun ErrorDialog(
    title: String = "Error",
    message: String,
    onDismiss: () -> Unit
) {
    BaseAppDialog(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ErrorRed,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = parseAnnotatedMessage(message),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            textAlign = TextAlign.Start,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed.copy(alpha = 0.15f),
                    contentColor = ErrorRed
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
            ) {
                Text(text = "OK", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun InfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        isVisible = true
    }

    Dialog(
        onDismissRequest = {
            isVisible = false
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0.65f)
            dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 0.9f, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
            exit = scaleOut(targetScale = 0.9f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(24.dp)),
                color = SurfaceLevel1,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = PrimaryLight,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = parseAnnotatedMessage(message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Start,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isVisible = false
                                onDismiss()
                            },
                            modifier = Modifier.height(40.dp),
                            colors = com.mobile.superiorchat.ui.components.luminaButtonColors(),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Got it",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionDialog(
    title: String,
    message: String,
    note: String? = null,
    noteIcon: androidx.compose.ui.graphics.vector.ImageVector? = Icons.Filled.Warning,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = PrimaryLight,
    confirmText: String,
    dismissText: String = "Dismiss",
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    autoDismiss: Boolean = true,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseAppDialog(onDismiss = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = iconTint,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = parseAnnotatedMessage(message, tint = iconTint),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            textAlign = TextAlign.Start,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (note != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(14.dp)) {
                    val annotatedNote = parseAnnotatedMessage(note, tint = iconTint, isWarning = true)
                    Text(
                        text = buildAnnotatedString {
                            if (noteIcon != null) {
                                appendInlineContent("note_icon", "[icon]")
                                append(" ")
                            }
                            append(annotatedNote)
                        },
                        inlineContent = if (noteIcon != null) mapOf(
                            "note_icon" to InlineTextContent(
                                Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)
                            ) {
                                Icon(noteIcon, null, tint = iconTint, modifier = Modifier.fillMaxSize())
                            }
                        ) else emptyMap(),
                        color = iconTint.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (neutralText != null && onNeutral != null) {
                TextButton(
                    onClick = onNeutral,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(neutralText, color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(40.dp)
            ) {
                Text(dismissText, color = TextSecondary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = {
                    if (!isLoading && !isSuccess) {
                        onConfirm()
                        if (autoDismiss) {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (iconTint == PrimaryLight) PrimaryLight else iconTint.copy(alpha = 0.15f),
                    contentColor = if (iconTint == PrimaryLight) MaterialTheme.colorScheme.onPrimaryContainer else iconTint
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = if (iconTint == PrimaryLight) MaterialTheme.colorScheme.onPrimaryContainer else iconTint
                    )
                } else if (isSuccess) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(confirmText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun GlobalDialogHandler(
    dialogState: com.mobile.superiorchat.ui.GlobalDialogState?,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    when (dialogState) {
        is com.mobile.superiorchat.ui.GlobalDialogState.PermissionPermanentlyDenied -> {
            ActionDialog(
                title = "Permission Denied",
                message = "This permission has been permanently denied. Please enable it in the App Settings.",
                icon = Icons.Filled.Warning,
                iconTint = ErrorRed,
                confirmText = "Go to Settings",
                onConfirm = {
                    context.startActivity(dialogState.intent)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorchat.ui.GlobalDialogState.PartialMediaAccessPermanentlyDenied -> {
            ActionDialog(
                title = "Media Access Denied",
                message = "You have previously denied full access to your media. To allow full access or select more photos, please go to Settings.",
                icon = Icons.Filled.Warning,
                iconTint = ErrorRed,
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
        is com.mobile.superiorchat.ui.GlobalDialogState.ManageStorageRequired -> {
            ActionDialog(
                title = "All Files Access Required",
                message = "The file explorer requires full access to your device storage to view and attach documents.",
                icon = Icons.Filled.Info,
                confirmText = "Open Settings",
                onConfirm = {
                    context.startActivity(dialogState.intent)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorchat.ui.GlobalDialogState.PartialMediaAccess -> {
            ActionDialog(
                title = "Limited Access Granted",
                message = "You have granted limited access to your media. Would you like to grant full access so you can easily select any photo?",
                icon = Icons.Filled.Info,
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
        is com.mobile.superiorchat.ui.GlobalDialogState.CameraPermissionRationale -> {
            ActionDialog(
                title = "Camera Permission",
                message = "We need access to your camera to take photos.",
                icon = Icons.Filled.Info,
                confirmText = "Agree",
                dismissText = "Cancel",
                onConfirm = {
                    onDismiss()
                    dialogState.onConfirm()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorchat.ui.GlobalDialogState.MicrophonePermissionRationale -> {
            ActionDialog(
                title = "Microphone Permission",
                message = "We need access to your microphone to record voice messages.",
                icon = Icons.Filled.Info,
                confirmText = "Agree",
                dismissText = "Cancel",
                onConfirm = {
                    onDismiss()
                    dialogState.onConfirm()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorchat.ui.GlobalDialogState.StoragePermissionRationale -> {
            ActionDialog(
                title = "Storage Permission",
                message = "We need access to your device storage to view and attach documents.",
                icon = Icons.Filled.Info,
                confirmText = "Agree",
                dismissText = "Cancel",
                onConfirm = {
                    onDismiss()
                    dialogState.onConfirm()
                },
                onDismiss = onDismiss
            )
        }
        is com.mobile.superiorchat.ui.GlobalDialogState.CallPermissionRationale -> {
            ActionDialog(
                title = "Camera & Microphone Required",
                message = "We need access to both your camera and microphone to initiate the secure WebRTC call.",
                icon = Icons.Filled.Info,
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
fun CredentialsPopup(
    initialToken: String = "",
    initialChatId: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var botToken by remember { mutableStateOf(initialToken) }
    var chatId by remember { mutableStateOf(initialChatId) }
    var tokenVisible by remember { mutableStateOf(false) }

    val isTokenValid by remember(botToken) { derivedStateOf { botToken.isBlank() || com.mobile.superiorchat.utils.Validator.isValidBotToken(botToken.trim()) } }
    val isChatIdValid by remember(chatId) { derivedStateOf { chatId.isBlank() || com.mobile.superiorchat.utils.Validator.isValidChatId(chatId.trim()) } }
    val canSave by remember(botToken, chatId, isTokenValid, isChatIdValid) { derivedStateOf { botToken.isNotBlank() && chatId.isNotBlank() && isTokenValid && isChatIdValid } }

    val title = if (initialToken.isNotBlank()) "Edit Credentials" else "Add Credentials"

    BlurredPopup(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = PrimaryLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            
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
                        placeholder = { Text("e.g. 1234567890", color = TextSecondary, fontSize = 13.sp) },
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
                    containerColor = if (!isTokenValid || !isChatIdValid) ErrorRed else PrimaryLight,
                    contentColor = if (!isTokenValid || !isChatIdValid) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = PrimaryLight.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
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
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceLevel2,
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun WebRtcConfigPopup(
    initialUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fallbackUrls = remember { context.resources.getStringArray(com.mobile.superiorchat.R.array.webrtc_fallback_urls).toList() }
    val defaultUrl = fallbackUrls.firstOrNull() ?: ""
    var baseUrl by remember { 
        mutableStateOf(if (fallbackUrls.contains(initialUrl) || initialUrl.isEmpty()) "" else initialUrl) 
    }
    val isValid by remember(baseUrl) { 
        derivedStateOf { 
            com.mobile.superiorchat.utils.Validator.isValidWebRtcUrl(baseUrl)
        } 
    }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BlurredPopup(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "WebRTC Server",
                color = PrimaryLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (!isValid) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Base URL", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { 
                            baseUrl = it
                            errorMessage = null 
                        },
                        placeholder = { Text("https://yourdomain.com", color = TextSecondary, fontSize = 13.sp) },
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
                        isError = !isValid,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Current: $initialUrl",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instructions:\n• Enter only the Base URL (e.g., https://your-server.com)\n• Do NOT include /#join= or /#host=\n• Ensure your server is accessible publicly",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        if (isValid && !isLoading) {
                            val finalUrl = if (baseUrl.isBlank()) defaultUrl else baseUrl.trim()
                            
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                
                                val isValidServer = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val url = java.net.URL("$finalUrl/call.html")
                                        val connection = url.openConnection() as java.net.HttpURLConnection
                                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                                        connection.connectTimeout = 3000
                                        connection.readTimeout = 3000
                                        connection.requestMethod = "GET"
                                        connection.connect()
                                        
                                        if (connection.responseCode == 200) {
                                            val html = connection.inputStream.bufferedReader().use { it.readText() }
                                            if (html.contains("<title>Superiorchat Connect</title>") || html.contains("id=\"ui-layer\"")) {
                                                true
                                            } else {
                                                errorMessage = "Invalid server format. call.html not found."
                                                false
                                            }
                                        } else {
                                            errorMessage = "Server returned error ${connection.responseCode}"
                                            false
                                        }
                                    } catch (e: Exception) {
                                        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                                        val activeNetwork = cm.activeNetwork
                                        val capabilities = activeNetwork?.let { cm.getNetworkCapabilities(it) }
                                        val hasInternet = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                                        
                                        if (!hasInternet) {
                                            errorMessage = "No internet connection detected."
                                        } else {
                                            errorMessage = "Failed to connect to the server."
                                        }
                                        false
                                    }
                                }
                                
                                isLoading = false
                                if (isValidServer) {
                                    onSave(finalUrl)
                                }
                            }
                        }
                    }
                    .background(if (!isValid) ErrorRed else PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (!isValid) "Invalid URL" else "Save Settings",
                        color = if (!isValid) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cancel Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        if (!isLoading) onDismiss()
                    }
                    .background(Color.Transparent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun FakeCrashDialog(
    onBypass: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val appName = androidx.compose.ui.res.stringResource(id = com.mobile.superiorchat.R.string.app_name)

    var isPressed by remember { mutableStateOf(false) }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(1800)
            onBypass()
        }
    }

    Dialog(
        onDismissRequest = { activity?.finishAffinity() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0.65f)
            dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = SurfaceLevel1,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // System Crash Title
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$appName keeps stopping",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { 
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    }
                                )
                            }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "A system error caused the application to stop responding.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Start,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { activity?.finishAffinity() },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(text = "Close app", color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun PinSetupPopup(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var errorMsg by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    BaseAppDialog(onDismiss = onDismiss) {
        Text(
            text = if (step == 1) "Set PIN" else "Confirm PIN",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Column {
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            OutlinedTextField(
                value = if (step == 1) pin else confirmPin,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        if (step == 1) pin = it else confirmPin = it
                        errorMsg = ""
                    }
                },
                visualTransformation = if (pinVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { pinVisible = !pinVisible }) {
                        Icon(
                            if (pinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, 
                            contentDescription = null, 
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true,
                placeholder = { Text("Enter 4-6 digits", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryLight,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (step == 1) {
                        if (pin.length >= 4) step = 2 else errorMsg = "PIN must be at least 4 digits"
                    } else {
                        if (pin == confirmPin) onSave(pin) else {
                            errorMsg = "PINs do not match"
                            confirmPin = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight.copy(alpha = 0.15f), contentColor = PrimaryLight),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(if (step == 1) "Next" else "Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PinVerifyPopup(errorMsg: String, onDismiss: () -> Unit, onVerify: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }

    BaseAppDialog(onDismiss = onDismiss) {
        Text(
            text = "Enter Current PIN",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Column {
            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            OutlinedTextField(
                value = pin,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        pin = it
                    }
                },
                visualTransformation = if (pinVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { pinVisible = !pinVisible }) {
                        Icon(
                            if (pinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, 
                            contentDescription = null, 
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                singleLine = true,
                placeholder = { Text("PIN", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryLight,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { if (pin.isNotEmpty()) onVerify(pin) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryLight.copy(alpha = 0.15f), contentColor = PrimaryLight),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Verify", fontWeight = FontWeight.Bold)
            }
        }
    }
}
