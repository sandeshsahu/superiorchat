package com.mobile.superiorchat.ui.components.popups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.bot.TelegramApi
import com.mobile.superiorchat.bot.User
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.utils.Validator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Administrative domain texts and constants.
 */
object AdminTexts {
    const val ADMIN_MODE_ACTIVE_TITLE = "Admin Mode Active"
    const val ADMIN_MODE_ACTIVE_MESSAGE =
        "You currently have *I am Admin* turned on.\n\nPlease configure your bot credentials inside the *Admin Settings* screen instead. Turn off Admin Mode if you want to configure regular client credentials."

    const val PEERLINK_INFO_TITLE = "PeerLink Configuration"
    const val PEERLINK_INFO_MESSAGE =
        "PeerLink enables direct app-to-app communication between you and your partner using Telegram's bot-to-bot group routing.\n\n" +
        "• **Read Only**:\nLocks all administrative settings and prevents accidental modification or disabling of PeerLink routing.\n\n" +
        "• **Route Messages**:\nInforms the app that the partner (Admin) will chat directly through the Superior Chat app rather than standard Telegram bot DMs. When enabled, it also unlocks the *Partner Bot Username* field in normal App Settings credentials so the client app can filter and accept messages exclusively from your bot."

    const val LIFECYCLE_INFO_TITLE = "Lifecycle & Task Management"
    const val LIFECYCLE_INFO_MESSAGE =
        "Controls application background behavior, task execution, and system-level visibility.\n\n" +
        "• **Hide from Recent**:\nHides Superior Chat from Android's Recent Apps / Overview screen when the application is closed or minimized, ensuring zero trace of multitasking activity. Reopen anytime via dialer code or secret shortcut.\n\n" +
        "• **Background Calls**:\nKeeps calls and audio active in the background when the app is minimized or the screen is locked. When video is enabled, automatically transitions into an OS-level Picture-in-Picture (PiP) floating window.\n\n" +
        "• **Call Ringing**:\nPlays system ringtone, vibrates, and shows high-priority heads-up banner with Accept and Decline actions for incoming calls in Admin mode."

    const val CALL_RINGING_INFO_TITLE = "Call Ringing"
    const val CALL_RINGING_INFO_MESSAGE =
        "Enables incoming call heads-up notifications and continuous phone ringing for Admin mode.\n\n" +
        "• **Continuous Ringing**:\nWhen an incoming call is received from your partner in the background, your device will play the system ringtone and vibrate rhythmically until answered, declined, or timed out.\n\n" +
        "• **Interactive Actions**:\nShows a high-priority heads-up banner with *Accept* and *Decline* buttons. Tapping Accept instantly connects to the live WebRTC call.\n\n" +
        "• **Silent Rejection**:\nTapping Decline automatically rejects the call and notifies your partner without opening the application."


    const val I_AM_ADMIN_INFO_TITLE = "I Am Admin (User B)"
    const val I_AM_ADMIN_INFO_MESSAGE =
        "This section is strictly for **User B (Admin)** who wants to chat and call directly inside this application while connected to their partner.\n\n" +
        "• **I will chat here**:\nUnlocks the dedicated *Credentials* card below. When enabled, credentials cannot be entered in regular App Settings—they must be configured in the Admin Credentials card below.\n\n" +
        "• **Dual Bot Bridge**:\nOutgoing messages and calls are dispatched via your own Admin bot directly to the shared private group, where your partner's bot receives them securely."

    const val APP_TO_APP_GUIDE_TITLE = "App-to-App Setup Guide"
    const val APP_TO_APP_GUIDE_MESSAGE =
        "To chat directly on this app, two bots communicate inside a private Telegram group:\n\n" +
        "1. **Create Two Bots**:\nCreate a bot for yourself and another for your partner via @BotFather.\n" +
        "2. **Enable Bot-to-Bot Communication**:\nIn @BotFather, Click on **Open** button then select your bot -> Bot Settings -> **Enable** the **Bot-to-Bot Communication Mode** and do same for partner's bot. This is required.\n" +
        "3. **Create Private Group**:\nCreate a private Telegram group and add *Both Bots* to it.\n" +
        "4. **Configure This Device (Admin)**:\nEnter your *Credentials* and *Partner's Bot Username* here only.\n" +
        "5. **Configure Partner Device**:\nOn your partner's app, enable *Route Messages*"
    const val APP_TO_APP_GUIDE_NOTE =
        "Setup App QR Shortcut: Generate a setup QR code in the Setup App and scan it on your partner's device to configure everything automatically!"

