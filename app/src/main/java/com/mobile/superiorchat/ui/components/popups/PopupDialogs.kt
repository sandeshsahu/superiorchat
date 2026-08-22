package com.mobile.superiorchat.ui.components.popups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.core.call.CallError
import com.mobile.superiorchat.theme.ErrorRed
import com.mobile.superiorchat.theme.PrimaryLight
import com.mobile.superiorchat.theme.WarningAmber
import com.mobile.superiorchat.theme.SurfaceLevel2
import com.mobile.superiorchat.theme.TextPrimary
import com.mobile.superiorchat.theme.TextSecondary
import com.mobile.superiorchat.theme.Background
import com.mobile.superiorchat.theme.SurfaceLevel1
import com.mobile.superiorchat.theme.DividerColor
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Centralized registry of all popup and dialog text contents, markdown descriptions,
 * note callouts, and icon configurations across the application.
 */
object PopupTexts {
    // ── Main Activity Popups ──
    object Main {
        const val SETUP_UNINSTALL_TITLE = "Uninstall Setup App"
        fun getSetupUninstallMessage(accessInstructions: String): String =
            "The main app is now configured and hidden. It is highly recommended to uninstall the Setup application to maintain absolute stealth.\n\n$accessInstructions"

        const val SETUP_UNINSTALL_CONFIRM = "Uninstall"
        const val SETUP_UNINSTALL_DISMISS = "Keep"

        const val DIALER_ACCESS_TITLE = "Open App Via Dialer"
        const val DIALER_ACCESS_MESSAGE =
            "The application icon is completely hidden from your phone's app drawer.\n\nTo secretly open the app, \nSimply go to your phone's *Dialer* and type the secret code ** *#*#9131#*#* **."

        const val QS_SETUP_TITLE = "Add Tile To Panel"
        const val QS_SETUP_MESSAGE =
            "You can add a stealth tile to your notification panel for quick access.\n\nOpen your *Notification Panel*, click the *Pencil Icon* (Edit), find the stealth tile, and drag it to add it."

        const val QS_ACCESS_TITLE = "Open App Via Tile"
        const val QS_ACCESS_MESSAGE =
            "When you want to open the chat application, perform the following sequence on the tile:\n\n1. *Tap* to Enable\n2. *Tap* to Disable\n3. *Tap* to Enable\n4. *Hold Tile* under 3 seconds to instantly launch the app"
    }

    // ── Settings Screen Popups ──
    object Settings {
        const val DEVELOPER_WARNING_TITLE = "Developer Setting"
        const val DEVELOPER_WARNING_MESSAGE =
            "This setting is strictly for *Developers*! Changing the *Server URL* can permanently *Break* the Calling feature. If you are not a developer, please *Cancel* this."
        const val DEVELOPER_WARNING_CONFIRM = "I Understand"

        const val SERVERS_UNAVAILABLE_TITLE = "Servers Unavailable"
        const val SERVERS_UNAVAILABLE_MESSAGE =
            "All working servers are currently *Unavailable*.\nPlease contact the *Developer* or check the GitHub page to learn how to deploy your own static *PeerJS signaling server*."

        const val NETWORK_ERROR_TITLE = "No Internet Connection"
        const val NETWORK_ERROR_MESSAGE = "Please check your network connection and try again."

        const val APP_LOCK_INFO_TITLE = "App Lock"
        const val APP_LOCK_INFO_MESSAGE =
            "App Lock secures your chats by requiring a PIN code every time you open the app or return from the background."

        const val FAKE_CRASH_TITLE = "Fake Crash Protection"
        const val FAKE_CRASH_MESSAGE =
            "You are about to enable Fake Crash. This displays a fake **Crash Dialog** on startup to fool intruders.\n\nTo safely bypass it and open the app, you must **Hold** the following Word:"
        const val FAKE_CRASH_CONFIRM = "Enable"

        const val FAKE_CRASH_INFO_MESSAGE =
            "When enabled, an authentic-looking system fake crash dialog will appear when opening app.\n\nTo open the Chat, you must **Hold** the following Word:"

        const val SAFEGUARD_TITLE = "Emergency Safeguard PIN (1234)"
        const val SAFEGUARD_MESSAGE =
            "Emergency Safeguard is **Always Active** by default for maximum security.\n\nIf forced to unlock, enter **1234** as your PIN.\n\n• **Hidden Vault**: Instantly opens fully working media vault, users can hide media files there, but its not recommended to hide your private Images/Videos.\n• **Defence**: In case of forced to open then use 1234 code to justify as its a media hider app."
        const val SAFEGUARD_NOTE =
            "Do NOT set 1234 as your normal PIN! It is reserved exclusively for emergency stealth mode."

        const val CAMO_NOTIF_INFO_TITLE = "Notification Camouflage"
        const val CAMO_NOTIF_INFO_MESSAGE =
            "Camouflage Notifications disguise incoming Telegram messages as innocent system or weather alerts.\n\nWhen enabled, sensitive chat text and sender names are completely hidden from lock screen notifications."

