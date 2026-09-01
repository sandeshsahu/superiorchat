package com.mobile.superiorsetup.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import com.mobile.superiorsetup.theme.ErrorRed
import com.mobile.superiorsetup.theme.PrimaryLight

/**
 * Centralized registry of all popup and dialog text contents for Setup App.
 */
object PopupTexts {
    object Admin {
        const val BOT_SETUP_GUIDE_TITLE = "Bot-to-Bot Setup Guide"
        const val BOT_SETUP_GUIDE_MESSAGE =
            "Follow these steps to configure both bots in Telegram:\n\n" +
            "1. Open **@BotFather** on Telegram.\n" +
            "2. Send **/mybots** and choose your first bot.\n" +
            "3. Click **Open** button where text input box is\n" +
            "4. Select bot and go to **Bot Settings** -> find **Bot to Bot Communication Mode** -> **Enable** it.\n" +
            "5. Find **Group Privacy** -> and **Disable** it and **Enable** the **Allow Groups**.\n" +
            "6. Repeat the exact same steps for your second bot.\n" +
            "7. Create a private group, add both bots, and grant them **Admin** rights."

        const val CONNECTION_ERROR_TITLE = "Connection Error"

        const val FLAVOR_OPTIONS_TITLE = "Flavor Specific Options"
        const val FLAVOR_OPTIONS_MESSAGE =
            "These settings allow you to pre-configure secret access codes for disguised builds:\n\n" +
            "• **Weather Flavor**: Sets the secret access word for the weather search bar.\n" +
            "• **Captive Portal / Play Support**: Sets the secret dialer code (`*#*#<code>#*#*`).\n\n" +
            "When your partner scans the QR code, the app will *automatically apply* the setting corresponding to their installed flavor and safely ignore the rest."

        const val CUSTOM_ACCESS_TITLE = "Custom Access Word"
        const val CUSTOM_ACCESS_MESSAGE =
            "Set a secret word or phrase that can be typed into the **Weather** app's search bar to unlock Superior Chat.\n\n" +
            "• *Min 4 to 14 characters*.\n" +
            "• If left empty, default **Superior Chat** is used."

        const val CUSTOM_DIALER_TITLE = "Custom Dialer Code"
        const val CUSTOM_DIALER_MESSAGE =
            "Set a custom numeric code that can be typed in the phone's **Dialer** (`*#*#<code>#*#*`) to open the hidden app.\n\n" +
            "• *2 to 5 numeric digits*.\n" +
            "• If left empty, default `*#*#9131#*#*` is used."

        const val CALL_NOTIFICATIONS_TITLE = "Call Notifications"
        const val CALL_NOTIFICATIONS_MESSAGE =
            "When enabled, incoming voice calls will display notifications and alerts so your partner never misses a call.\n\n" +
            "• **Original Flavor**: Alerts with incoming ringtones.\n" +
            "• **Disguised Flavor**: Shows stealth notifications on incoming calls."

        const val DEVELOPER_WARNING_TITLE = "Developer Setting"
        const val DEVELOPER_WARNING_MESSAGE =
            "This setting is strictly for *Developers*!\n" +
            "Changing the *Server URL* can permanently *Break* the Calling feature. If you are not a developer, please *Cancel* this."

        const val CALL_CONFIG_TITLE = "Call Configuration"
        const val CALL_CONFIG_MESSAGE =
            "You can configure your custom *WebRTC Server* URL for voice calls, or reset it to the default server if you experience connection issues.\n\n" +
            "Check developer's *Github Page* for more information"

        const val SECURITY_WARNING_TITLE = "Security Warning"
        const val SECURITY_WARNING_MESSAGE =
            "Disabling **Require PIN** will generate setup QR codes without two step PIN protection.\n\n" +
            "Anyone who gets access to your QR image (or if it leaks) can able to decrypt your credentials since the full project is opensource. They can **Misuse** of your credentials.\n\n" +
            "Are you sure you want to proceed?"

        const val SERVER_UNREACHABLE_TITLE = "Server Unreachable"
        const val SERVER_UNREACHABLE_MESSAGE =
            "Could not verify *call.html* on the provided server. Please check the URL and ensure the server is accessible."

        const val HARD_LOCK_INFO_TITLE = "Hard Lock Admin Settings"
        const val HARD_LOCK_INFO_MESSAGE =
            "Hard Lock embeds a tamper-proof restriction into the setup QR code.\n\n" +
            "• **When Enabled**:\n" +
            "The recipient device will have its **Read Only** mode permanently locked.\n" +
            "Administrative settings (including PeerLink routing, Admin mode, and Lifecycle options) cannot be modified or unlocked from the app interface.\n\n" +
            "• **Unlocking**:\n" +
            "Can only be unlocked by scanning a new setup QR code with Hard Lock disabled."

