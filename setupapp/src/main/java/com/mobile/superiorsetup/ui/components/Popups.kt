package com.mobile.superiorsetup.ui.components

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.platform.LocalConfiguration
import com.mobile.superiorsetup.core.Validator
import com.mobile.superiorsetup.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BaseAppDialog(
    cancellable: Boolean = true,
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
            if (cancellable) {
                isVisible = false
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = cancellable,
            dismissOnClickOutside = cancellable,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        LaunchedEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0.65f)
            dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        val configuration = LocalConfiguration.current
        val maxDialogHeight = (configuration.screenHeightDp * 0.78f).dp

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 0.9f, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
            exit = scaleOut(targetScale = 0.9f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .heightIn(max = maxDialogHeight)
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
fun parseAnnotatedMessage(
    text: String,
    tint: Color = PrimaryLight,
    isWarning: Boolean = false
): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val regex = "(`.*?`)|(\\*\\*.*?\\*\\*)|(\\*.*?\\*)".toRegex()
        var lastIndex = 0
        val results = regex.findAll(text)

        val highlightColor = if (isWarning) ErrorRed else tint
        val bgColor = if (isWarning) ErrorRed.copy(alpha = 0.15f) else (if (tint == PrimaryLight) SurfaceLevel2 else tint.copy(alpha = 0.15f))

        for (match in results) {
            append(text.substring(lastIndex, match.range.first))
            val rawMatch = match.value
            val isCode = rawMatch.startsWith("`") && rawMatch.endsWith("`")
            val isBold = rawMatch.startsWith("**") && rawMatch.endsWith("**")

            val content = if (isCode) {
                rawMatch.removeSurrounding("`")
            } else if (isBold) {
                rawMatch.removeSurrounding("**")
            } else {
                rawMatch.removeSurrounding("*")
            }

            withStyle(
                style = androidx.compose.ui.text.SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isCode) androidx.compose.ui.text.font.FontFamily.Monospace else null,
                    background = bgColor
                )
            ) {
                append(content)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun ScrollableDialogBody(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    tint: Color = PrimaryLight,
    content: @Composable ColumnScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val canScrollDown by remember { derivedStateOf { scrollState.maxValue in 1 until Int.MAX_VALUE && scrollState.canScrollForward } }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            content = content
        )

        if (scrollState.maxValue in 1 until Int.MAX_VALUE) {
            androidx.compose.animation.AnimatedVisibility(
                visible = canScrollDown,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (tint == PrimaryLight) PrimaryLight else tint,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            coroutineScope.launch {
                                scrollState.animateScrollBy(250f)
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Scroll down for more",
                            tint = if (tint == PrimaryLight) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdaptiveDialogActions(
    modifier: Modifier = Modifier,
    confirmText: String? = null,
    confirmTint: Color = PrimaryLight,
    isConfirmLoading: Boolean = false,
    isConfirmSuccess: Boolean = false,
    isConfirmEnabled: Boolean = true,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        if (neutralText != null && onNeutral != null) {
            TextButton(
                onClick = onNeutral,
                modifier = Modifier.heightIn(min = 40.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = neutralText,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (dismissText != null && onDismiss != null) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 40.dp),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = dismissText,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (confirmText != null && onConfirm != null) {
            Button(
                onClick = {
                    if (!isConfirmLoading && !isConfirmSuccess) {
                        onConfirm()
                    }
                },
                enabled = isConfirmEnabled,
                modifier = Modifier.heightIn(min = 40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (confirmTint == PrimaryLight) PrimaryLight else confirmTint.copy(alpha = 0.15f),
                    contentColor = if (confirmTint == PrimaryLight) MaterialTheme.colorScheme.onPrimaryContainer else confirmTint,
                    disabledContainerColor = SurfaceLevel2,
                    disabledContentColor = TextSecondary
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
            ) {
                if (isConfirmLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = if (confirmTint == PrimaryLight) MaterialTheme.colorScheme.onPrimaryContainer else confirmTint
                    )
                } else if (isConfirmSuccess) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = confirmText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
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
    customContent: @Composable (() -> Unit)? = null,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    autoDismiss: Boolean = true,
    cancellable: Boolean = true,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BaseAppDialog(cancellable = cancellable, onDismiss = onDismiss) {
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

        ScrollableDialogBody(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            tint = iconTint
        ) {
            Text(
                text = parseAnnotatedMessage(message, tint = iconTint, isWarning = iconTint == ErrorRed),
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
                            text = androidx.compose.ui.text.buildAnnotatedString {
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

            if (customContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                customContent()
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        AdaptiveDialogActions(
            confirmText = confirmText,
            confirmTint = iconTint,
            isConfirmLoading = isLoading,
            isConfirmSuccess = isSuccess,
            onConfirm = {
                onConfirm()
                if (autoDismiss) {
                    onDismiss()
                }
            },
            dismissText = dismissText.takeIf { it.isNotEmpty() },
            onDismiss = onDismiss,
            neutralText = neutralText,
            onNeutral = onNeutral
        )
    }
}

@Composable
fun InfoDialog(
    title: String,
    message: String,
    note: String? = null,
    noteIcon: androidx.compose.ui.graphics.vector.ImageVector? = Icons.Filled.Info,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Info,
    iconTint: Color = PrimaryLight,
    customContent: @Composable (() -> Unit)? = null,
    extraButtonText: String? = null,
    onExtraButtonClick: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    BaseAppDialog(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
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

        ScrollableDialogBody(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            tint = iconTint
        ) {
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
                        val annotatedNote = parseAnnotatedMessage(note, tint = iconTint, isWarning = iconTint == ErrorRed)
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
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

            if (customContent != null) {
                Spacer(modifier = Modifier.height(16.dp))
                customContent()
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        AdaptiveDialogActions(
            confirmText = "Got it",
            confirmTint = iconTint,
            onConfirm = onDismiss,
            neutralText = extraButtonText,
            onNeutral = onExtraButtonClick
        )
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
                .background(Color.Black.copy(alpha = 0.5f))
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
    initialPartnerUsername: String = "",
    isPeerLinkEnabled: Boolean = false,
    isAdminMode: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var botToken by remember { mutableStateOf(initialToken) }
    var chatId by remember { mutableStateOf(initialChatId) }
    var partnerUsername by remember { mutableStateOf(initialPartnerUsername) }
    var tokenVisible by remember { mutableStateOf(false) }
    var isPartnerExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var tokenApiError by remember { mutableStateOf<String?>(null) }
    var chatApiError by remember { mutableStateOf<String?>(null) }
    var partnerApiError by remember { mutableStateOf<String?>(null) }

    val isGroupChat by remember(chatId) { derivedStateOf { chatId.trim().startsWith("-") } }

    val isTokenValid by remember(botToken) { derivedStateOf { botToken.isBlank() || Validator.isValidBotToken(botToken.trim()) } }
    val isChatIdValid by remember(chatId, isAdminMode) {
        derivedStateOf {
            if (chatId.isBlank()) true
            else if (isAdminMode) chatId.trim().startsWith("-") && Validator.isValidChatId(chatId.trim())
            else Validator.isValidChatId(chatId.trim())
        }
    }
    val isPartnerValid by remember(partnerUsername, isGroupChat, isPeerLinkEnabled, isAdminMode) {
        derivedStateOf {
            if (isAdminMode) {
                partnerUsername.isBlank() || Validator.isValidPartnerBotUsername(partnerUsername.trim(), isAdminMode = true)
            } else {
                !isPeerLinkEnabled || !isGroupChat || partnerUsername.isBlank() || Validator.isValidPartnerBotUsername(partnerUsername.trim(), isAdminMode = false)
            }
        }
    }

    val isAnyInvalid by remember(isTokenValid, isChatIdValid, isPartnerValid, tokenApiError, chatApiError, partnerApiError) {
        derivedStateOf { !isTokenValid || !isChatIdValid || !isPartnerValid || tokenApiError != null || chatApiError != null || partnerApiError != null }
    }
    val canSave by remember(botToken, chatId, partnerUsername, isAnyInvalid, isLoading, isAdminMode) { 
        derivedStateOf { 
            if (isAdminMode) {
                botToken.isNotBlank() && chatId.isNotBlank() && partnerUsername.isNotBlank() && !isAnyInvalid && !isLoading
            } else {
                botToken.isNotBlank() && chatId.isNotBlank() && !isAnyInvalid && !isLoading
            }
        } 
    }

    val title = if (isAdminMode) {
        if (initialToken.isNotBlank()) "Edit Admin Credentials" else "Add Admin Credentials"
    } else {
        if (initialToken.isNotBlank()) "Edit Credentials" else "Add Credentials"
    }

    BlurredPopup(onDismiss = { if (!isLoading) onDismiss() }) {
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
                border = BorderStroke(1.dp, if (!isTokenValid || tokenApiError != null) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAdminMode) "Admin Bot Token" else "Bot Token", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { 
                            botToken = it
                            tokenApiError = null
                            validationError = null
                        },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isTokenValid || tokenApiError != null,
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
                border = BorderStroke(1.dp, if (!isChatIdValid || chatApiError != null) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAdminMode) "Group Chat ID" else "Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { 
                            chatId = it
                            chatApiError = null
                            validationError = null
                        },
                        placeholder = { Text(if (isAdminMode) "e.g. -100123456789" else "e.g. 1234567890 or -100...", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isChatIdValid || chatApiError != null,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            
            if (isAdminMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = SurfaceLevel1,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (!isPartnerValid || partnerApiError != null) ErrorRed else DividerColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Partner Bot Username", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Required", color = PrimaryLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = partnerUsername,
                            onValueChange = { 
                                partnerUsername = it 
                                partnerApiError = null
                                validationError = null
                            },
                            placeholder = { Text("e.g. @partner_bot", color = TextSecondary, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = SurfaceLevel2,
                                focusedContainerColor = SurfaceLevel2,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = PrimaryLight,
                                unfocusedTextColor = TextPrimary,
                                focusedTextColor = TextPrimary,
                                errorBorderColor = ErrorRed
                            ),
                            isError = !isPartnerValid || partnerApiError != null,
                            shape = RoundedCornerShape(10.dp)
                        )
                        if (!isPartnerValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Must start with @ and end with 'bot' (e.g. @partner_bot)",
                                color = ErrorRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (validationError != null) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(validationError!!, color = ErrorRed, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                } else {
                    Surface(
                        color = PrimaryLight.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryLight.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = PrimaryLight,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Admin Configuration Guide",
                                    color = PrimaryLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Bot Token: Your own side bot's token (not partner's)\n• Group Chat ID: Same group ID as set on partner's app\n• Partner Username: Your partner's bot @username",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            } else if (isPeerLinkEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                
                // Slidable Divider Header "App To App Support"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isPartnerExpanded = !isPartnerExpanded }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = DividerColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "App To App Support",
                            color = if (isPartnerExpanded) PrimaryLight else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            if (isPartnerExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isPartnerExpanded) PrimaryLight else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = DividerColor
                    )
                }
                
                AnimatedVisibility(
                    visible = isPartnerExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!isGroupChat) {
                            Surface(
                                color = SurfaceLevel1,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, DividerColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = PrimaryLight,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = parseAnnotatedMessage("Direct 1-on-1 **User ID** detected. Partner bot filtering is not applicable for direct user chats. Saving will switch the app to **Direct DM** mode.", tint = PrimaryLight),
                                        color = PrimaryLight,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = SurfaceLevel1,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, if (!isPartnerValid || partnerApiError != null) ErrorRed else DividerColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Partner Bot Username", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("Optional", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = partnerUsername,
                                        onValueChange = { 
                                            partnerUsername = it 
                                            partnerApiError = null
                                            validationError = null
                                        },
                                        placeholder = { Text("e.g. @partner_bot", color = TextSecondary, fontSize = 13.sp) },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedContainerColor = SurfaceLevel2,
                                            focusedContainerColor = SurfaceLevel2,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = PrimaryLight,
                                            unfocusedTextColor = TextPrimary,
                                            focusedTextColor = TextPrimary,
                                            errorBorderColor = ErrorRed
                                        ),
                                        isError = !isPartnerValid || partnerApiError != null,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    if (!isPartnerValid) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Must start with @ (e.g. @bot_username)",
                                            color = ErrorRed,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isAdminMode && validationError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = ErrorRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(validationError!!, color = ErrorRed, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (canSave && !isLoading) {
                        isLoading = true
                        validationError = null
                        tokenApiError = null
                        chatApiError = null
                        partnerApiError = null
                        scope.launch(Dispatchers.IO) {
                            val trimmedToken = botToken.trim()
                            val trimmedChat = chatId.trim()
                            val trimmedPartner = partnerUsername.trim()

                            val tokenRes = Validator.verifyBotToken(trimmedToken)
                            if (tokenRes is Validator.ValidationResult.Error) {
                                withContext(Dispatchers.Main) {
                                    tokenApiError = tokenRes.message
                                    validationError = tokenRes.message
                                    isLoading = false
                                }
                                return@launch
                            }
                            val botUser = (tokenRes as Validator.ValidationResult.Success).data

                            val chatRes = Validator.verifyChatId(
                                token = trimmedToken,
                                chatId = trimmedChat,
                                botUser = botUser,
                                isPeerLinkEnabled = isPeerLinkEnabled || isAdminMode,
                                requireGroupOnly = isAdminMode
                            )
                            if (chatRes is Validator.ValidationResult.Error) {
                                withContext(Dispatchers.Main) {
                                    chatApiError = chatRes.message
                                    validationError = chatRes.message
                                    isLoading = false
                                }
                                return@launch
                            }

                            if (isAdminMode && trimmedPartner.isBlank()) {
                                withContext(Dispatchers.Main) {
                                    partnerApiError = "Partner Bot Username is required in Admin mode."
                                    validationError = "Partner Bot Username is required in Admin mode."
                                    isLoading = false
                                }
                                return@launch
                            }

                            val partnerRes = Validator.verifyPartnerUsername(
                                partnerUsername = trimmedPartner,
                                botUser = botUser,
                                isPeerLinkEnabled = isPeerLinkEnabled || isAdminMode,
                                isGroup = isGroupChat,
                                isAdminMode = isAdminMode
                            )
                            if (partnerRes is Validator.ValidationResult.Error) {
                                withContext(Dispatchers.Main) {
                                    partnerApiError = partnerRes.message
                                    validationError = partnerRes.message
                                    isLoading = false
                                }
                                return@launch
                            }

                            withContext(Dispatchers.Main) {
                                isLoading = false
                                val finalPartner = if (isAdminMode || (isGroupChat && isPeerLinkEnabled)) trimmedPartner else ""
                                onSave(trimmedToken, trimmedChat, finalPartner)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAnyInvalid) ErrorRed else PrimaryLight,
                    contentColor = if (isAnyInvalid) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = if (isAnyInvalid) ErrorRed.copy(alpha = 0.22f) else SurfaceLevel2,
                    disabledContentColor = if (isAnyInvalid) ErrorRed else TextSecondary
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = canSave
            ) {
                if (isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PrimaryLight,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = if (isAnyInvalid) "Credentials Invalid" else "Save Credentials",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceLevel2,
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AddManuallyPopup(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    CredentialsPopup(
        initialToken = "",
        initialChatId = "",
        initialPartnerUsername = "",
        isPeerLinkEnabled = false,
        isAdminMode = false,
        onDismiss = onDismiss,
        onSave = { token, chat, _ -> onSave(token, chat) }
    )
}

@Composable
fun QrDisplayInfoDialog(
    title: String,
    label: String?,
    pin: String,
    onDismiss: () -> Unit
) {
    val isAdmin = title.contains("Admin", ignoreCase = true) || label?.contains("Admin", ignoreCase = true) == true
    val isAppToApp = label?.contains("app-to-app", ignoreCase = true) == true || title.contains("app-to-app", ignoreCase = true)

    val (dialogTitle, baseMessage) = when {
        isAdmin -> {
            PopupTexts.Admin.ADMIN_QR_INFO_TITLE to PopupTexts.Admin.ADMIN_QR_INFO_MESSAGE
        }
        isAppToApp -> {
            PopupTexts.Admin.PARTNER_APP_TO_APP_QR_INFO_TITLE to PopupTexts.Admin.PARTNER_APP_TO_APP_QR_INFO_MESSAGE
        }
        else -> {
            PopupTexts.Admin.PARTNER_TELEGRAM_QR_INFO_TITLE to PopupTexts.Admin.PARTNER_TELEGRAM_QR_INFO_MESSAGE
        }
    }

    val pinSection = if (pin.isNotEmpty()) {
        "\n\n• **Security PIN**:\nProtected with 4-digit PIN (`$pin`). The scanning device must enter this PIN to decrypt and apply the configuration."
    } else {
        "\n\n• **Direct Encryption**:\nEncrypted directly with AES-GCM. Scanning this code will instantly decrypt and apply settings without requiring a PIN."
    }

    ActionDialog(
        title = dialogTitle,
        message = baseMessage + pinSection,
        icon = androidx.compose.material.icons.Icons.Filled.Info,
        iconTint = PrimaryLight,
        confirmText = "Understood",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun PinInfoPopup(hasPin: Boolean, onDismiss: () -> Unit) {
    QrDisplayInfoDialog(title = "", label = null, pin = if (hasPin) "PIN" else "", onDismiss = onDismiss)
}

@Composable
fun RequirePinInfoDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = "Require PIN Setting",
        message = "When **Require PIN** is enabled:\n- Setup QR codes will be encrypted using a random *4-Digit PIN*.\n- Scanning devices must enter the PIN to decrypt and apply the configured settings.\n\nWhen **Require PIN** is disabled:\n- Setup QR codes will be encrypted directly without a two-step PIN.\n- Scanning devices can decrypt and apply the configured settings instantly.",
        icon = androidx.compose.material.icons.Icons.Filled.Info,
        iconTint = PrimaryLight,
        confirmText = "Understood",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun DisplayQrPopup(
    payloadJson: String,
    pin: String,
    title: String = "Scan Configuration",
    label: String? = null,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showInfo by remember { mutableStateOf(false) }
    var showFullscreen by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    // payloadJson is already encrypted in AdminScreens.kt using encryptAESWithPin
    LaunchedEffect(payloadJson, label) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            qrBitmap = com.mobile.superiorsetup.core.QrManager.generateQrCode(payloadJson, label = label, size = 1024)
        }
    }

    if (showInfo) {
        QrDisplayInfoDialog(title = title, label = label, pin = pin, onDismiss = { showInfo = false })
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
        val activity = view.context as? Activity
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        // Auto screen brightness boost during QR viewing
        DisposableEffect(activity) {
            val originalBrightness = activity?.window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                win.attributes = lp
            }
            onDispose {
                activity?.window?.let { win ->
                    val lp = win.attributes
                    lp.screenBrightness = originalBrightness
                    win.attributes = lp
                }
            }
        }

        LaunchedEffect(dialogWindow) {
            dialogWindow?.setDimAmount(0.7f)
            dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
            dialogWindow?.attributes = dialogWindow?.attributes?.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 0.9f, animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
            exit = scaleOut(targetScale = 0.9f, animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(24.dp)),
                color = SurfaceLevel1,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, DividerColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(title, color = PrimaryLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showInfo = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Info, contentDescription = "Info", tint = PrimaryLight)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // High-contrast clean white QR Card with exact bitmap aspect ratio
                    val qrAspectRatio = if (!label.isNullOrBlank()) (1024f / 1146f) else 1f
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(qrAspectRatio)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showFullscreen = true },
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "QR Code (Tap to Enlarge)",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                CircularProgressIndicator(color = PrimaryLight)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    // Tap to enlarge hint
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showFullscreen = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tap QR to expand fullscreen", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Save to Gallery
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                qrBitmap?.let { bmp ->
                                    try {
                                        android.provider.MediaStore.Images.Media.insertImage(context.contentResolver, bmp, java.util.UUID.randomUUID().toString(), null)
                                        android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed to save", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .background(PrimaryLight, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.SaveAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Gallery", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (pin.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .bounceClick(scaleDown = 0.95f) {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(pin))
                                    android.widget.Toast.makeText(context, "PIN Copied", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .background(SurfaceLevel2, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Key, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy $pin PIN", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Share QR
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                qrBitmap?.let { bmp ->
                                    try {
                                        val cachePath = java.io.File(context.cacheDir, "images")
                                        cachePath.mkdirs()
                                        val file = java.io.File(cachePath, "${java.util.UUID.randomUUID()}.png")
                                        val stream = java.io.FileOutputStream(file)
                                        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                                        stream.close()
                                        
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                             putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share Configuration"))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Failed to share", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .background(SurfaceLevel2, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share QR", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                isVisible = false
                                onDismiss()
                            }
                            .background(Color.Transparent, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Close", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // ── Fullscreen Immersive QR Dialog ─────────────────────────────────────
    if (showFullscreen && qrBitmap != null) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            val view = LocalView.current
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window
            LaunchedEffect(dialogWindow) {
                dialogWindow?.setDimAmount(0.9f)
                dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
                dialogWindow?.attributes = dialogWindow?.attributes?.apply {
                    screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                }
            }

            val qrAspectRatio = if (!label.isNullOrBlank()) (1024f / 1146f) else 1f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(horizontal = 10.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(qrAspectRatio),
                        shadowElevation = 12.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "Fullscreen QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (pin.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = SurfaceLevel1,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DividerColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Security PIN: ", color = TextSecondary, fontSize = 14.sp)
                                Text(pin, color = PrimaryLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Hold scanning phone 1–2 feet away",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Centered Bottom Close Button
                    IconButton(
                        onClick = { showFullscreen = false },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
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
    var baseUrl by remember { mutableStateOf(initialUrl) }
    var isCheckingUrl by remember { mutableStateOf(false) }
    var showNetworkError by remember { mutableStateOf(false) }
    
    val isValidUrl = com.mobile.superiorsetup.core.Validator.isValidWebRtcUrl(baseUrl)

    BaseAppDialog(onDismiss = onDismiss) {
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
                border = BorderStroke(1.dp, if (!isValidUrl) ErrorRed else DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Base URL", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { 
                            baseUrl = it 
                            showNetworkError = false
                        },
                        placeholder = { Text("https://yourdomain.com", color = TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = SurfaceLevel2,
                            focusedContainerColor = SurfaceLevel2,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = PrimaryLight,
                            unfocusedTextColor = TextPrimary,
                            focusedTextColor = TextPrimary,
                            errorBorderColor = ErrorRed
                        ),
                        isError = !isValidUrl,
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
                    
                    if (showNetworkError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parseAnnotatedMessage("Could not verify server. Ensure webserver is running and has *call.html* configured correctly.", tint = ErrorRed, isWarning = true),
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = parseAnnotatedMessage(
                            "**Instructions**:\n• Read `Deployment.md` for all server instructions.\n• Enter only the **Base URL** (e.g., `https://your-server.com`)\n• Do *not* include `/#join=` or `/#host=`\n• Ensure your server is accessible publicly.",
                            tint = PrimaryLight
                        ),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        if (isValidUrl && !isCheckingUrl && baseUrl.isNotEmpty()) {
                            isCheckingUrl = true
                            showNetworkError = false
                            
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                val success = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val finalBaseUrl = baseUrl.trim().removeSuffix("/")
                                        val url = java.net.URL("$finalBaseUrl/call.html")
                                        val connection = url.openConnection() as java.net.HttpURLConnection
                                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                                        connection.connectTimeout = 5000
                                        connection.readTimeout = 5000
                                        connection.requestMethod = "GET"
                                        connection.connect()
                                        
                                        if (connection.responseCode == 200) {
                                            val html = connection.inputStream.bufferedReader().use { it.readText() }
                                            html.contains("<title>Superiorchat Connect</title>") || 
                                            html.contains("id=\"ui-layer\"")
                                        } else {
                                            false
                                        }
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                                
                                isCheckingUrl = false
                                if (success) {
                                    onSave(baseUrl.trim().removeSuffix("/"))
                                } else {
                                    showNetworkError = true
                                }
                            }
                        } else if (baseUrl.isEmpty()) {
                            onSave("") // Empty saves as empty immediately
                        }
                    }
                    .background(if (!isValidUrl) ErrorRed else PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isCheckingUrl) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (!isValidUrl) "Invalid URL" else "Save Settings",
                        color = if (!isValidUrl) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
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
                        onDismiss()
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
fun PinEntryDialog(
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BaseAppDialog(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Enter PIN",
                color = PrimaryLight,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = parseAnnotatedMessage("This QR code is protected. Please enter the **4-digit PIN** to proceed.", tint = PrimaryLight),
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Surface(
                color = SurfaceLevel1,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security PIN", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("4-digit PIN", color = TextSecondary, fontSize = 13.sp) },
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
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide PIN" else "Show PIN",
                                    tint = TextSecondary
                                )
                            }
                        }
                    )
                    
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        if (pin.length >= 4 && !isVerifying) {
                            scope.launch {
                                isVerifying = true
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                    onSubmit(pin)
                                }
                                isVerifying = false
                            }
                        }
                    }
                    .background(if (pin.length >= 4 && !isVerifying) PrimaryLight else SurfaceLevel2, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Unlock & Continue",
                        color = if (pin.length >= 4) MaterialTheme.colorScheme.onPrimaryContainer else TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        onDismiss()
                    }
                    .background(SurfaceLevel2, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
