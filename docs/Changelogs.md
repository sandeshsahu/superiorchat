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
- [Version 1.0.2 (Aug 2026)](#v102)
- [Version 1.0.1 (Jul 2026)](#v101)

---

<h2 id="v102">🏷️ Version 1.0.2 — WebRTC Calling & Media Export</h2>

### 🚀 New Features
- 📞 **One-Way Calling**: Introduced private voice and video calling feature from scratch.
- 🔗 **Telegram Invites**: Send an instant "Join Call" button to the chat for the other person to join.
- 💾 **Save to Device**: Securely save photos, videos, and documents directly to your phone's gallery.
- ⚙️ **Advanced Admin Settings**: Added new configuration toggles in SetupApp for Options like Auto Download Media, Notifications, Screenshots, and Custom Call Servers before generating QR Code.
- 🗃️ **QR Configuration Bundle**: QR codes generated in SetupApp now encrypt and bundle all advanced admin settings.
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
- 🎙️ **Live Audio Visualizers**: Avatars pulse in real-time based on voice volume.
- ✨ **Bouncy Buttons**: Added fluid bounce and glow effects to interactive buttons across all screens.
- 🎭 **Smooth Transitions**: Added slick, horizontal sliding animations when navigating through app settings.
- 🪟 **Unified Dialogs**: Upgraded legacy setup popups to match the main app's beautiful animations and styles.
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

### 🛠️ Bug Fixes
- 📷 **Camera Bug Fix**: Fixed a major bug where the camera would get stuck open after scanning a QR code.
- 💥 **Startup Crash Fix**: Fixed a critical bug that caused the app to crash immediately upon opening due to an uninitialized property.

---

<h2 id="v101">🏷️ Version 1.0.1 — Feature & Stealth Update</h2>

### 🚀 New Features
- 🌦️ **Weather App Disguise**: Added a fake Weather app disguise to completely hide the chat app.
- 📱 **Setup App Integration**: The setup app can now securely install the Weather app disguise.
- 🔑 **Custom Secret Password**: You can now choose your own secret word to unlock the app from the fake search bar.
- 🎭 **Reaction Badges**: Improved message reactions, grouping them neatly (like `👍 2`) and coloring yours differently from your partner's.
- 📊 **Media Transfer Panel**: Added a sliding panel that shows the live download/upload progress for your files.
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
