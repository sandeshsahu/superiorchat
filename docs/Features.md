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
- 🔄 **Instant Camera Swapping & PiP**: Seamlessly swap between front/rear cameras or minimize the active call into a floating Picture-in-Picture window without dropping connection.
- 🎧 **Smart Audio Routing**: Native integration with Android's sensors seamlessly switches audio between earpiece, speakerphone, and Bluetooth devices.
- 📱 **Telegram Bot Integration**: Calls trigger an instant "Join Call" inline button via the Telegram Bot, allowing the recipient to join from any browser.
- 🔒 **Cryptographic Call Rooms**: Generates random UUID rooms with secret cryptographic parameters to prevent zero-click mass-surveillance and link scraping.
- ⚡ **Auto-Fallback Engine**: Employs a "Race to Connect" algorithm that dynamically shuffles and tests backup signaling server URLs to ensure calls always connect even if the primary server goes offline.

> [!WARNING]
> **Network Limitations**: Because this relies on P2P WebRTC, it may not connect reliably out-of-the-box on strict VPNs, certain public WiFi, or symmetric NAT carriers without a dedicated TURN server.

> [!TIP]
> **For Developers**: To understand the call mechanics, host your own engine, or add custom STUN/TURN servers, explore the comprehensive **[WebRTC Documentation Suite](../webrtc/docs/)**. For an in-depth understanding of our privacy & security threat models, refer directly to the **[WebRTC Security Audit](../webrtc/docs/Security.md)**.

---

<h2 id="media">📸 3. Rich Media & Files</h2>

- 📷 **Photos & Videos**: In-app camera capture, high-resolution photo sharing, and video playback with smart thumbnails.
- 🎙️ **Voice Messages**: Record voice notes featuring dynamic animated waveform visualizations and interactive playback controls.
- 📁 **File & Document Sharing**: Send any file type (PDF, DOC, APK) up to `50MB` and download up to `20MB` with automatic file type icons.
- 💾 **Save to Device**: Securely export downloaded photos, videos, audio, and documents directly to your device's native public folders (`Pictures`, `Movies`, `Music`, `Downloads`) with zero trace of the app's identity.
- 📊 **Real-Time Transfers**: Global, expandable overlay displaying live upload/download progress categorized by media type, featuring individual cancellation controls.
- 🗄️ **Integrated File Explorer**: Built-in, hierarchical file browser with recent items, folder navigation, search, and native media thumbnails.
- 🖼️ **Smart Media Picker**: Custom gallery grid neatly organized by albums, featuring camera shortcuts, multi-file support, and buttery-smooth swipe gestures.

---

<h2 id="profile">🤖 4. Profile & Credential Management</h2>

- 🎨 **Profile Customization**: Change the bot's profile photo (using a professional pan-and-zoom cropping tool), display name, and bio directly from within the app.
- 🔑 **Credential Setup**: Quickly configure your Bot Token and Chat ID via QR Code scanning or manual entry with real-time format validation.
- 🧹 **Chat Cleanup**: Dedicated options to completely wipe local chat history and locally downloaded media from the device.
- ⚠️ **Danger Zone Controls**: Dedicated safety options to clear credentials (with warnings) or completely uninstall the app cleanly.

---

<h2 id="stealth">🛡️ 5. Stealth & Privacy Protection (`Flavor Specific`)</h2>

- 👻 **Icon Concealment**: Completely `Hides` the main application icon from the Android app drawer.
- 📞 **Secret Dialer Access**: Open the application privately by dialing a secret code (`*#*#9131#*#*`).
- 🎛️ **Secret Access Via Tile**: Open the chat app via a Quick Settings Tile. 
  - *Access sequence*: `ON ➔ OFF ➔ ON and HOLD tile` to enter (Can be disabled from Application Settings).
- 🔍 **App Search Interception**: Type a secret phrase (`superior chat`) into the innocent weather search bar and hit Search to silently launch the chat engine. (weather flavor only)
- 🔔 **Camouflage Notifications**: Incoming messages appear as harmless system, carrier, or weather alerts to prevent shoulder-surfing.
- 🔄 **Decoy Redirects**: If a snooper clicks the camouflaged notification or taps the Quick Settings tile without the correct sequence, they are instantly redirected to the native Android Network Settings to completely avoid suspicion.
- 🗑️ **Recent Apps Protection**: Excluded from the phone's recent apps list to leave zero trace when switching screens.
- 🔒 **Screen Security**: Universal option to block screenshots and screen recording throughout the entire application.

> [!NOTE]
> We have plans to introduce additional, highly discreet access methods in future updates.

---

<h2 id="flavors">🎭 6. App Flavors & Disguises</h2>

| Flavor Variant | Identity / App Name | Stealth Level | Notification Behavior |
|----------------|---------------------|---------------|-----------------------|
| 🟢 **Original** | Superior Chat | **None** | Standard chat notifications. Ideal for non-stealth or testing use. |
| 🔵 **Weather** | `Weather`<br>*Adaptive weather icons* | **Advanced** | Live, context-aware notifications mimicking real meteorological data. |
| 🔴 **Captive Portal** | `Android System`<br>*(Gear Icon)* | **Maximum** | Camouflaged as system networking alerts based on data states. |

**Captive Portal Camouflage States:**
- **Idle**: `"[Carrier] - Standard rates apply"`
- **New Message**: `"[Carrier] - High data usage detected"`
- **Offline**: `"[Carrier] - Internet not connected"`
- **API Issues**: `"[Carrier] - Check your data plan"`

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
- 🌊 **Fluid Animations**: Smooth, responsive transitions for message bubbles, media viewports, profile sheets, and screen navigation.
- 🔍 **Full Media Viewer**: Embedded, immersive viewer with zoom, pan, and preview support for images and videos.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>