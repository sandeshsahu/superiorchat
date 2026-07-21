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
- [2. Rich Media & Files](#media)
- [3. Profile & Credential Management](#profile)
- [4. Stealth & Privacy Protection](#stealth)
- [5. App Flavors & Disguises](#flavors)
- [6. Modern User Interface](#ui)

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

<h2 id="media">📸 2. Rich Media & Files</h2>

- 📷 **Photos & Videos**: In-app camera capture, high-resolution photo sharing, and video playback with smart thumbnails.
- 🎙️ **Voice Messages**: Record voice notes featuring dynamic animated waveform visualizations and interactive playback controls.
- 📁 **File & Document Sharing**: Send any file type (PDF, DOC, APK) up to `50MB` and download up to `20MB` with automatic file type icons.
- 📊 **Real-Time Transfers**: Global, expandable overlay displaying live upload/download progress categorized by media type, featuring individual cancellation controls.
- 🗄️ **Integrated File Explorer**: Built-in, hierarchical file browser with recent items, folder navigation, search, and native media thumbnails.
- 🖼️ **Smart Media Picker**: Custom gallery grid neatly organized by albums, featuring camera shortcuts, multi-file support, and buttery-smooth swipe gestures.

---

<h2 id="profile">🤖 3. Profile & Credential Management</h2>

- 🎨 **Profile Customization**: Change the bot's profile photo (using a professional pan-and-zoom cropping tool), display name, and bio directly from within the app.
- 🔑 **Credential Setup**: Quickly configure your Bot Token and Chat ID via QR Code scanning or manual entry with real-time format validation.
- 🧹 **Chat Cleanup**: Dedicated options to completely wipe local chat history and locally downloaded media from the device.
- ⚠️ **Danger Zone Controls**: Dedicated safety options to clear credentials (with warnings) or completely uninstall the app cleanly.

---

<h2 id="stealth">🛡️ 4. Stealth & Privacy Protection (`Flavor Specific`)</h2>

- 👻 **Icon Concealment**: Completely `Hides` the main application icon from the Android app drawer.
- 📞 **Secret Dialer Access**: Open the application privately by dialing a secret code (`*#*#9131#*#*`).
- 🎛️ **Secret Access Via Tile**: Open the chat app via a Quick Settings Tile. 
  - *Access sequence*: `ON ➔ OFF ➔ ON and HOLD tile` to enter (Can be disabled from Application Settings).
- 🔔 **Camouflage Notifications**: Incoming messages appear as harmless system/carrier alerts to prevent shoulder-surfing.
- 🔄 **Decoy Redirects**: If a snooper clicks the camouflaged notification or taps the Quick Settings tile without the correct sequence, they are instantly redirected to the native Android Network Settings to completely avoid suspicion.
- 🗑️ **Recent Apps Protection**: Excluded from the phone's recent apps list to leave zero trace when switching screens.
- 🔒 **Screen Security**: Universal option to block screenshots and screen recording throughout the entire application.

> [!NOTE]
> We have plans to introduce additional, highly discreet access methods in future updates.

---

<h2 id="flavors">🎭 5. App Flavors & Disguises</h2>

| Flavor Variant | Identity / App Name | Stealth Level | Notification Behavior |
|----------------|---------------------|---------------|-----------------------|
| 🟢 **Original** | Superior Chat | **None** | Standard chat notifications. Ideal for non-stealth or testing use. |
| 🔴 **Captive Portal** | `Android System`<br>*(Gear Icon)* | **High** | Camouflaged as system networking alerts based on data states. |

**Captive Portal Camouflage States:**
- **Idle**: `"[Carrier] - Standard rates apply"`
- **New Message**: `"[Carrier] - High data usage detected"`
- **Offline**: `"[Carrier] - Internet not connected"`
- **API Issues**: `"[Carrier] - Check your data plan"`

*(The app auto-resets to the Idle state when opened to avoid suspicion).*

> [!NOTE]
> We are planning to add more flavors with even stronger dynamic camouflage mechanisms in future releases.

---

<h2 id="ui">✨ 6. Modern User Interface</h2>

- 🎨 **Material Design 3**: Modern, highly polished dark theme tailored for low-light visual comfort.
- 🌊 **Fluid Animations**: Smooth, responsive transitions for message bubbles, media viewports, profile sheets, and screen navigation.
- 🔍 **Full Media Viewer**: Embedded, immersive viewer with zoom, pan, and preview support for images and videos.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>