        const val CAMO_NOTIF_CONFIRM_TITLE = "Enable Camouflage Notifications"
        const val CAMO_NOTIF_CONFIRM_MESSAGE =
            "This will replace standard chat notifications with stealth camouflage alerts."
        const val CAMO_NOTIF_CONFIRM_TEXT = "Enable Camouflage"

        const val QS_TILE_INFO_TITLE = "How To Open Via Tile"
        const val QS_TILE_INFO_MESSAGE =
            "Open notification panel, click on the pencil icon, find *%s* and add it.\n\nThen when you want to open chat:\n1. *Enable*\n2. *Disable*\n3. *Enable*\n4. *Hold Tile* to open chat app"

        const val QS_TILE_DISABLE_TITLE = "Disable Tile Access"
        const val QS_TILE_DISABLE_MESSAGE =
            "If you disable this, you will no longer be able to *Open The App* using the *Notification Tile*.\nIf access by dialer fails, you may be *Completely Locked Out* of the app.\nAre you sure you want to *Proceed*?"
        const val QS_TILE_DISABLE_CONFIRM = "Disable"

        const val PERSISTENT_NOTIF_INFO_TITLE = "Persistent Notification"
        const val PERSISTENT_NOTIF_INFO_MESSAGE =
            "Foreground service keeps the Telegram bot polling active continuously in the background.\n\nDisabling this may cause background message sync delays on some Android devices."

        const val WEBRTC_INFO_TITLE = "WebRTC Calling Server"
        const val WEBRTC_INFO_MESSAGE =
            "You can configure your custom *WebRTC Server* URL for voice calls, or reset it to the default server if you experience connection issues.\n\nCheck developer's *Github Page* for more information"

        const val CUSTOM_ACCESS_INFO_TITLE = "Custom Access Word"
        const val CUSTOM_ACCESS_INFO_MESSAGE =
            "Set a secret phrase that you can type into the weather app's search bar to open Superior Chat. The default *Superior Chat* will always work as a fallback."

        fun getCustomAccessConfirmMessage(word: String): String =
            "Are you sure you want to set your access word to *$word*? If you forget this word, you can always use the default *Superior Chat* fallback to regain access."

        const val CUSTOM_DIALER_INFO_TITLE = "Custom Dialer Code"
        const val CUSTOM_DIALER_INFO_MESSAGE =
            "Set a custom secret code that you can type into your phone's dialer to open the app. The default ** *#*#9131#*#* ** will always work as a fallback."

        fun getCustomDialerConfirmMessage(code: String): String =
            "Are you sure you want to set your custom dialer code to *$code*? (You will dial ** *#*#$code#*#* **). If you forget it, you can always use the default ** *#*#9131#*#* ** fallback."

        const val BOT_INFO_TITLE = "Bot Credentials"
        const val BOT_INFO_MESSAGE =
            "You can manually enter your *Bot Token* and *Chat ID*, or securely import them by scanning a configuration *QR Code*."

        const val INVALID_CREDENTIALS_TITLE = "Invalid Credentials"

        const val CLEAR_CREDENTIALS_TITLE = "Clear Credentials"
        const val CLEAR_CREDENTIALS_MESSAGE =
            "This will disconnect the bot and stop the end-to-end chat. *Both Users* will lose access\nto the current *Chat Session*. Do you want to proceed?"

        const val UNINSTALL_TITLE = "Uninstall App"
        const val UNINSTALL_MESSAGE =
            "This will *permanently remove* the application from your device. Do you want to proceed?"

        const val BG_POLLING_INFO_TITLE = "Background Polling"
        const val BG_POLLING_INFO_MESSAGE =
            "Background polling periodically checks for new Telegram updates using WorkManager when the app is minimized."

        const val CLEAR_DB_TITLE = "Clear Database"
        const val CLEAR_DB_MESSAGE =
            "Are you sure you want to clear local database records? All stored messages and chat history will be removed from this device."
        const val CLEAR_DB_CONFIRM = "Clear Database"

        const val RESET_APP_TITLE = "Reset Application"
        const val RESET_APP_MESSAGE =
            "Are you sure you want to reset all app settings and credentials? You will need to setup the application again."
        const val RESET_APP_CONFIRM = "Reset Everything"

        const val QR_PROMPT_TITLE = "Scan Configuration"
        const val QR_PROMPT_MESSAGE = "Scan a QR code to *Quickly Configure* application settings."
        const val QR_PROMPT_CONFIRM = "Scan"

        const val AUTO_DOWNLOAD_INFO_TITLE = "Auto-Download Media"
        const val AUTO_DOWNLOAD_INFO_MESSAGE =
            "When enabled, photos and videos will automatically download when you receive them in chat. \n\nTurn this off to save mobile data."