        const val PARTNER_READ_ONLY_WARNING_TITLE = "Disable Read-Only for Partner?"
        const val PARTNER_READ_ONLY_WARNING_MESSAGE =
            "Disabling Read-Only on your partner's device grants full access to Admin Settings:\n\n" +
            "• **Stealth Risk**:\n" +
            "If they turn off *Hide from Recent*, Superior Chat will appear in Android's Recent Apps menu, risking immediate discovery.\n\n" +
            "• **Chat Disruption**:\n" +
            "If they accidentally toggle *Route Messages* or alter bot credentials, message routing will break and you will not be able to chat.\n\n" +
            "• **Recommendation**:\n" +
            "Keep Read-Only enabled for your partner to prevent accidental disruption.\n\n" +
            "Are you sure you want to leave Admin Settings unlocked for your partner?"

        const val ADMIN_READ_ONLY_WARNING_TITLE = "Hard-Lock Your Admin Settings?"
        const val ADMIN_READ_ONLY_WARNING_MESSAGE =
            "Enabling Hard Lock on your own QR code will permanently lock Admin Settings on your device:\n\n" +
            "• **Locked Controls**:\n" +
            "You will not be able to modify *Admin Credentials*, adjust *Route Messages*, or toggle *Hide from Recent* locally from the application.\n\n" +
            "• **Unlocking Limitation**:\n" +
            "Once applied, this lock cannot be disabled from settings. It can only be unlocked by scanning a new setup QR code with Hard Lock disabled.\n\n" +
            "• **Recommendation**:\n" +
            "Keep this unlocked for yourself so you can adjust credentials and settings whenever needed.\n\n" +
            "Are you sure you want to hard-lock your own device?"

        const val TELEGRAM_MODE_INFO_TITLE = "Chat on Telegram (Direct DM)"
        const val TELEGRAM_MODE_INFO_MESSAGE =
            "Connect and message directly from your standard Telegram app:\n\n" +
            "• **How It Works**:\n" +
            "You chat directly with your bot in a private 1-on-1 chat from official Telegram. Your partner uses the hidden Superior Chat app.\n\n" +
            "• **Supported Features**:\n" +
            "All core features are supported — one-way voice/video calls (initiated by partner), instant messaging, emoji reactions, media sharing (photos, videos, voice notes), and offline sync.\n\n" +
            "• **Fast Setup**:\n" +
            "Requires only 1 bot token from @BotFather and your Telegram User ID. No private groups needed."

        const val APP_TO_APP_MODE_INFO_TITLE = "Both Use Superior Chat App"
        const val APP_TO_APP_MODE_INFO_MESSAGE =
            "Both you and your partner chat inside the hidden Superior Chat application using secure bot routing:\n\n" +
            "• **Two-Way Calling**:\n" +
            "Both sides can initiate and receive secure voice/video calls anytime.\n\n" +
            "• **Ringing & Disguised Alerts**:\n" +
            "Supports full incoming call ringing on original flavor, and discreet disguised alerts on camouflage flavors (Weather, Dialer, etc.).\n\n" +
            "• **Background Calls (PiP)**:\n" +
            "Enables OS-level Picture-in-Picture (PiP) during active calls so you stay connected even when minimized or switching apps.\n\n" +
            "• **Dedicated Admin Settings**:\n" +
            "Unlocks a dedicated Admin Settings menu where you can manage Read Only mode and toggle stealth options like *Hide from Recent* and **Call Background**."

        const val APP_TO_APP_MODE_INFO_NOTE =
            "Caution: Keep **Hard Lock Admin Settings** enabled for your partner's QR code. Leaving admin settings unlocked on your partner's device risks **Accidental discovery** (if Hide from Recent is turned off) or **Communication Failure** if routing settings are changed."

        const val ADMIN_QR_INFO_TITLE = "Admin QR Configuration"
        const val ADMIN_QR_INFO_MESSAGE =
            "This QR code configures your personal device as **Admin** in App-to-App mode:\n\n" +
            "• **Scanning Target**:\n" +
            "Scan this QR code with **your own** Superior Chat app.\n\n" +
            "• **What It Configures**:\n" +
            "Applies your Bot token and private group ID. Configures your app in Admin Mode with access to Admin Settings, two-way calling controls, and OS-level PiP background calls."

        const val PARTNER_APP_TO_APP_QR_INFO_TITLE = "Partner QR (App-to-App)"
        const val PARTNER_APP_TO_APP_QR_INFO_MESSAGE =
            "This QR code configures your partner's device in **App-to-App mode**:\n\n" +
            "• **Scanning Target**:\n" +
            "Have your **partner scan this QR code** in their Superior Chat app.\n\n" +
            "• **What It Configures**:\n" +
            "Applies their dedicated Bot token, sets your bot's username for automatic message routing, and enables secure two-way communication inside your private group."

