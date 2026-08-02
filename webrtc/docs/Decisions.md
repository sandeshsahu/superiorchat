<div align="center">
  <h1>📐 Architecture Decision Records (ADR)</h1>
  <p><strong>Design choices, tradeoffs, and stealth rules for SuperiorChat WebRTC</strong></p>
</div>

---

> [!NOTE]
> This document explains **why** SuperiorChat was designed the way it is. It details the rationale behind the Headless WebView, our native visual overlay, and the strict operational constraints required for stealth protection.

---

## 📑 Quick Navigation

- [1. Headless WebView vs Native WebRTC SDK](#why-not-native)
- [2. The Hybrid Video Layer Architecture](#ui-rendering)
- [3. Pre-Flight Server Fallback Validation](#load-balancing)
- [4. Custom PiP vs OS-Level PiP](#stealth-pip)
- [5. The "No Background Service" Stealth Rule](#stealth-background)
- [6. Asymmetric Telegram VoIP Workaround](#telegram-limitation)
- [7. Proximity Sensor Earpiece Synchronization](#proximity-wakelock)
- [8. Zero-Knowledge Hash Routing](#hash-routing)
- [9. The "Fake Video Mute" (UI-Level Camera Hide)](#fake-video-mute)

---

<h2 id="why-not-native">1. Headless WebView vs Native WebRTC SDK</h2>

SuperiorChat abandons standard native WebRTC C++ binaries (`org.webrtc`) in favor of a **Headless WebView** running WebRTC via JavaScript.

### Why we made this decision:
- 📦 **Zero Bloat**: Native C++ binaries add 30-50 MB. The built-in Android WebView adds **0 MB**, keeping the app ultra-lightweight.
- ⚡ **Instant Patching**: Web engines can be updated instantly over the air (Vercel) without forcing users to download APK updates.
- **Result**: Perfect cross-platform compatibility since the Android host and Telegram guest execute the exact same JavaScript engine.

---

<h2 id="ui-rendering">2. The Hybrid Video Layer Architecture</h2>

We render the WebView as a full-screen background layer instead of extracting video frames into Jetpack Compose.

### Why we made this decision:
- 🔋 **Performance**: Copying raw pixel buffers from a browser into native memory causes severe frame drops and battery drain.
- 🎨 **Hybrid Z-Index**: Native Compose buttons (mute, avatars) are drawn with a transparent background *directly on top* of the WebView.
- **Result**: The illusion of a 100% native calling interface while maintaining zero-lag hardware video acceleration.

---

<h2 id="load-balancing">3. Pre-Flight Server Fallback Validation</h2>

Instead of a single hardcoded URL, `CallManager.kt` actively shuffles and pings backup domains before connecting.

### Why we made this decision:
- 🚫 **Rate Limits**: Free-tier static hosts (Vercel, Netlify) can crash or block traffic during spikes.
- 🔍 **Validation**: The app pings a backup URL and strictly parses the response for `id="ui-layer"` before trusting it.
- **Result**: We never spin up a WebView pointing to a dead or hijacked domain.

---

<h2 id="stealth-pip">4. Custom PiP vs OS-Level PiP</h2>

We built a custom floating UI physics engine inside Compose instead of using Android's native Picture-in-Picture (`enterPictureInPictureMode`).

### Why we made this decision:
- 🕵️ **Stealth Risk**: OS-level PiP creates a highly visible floating window on the Android home screen, instantly blowing the app's camouflage.
- 📦 **Containment**: A custom physics engine confines the floating PiP window strictly inside the app's boundaries.
- **Result**: Maximum camouflage is maintained when minimizing or navigating the app.

---

<h2 id="stealth-background">5. The "No Background Service" Stealth Rule</h2>

If the user minimizes the app (`ON_STOP`) during an active call, the call **immediately hangs up**.

### Why we made this decision:
- 🟢 **Privacy Indicators**: Background calls require a `ForegroundService`, which triggers Android's permanent Green Camera/Mic dots in the status bar.
- 🚨 **Suspicion**: A "calculator" showing an active microphone dot on the home screen is an immediate red flag.
- **Result**: Immediate teardown is a mandatory tradeoff for perfect stealth.

---

<h2 id="telegram-limitation">6. Asymmetric Telegram VoIP Workaround</h2>

Calls are strictly **one-way** (Android ringing Telegram), using injected URL buttons in the chat.

### Why we made this decision:
- 🤖 **API Limits**: Official Telegram Bot API accounts do not support native 1-on-1 MTProto voice calls.
- 🗑️ **Auto-Destruction**: If ignored, the Android app surgically deletes the "Join Call" button via `editMessageText`.
- **Result**: Secure, ephemeral connections that cannot be scraped or reused by unauthorized group members.

---

<h2 id="proximity-wakelock">7. Proximity Sensor Earpiece Synchronization</h2>

`CallManager.kt` binds directly to the hardware Proximity Sensor to toggle screen wakelocks.

### Why we made this decision:
- 📱 **Native Feel**: Placing the phone to the ear turns the screen black to prevent accidental touches (`PROXIMITY_SCREEN_OFF_WAKE_LOCK`).
- 🔊 **Speaker Override**: Tapping the speakerphone dynamically releases the wakelock.
- **Result**: The screen stays brightly lit on speaker mode so users can toggle mute/video without fighting the proximity sensor.

---

<h2 id="hash-routing">8. Zero-Knowledge Hash Routing</h2>

SuperiorChat intentionally uses URL Hash fragments (`#join=UUID`) instead of standard Query parameters (`?join=UUID`).

### Why we made this decision:
- 📡 **Server Logging**: Query parameters (`?`) are always sent to the web server and permanently logged in access logs.
- 🛡️ **Mathematical Blinding**: Hash fragments (`#`) are processed strictly locally by the browser and are never sent to the server.
- **Result**: This architecture prevents third-party hosts (like Vercel) from spying on the cryptographic call secrets.

---

<h2 id="fake-video-mute">9. The "Fake Video Mute" (UI-Level Camera Hide)</h2>

Tapping "Mute Video" intentionally hides the video player with CSS instead of turning off the physical camera.

### Why we made this decision:
- 💥 **SDP Crashes**: PeerJS struggles to dynamically add video tracks to an active audio-only call. Turning the camera on later usually crashes the connection.
- 🧊 **Frozen Frames**: Starving the remote device of video frames permanently freezes the recipient's video decoder.
- **Result**: We are forced to keep the camera actively streaming in the background from the very first second to guarantee call stability.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>