        const val SCREEN_SECURITY_INFO_TITLE = "Screen Security"
        const val SCREEN_SECURITY_INFO_MESSAGE =
            "This prevents any app, screen recorder, or screen cast from capturing the chat. \n\n*Screenshots* will appear pure black."

        const val APP_NOTIFICATIONS_INFO_TITLE = "App Notifications"
        const val APP_NOTIFICATIONS_INFO_MESSAGE =
            "Controls the underlying Android System notification permissions.\n\nWhen disabled, the app is completely blocked from showing *Any background notifications*, making it ultra-stealthy. *Background sync* will still work perfectly."

        const val DISABLE_NOTIFICATIONS_TITLE = "Disable App Notifications"
        const val DISABLE_NOTIFICATIONS_MESSAGE =
            "To completely disable notifications without crashing the background service, you must turn them off from Android's System Settings.\n\nClick Proceed to open the *App Info* page, then tap *Notifications* and turn them off."

        const val NEW_MESSAGE_NOTIF_INFO_TITLE = "New Message Notifications"
        const val NEW_MESSAGE_NOTIF_INFO_MESSAGE =
            "When *Enabled*, the stealth app's background service notification will visually change states (e.g., \"*Live Update*\" or \"*Heavy data usage detected*\") to alert you of new incoming messages.\n\nWhen *Disabled*, messages will still sync silently in the background, but the decoy *notification* will never change its idle state."

        const val ADMIN_MODE_ACTIVE_TITLE = "Admin Mode Active"
        const val ADMIN_MODE_ACTIVE_MESSAGE =
            "You currently have *I am Admin* turned on.\n\nPlease configure your bot credentials inside the *Admin Settings* screen instead. Turn off Admin Mode if you want to configure regular client credentials."
    }

    // ── Admin Settings Popups ──
    object Admin {
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
            "5. **Configure Partner Device**:\nOn your partner's app, enable *Route Messages*, enter the same *Group Chat ID*, and enter *Your Bot Username* on APP Settings Page under *Credentials* section."
        const val APP_TO_APP_GUIDE_NOTE =
            "Setup App QR Shortcut: Generate a setup QR code in the Setup App and scan it on your partner's device to configure everything automatically!"

        const val CREDENTIALS_INFO_TITLE = "Admin Credentials"
        const val CREDENTIALS_INFO_MESSAGE =
            "Configure your own *Bot Token*, *Private Group Chat ID*, and your *Partner's Bot Username* manually, or scan the configuration *QR Code* from the Setup App."
    }

    // ── PIN & Security Popups ──
    object Security {
        const val ENTER_PIN_TITLE = "Enter PIN"
        const val RESERVED_PIN_ERROR = "Choose a different PIN. \n1234 is reserved for Emergency Safeguard!"
        const val PIN_MIN_LENGTH_ERROR = "PIN must be at least 4 digits"
        const val PIN_MISMATCH_ERROR = "PINs do not match"
        const val INCORRECT_PIN_ERROR = "Incorrect PIN"
    }

    // ── Profile Screen Popups ──
    object Profile {
        const val RATE_LIMIT_WARNING_TITLE = "Warning: Rate Limits"
        const val RATE_LIMIT_WARNING_MESSAGE =
            "Telegram strictly limits how often you can change your bot's name and description. Frequent updates will result in a 24-hour ban. Are you sure you want to proceed?"
        const val RATE_LIMIT_WARNING_CONFIRM = "Proceed"
        fun getRateLimitErrorMessage(timeStr: String): String =
            "Telegram rate limit reached. Please try again in $timeStr."

        const val DEFAULT_UPDATE_FAILED_ERROR = "Failed to update profile info."
        const val REMOVE_PHOTO_NOT_SUPPORTED_ERROR =
            "Removing profile photos is only supported via @BotFather in Telegram."
    }

    // ── Chat & Media Popups ──
    object Chat {
        const val DELETE_SINGLE_TITLE = "Delete message?"
        const val DELETE_SINGLE_MESSAGE = "Are you sure you want to delete this message?"
        const val DELETE_BULK_TITLE = "Delete selected messages?"
        const val DELETE_BULK_MESSAGE = "Are you sure you want to delete all selected messages?"

        const val CLEAR_CHAT_TITLE = "Clear Chat History"
        const val CLEAR_CHAT_MESSAGE =
            "Are you sure you want to clear all chat history? This will delete messages from local database."

        const val APK_INSTALL_PERMISSION_TITLE = "Installation Permission Required"
        const val APK_INSTALL_PERMISSION_MESSAGE =
            "To install this app, you need to allow SuperiorChat to install unknown apps."

        fun getSaveMediaMessage(typeName: String, storageType: String): String =
            "Do you want to save this *$typeName* to your device's *$storageType folder*?"

        fun getFileTooLargeErrorMessage(fileName: String, formattedSize: String): String =
            "The selected file '$fileName' ($formattedSize) exceeds the 50MB limit.\n\nFiles larger than 50MB are not supported."
    }