        const val PARTNER_TELEGRAM_QR_INFO_TITLE = "Partner QR (Direct Telegram)"
        const val PARTNER_TELEGRAM_QR_INFO_MESSAGE =
            "This QR code configures your partner's device for **Direct Telegram messaging**:\n\n" +
            "• **Scanning Target**:\n" +
            "Have your **partner scan this QR code** in their Superior Chat app.\n\n" +
            "• **What It Configures**:\n" +
            "Connects their app directly to your Telegram Bot in private 1-on-1 DM. You message them directly from your standard Telegram app."
    }
}

// ── Reusable Specialized Dialog Composables ──

@Composable
fun AdminTelegramModeInfoDialog(onDismiss: () -> Unit) {
    InfoDialog(
        title = PopupTexts.Admin.TELEGRAM_MODE_INFO_TITLE,
        message = PopupTexts.Admin.TELEGRAM_MODE_INFO_MESSAGE,
        icon = Icons.Filled.Info,
        iconTint = PrimaryLight,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminAppToAppModeInfoDialog(onDismiss: () -> Unit) {
    InfoDialog(
        title = PopupTexts.Admin.APP_TO_APP_MODE_INFO_TITLE,
        message = PopupTexts.Admin.APP_TO_APP_MODE_INFO_MESSAGE,
        note = PopupTexts.Admin.APP_TO_APP_MODE_INFO_NOTE,
        noteIcon = Icons.Filled.Warning,
        icon = Icons.Filled.Info,
        iconTint = PrimaryLight,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminBotSetupGuideDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.BOT_SETUP_GUIDE_TITLE,
        message = PopupTexts.Admin.BOT_SETUP_GUIDE_MESSAGE,
        icon = Icons.Filled.Info,
        iconTint = PrimaryLight,
        confirmText = "Understood",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminConnectionErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Admin.CONNECTION_ERROR_TITLE,
        message = message,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminFlavorOptionsInfoDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.FLAVOR_OPTIONS_TITLE,
        message = PopupTexts.Admin.FLAVOR_OPTIONS_MESSAGE,
        icon = Icons.Filled.Info,
        iconTint = PrimaryLight,
        confirmText = "Got it",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminCustomAccessInfoDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.CUSTOM_ACCESS_TITLE,
        message = PopupTexts.Admin.CUSTOM_ACCESS_MESSAGE,
        icon = Icons.Filled.Info,
        iconTint = PrimaryLight,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminCustomDialerInfoDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.CUSTOM_DIALER_TITLE,
        message = PopupTexts.Admin.CUSTOM_DIALER_MESSAGE,
        icon = Icons.Filled.Info,
        iconTint = PrimaryLight,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminCallNotificationsInfoDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.CALL_NOTIFICATIONS_TITLE,
        message = PopupTexts.Admin.CALL_NOTIFICATIONS_MESSAGE,
        icon = Icons.Filled.PhoneInTalk,
        iconTint = PrimaryLight,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminDeveloperWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Admin.DEVELOPER_WARNING_TITLE,
        message = PopupTexts.Admin.DEVELOPER_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "I Understand",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminCallConfigInfoDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.CALL_CONFIG_TITLE,
        message = PopupTexts.Admin.CALL_CONFIG_MESSAGE,
        icon = Icons.Filled.Info,
        iconTint = PrimaryLight,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminDisablePinWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Admin.SECURITY_WARNING_TITLE,
        message = PopupTexts.Admin.SECURITY_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Disable",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminServerUnreachableDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.SERVER_UNREACHABLE_TITLE,
        message = PopupTexts.Admin.SERVER_UNREACHABLE_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Okay",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminHardLockInfoDialog(onDismiss: () -> Unit) {
    ActionDialog(
        title = PopupTexts.Admin.HARD_LOCK_INFO_TITLE,
        message = PopupTexts.Admin.HARD_LOCK_INFO_MESSAGE,
        icon = Icons.Filled.Lock,
        iconTint = PrimaryLight,
        confirmText = "Understood",
        dismissText = "",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminPartnerReadOnlyWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Admin.PARTNER_READ_ONLY_WARNING_TITLE,
        message = PopupTexts.Admin.PARTNER_READ_ONLY_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Unlock Settings",
        dismissText = "Keep Locked",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun AdminSelfReadOnlyWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ActionDialog(
        title = PopupTexts.Admin.ADMIN_READ_ONLY_WARNING_TITLE,
        message = PopupTexts.Admin.ADMIN_READ_ONLY_WARNING_MESSAGE,
        icon = Icons.Filled.Warning,
        iconTint = ErrorRed,
        confirmText = "Lock My Settings",
        dismissText = "Cancel",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