    const val CREDENTIALS_INFO_TITLE = "Admin Credentials Setup"
    const val CREDENTIALS_INFO_MESSAGE =
        "Configure your own Admin bot credentials to establish a secure link with your partner:\n\n" +
        "• **Bot Token**:\nThe token of your *own* dedicated bot created via @BotFather (must be a separate bot from your partner's bot).\n\n" +
        "• **Group Chat ID**:\nThe private Telegram group ID connecting both bots (e.g., -100...). Both bots must be members and promoted to **Administrators** with full permissions.\n\n" +
        "• **Partner Bot's Username**:\nThe `@username` of your *partner's* bot (not your own bot). This acts as an intruder shield, ensuring the app strictly drops messages from anyone else in the group and only accepts messages from your partner."

    const val DISABLE_ROUTE_MESSAGES_WARNING_TITLE = "Disable Route Messages?"
    const val DISABLE_ROUTE_MESSAGES_WARNING_MESSAGE =
        "Disabling Route Messages will switch the application back to standard direct 1-on-1 mode.\n\n" +
        "• The *Partner Bot Username* filter will be cleared.\n" +
        "• If *Admin Mode* is active on this device, it will be turned off and Admin credentials will be cleared.\n\n" +
        "Are you sure you want to proceed?"

    const val DISABLE_ADMIN_MODE_WARNING_TITLE = "Disable Admin Mode?"
    const val DISABLE_ADMIN_MODE_WARNING_MESSAGE =
        "Disabling Admin Mode indicates this device will no longer act as the Admin (User B).\n\n" +
        "• Your Admin Bot credentials and Partner Bot Username will be removed from this device.\n" +
        "• Normal App Settings will unlock for standard client configuration.\n\n" +
        "Are you sure you want to proceed?"

    const val PIP_REQUIRED_TITLE = "Picture-in-Picture Required"
    const val PIP_REQUIRED_MESSAGE =
        "*Background Calls* requires *Picture-in-Picture* permission to keep calls alive when the app is minimized.\n\n" +
        "• Without PiP, active calls will drop when you leave the app\n" +
        "• Enable PiP directly on the *App Info* page\n\n" +
        "• Go to *App Info* Page\n" +
        "• Find *Picture-in-Picture* and enable it."
    const val PIP_REQUIRED_CONFIRM = "App Info"

    const val NOTIFICATION_REQUIRED_TITLE = "Notification Permission Required"
    const val NOTIFICATION_REQUIRED_MESSAGE =
        "*Call Ringing* requires *Notification* permission to show incoming call heads-up alerts and ring your device.\n\n" +
        "• Without notifications, incoming calls cannot alert or ring your device in the background\n" +
        "• Enable Notifications in Android Settings to activate Call Ringing."
    const val NOTIFICATION_REQUIRED_CONFIRM = "Open Settings"
}

/**
 * Administrative domain dialogs, including the PeerLink multi-step configuration wizard,
 * role verification diagnostics, and administrative feature info dialogs.
 */

@Composable
fun SettingsAdminModeActiveDialog(
    onDismiss: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    ActionDialog(
        title = AdminTexts.ADMIN_MODE_ACTIVE_TITLE,
        message = AdminTexts.ADMIN_MODE_ACTIVE_MESSAGE,
        icon = Icons.Default.AdminPanelSettings,
        iconTint = ErrorRed,
        confirmText = "Go to Settings",
        onConfirm = {
            onNavigateToAdmin()
            onDismiss()
        },
        dismissText = "Dismiss",
        onDismiss = onDismiss
    )
}

