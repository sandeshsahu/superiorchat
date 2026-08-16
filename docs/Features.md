<h1 align="center">
  Features & Capabilities
</h1>

<p align="center">
  <strong>Key User-Facing Features & Stealth Mechanics</strong>
</p>

---

> [!NOTE]
> This document summarizes the core functional features of Superior Chat. For setup instructions or technical backend details, refer to `Instructions.md` and `Backend.md`.

---

## Table of Contents
- [1. Core Messaging & Chat](#core)
- [2. Secure Voice & Video Calling](#calling)
- [3. Rich Media & Files](#media)
- [4. Profile & Credential Management](#profile)
- [5. Stealth & Privacy Protection](#stealth)
- [6. App Flavors & Disguises](#flavors)
- [7. Modern User Interface](#ui)

---

<h2 id="core">💬 1. Core Messaging & Chat</h2>

- 📝 **Formatted Text**: Send text with rich markdown styling (bold, italic, strikethrough, monospace).
- 🎭 **Quick Reactions**: Double-tap any message to instantly react with emojis (👍 ❤️ 🤣 😱 😢 🔥).
- 🖼️ **Profile Pictures**: View contact profile pictures with sleek initials fallback for unidentified users.
- ✏️ **Message Editing & Deletion**: Edit sent messages on the fly or perform single/bulk message deletions.
- 📌 **Replies & Pinning**: Swipe to reply to specific messages or pin critical messages to the top banner.
- 🏷️ **Status Indicators**: Real-time delivery status updates (`Sending`, `Sent`, `Failed`) and `Edited` labels.
- ✅ **Multi-Selection Mode**: Select multiple messages at once for bulk self-delete or "Delete for everyone".
- 🕰️ **Message Timestamps**: Clear 12-hour (AM/PM) timestamps for all chat messages and system logs.
- 💬 **Multi-line Input**: Smoothly expanding chat input box supporting multi-line text drafting.

---

<h2 id="calling">📞 2. Secure Voice & Video Calling</h2>

- 🎙️ **P2P Voice & Video Calls**: Instant, zero-auth peer-to-peer WebRTC calling directly within the chat interface.
- 📜 **Call History Logs**: Comprehensive call history log tracking answered, missed, and network-failed calls with clear, interactive detail sheets.
- 💬 **Timeline Call Events**: Call statuses are visually embedded directly into the chat timeline as interactive, tappable event pills that jump directly to your call history.
- 🛡️ **One-Way Initiation**: For maximum privacy and to prevent accidental discovery, calls can only be initiated from the Superior Chat app side (Telegram cannot initiate calls).
- 🔄 **Instant Camera Swapping**: Seamlessly swap between front and rear cameras during active calls without dropping connection.
- 🖼️ **OS-Level Picture-in-Picture (Telegram Side)**: Full OS-level floating PiP support for Telegram guest users. Seamlessly floats live video or pulsing audio visualizers over other apps on Chrome, Edge, and iOS Safari.
- 🎧 **Smart Audio Routing**: Native integration with Android's sensors seamlessly switches audio between earpiece, speakerphone, and Bluetooth devices.
- 📱 **Telegram Bot Integration**: Calls trigger an instant "Join Call" inline button via the Telegram Bot, allowing the recipient to join from any browser.
- 🔒 **Cryptographic Call Rooms**: Generates random UUID rooms with secret cryptographic parameters to prevent zero-click mass-surveillance and link scraping.
- ⚡ **Auto-Fallback Engine**: Employs a "Race to Connect" algorithm that dynamically shuffles and tests backup signaling server URLs to ensure calls always connect even if the primary server goes offline.
- ⚙️ **Custom TURN/STUN Servers**: Option to change the default WebRTC servers with your own custom servers for guaranteed reliability (`Application Page -> App Settings > Call Configuration`).

> [!IMPORTANT]
> **Browser Recommendation**: For optimal call performance, zero-lag video rendering, and full hardware support, always use the **Google Chrome** browser.

> [!WARNING]
> **Telegram Side Background Notice**: Do not switch apps or move the browser tab to the background without tapping the **Minimize** (PiP) button first. Moving to the background without PiP prevents audio/video access and may disconnect the call.

> [!WARNING]
> **Network Limitations**: Because this relies on P2P WebRTC, it may not connect reliably out-of-the-box on strict VPNs, certain public WiFi, or symmetric NAT carriers without a dedicated TURN server.

> [!TIP]
> **For Developers**: To understand the call mechanics, host your own engine, or add custom STUN/TURN servers, explore the comprehensive **[WebRTC Documentation Suite](../webrtc/docs/)**. For an in-depth understanding of our privacy & security threat models, refer directly to the **[WebRTC Security Audit](../webrtc/docs/Security.md)**.

---

<h2 id="media">📸 3. Rich Media & Files</h2>

- 📷 **Photos & Videos**: In-app camera capture, high-resolution photo sharing, and video playback with smart thumbnails.
- 🖼️ **Interactive Media Captions**: Full bidirectional support for image and video captions. Type captions directly on media before sending, seamlessly syncing with Telegram.
- 🎙️ **Voice Messages**: Record voice notes featuring dynamic animated waveform visualizations and interactive playback controls.
- 📁 **File & Document Sharing**: Send any file type (PDF, DOC, APK) up to `50MB` and download up to `20MB` with automatic file type icons.
- 📥 **Auto-Download Preferences**: Configure automatic downloading for media and files to save data (`Profile -> Settings > Chat settings`).
- 💾 **Save to Device**: Securely export downloaded photos, videos, audio, and documents directly to your device's native public folders (`Pictures`, `Movies`, `Music`, `Downloads`) with zero trace of the app's identity.
- 📊 **Real-Time Transfers**: Global, expandable overlay displaying live upload/download progress categorized by media type, featuring individual cancellation controls.
- 🗄️ **Integrated File Explorer**: Built-in, hierarchical file browser with recent items, folder navigation, search, and native media thumbnails.
- 🖼️ **Smart Media Picker**: Custom gallery grid neatly organized by albums, featuring camera shortcuts, buttery-smooth swipe gestures, and the ability to **select and send multiple media/files at once**.

---

<h2 id="profile">🤖 4. Profile & Credential Management</h2>

- 🎨 **Profile Customization**: Change the bot's profile photo (using a professional pan-and-zoom cropping tool), display name, and bio directly from within the app.
- 🔑 **Credential Setup**: Quickly configure your Bot Token and Chat ID via manual entry with real-time format validation.
- 📱 **Admin Mode QR Provisioning**: The Telegram-side user can generate an Encrypted QR Code using the `SetupApp` (Admin Mode) and send it to the Superior Chat user, who can instantly scan it to automatically import all connection credentials and settings.
- 🔐 **Advanced QR Cryptography**: Setup configurations use robust `AES-GCM` encryption paired with `PBKDF2WithHmacSHA256` key derivation for enterprise-grade security.
- 📌 **Dual-Mode QR Security**: Generate QR codes that are explicitly PIN-protected (`SEC_QR`) or sent directly (`DIR_QR`), giving the admin total control over credential exposure.
- ⚡ **Turbo QR Scanner**: Entirely overhauled QR scanning engine that is 10x faster, supports pinch-to-zoom, features animated glowing scan lines, haptic feedback, and a built-in torch toggle.
- 🚫 **Magic Prefix Filtering**: The QR scanner instantly rejects random non-app QR codes without processing them, stopping misleading PIN prompts.
- 🕵️ **QR Code Privacy Export**: When saving the configuration QR code to your device, it uses randomized UUID filenames instead of hardcoded names to prevent application identity leakage.
- 🧹 **Chat Cleanup**: Dedicated options to completely wipe local chat history and locally downloaded media from the device.
- ⚠️ **Danger Zone Controls**: Dedicated safety options to clear credentials (with warnings) or completely uninstall the app cleanly.

---

<h2 id="stealth">🛡️ 5. Stealth & Privacy Protection (`Flavor Specific`)</h2>

- 👻 **Icon Concealment**: Completely `Hides` the main application icon from the Android app drawer.
- 🗃️ **Universal Vault Decoy**: High-fidelity native Media Vault. When forced to enter the emergency PIN (`1234`), the app opens a fully functional photo/video vault, providing absolute deniability.
- 🔐 **Fake Crash Extra Protection**: Optional security layer available across all flavors. Displays a fake "App not responding" crash dialog on startup. Users must secretly press and hold the crash title for 2 seconds to bypass it.
- 🛡️ **App Lock PIN Support**: Set a secure custom PIN to lock the app when minimized. Includes dynamically animated PIN dots on the LockScreen to conceal your PIN length.
- 🏃 **Screen Lock Auto-Kill**: Listens for the device screen turning off (`ACTION_SCREEN_OFF`) and instantly locks and completely removes itself from recent apps (`finishAndRemoveTask()`) to prevent any background snooping.
- 📞 **Secret Dialer Access**: Open the application privately by dialing a secret code (`*#*#9131#*#*` or dynamically configure a custom code in settings).
- 🎛️ **Secret Access Via Tile**: **(Carrier Sync)** Open the chat app via a Quick Settings Tile. 
  - *Access sequence*: `ON ➔ OFF ➔ ON and HOLD tile` to enter (Can be disabled from App Settings).
- 🔍 **App Search Interception**: Type a secret phrase (`superior chat`) into the innocent weather search bar and hit Search to silently launch the chat engine. (weather flavor only)
- 🔔 **Camouflage Notifications**: Incoming messages appear as harmless system, carrier, or weather alerts to prevent shoulder-surfing.
- 🔕 **Notification Controls**: Dedicated toggles to disable all app notifications completely, or selectively disable new message notifications.
- 🔄 **Decoy Redirects**: If a snooper clicks the camouflaged notification or taps the Quick Settings tile without the correct sequence, they are instantly redirected to the native Android Network Settings to completely avoid suspicion.
- 🗑️ **Recent Apps Protection**: Automatically hidden and excluded from the phone's recent apps menu to leave zero trace when switching screens.
- 🏃 **Panic & Auto-Kill Mechanisms**: Automatically disconnects active calls and stops any playing music/voice notes the exact moment the app is closed or the home button is pressed.
- 🔒 **Screen Security**: Option to enable/disable blocking of screenshots and screen recording throughout the entire application (`Profile -> Settings > Privacy and Security`).

> [!NOTE]
> We have plans to introduce additional, highly discreet access methods in future updates.

---

<h2 id="flavors">🎭 6. App Flavors & Disguises</h2>

| Flavor Variant | Identity / App Name | Stealth Level | Notification Behavior |
|----------------|---------------------|---------------|-----------------------|
| 🟢 **Original** | Superior Chat | **None** | Standard chat notifications. Ideal for non-stealth or testing use. |
| 🔵 **Weather** | `Weather`<br>*Adaptive weather icons* | **Advanced** | Live, context-aware notifications mimicking real meteorological data. |
| 🔴 **Captive Portal** | `Android System`<br>*(Gear Icon)* | **Maximum** | Camouflaged as system networking alerts based on data states. |
| 🎭 **Play Support** | `Google Play Support`<br>*(Play Protect logo)* | **Maximum** | Disguises as a background Google Play Store process showing random app recommendations and trending alerts. |

**Captive Portal Camouflage States:**
- **Idle**: `"[Carrier] - Standard rates apply"`
- **New Message**: `"[Carrier] - High data usage detected"`
- **Offline**: `"[Carrier] - Internet not connected"`
- **API Issues**: `"[Carrier] - Check your data plan"`

**Play Support Camouflage States:**
- **Idle**: `"Explore Play Points"` or `"Discover The Library"` (Randomized promotional phrases)
- **New Message**: `"[Recommended for you] - Check out the top trending apps of the week."`
- **Offline**: `"[Editors' Choice] - Handpicked apps and games for you."`
- **API Issues**: `"[Special Offer] - Unlock exclusive rewards in your favorite apps."`

**Weather Camouflage States (Powered by Live APIs):**
- **Idle**: `"Currently In [City] • [Condition], [Temp]°C • Humidity [Hum]%"`
- **New Message**: `"Live Update • [City] • [Condition], [Temp]°C • Humidity [Hum]%"`
- **Offline**: `"Offline • Last Known data: [City] • [Condition], [Temp]°C"`
- **API Issues**: `"Failed to reach servers • Last known Data: [Condition], [Temp]°C • [City]"`

*(The app auto-resets to the Idle state when opened to avoid suspicion).*

> [!NOTE]
> We are planning to add more flavors with even stronger dynamic camouflage mechanisms in future releases.

> [!IMPORTANT]
> For complete technical details on how the disguises, entry points, and fake notifications are implemented for each variant, please refer to the specific flavor documentation:
> - **[Captive Portal Details](flavors/CaptivePortal.md)**
> - **[Weather Details](flavors/FlavorWeather.md)**
---

<h2 id="ui">✨ 7. Modern User Interface</h2>

- 🎨 **Material Design 3**: Modern, highly polished dark theme tailored for low-light visual comfort.
- 🎨 **Dynamic Theme Engine**: Built-in theme selector in App Settings featuring 4 themes (Lavender, Sage, Amber, Rose). All UI components seamlessly synchronize with the active theme instantly without requiring an app restart.
- 🧘 **Interactive Zen Mode**: A "Zen Mode" top bar state that replaces the standard Header & Icons with an animated character (Sleeping Miku). It triggers automatically after 5 seconds of inactivity or manually via tapping the header, providing a cleaner resting interface.
- 🌈 **Color-Coded Settings Architecture**: Fully restyled Settings Screen with categorized color hierarchies (Security in Primary, Flavor Specific in Secondary, Developer in Warning, Danger Zone in ErrorRed).
- 💬 **Reply Jumping**: Tapping a replied message now instantly snaps to the target message and applies a sleek 2-second visual highlight effect.
- 📌 **Message Pin Indicators**: Professional pin icons directly on the message status row for pinned text, media, and documents.
- ⏳ **Queued Media Indicators**: Professional "Waiting..." indeterminate spinners for queued media transfers.
- 🎨 **Centralized UX Catalog**: 100% of popup dialogs across the app use a centralized, color-coded, animated composable architecture for maximum consistency.
- 🌊 **Fluid Animations**: Smooth, responsive transitions for message bubbles, media viewports, profile sheets, and screen navigation.
- 🔍 **Full Media Viewer**: Embedded, immersive viewer with zoom, pan, and preview support for images and videos.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>