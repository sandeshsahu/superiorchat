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

    const val READ_ONLY_INFO_TITLE = "Read Only Lock"
    const val READ_ONLY_INFO_MESSAGE =
        "When *Enabled*, all administrative settings are locked.\n\nThis prevents accidental toggling or modifying of *Routing* and *Admin Mode*, ensuring your chat configuration remains protected."

    const val ROUTE_MESSAGES_INFO_TITLE = "Route Messages"
    const val ROUTE_MESSAGES_INFO_MESSAGE =
        "Enables *App-to-App Chat* using Telegram's bot-to-bot communication in a private group.\n\n• **Direct Chat & Calls**: Both partners can chat and make calls directly inside the Superior Chat app.\n• **Intruder Shield**: Verifies sender usernames so the app strictly accepts messages from your partner's bot and ignores unauthorized intruders.\n• **Partner Setup**: On your partner's app, enable *Route Messages*, enter the same *Group Chat ID*, and provide *Your Bot Username* in Credentials.\n• **Direct DM Fallback**: When disabled, the app operates in standard 1-on-1 direct bot mode."

    const val I_AM_ADMIN_INFO_TITLE = "I Am Admin"
    const val I_AM_ADMIN_INFO_MESSAGE =
        "Turn this on if you are the *Admin (User B)* who will also be chatting directly inside this app.\n\n• **Admin Bot Configuration**: Unlocks the *Credentials* section where you can enter your bot token, private group chat ID, and partner's bot username.\n• **Bridge Mode**: Messages you send are dispatched via your admin bot to the shared group, where your partner's bot receives them securely."

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

    const val CREDENTIALS_INFO_TITLE = "Admin Credentials"
    const val CREDENTIALS_INFO_MESSAGE =
        "Configure your own *Bot Token*, *Private Group Chat ID*, and your *Partner's Bot Username* manually, or scan the configuration *QR Code* from the Setup App."

    const val HIDE_FROM_RECENTS_INFO_TITLE = "Hide from Recent Apps"
    const val HIDE_FROM_RECENTS_INFO_MESSAGE =
        "Controls whether Superior Chat appears in Android's **Overview / Recent Apps** multitasking list.\n\n" +
        "• **Enabled (Recommended for Stealth)**:\nWhen you minimize or leave the app, it completely disappears from Recent Apps, leaving zero trace on the multitasking screen. You can reopen via the dialer code or secret shortcut.\n\n" +
        "• **Disabled**:\nThe app remains visible in Recent Apps, allowing quick task switching. (If Screen Security is enabled, the preview card screenshot will still remain pitch-black)."
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
        iconTint = WarningAmber,
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
                val trimmedToken = botToken.trim()
                if (trimmedToken.isBlank()) {
                    step1Error = "Please enter your Telegram Bot Token."
                    return@DialogStep
                }
                if (!Validator.isValidBotToken(trimmedToken)) {
                    step1Error = "Invalid Bot Token format. It should look like 1234567890:AAH..."
                    return@DialogStep
                }
                step1Loading = true
                step1Error = null
                scope.launch(Dispatchers.IO) {
                    try {
                        val getMeResp = TelegramApi.getMeSuspend(trimmedToken)
                        if (getMeResp == null || !getMeResp.ok || getMeResp.result == null) {
                            withContext(Dispatchers.Main) {
                                step1Error = "Invalid bot token or token has been revoked by @BotFather."
                                step1Loading = false
                            }
                            return@launch
                        }
                        val user = getMeResp.result
                        if (user.can_read_all_group_messages == false) {
                            withContext(Dispatchers.Main) {
                                step1Error = "Group Privacy is Enabled in @BotFather. You must disable Group Privacy and enable Bot-to-Bot Communication Mode for this bot in @BotFather."
                                step1Loading = false
                            }
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            step1VerifiedBot = user
                            step1Loading = false
                            currentStep = 1
                        }
                    } catch (e: java.io.IOException) {
                        withContext(Dispatchers.Main) {
                            step1Error = "Cannot connect to Telegram servers. Please check your internet connection."
                            step1Loading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            step1Error = "Validation failed: ${e.localizedMessage ?: "Unknown error"}"
                            step1Loading = false
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
                val trimmedChatId = groupChatId.trim()
                if (trimmedChatId.isBlank()) {
                    step2Error = "Please enter the Group Chat ID."
                    return@DialogStep
                }
                if (!Validator.isValidGroupChatId(trimmedChatId)) {
                    step2Error = "Must be a negative Group ID (e.g. -100123456789). Positive IDs (private 1-on-1 user chats) are not supported."
                    return@DialogStep
                }
                val token = botToken.trim()
                val botUser = step1VerifiedBot
                step2Loading = true
                step2Error = null
                scope.launch(Dispatchers.IO) {
                    try {
                        val chatResp = TelegramApi.getChatSuspend(token, trimmedChatId)
                        if (chatResp == null || !chatResp.ok || chatResp.result == null) {
                            withContext(Dispatchers.Main) {
                                step2Error = "Bot has not joined this group. Please make sure 'Allow Groups' is enabled in @BotFather, then add @${botUser?.username ?: "bot"} to your group."
                                step2Loading = false
                            }
                            return@launch
                        }
                        val chat = chatResp.result
                        if (chat.type == "private" || chat.type == "channel") {
                            withContext(Dispatchers.Main) {
                                step2Error = "This ID belongs to a ${chat.type}. App-to-App routing requires a private Group or Supergroup ID."
                                step2Loading = false
                            }
                            return@launch
                        }
                        if (botUser != null) {
                            val member = TelegramApi.getChatMember(token, trimmedChatId, botUser.id)
                            if (member == null || member.status in listOf("left", "kicked")) {
                                withContext(Dispatchers.Main) {
                                    step2Error = "Admin Bot (@${botUser.username ?: "bot"}) is not in this group.\n\nPlease invite @${botUser.username ?: "bot"} to this group."
                                    step2Loading = false
                                }
                                return@launch
                            }
                            if (member.status !in listOf("administrator", "creator")) {
                                withContext(Dispatchers.Main) {
                                    step2Error = "Admin Bot (@${botUser.username ?: "bot"}) is a regular member, NOT an Admin (Status: ${member.status}).\n\nPlease promote @${botUser.username ?: "bot"} to Administrator in Group Settings > Administrators."
                                    step2Loading = false
                                }
                                return@launch
                            }
                        }
                        withContext(Dispatchers.Main) {
                            step2VerifiedGroup = chat.title ?: "Private Group"
                            step2Loading = false
                            currentStep = 2
                        }
                    } catch (e: java.io.IOException) {
                        withContext(Dispatchers.Main) {
                            step2Error = "Cannot connect to Telegram servers. Please check your internet connection."
                            step2Loading = false
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            step2Error = "Validation failed: ${e.localizedMessage ?: "Unknown error"}"
                            step2Loading = false
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
                val trimmedPartner = partnerUsername.trim()
                if (trimmedPartner.isBlank()) {
                    step3Error = "Please enter your partner's bot username."
                    return@DialogStep
                }
                if (!Validator.isValidPartnerBotUsername(trimmedPartner)) {
                    step3Error = "Must start with @ (e.g. @partner_bot) and be between 4 and 32 characters."
                    return@DialogStep
                }
                val myBotUsername = step1VerifiedBot?.username
                if (myBotUsername != null && trimmedPartner.equals("@$myBotUsername", ignoreCase = true)) {
                    step3Error = "Partner Bot Username cannot be your own Admin Bot (@$myBotUsername). Enter your partner's bot username."
                    return@DialogStep
                }
                val token = botToken.trim()
                val chatId = groupChatId.trim()
                onComplete(token, chatId, trimmedPartner)
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
fun AdminReadOnlyInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.READ_ONLY_INFO_TITLE,
        message = AdminTexts.READ_ONLY_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminRouteMessagesInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.ROUTE_MESSAGES_INFO_TITLE,
        message = AdminTexts.ROUTE_MESSAGES_INFO_MESSAGE,
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
fun AdminExcludeFromRecentsInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = AdminTexts.HIDE_FROM_RECENTS_INFO_TITLE,
        message = AdminTexts.HIDE_FROM_RECENTS_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}