    // ── Global System Permissions Popups ──
    object GlobalPermissions {
        const val PERMISSION_DENIED_TITLE = "Permission Denied"
        const val PERMISSION_DENIED_MESSAGE =
            "This permission has been permanently denied. Please enable it in the App Settings."

        const val MEDIA_DENIED_TITLE = "Media Access Denied"
        const val MEDIA_DENIED_MESSAGE =
            "You have previously denied full access to your media. To allow full access or select more photos, please go to Settings."

        const val ALL_FILES_REQUIRED_TITLE = "All Files Access Required"
        const val ALL_FILES_REQUIRED_MESSAGE =
            "The file explorer requires full access to your device storage to view and attach documents."

        const val LIMITED_ACCESS_TITLE = "Limited Access Granted"
        const val LIMITED_ACCESS_MESSAGE =
            "You have granted limited access to your media. Would you like to grant full access so you can easily select any photo?"

        const val CAMERA_RATIONALE_TITLE = "Camera Permission"
        const val CAMERA_RATIONALE_MESSAGE = "We need access to your camera to take photos."

        const val MIC_RATIONALE_TITLE = "Microphone Permission"
        const val MIC_RATIONALE_MESSAGE = "We need access to your microphone to record voice messages."

        const val STORAGE_RATIONALE_TITLE = "Storage Permission"
        const val STORAGE_RATIONALE_MESSAGE = "We need access to your device storage to view and attach documents."

        const val CALL_RATIONALE_TITLE = "Camera & Microphone Required"
        const val CALL_RATIONALE_MESSAGE =
            "We need access to both your camera and microphone to initiate the secure WebRTC call."
    }

    // ── Call Engine Popups ──
    object Call {
        const val CLEAR_LOGS_TITLE = "Clear History"
        const val CLEAR_LOGS_MESSAGE =
            "Are you sure you want to clear your entire call history? This will delete all logs."
        const val CLEAR_LOGS_CONFIRM = "Clear"

        const val CALL_HISTORY_INFO_TITLE = "Recent Calls"
        const val CALL_HISTORY_INFO_MESSAGE =
            "A history of all secure peer-to-peer WebRTC calls initiated from this device."

        fun getErrorSpec(error: CallError): Triple<String, String, String> {
            return when (error) {
                CallError.NETWORK_ERROR -> Triple(
                    "Network Error",
                    "The call *Failed to Connect*.\nYour internet connection might be *Unstable* or device is completely *Offline*.\n\nPlease check your *Internet Connection*.",
                    "Okay"
                )

                CallError.NO_ANSWER -> Triple(
                    "No Answer",
                    "The call was *Not Answered*\n\nYour friend is *Busy* or *Not Available*.\nTry Later.",
                    "Okay"
                )

                CallError.HARDWARE_ERROR -> Triple(
                    "Hardware Initialization Failed",
                    "The secure WebRTC environment failed to load properly.\nThis is usually caused by an *Invalid Server Path* blocking necessary Javascript files, or a camera/microphone hardware lock.\n\nPlease check application permissions or would you like to *Reset to Default*?",
                    "Go to Settings"
                )

                CallError.DECLINED -> Triple(
                    "Call Declined",
                    "The call was *Declined*\n\nYour friend declined the call, they might be *Busy* right now.\nTry again later.",
                    "Okay"
                )

                else -> Triple(
                    "Call Failed",
                    "The call failed to connect. This is often caused by an *Invalid*, *Unreachable* Server URL. Would you like to check your Settings and *Reset to Default*?",
                    "Go to Settings"
                )
            }
        }
    }

    // ── Logs Screen Popups ──
    object Logs {
        const val LOGS_INFO_TITLE = "Live Logs"
        const val LOGS_INFO_MESSAGE =
            "These logs record system background activity, network requests, and bot interactions for troubleshooting.\n\nOnly *Last 150 Logs* will be displayed."
        const val LOG_DETAIL_TITLE = "Log Details"
        const val CLEAR_LOGS_TITLE = "Clear Diagnostics"
        const val CLEAR_LOGS_MESSAGE = "Are you sure you want to clear all recorded diagnostic logs?"
        const val CLEAR_LOGS_CONFIRM = "Flush Logs"
    }

    // ── Navigation & Auth Popups ──
    object Nav {
        const val LOGOUT_TITLE = "Logout Confirmation"
        const val LOGOUT_MESSAGE = "Are you sure you want to logout? Active background service will be stopped."
        const val LOGOUT_CONFIRM = "Logout"
    }

    // ── First Launch Terms & Conditions ──
    // Text is derived directly from docs/Notes.md, webrtc/docs/Security.md, and webrtc/docs/Notes.md
    object Terms {
        const val TITLE = "Terms & Conditions"
        const val SUBTITLE = "Please read carefully before continuing"

