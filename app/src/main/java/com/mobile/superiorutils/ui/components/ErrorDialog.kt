package com.mobile.superiorutils.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.mobile.superiorutils.theme.Background
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