@Composable
fun PeerLinkSetupFlowDialog(
    initialBotToken: String,
    initialGroupChatId: String,
    initialPartnerUsername: String,
    onComplete: (botToken: String, groupChatId: String, partnerUsername: String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }

    var botToken by remember { mutableStateOf(initialBotToken) }
    var groupChatId by remember { mutableStateOf(initialGroupChatId) }
    var partnerUsername by remember { mutableStateOf(initialPartnerUsername) }

    // Step 1 states
    var step1Loading by remember { mutableStateOf(false) }
    var step1Error by remember { mutableStateOf<String?>(null) }
    var step1VerifiedBot by remember { mutableStateOf<User?>(null) }

    // Step 2 states
    var step2Loading by remember { mutableStateOf(false) }
    var step2Error by remember { mutableStateOf<String?>(null) }
    var step2VerifiedGroup by remember { mutableStateOf<String?>(null) }

    // Step 3 states
    var step3Error by remember { mutableStateOf<String?>(null) }

    val steps = listOf(
        DialogStep(
            title = "Admin Bot Token",
            message = "Enter the Telegram Bot Token for your Admin bot. This bot routes messages and syncs state.",
            icon = Icons.Filled.SmartToy,
            iconTint = PrimaryLight,
            confirmText = "Next",
            isConfirmEnabled = botToken.isNotBlank() && !step1Loading,
            isConfirmLoading = step1Loading,
            onConfirmClick = {
                step1Loading = true
                step1Error = null
                scope.launch(Dispatchers.IO) {
                    val result = Validator.verifyBotToken(botToken)
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Validator.ValidationResult.Success -> {
                                step1VerifiedBot = result.data
                                step1Loading = false
                                currentStep = 1
                            }
                            is Validator.ValidationResult.Error -> {
                                step1Error = result.message
                                step1Loading = false
                            }
                        }
                    }
                }
            },
            customContent = {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(100)
                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                }
                Column {
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = {
                            botToken = it
                            step1Error = null
                            step1VerifiedBot = null
                        },
                        placeholder = { Text("123456789:ABCdefGHIjklMNOpqrSTUvwxYZ", color = TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        isError = step1Error != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLight,
                            unfocusedBorderColor = DividerColor,
                            errorBorderColor = ErrorRed,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryLight
                        )
                    )

                    if (step1Error != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ErrorRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(step1Error!!, color = ErrorRed, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    if (step1VerifiedBot != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = PrimaryLight.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PrimaryLight.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Verified: @${step1VerifiedBot?.username ?: step1VerifiedBot?.first_name}",
                                    color = PrimaryLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        ),
        DialogStep(
            title = "Group Chat ID",
            message = "Enter the ID of the private Telegram Group connecting both bots. Admin Bot must be in this group with Admin rights.",
            icon = Icons.Filled.Groups,
            iconTint = PrimaryLight,
            confirmText = "Next",
            isConfirmEnabled = groupChatId.isNotBlank() && !step2Loading,
            isConfirmLoading = step2Loading,
            onConfirmClick = {
                val botUser = step1VerifiedBot ?: return@DialogStep
                step2Loading = true
                step2Error = null
                scope.launch(Dispatchers.IO) {
                    val result = Validator.verifyChatId(
                        token = botToken,
                        chatId = groupChatId,
                        botUser = botUser,
                        isPeerLinkEnabled = true,
                        requireGroupOnly = true
                    )
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is Validator.ValidationResult.Success -> {
                                step2VerifiedGroup = result.data
                                step2Loading = false
                                currentStep = 2
                            }
                            is Validator.ValidationResult.Error -> {
                                step2Error = result.message
                                step2Loading = false
                            }
                        }
                    }
                }
            },
            customContent = {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(100)
                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                }
                Column {
                    OutlinedTextField(
                        value = groupChatId,
                        onValueChange = {
                            groupChatId = it
                            step2Error = null
                            step2VerifiedGroup = null
                        },
                        placeholder = { Text("-100123456789", color = TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        isError = step2Error != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLight,
                            unfocusedBorderColor = DividerColor,
                            errorBorderColor = ErrorRed,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryLight
                        )
                    )

                    if (step2Error != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ErrorRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(step2Error!!, color = ErrorRed, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    if (step2VerifiedGroup != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = PrimaryLight.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PrimaryLight.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connected: $step2VerifiedGroup (Admin rights verified)",
                                    color = PrimaryLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        ),
        DialogStep(
            title = "Partner Bot Username",
            message = "Enter the @username of your partner's bot to route messages and filter intruders.",
            icon = Icons.Filled.PersonSearch,
            iconTint = PrimaryLight,
            confirmText = "Save",
            isConfirmEnabled = partnerUsername.isNotBlank(),
            isConfirmLoading = false,
            onConfirmClick = {
                val botUser = step1VerifiedBot ?: return@DialogStep
                val result = Validator.verifyPartnerUsername(
                    partnerUsername = partnerUsername,
                    botUser = botUser,
                    isPeerLinkEnabled = true,
                    isGroup = true
                )
                if (result is Validator.ValidationResult.Error) {
                    step3Error = result.message
                    return@DialogStep
                }
                val token = botToken.trim()
                val chatId = groupChatId.trim()
                onComplete(token, chatId, partnerUsername.trim())
            },
            customContent = {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(100)
                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                }
                Column {
                    OutlinedTextField(
                        value = partnerUsername,
                        onValueChange = {
                            partnerUsername = it
                            step3Error = null
                        },
                        placeholder = { Text("@partner_bot", color = TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        isError = step3Error != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryLight,
                            unfocusedBorderColor = DividerColor,
                            errorBorderColor = ErrorRed,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = PrimaryLight
                        )
                    )

                    if (step3Error != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = ErrorRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(step3Error!!, color = ErrorRed, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = PrimaryLight.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryLight.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
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
                                text = "Please ensure your partner bot has also joined the same group. Adding a username in this field will strictly check and allow messages only from that bot; others will not be able to chat with your partner.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        )
    )

    MultiStepActionDialog(
        steps = steps,
        initialStep = 0,
        currentStep = currentStep,
        onStepChange = { currentStep = it },
        cancellable = true,
        onComplete = { onComplete(botToken.trim(), groupChatId.trim(), partnerUsername.trim()) },
        onDismiss = onDismiss
    )
}

@Composable
fun AdminPeerLinkInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.PEERLINK_INFO_TITLE,
        message = AdminTexts.PEERLINK_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminLifecycleInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.LIFECYCLE_INFO_TITLE,
        message = AdminTexts.LIFECYCLE_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminCallRingingInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.CALL_RINGING_INFO_TITLE,
        message = AdminTexts.CALL_RINGING_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminIAmAdminInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.I_AM_ADMIN_INFO_TITLE,
        message = AdminTexts.I_AM_ADMIN_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminAppToAppGuideDialog(
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = AdminTexts.APP_TO_APP_GUIDE_TITLE,
        message = AdminTexts.APP_TO_APP_GUIDE_MESSAGE,
        note = AdminTexts.APP_TO_APP_GUIDE_NOTE,
        noteIcon = Icons.Filled.QrCodeScanner,
        icon = Icons.AutoMirrored.Filled.MenuBook,
        iconTint = PrimaryLight,
        confirmText = "Got it",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminCredentialsInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.CREDENTIALS_INFO_TITLE,
        message = AdminTexts.CREDENTIALS_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminDisableRouteMessagesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = AdminTexts.DISABLE_ROUTE_MESSAGES_WARNING_TITLE,
        message = AdminTexts.DISABLE_ROUTE_MESSAGES_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Disable",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminDisableAdminModeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = AdminTexts.DISABLE_ADMIN_MODE_WARNING_TITLE,
        message = AdminTexts.DISABLE_ADMIN_MODE_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Disable",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminPipRequiredDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = AdminTexts.PIP_REQUIRED_TITLE,
        message = AdminTexts.PIP_REQUIRED_MESSAGE,
        icon = Icons.Filled.PictureInPicture,
        iconTint = ErrorRed,
        confirmText = AdminTexts.PIP_REQUIRED_CONFIRM,
        dismissText = "Not Now",
        onConfirm = {
            onGoToSettings()
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

@Composable
fun AdminNotificationRequiredDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = AdminTexts.NOTIFICATION_REQUIRED_TITLE,
        message = AdminTexts.NOTIFICATION_REQUIRED_MESSAGE,
        icon = Icons.Filled.NotificationsOff,
        iconTint = ErrorRed,
        confirmText = AdminTexts.NOTIFICATION_REQUIRED_CONFIRM,
        dismissText = "Not Now",
        onConfirm = {
            onGoToSettings()
            onDismiss()
        },
        onDismiss = onDismiss
    )
}
