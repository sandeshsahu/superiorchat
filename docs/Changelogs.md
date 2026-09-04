<h1 align="center">
  Version Changelogs
</h1>

<p align="center">
  <strong>Release Notes & Version History</strong>
</p>

---

> [!NOTE]
> This document details major features, UI enhancements, media optimizations, and bug fixes across Superior Chat releases.

---

## Table of Contents
- [Version 1.0.4 (Sep 2026)](#v104)
- [Version 1.0.3 (Aug 2026)](#v103)
- [Version 1.0.2 (Aug 2026)](#v102)
- [Version 1.0.1 (Jul 2026)](#v101)

---

<h2 id="v104">🏷️ Version 1.0.4 — App-to-App Bot Bridge</h2>

### 🚀 New Features
- 🤖 **App-to-App Dual Chat**: Both users can now chat and call directly inside the disguised app via Telegram's Bot-to-Bot group communication mode, eliminating the need for the official Telegram app.
- ⚙️ **Admin Settings Hub**: Dedicated settings screen (`AdminSettings.kt`) managing Read-Only locks, message routing, Admin Mode toggles, credentials, and app lifecycles.
- 🧙‍♂️ **6-Step Setup App Wizard**: Guided multi-role setup wizard with live Telegram API diagnostics (token validation, bot collision checks, admin privileges, and group privacy verification).
- 📱 **Dual QR Code Provisioning**: Generates scoped **Partner QRs (Client)** and **Your QRs (Admin)** with 1024px high-contrast rendering and brightness override.
- 🔒 **Hard Lock Security**: Administrators can permanently lock Admin Settings on their partner's phone (`isPeerLinkHardLocked`) to prevent tampering.
- 🗑️ **Two-Way Batch Message Deletion**: Simultaneously delete up to 100 messages for both users in one batch with live progress and automatic background sync (`[SYS-MSG-DELETE]`).
- 👤 **Dynamic Per-Sender Profiles**: Resolves and displays individual sender photos, names, and bios for each group bubble and profile dialog, backed by local Room database persistence.
- 🔄 **Intelligent 3-Mode Credential Routing**: The sync and credentials engine dynamically auto-detects and adapts across 3 modes: Direct 1-on-1 Telegram DM (with personal user ID), Normal Group (chatting in a group without partner bot restrictions, accepting messages from all group members with per-sender profile resolution), and App-to-App Dual Bot Bridge (group with strict partner bot username filtering and bidirectional signaling).

---

### 📞 Calling & Audio Enhancements
- 🔄 **Background Calls & OS PiP**: Active voice and video calls now seamlessly continue in the background while multitasking, with native OS floating Picture-in-Picture for video streams.
- 🎛️ **Symmetrical 2x3 Call Controls**: Upgraded in-call controls into an intuitive 2x3 grid with independent "Mute Call" (inbound audio muting), dynamic earpiece/speaker routing, and adaptive minimize buttons.
- 🔔 **Flavor-Specific Call Ringing & Alerts**:
  - **Original Flavor**: Plays continuous audible ringtone, standard heavy vibration, and renders native Android `CallStyle` heads-up notification cards with caller avatars and Accept/Decline actions.
  - **Camouflage Flavors**: Operates with completely silent audio, subtle low-amplitude pocket double-pulse haptics (no table rattle), and natural decoy alerts (e.g. live weather updates).
- 📜 **Call Direction & History Logging**: Call History accurately logs `Incoming` vs `Outgoing` directions with live duration timers and distinct directional icons.
- 🏝️ **In-App Call Minimizing & Status Pill**: Incoming calls can be minimized into the floating Status Pill, letting users continue browsing chats while ringing.
- 🚫 **Contextual Call Blocked Diagnostics**: Tapping the call button when unavailable displays clear diagnostic dialogs (offline, missing credentials, or Telegram rate limits).

---

### 💬 Messaging & Markdown Precision
- 📝 **Telegram-Grade Markdown Renderer**: Full Compose support for nested styles, code blocks, spoilers (`||spoiler||`), strikethroughs, and underlines across bubbles, reply previews, and pinned headers.
- 📋 **Inline Code Tap-to-Copy**: Tapping an inline or preformatted code block copies only that specific snippet to the clipboard.
- 🖼️ **Universal Caption Tap-to-Copy & Editing**: Tapping text on media captions copies clean text to clipboard; editing sent media captions is now fully supported.
- 🎭 **Batch Message Reactions**: Consolidated batch reactions with debouncing, staggering, and dynamic circular avatar badges for reactors.
- 🛡️ **Live API Ban Protection & Rate Limiting**: Intelligent flood control engine preventing bot bans and token revoking by enforcing a 19 msgs/min group sliding window, 0.5s dispatch pacing, HTTP 429 retry handling, and live input countdown banners.
- 🛡️ **Plain-Text Formatting Fallback**: Automatic plain-text retry fallback on entity parsing errors, guaranteeing 100% deliverability for raw text with unmatched formatting symbols.

---

### 🛡️ Stealth & Security
- 🛡️ **3-Tier Window Security Shield**: Returning from background, expanding from PiP, or dialing the secret code strictly enforces the Fake Crash and PIN Lock Screen first, while keeping active call audio streaming unbroken in the background.
- 🔕 **In-App Notification & Ringing Suppression**: Automatically silences notifications, vibrations, and heads-up banners across all flavors when actively chatting inside the app.
- 👁️ **Dynamic "Hide from Recent" Setting**: Converted static manifest exclusion into an Admin Settings toggle, defaulting to enabled for camouflage flavors and disabled for Original.
- 🔢 **In-Call Secret Dialer Code & Tile Access**: Dialing the secret entry code or tapping the Quick Settings tile during an active call safely maximizes the app without terminating the call.
- 🔍 **Role-Scoped Universal QR Scanner**: Settings screen strictly accepts Client QRs; Admin Settings strictly accepts Admin QRs; Chat Screen scanner auto-applies both.
- 🚨 **System Signal Hardening**: Internal control signals are authenticated at the core engine level and automatically purged from Telegram by the receiver without leaking to chat logs.
- 💾 **Offline-First Schema Migration (v14)**: Upgraded Room schema to version 14, capturing edit timestamps, entities, replies, and thumbnails offline.

---

### 🎨 UI & UX Enhancements
- 🧘 **Zen Mode Settings Control**: Added a toggle in App Settings to enable or disable the Sleeping Miku inactivity resting mode.
- 🪟 **Responsive Adaptive Dialogs**: Upgraded dialogs with `AdaptiveDialogActions` (dynamic button wrapping) and `ScrollableDialogBody` (scroll-state indicator arrows).
- 📱 **Montserrat Branding & Drawer Polish**: Clean Montserrat branding header in the navigation drawer and relocated project links into the About page.
- 🎨 **Theme-Safe Semantic Status Colors**: Standardized completed calls to theme-safe Emerald Green and termination events to ErrorRed across all color themes.
- ⚠️ **Manual Credentials Warning Dialogs**: Disconnection warnings before manual credentials editing to prevent accidental misconfigurations.
- 🔋 **Streamlined Startup Permissions**: Sequential native OS permission prompts (Notification $\rightarrow$ Battery Optimization) with custom explanatory guidance dialogs only upon denial.

---

### 🛠️ Bug Fixes & Reliability
- ⚡ **Hanging Long-Poll Socket & Sync Delay**: Updating bot credentials or switching chat modes previously left the 80-second OkHttp long-poll connection hanging on the old socket, causing inbound messages to be delayed or dropped. Fixed via `ACTION_RESTART_POLLING`, which immediately aborts pending connections (`dispatcher.cancelAll()`) and resets the update sequence to sync instantly.
- 🗑️ **Setup App Uninstall Dialog Lingering**: Tapping "Uninstall" on the Setup App handoff dialog previously left the custom dialog visible on screen behind the system uninstall prompt. The dialog state now dismisses immediately upon tapping the action.
- 📦 **Version-Aware App Updates**: Setup App checks the installed camouflage app's build version and prompts in-place updates when required.

---

<h2 id="v103">🏷️ Version 1.0.3 — UI Polish, PlaySupport Flavor & Setup Security</h2>

### 🚀 New Features
- 📜 **Call History**: Added a comprehensive call history log with rich status reporting (Completed, Missed, Network Error), clear history options, and an interactive bottom sheet for call details.
- 💬 **Timeline Call Events**: Call statuses are now visually embedded directly into the chat timeline as interactive, tappable event pills that jump to the Call History.
- 🖼️ **Interactive Media Captions**: Added full bidirectional support for image and video captions. You can now type captions directly on media before sending them, seamlessly syncing with Telegram.
- 🗃️ **Universal Vault Decoy**: Completely replaced flavor-specific decoys with a high-fidelity native Media Vault. When forced to enter the emergency PIN (1234), the app opens a fully functional photo/video vault, providing absolute deniability.
- 🎭 **Play Support Disguise**: Introduced the new `playSupport` stealth flavor. Disguises the background sync engine as a "Google Play Support" process, showing randomized Play Store promotions or "Trending apps" instead of message alerts.

---

### 🛡️ Stealth & Privacy
- 🔐 **Fake Crash Extra Protection**: An optional extra security layer available across all flavors. On startup, it displays a fake "App not responding" crash dialog. Users must secretly press and hold the crash title (e.g., "`Superior Chat isn't responding`") for 2 seconds to bypass it and enter the app. Rebuilt using a true transparent activity layer for seamless entry.
- 🛡️ **App Lock PIN Support**: Added a built-in App Lock feature allowing you to set a secure custom PIN to lock the app when minimized.
- 🔐 **Advanced QR Cryptography**: Upgraded SetupApp provisioning to use robust `AES-GCM` encryption paired with `PBKDF2WithHmacSHA256` key derivation.
- 📌 **Dual-Mode QR Security**: Setup configurations can now be PIN-protected (`SEC_QR`) or sent directly (`DIR_QR`), giving the admin total control over credential exposure.
- 🚫 **Magic Prefix Filtering**: The QR scanner instantly rejects random non-app QR codes without processing them, stopping misleading PIN prompts.
- 📱 **Dynamic Dialer Codes**: Custom secret dialer codes can now be configured in the settings for `captivePortal` and `playSupport` flavors.
- 🏃 **Screen Lock Auto-Kill**: The app now listens for the device screen turning off (`ACTION_SCREEN_OFF`) and instantly locks and completely removes itself from recent apps (`finishAndRemoveTask()`) to prevent any background snooping if you lock your phone mid-chat.
- 🕵️ **QR Code Privacy Export**: When saving the configuration QR code to your device, it now uses randomized UUIDs for filenames instead of hardcoded name (e.g. `superiorchat_config.png`) to prevent application identity leakage.

---

### 🎨 UI Enhancements
- 🎨 **Dynamic Theme Engine**: Introduced theme selector in App Settings featuring 4 color themes (Lavender, Sage, Amber, Rose). All UI components (headers, dialogs, icons, attachment menus) now seamlessly synchronize with the active theme instantly without requiring an app restart.
- 🧘 **Interactive Zen Mode**: Added a "Zen Mode" top bar state that replaces the standard Header & Icons with an animated character (Sleeping Miku). It triggers automatically after 5 seconds of inactivity or manually via tapping the header, providing a cleaner resting interface.
- 🖌️ **Semantic Color Refactor**: Completely deprecated the static `Primary` color token and eliminated all hardcoded hex colors across the entire application in favor of 40+ specific semantic tokens, laying the groundwork for robust future UI customizations.
- ⚡ **QR Scanner Enhancement**: Entirely overhauled the QR scanning engine. It is now 10x faster, supports pinch-to-zoom, features animated glowing scan lines, haptic feedback, and a built-in torch toggle.
- 💬 **Reply Jumping**: Tapping a replied message now instantly snaps to the target message and applies a sleek 2-second visual highlight effect.
- 📌 **Message Pin Indicators**: Added a pin icon directly to the message status row for pinned text, media, and documents.
- ⏳ **Queued Media Indicators**: Replaced static cancel buttons with a "Waiting..." indeterminate spinner for queued media transfers.
- 🌈 **Color-Coded Settings Architecture**: Fully restyled the Settings Screen with categorized color hierarchies (Security in Primary, Flavor Specific in Secondary, Developer in Warning, Danger Zone in ErrorRed) for a much cleaner UX.
- 📺 **Interactive Stealth Previews**: Added built-in animated visual tutorials in App Settings that demonstrate exactly how to configure and execute the secret entry sequences (features high-fidelity animations for the Dialer code, Tile setup, Tile sequence, and Fake Crash bypass).
- 🎨 **Centralized UX Catalog**: Popup dialogs across the app were rewritten using a centralized, color-coded, animated composable architecture for maximum consistency.

---

### 🛠️ Bug Fixes & Performance
- 💥 **Call Engine Crash Fix**: Fixed a critical SQLite database constraint crash that occurred when a call failed validation before a conversation row was established.
- 📷 **Scanner Memory Leaks**: Fixed severe memory leaks and frame drops when dealing with CameraX buffers and PIN entry overlays simultaneously.
- ⚙️ **Flavor Architecture Cleanup**: Cleaned up AndroidManifest rules for Android 14. Foreground services and permissions (like `dataSync` and `remoteMessaging`) are now strictly isolated to their respective flavors to avoid manifest merging leaks.
- 🚫 **Unnecessary Permissions Removed**: Removed redundant `READ_MEDIA_AUDIO` permission requests, as `MANAGE_EXTERNAL_STORAGE` inherently grants access.
- 🎨 **Original Flavor Polish**: Replaced the ugly default SMS icon used in notifications with a professional Telegram-style paper plane vector icon.

---

<h2 id="v102">🏷️ Version 1.0.2 — WebRTC Calling & Media Export</h2>

### 🚀 New Features
- 📞 **One-Way Calling**: Introduced private voice and video calling feature from scratch.
- 🔗 **Telegram Invites**: Send an instant "Join Call" button to the chat for the other person to join.
- 🖼️ **Telegram OS PiP**: Full OS-level floating Picture-in-Picture for Telegram guests with live video and audio visualizer support.
- 💾 **Save to Device**: Securely save photos, videos, and documents directly to your phone's gallery.
- ⚙️ **Advanced Admin Settings**: Added 3 Steps configurations in SetupApp Admin Mode for Options like Auto Download Media, Notifications, Screenshots, and Custom Call Servers before generating QR Code.
- 🗃️ **Custom Setup QR Bundle**: SetupApp can now generate encrypted QR codes containing custom main app settings; scanning automatically configures credentials and settings on the main app.
- ☢️ **Danger Zone**: Consolidated "Clear Chat", "Clear Credentials", and "Uninstall" options into a unified, organized **Application Page -> App Settings -> Danger Zone** secure bottom sheet.
- ℹ️ **Application Page**: Refactored and overhauled the Settings Screen to act as a central hub for **System Checks**, **About**, and **Shortcuts** for **Logs**, **Permissions**, and **App Settings**.

---

### 🛡️ Stealth & Privacy
- 📴 **Proximity Sensor**: Screen turns off automatically when held to the ear.
- 🏝️ **Minimized Call Status**: A small floating indicator shows when a call is running in the background.
- 🚫 **Auto-Hangup**: Calls disconnect instantly if the app is minimized to hide Android privacy indicators.
- 🔒 **Tile Access Warning**: Added a critical warning popup preventing accidental lockouts before disabling the "Access by Tile" feature.

---

### 🎨 UI Enhancements
- 📱 **WhatsApp-Style UI**: Immersive, full-screen calling interface.
- 🖼️ **Picture-in-Picture**: Drag and move the floating video window during active calls.
- 🎛️ **Smooth Video Minimizing**: The floating video window now shrinks smoothly without any flashing or lag.
- 📱 **Telegram Minimize Button**: Tap the top-left Minimize icon to float calls over other apps.
- 🎙️ **Live Audio Visualizers**: Avatars pulse in real-time based on voice volume.
- ✨ **Bouncy Buttons**: Added fluid bounce and glow effects to interactive buttons across all screens.
- 🎭 **Smooth Transitions**: Added slick, horizontal sliding animations when navigating through app settings.
- 🪟 **SetupApp Popup Redesign**: Redesigned all SetupApp popups and dialogs to match the main app's popup style and fluid animations.
- 🧼 **Cleaner Settings**: Stripped out bulky background boxes from settings rows for a wider, more professional look.

---

### ⚡ Media & Storage
- 🔄 **Instant Camera Swap**: Switch between front and rear cameras instantly without lag.
- 🎧 **Smart Audio Routing**: Calls automatically switch between the earpiece, speakerphone, and Bluetooth.
- 💾 **Save to device**: Added a "Save" option to the message long-press menu. The app now tells you exactly where your photo or video will be saved (Pictures, Movies, Music, Downloads).

---

### 🌐 Network & Reliability
- 🔄 **Auto-Switching Servers**: The app automatically switches to backup servers if the main calling server goes down.
- ⚙️ **Custom Servers**: Added the ability to set your own custom calling servers in Settings.
- ⚠️ **Custom Server Warnings**: The app warns you before changing to a custom calling server to prevent mistakes.
- 🛑 **Clear Error Messages**: You will now see clear reasons if a call fails (e.g., "Network Offline" or "No Answer").
- 📡 **WebRTC Validation**: SetupApp now validates custom server URLs in real-time to prevent misconfigurations.

---

### 🛠️ Bug Fixes & Performance
- 🎬 **MediaViewer Animation Fix**: Fixed Compose bug causing MediaViewer opening animations to snap abruptly instead of fading in by adding an OS Window attachment delay.
- ⚡ **R8 Release Animation Fix**: Fixed enter animations for all app popups and menus (Settings, Errors, Alerts, Context Menus) skipping or snapping on Release builds due to R8 compilation speed.
- 📷 **Camera Bug Fix**: Fixed a major bug where the camera would get stuck open after scanning a QR code.
- 💥 **Startup Crash Fix**: Fixed a critical bug that caused the app to crash immediately upon opening due to an uninitialized property.
- 💬 **UI Bug Fix**: Fixed "Copy" option incorrectly appearing in the context menu for pure media/file messages that had no text.


---

<h2 id="v101">🏷️ Version 1.0.1 — Feature & Stealth Update</h2>

### 🚀 New Features
- 🌦️ **Weather App Disguise**: Added a fake Weather app disguise to completely hide the chat app.
- 📱 **Setup App Integration**: The setup app can now securely install the Weather app disguise.
- 🔑 **Custom Secret Password**: You can now choose your own secret word to unlock the app from the fake search bar.
- 🎭 **Reaction Badges**: Improved message reactions, grouping them neatly (like `👍 2`) and coloring yours differently from your partner's.
- 📊 **Media Transfer Pill**: Added in-app, ios-style media transfer top floating pill to show live download/upload progress for your files.
- 💀 **Ghost Loading Screens**: Added smooth, shimmering placeholder animations while loading profiles, galleries, and chats.
- 🕒 **12-Hour Timestamps**: Changed message timestamps to show the standard AM/PM format.
- ⚡ **Instant Profile Loading**: User profiles now load instantly from a local database cache.
- 🔍 **Gallery Filters**: You can now filter your gallery by both "Type" (Images/Videos) and "Folder" (Camera/WhatsApp) at the same time.

---

### 🛡️ Stealth & Privacy
- 🔕 **Notification Toggles**: Added a setting to completely silence new message alerts while keeping the app syncing in the background.
- 🔄 **Smart Notifications**: Prevented notification spam; the app now properly resets alerts when messages are read.
- 🌦️ **Live Weather Notifications**: Fake notifications now display real, live weather data based on your location.
- 👻 **Recent Apps Hiding**: The secret app is now 100% invisible in your phone's "Recent Apps" menu.
- 🛡️ **Weather App Offline Mode**: The secret search bar remains fully usable even if the fake weather data fails to load.
- 🧹 **Clear Chat & Wipe Media**: Added an option to completely clear a chat and delete all downloaded photos and videos at once.
- ⚙️ **Clear Credentials & Self Uninstall**: Added an option to clear credentials and completely uninstall the chat app.
- 🗑️ **Auto-Cleanup**: The app now automatically deletes temporary camera and voice note files to save space.
- 🔔 **System Notification Sync**: Turning off notifications in the app now correctly redirects you to your phone's main system settings.

---

### 🎨 UI Enhancements
- 💙 **Beautiful Blue Theme**: Applied a consistent, vibrant blue theme across all buttons, switches, and menus.
- 💬 **Expanding Chat Box**: The chat input now expands properly when typing long, multi-line messages.
- ⚙️ **Organized Settings**: Cleaned up the settings menu with dedicated "Notifications" and "Privacy" sections.
- 🪟 **Polished Popups**: All warning and confirmation dialogs now look consistent and professional.
- ✂️ **Better Image Cropper**: The photo cropper now supports intuitive pinch-to-zoom and panning without getting blurry.
- 🖼️ **Smooth Image Galleries**: Swipe through your media gallery flawlessly without any stutter.
- 🔔 **In-App Alerts**: Replaced ugly system toasts with beautiful, modern popup pills for success and error messages.
- ⌨️ **Faster Typing**: Entering your Bot Token and Chat ID is now noticeably faster and smoother.
- ✨ **Sleek Dialog Transitions**: Fixed a glitch that caused an ugly black or white flash when closing image viewers.

---

### ⚡ Media & Storage
- 📁 **Smart Deduplication**: The app no longer wastes storage space by downloading the same file twice.
- 💾 **Memory Fixes**: Stopped the app from crashing when handling very large image uploads.
- 🖼️ **Native File Icons**: The file explorer now loads system icons for videos and photos instantly.
- 🛡️ **Safe Downloads**: Files will no longer get corrupted if your download is interrupted halfway.

---

### 🛠️ Bug Fixes
- 🔔 **Notification Crashes**: Fixed a bug where tapping a fake weather notification would crash the app.
- 🚀 **Gallery Scrolling Lag**: Fixed heavy frame drops when scrolling through hundreds of photos.
- 🎤 **Voice Note Bugs**: Fixed the voice recorder input box stretching out of proportion.
- 🔑 **Smoother Permissions**: Fixed an annoying issue where the app asked for camera permissions twice.
- 🖱️ **Cursor Visibility Fix**: Fixed a bug where the typing cursor was invisible on dark themes.
- 🌐 **Accurate Network Status**: "Online" and "Offline" status indicators in settings are now completely accurate.
- 🔙 **Settings Navigation Fix**: The Android back button now properly exits sub-menus instead of closing the entire settings screen.
- 🔙 **Attachment Menu Fix**: Pressing the back button while the attachment menu is open now correctly closes the menu instead of exiting the app.
- ⌨️ **Keyboard Search Fix**: The keyboard now correctly shows a "Search" button when using the fake weather app.
- 🔍 **Search Bar Fix**: The app now waits for you to press "Search" before unlocking the hidden chat.
- ❄️ **Chat Freeze Fix**: Fixed a critical bug where opening the app from a notification would freeze the chat screen.
- 👎 **Reaction Toggle Fix**: Fixed a bug where double-tapping a message would accidentally try to remove the other person's reaction.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>
