package com.mobile.superiorsetup.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
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
import com.mobile.superiorsetup.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BaseAppDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
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
fun parseAnnotatedMessage(text: String, tint: Color = PrimaryLight): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val regex = "\\*\\*(.*?)\\*\\*|\\*(.*?)\\*".toRegex()
        var lastIndex = 0
        val results = regex.findAll(text)
        
        for (match in results) {
            append(text.substring(lastIndex, match.range.first))
            withStyle(style = androidx.compose.ui.text.SpanStyle(
                color = tint,
                fontWeight = FontWeight.Bold,
                background = if (tint == PrimaryLight) SurfaceLevel2 else tint.copy(alpha = 0.15f)
            )) {
                val matchedText = match.groups[1]?.value ?: match.groups[2]?.value ?: ""
                append(" $matchedText ")
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun ActionDialog(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = PrimaryLight,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
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
        
        Spacer(modifier = Modifier.height(28.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dismissText.isNotEmpty()) {
                Text(
                    text = dismissText,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (iconTint == PrimaryLight) PrimaryLight else iconTint.copy(alpha = 0.15f),
                    contentColor = if (iconTint == PrimaryLight) MaterialTheme.colorScheme.onPrimaryContainer else iconTint
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
            ) {
                Text(text = confirmText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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

    val isTokenValid = botToken.isBlank() || com.mobile.superiorsetup.core.Validator.isValidBotToken(botToken.trim())
    val isChatIdValid = chatId.isBlank() || com.mobile.superiorsetup.core.Validator.isValidChatId(chatId.trim())
    val canSave = botToken.isNotBlank() && chatId.isNotBlank() && isTokenValid && isChatIdValid

    BaseAppDialog(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Add Manually",
                color = PrimaryLight,
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
                        Icon(Icons.Filled.Key, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bot Token", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        placeholder = { Text("e.g. 1234567890:AAH...", color = TextSecondary, fontSize = 13.sp) },
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
                        isError = !isTokenValid,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(if (tokenVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        },
                        singleLine = true
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
                        Icon(Icons.Filled.Chat, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chat ID", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        placeholder = { Text("e.g. 1234567890", color = TextSecondary, fontSize = 13.sp) },
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
                        isError = !isChatIdValid,
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        if (canSave) {
                            onSave(botToken.trim(), chatId.trim())
                        }
                    }
                    .glow(color = if (!isTokenValid || !isChatIdValid) Color.Transparent else PrimaryLight, radius = 20f, dx = 0f, dy = 10f, cornerRadius = 16.dp)
                    .background(if (!isTokenValid || !isChatIdValid) ErrorRed else PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!isTokenValid || !isChatIdValid) "Credentials invalid" else "Save Credentials",
                    color = if (!isTokenValid || !isChatIdValid) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) { onDismiss() }
                    .background(Color.Transparent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun DisplayQrPopup(
    payloadJson: String,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    LaunchedEffect(payloadJson) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val encrypted = com.mobile.superiorsetup.core.Security.encryptAES(payloadJson)
            qrBitmap = com.mobile.superiorsetup.core.QrManager.generateQrCode(encrypted)
        }
    }

    BaseAppDialog(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Scan Configuration", color = PrimaryLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (qrBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap!!.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(240.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryLight)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        qrBitmap?.let { bmp ->
                            try {
                                android.provider.MediaStore.Images.Media.insertImage(context.contentResolver, bmp, "SuperiorChat_Config", null)
                                android.widget.Toast.makeText(context, "Saved to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Failed to save", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .background(PrimaryLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SaveAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Gallery", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) {
                        qrBitmap?.let { bmp ->
                            try {
                                val cachePath = java.io.File(context.cacheDir, "images")
                                cachePath.mkdirs()
                                val file = java.io.File(cachePath, "qr_config.png")
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
                    .background(SurfaceLevel2, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share QR", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceClick(scaleDown = 0.95f) { onDismiss() }
                    .background(Color.Transparent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Close", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
                            text = "Could not verify server. Ensure webserver is runing and has call.html configured correctly",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instructions:\n Read Deployment.md for all Instructions.\n• Enter only the Base URL (e.g., https://your-server.com)\n• Do NOT include /#join= or /#host=\n• Ensure your server is accessible publicly",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
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