        const val CHECKBOX_LABEL =
            "I have read and understood all terms above. I accept full legal responsibility for my use of this application."
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PRESET DOMAIN COMPOSABLE DIALOGS
// ─────────────────────────────────────────────────────────────────────────────

// ── Main Activity Domain Dialogs ──

@Composable
fun SetupUninstallDialog(
    flavor: String,
    accessInstructions: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appName = remember { context.packageManager.getApplicationLabel(context.applicationInfo).toString() }
    val isStealth = flavor == "captivePortal" || flavor == "playSupport" || flavor == "decoyEngine"

    val steps = listOf(
        com.mobile.superiorchat.ui.components.popups.DialogStep(
            title = PopupTexts.Main.DIALER_ACCESS_TITLE,
            message = PopupTexts.Main.DIALER_ACCESS_MESSAGE,
            icon = Icons.Filled.Phone,
            iconTint = PrimaryLight,
            confirmText = "Next",
            customContent = { com.mobile.superiorchat.ui.components.popups.DialerAccessAnimPreview(dialerCode = "9131") }
        ),
        com.mobile.superiorchat.ui.components.popups.DialogStep(
            title = PopupTexts.Main.QS_SETUP_TITLE,
            message = PopupTexts.Main.QS_SETUP_MESSAGE,
            icon = Icons.Filled.Info,
            iconTint = PrimaryLight,
            confirmText = "Next",
            customContent = { com.mobile.superiorchat.ui.components.popups.QsTileSetupAnimPreview() }
        ),
        com.mobile.superiorchat.ui.components.popups.DialogStep(
            title = PopupTexts.Main.QS_ACCESS_TITLE,
            message = PopupTexts.Main.QS_ACCESS_MESSAGE,
            icon = Icons.Filled.TouchApp,
            iconTint = PrimaryLight,
            confirmText = "Next",
            customContent = { com.mobile.superiorchat.ui.components.popups.TileAccessAnimPreview(isDisabledMode = false) }
        ),
        com.mobile.superiorchat.ui.components.popups.DialogStep(
            title = PopupTexts.Main.SETUP_UNINSTALL_TITLE,
            message = PopupTexts.Main.getSetupUninstallMessage(accessInstructions),
            icon = Icons.Filled.Delete,
            iconTint = ErrorRed,
            confirmText = PopupTexts.Main.SETUP_UNINSTALL_CONFIRM,
            dismissText = PopupTexts.Main.SETUP_UNINSTALL_DISMISS
        )
    )

    com.mobile.superiorchat.ui.components.popups.MultiStepActionDialog(
        steps = if (isStealth) steps else listOf(steps.last()),
        initialStep = 0,
        cancellable = false,
        onComplete = onConfirm,
        onDismiss = onDismiss
    )
}

// ── Settings & Profile Sheet Domain Dialogs ──

@Composable
fun SettingsDeveloperWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.DEVELOPER_WARNING_TITLE,
        message = PopupTexts.Settings.DEVELOPER_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Settings.DEVELOPER_WARNING_CONFIRM,
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsServersUnavailableDialog(
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.SERVERS_UNAVAILABLE_TITLE,
        message = PopupTexts.Settings.SERVERS_UNAVAILABLE_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsNetworkErrorDialog(
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.NETWORK_ERROR_TITLE,
        message = PopupTexts.Settings.NETWORK_ERROR_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsAppLockInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.APP_LOCK_INFO_TITLE,
        message = PopupTexts.Settings.APP_LOCK_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsFakeCrashInfoDialog(
    appName: String,
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = "Fake Crash Decoy",
        message = PopupTexts.Settings.FAKE_CRASH_INFO_MESSAGE,
        customContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val annotatedString = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryLight,
                            background = SurfaceLevel2
                        )
                    ) {
                        append(appName)
                    }
                    append(" ")
                    appendInlineContent("arrow_icon", "[icon]")
                    append(" for ")
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = PrimaryLight,
                            background = SurfaceLevel2
                        )
                    ) {
                        append("2 seconds")
                    }
                    append(".")
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp,
                    inlineContent = mapOf(
                        "arrow_icon" to InlineTextContent(
                            Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                FakeCrashAnimPreview()
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsFakeCrashDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appName = context.getString(com.mobile.superiorchat.R.string.app_name)

    ActionDialog(
        title = PopupTexts.Settings.FAKE_CRASH_TITLE,
        message = PopupTexts.Settings.FAKE_CRASH_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Settings.FAKE_CRASH_CONFIRM,
        customContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val annotatedString = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ErrorRed, background = SurfaceLevel2)) {
                        append(appName)
                    }
                    append(" ")
                    appendInlineContent("arrow_icon", "[icon]")
                    append(" for ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ErrorRed, background = SurfaceLevel2)) {
                        append("2 seconds")
                    }
                    append(".")
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    lineHeight = 22.sp,
                    inlineContent = mapOf(
                        "arrow_icon" to InlineTextContent(
                            Placeholder(16.sp, 16.sp, PlaceholderVerticalAlign.TextCenter)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                FakeCrashAnimPreview()
            }
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsSafeguardInfoDialog(
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.SAFEGUARD_TITLE,
        message = PopupTexts.Settings.SAFEGUARD_MESSAGE,
        note = PopupTexts.Settings.SAFEGUARD_NOTE,
        noteIcon = Icons.Filled.Warning,
        icon = Icons.Filled.Security,
        iconTint = PrimaryLight,
        confirmText = "Got it",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsCamoNotifInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.CAMO_NOTIF_INFO_TITLE,
        message = PopupTexts.Settings.CAMO_NOTIF_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsCamoNotifConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.CAMO_NOTIF_CONFIRM_TITLE,
        message = PopupTexts.Settings.CAMO_NOTIF_CONFIRM_MESSAGE,
        icon = Icons.Filled.VisibilityOff,
        confirmText = PopupTexts.Settings.CAMO_NOTIF_CONFIRM_TEXT,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsQsTileInfoDialog(
    onDismiss: () -> Unit
) {
    var showSetupGuide by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val qsTileNameId = remember { context.resources.getIdentifier("qs_tile_name", "string", context.packageName) }
    val appName =
        if (qsTileNameId != 0) androidx.compose.ui.res.stringResource(id = qsTileNameId) else androidx.compose.ui.res.stringResource(
            id = com.mobile.superiorchat.R.string.app_name
        )

    if (showSetupGuide) {
        val guideMessage =
            "To add the stealth tile:\n\n1. Pull down your notification shade fully.\n2. Tap the *Pencil* (Edit) icon.\n3. Scroll down to find \nthe tile named *$appName*.\n4. *Hold and drag* it into your active tiles."
        InfoDialog(
            title = "How to Add Tile",
            message = guideMessage,
            customContent = { QsTileSetupAnimPreview() },
            onDismiss = onDismiss
        )
    } else {
        val message = PopupTexts.Settings.QS_TILE_INFO_MESSAGE.format(appName)

        InfoDialog(
            title = PopupTexts.Settings.QS_TILE_INFO_TITLE,
            message = message,
            customContent = { TileAccessAnimPreview() },
            extraButtonText = "Learn how to add",
            onExtraButtonClick = { showSetupGuide = true },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun SettingsQsTileDisableDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.QS_TILE_DISABLE_TITLE,
        message = PopupTexts.Settings.QS_TILE_DISABLE_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Settings.QS_TILE_DISABLE_CONFIRM,
        customContent = { TileAccessAnimPreview(isDisabledMode = true) },
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsCustomAccessWordInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.CUSTOM_ACCESS_INFO_TITLE,
        message = PopupTexts.Settings.CUSTOM_ACCESS_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsCustomAccessWordConfirmDialog(
    accessWord: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = "IMPORTANT",
        message = PopupTexts.Settings.getCustomAccessConfirmMessage(accessWord),
        icon = Icons.Filled.Warning,
        iconTint = PrimaryLight,
        confirmText = "Save",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsCustomDialerInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.CUSTOM_DIALER_INFO_TITLE,
        message = PopupTexts.Settings.CUSTOM_DIALER_INFO_MESSAGE,
        customContent = { DialerAccessAnimPreview() },
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsCustomDialerConfirmDialog(
    dialerCode: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = "IMPORTANT",
        message = PopupTexts.Settings.getCustomDialerConfirmMessage(dialerCode),
        icon = Icons.Filled.Warning,
        iconTint = PrimaryLight,
        confirmText = "Save",
        customContent = { DialerAccessAnimPreview(dialerCode = dialerCode) },
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsAdminModeActiveDialog(
    onDismiss: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    com.mobile.superiorchat.ui.components.popups.ActionDialog(
        title = PopupTexts.Settings.ADMIN_MODE_ACTIVE_TITLE,
        message = PopupTexts.Settings.ADMIN_MODE_ACTIVE_MESSAGE,
        icon = androidx.compose.material.icons.Icons.Default.AdminPanelSettings,
        iconTint = com.mobile.superiorchat.theme.WarningAmber,
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
fun SettingsBotCredentialsInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.BOT_INFO_TITLE,
        message = PopupTexts.Settings.BOT_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsInvalidCredentialsDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        title = PopupTexts.Settings.INVALID_CREDENTIALS_TITLE,
        message = errorMessage,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsClearCredentialsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.CLEAR_CREDENTIALS_TITLE,
        message = PopupTexts.Settings.CLEAR_CREDENTIALS_MESSAGE,
        icon = Icons.Filled.NoAccounts,
        iconTint = ErrorRed,
        confirmText = "Proceed",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsClearChatDialog(
    onConfirmClear: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ClearChatWarningDialog(
        onDismiss = onDismiss,
        onConfirmClear = onConfirmClear
    )
}

@Composable
fun SettingsUninstallAppDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.UNINSTALL_TITLE,
        message = PopupTexts.Settings.UNINSTALL_MESSAGE,
        icon = Icons.Filled.DeleteForever,
        iconTint = ErrorRed,
        confirmText = "Proceed",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsPersistentNotifInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.PERSISTENT_NOTIF_INFO_TITLE,
        message = PopupTexts.Settings.PERSISTENT_NOTIF_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsWebRtcInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.WEBRTC_INFO_TITLE,
        message = PopupTexts.Settings.WEBRTC_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsBackgroundPollingInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.BG_POLLING_INFO_TITLE,
        message = PopupTexts.Settings.BG_POLLING_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsClearDatabaseDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.CLEAR_DB_TITLE,
        message = PopupTexts.Settings.CLEAR_DB_MESSAGE,
        icon = Icons.Filled.DeleteForever,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Settings.CLEAR_DB_CONFIRM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsResetAppDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.RESET_APP_TITLE,
        message = PopupTexts.Settings.RESET_APP_MESSAGE,
        icon = Icons.Filled.RestartAlt,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Settings.RESET_APP_CONFIRM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SettingsQrScanPromptDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.QR_PROMPT_TITLE,
        message = PopupTexts.Settings.QR_PROMPT_MESSAGE,
        icon = Icons.Filled.QrCodeScanner,
        iconTint = PrimaryLight,
        confirmText = PopupTexts.Settings.QR_PROMPT_CONFIRM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ProfileAutoDownloadMediaInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.AUTO_DOWNLOAD_INFO_TITLE,
        message = PopupTexts.Settings.AUTO_DOWNLOAD_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun ProfileScreenSecurityInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.SCREEN_SECURITY_INFO_TITLE,
        message = PopupTexts.Settings.SCREEN_SECURITY_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun ProfileAppNotificationsInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.APP_NOTIFICATIONS_INFO_TITLE,
        message = PopupTexts.Settings.APP_NOTIFICATIONS_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun ProfileDisableAppNotificationsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Settings.DISABLE_NOTIFICATIONS_TITLE,
        message = PopupTexts.Settings.DISABLE_NOTIFICATIONS_MESSAGE,
        confirmText = "Proceed",
        dismissText = "Cancel",
        icon = Icons.Filled.Notifications,
        iconTint = PrimaryLight,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ProfileNewMessageNotificationsInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Settings.NEW_MESSAGE_NOTIF_INFO_TITLE,
        message = PopupTexts.Settings.NEW_MESSAGE_NOTIF_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

// ── Profile Domain Dialogs ──

@Composable
fun ProfileRateLimitWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Profile.RATE_LIMIT_WARNING_TITLE,
        message = PopupTexts.Profile.RATE_LIMIT_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = WarningAmber,
        confirmText = PopupTexts.Profile.RATE_LIMIT_WARNING_CONFIRM,
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun ProfileRateLimitErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        message = errorMessage,
        onDismiss = onDismiss
    )
}

// ── Chat & Media Domain Dialogs ──

@Composable
fun ChatErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        message = errorMessage,
        onDismiss = onDismiss
    )
}

@Composable
fun ApkInstallPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Chat.APK_INSTALL_PERMISSION_TITLE,
        message = PopupTexts.Chat.APK_INSTALL_PERMISSION_MESSAGE,
        icon = Icons.Filled.Warning,
        confirmText = "Settings",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SaveMediaConfirmDialog(
    typeName: String,
    storageType: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = "Save Media",
        message = PopupTexts.Chat.getSaveMediaMessage(typeName, storageType),
        icon = Icons.Filled.Download,
        confirmText = "Save",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

// ── Call Engine Domain Dialogs ──

@Composable
fun CallInitiationDialog(
    title: String,
    message: String,
    note: String?,
    isFailed: Boolean,
    isLoading: Boolean,
    isSuccess: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = title,
        message = message,
        note = note,
        noteIcon = null,
        icon = if (isFailed) Icons.Filled.Warning else Icons.Filled.Phone,
        iconTint = if (isFailed) ErrorRed else PrimaryLight,
        confirmText = if (isFailed) "Retry" else "Start Call",
        dismissText = "Cancel",
        autoDismiss = false,
        cancellable = false,
        isLoading = isLoading,
        isSuccess = isSuccess,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun CallErrorDialog(
    callError: CallError,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (title, message, confirm) = PopupTexts.Call.getErrorSpec(callError)
    val isSettingsNav = callError == CallError.INVALID_URL || callError == CallError.HARDWARE_ERROR
    ActionDialog(
        title = title,
        message = message,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = confirm,
        dismissText = if (isSettingsNav) "Cancel" else "Dismiss",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun CallHistoryClearDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Call.CLEAR_LOGS_TITLE,
        message = PopupTexts.Call.CLEAR_LOGS_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Call.CLEAR_LOGS_CONFIRM,
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun CallHistoryInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Call.CALL_HISTORY_INFO_TITLE,
        message = PopupTexts.Call.CALL_HISTORY_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

// ── Logs Domain Dialogs ──

@Composable
fun LogsInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Logs.LOGS_INFO_TITLE,
        message = PopupTexts.Logs.LOGS_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun LogDetailDialog(
    logMessage: String,
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Logs.LOG_DETAIL_TITLE,
        message = logMessage,
        onDismiss = onDismiss
    )
}

@Composable
fun LogClearConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Logs.CLEAR_LOGS_TITLE,
        message = PopupTexts.Logs.CLEAR_LOGS_MESSAGE,
        icon = Icons.Filled.Delete,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Logs.CLEAR_LOGS_CONFIRM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

// ── Navigation Domain Dialogs ──

@Composable
fun AppLogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Nav.LOGOUT_TITLE,
        message = PopupTexts.Nav.LOGOUT_MESSAGE,
        icon = Icons.AutoMirrored.Filled.Logout,
        iconTint = ErrorRed,
        confirmText = PopupTexts.Nav.LOGOUT_CONFIRM,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

// ── Admin Domain Dialogs ──

@Composable
fun PeerLinkSetupFlowDialog(
    initialBotToken: String,
    initialGroupChatId: String,
    initialPartnerUsername: String,
    onComplete: (botToken: String, groupChatId: String, partnerUsername: String) -> Unit,
    onDismiss: () -> Unit
) {
    var botToken by remember { mutableStateOf(initialBotToken) }
    var groupChatId by remember { mutableStateOf(initialGroupChatId) }
    var partnerUsername by remember { mutableStateOf(initialPartnerUsername) }

    val steps = listOf(
        com.mobile.superiorchat.ui.components.popups.DialogStep(
            title = "Admin Bot Token",
            message = "Enter the standard Telegram Bot Token. This bot will be used to route all messages.",
            icon = Icons.Filled.SmartToy,
            iconTint = PrimaryLight,
            confirmText = "Next",
            customContent = {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                }
                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    placeholder = { Text("123456789:ABCdefGHIjklMNOpqrSTUvwxYZ", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryLight,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryLight
                    )
                )
            }
        ),
        com.mobile.superiorchat.ui.components.popups.DialogStep(
            title = "Group Chat ID",
            message = "Enter the ID of the Telegram Group that will act as the bridge between both bots.",
            icon = Icons.Filled.Groups,
            iconTint = PrimaryLight,
            confirmText = "Next",
            customContent = {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                }
                OutlinedTextField(
                    value = groupChatId,
                    onValueChange = { groupChatId = it },
                    placeholder = { Text("-100123456789", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryLight,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryLight
                    )
                )
            }
        ),
        com.mobile.superiorchat.ui.components.popups.DialogStep(
            title = "Partner Bot Username",
            message = "Enter the @username of the partner's bot. This ensures we only process messages from the correct source in the group.",
            icon = Icons.Filled.PersonSearch,
            iconTint = PrimaryLight,
            confirmText = "Save",
            customContent = {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(100)
                    try { focusRequester.requestFocus() } catch (e: Exception) {}
                }
                OutlinedTextField(
                    value = partnerUsername,
                    onValueChange = { partnerUsername = it },
                    placeholder = { Text("@partner_bot", color = TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryLight,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryLight
                    )
                )
            }
        )
    )

    com.mobile.superiorchat.ui.components.popups.MultiStepActionDialog(
        steps = steps,
        initialStep = 0,
        cancellable = true,
        onComplete = { onComplete(botToken, groupChatId, partnerUsername) },
        onDismiss = onDismiss
    )
}

@Composable
fun IncomingCallDialog(
    callerName: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    BaseAppDialog(cancellable = false, onDismiss = onDecline) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = com.mobile.superiorchat.theme.CallSuccess.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = null,
                    tint = com.mobile.superiorchat.theme.CallSuccess,
                    modifier = Modifier.padding(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Incoming Call",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$callerName is inviting you to a secure call",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onDecline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(Icons.Filled.CallEnd, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decline", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.mobile.superiorchat.theme.CallSuccess,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(Icons.Filled.Call, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Accept", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AdminReadOnlyInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Admin.READ_ONLY_INFO_TITLE,
        message = PopupTexts.Admin.READ_ONLY_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminRouteMessagesInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Admin.ROUTE_MESSAGES_INFO_TITLE,
        message = PopupTexts.Admin.ROUTE_MESSAGES_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminIAmAdminInfoDialog(
    onDismiss: () -> Unit
) {
    InfoDialog(
        title = PopupTexts.Admin.I_AM_ADMIN_INFO_TITLE,
        message = PopupTexts.Admin.I_AM_ADMIN_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminAppToAppGuideDialog(
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Admin.APP_TO_APP_GUIDE_TITLE,
        message = PopupTexts.Admin.APP_TO_APP_GUIDE_MESSAGE,
        note = PopupTexts.Admin.APP_TO_APP_GUIDE_NOTE,
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
        title = PopupTexts.Admin.CREDENTIALS_INFO_TITLE,
        message = PopupTexts.Admin.CREDENTIALS_INFO_MESSAGE,
        onDismiss = onDismiss
    )
}
