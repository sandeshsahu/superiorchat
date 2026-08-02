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

---

<h2 id="why-not-native">1. Headless WebView vs Native WebRTC SDK</h2>

Most Android VoIP applications compile Google's native WebRTC C++ library (`org.webrtc`). SuperiorChat intentionally abandons this pattern in favor of a **Headless WebView** running WebRTC via JavaScript (`CallEngine.kt`).

### Why we made this decision:
1. 📦 **Zero Added APK Bloat**: Native C++ WebRTC binaries add between 30 MB–50 MB to an APK. By utilizing the OS-level Android System WebView, we add **0 MB**, keeping the stealth app exceptionally lightweight.
2. ⚡ **Instant Remote Patching**: A bug in native WebRTC requires compiling a new APK and pushing an OTA update. With our web-powered `webrtc.js` engine, signaling fixes and security patches deploy instantly via Vercel without requiring the user to update the app.
3. 🔄 **100% Protocol Parity**: The Android caller and the Telegram web browser guest execute the exact same JavaScript engine, completely eliminating cross-platform SDP parsing bugs.

---

<h2 id="ui-rendering">2. The Hybrid Video Layer Architecture</h2>

Extracting raw high-performance video frames out of a browser `<video>` tag into native Jetpack Compose memory buffers causes severe frame drops and battery drain. 

### Why we made this decision:
Instead of copying pixel buffers, we use a hybrid Z-index approach:
1. `CallScreen.kt` renders the `AndroidView` (WebView) as a full-screen, visually bottom-layer background.
2. The web engine positions the local camera preview in the upper corner using pure CSS (`style.css`).
3. The native Jetpack Compose UI (mute buttons, avatars, PiP borders) is drawn **directly on top** of the WebView with a transparent background.

This creates the illusion of a 100% native calling interface while maintaining zero-lag hardware video acceleration.

---

<h2 id="load-balancing">3. Pre-Flight Server Fallback Validation</h2>

Rather than relying on a single hardcoded server, `CallManager.kt` uses a dynamic client-side fallback engine.

### Why we made this decision:
Free-tier WebRTC servers (like Vercel/PeerJS) are subject to rate-limiting and regional blocking. To guarantee connection reliability:
1. The app shuffles an array of backup domains from `webrtc_urls.xml`.
2. It executes an asynchronous `HttpURLConnection` GET request (`?host=UUID&secret=UUID`).
3. It actively parses the HTTP payload for `<title>Superiorchat Connect</title>` or `id="ui-layer"`.
4. The first server to respond with the correct payload wins the "Race to Connect" and is saved to `Prefs.webrtcBaseUrl`.

This guarantees we never spin up a WebView pointing to a dead or hijacked domain.

---

<h2 id="stealth-pip">4. Custom PiP vs OS-Level PiP</h2>

Android supports system-level Picture-in-Picture (`enterPictureInPictureMode()`), creating a floating video box on the phone's home screen when minimizing an app.

### Why we made this decision:
SuperiorChat is fundamentally a stealth application. Using OS-level PiP would leave a highly visible floating call window floating over the Android home screen, instantly exposing the hidden app to anyone looking at the phone. 
Instead, we built a custom physics engine using Compose `pointerInput` and `detectDragGestures` inside `CallScreen.kt` that strictly confines the PiP window inside the app's boundaries.

---

<h2 id="stealth-background">5. The "No Background Service" Stealth Rule</h2>

If an active call is ongoing and the user minimizes the app (`ON_STOP`), the call **immediately hangs up**.

### Why we made this decision:
To maintain a VoIP call in the background on modern Android, you must run a `ForegroundService` with microphone/camera types. This triggers Android's permanent **Green Privacy Dots** in the system status bar. A stealth calculator or weather app displaying active camera/mic indicators while minimized completely compromises the disguise. Immediate teardown is a mandatory tradeoff for perfect stealth.

---

<h2 id="telegram-limitation">6. Asymmetric Telegram VoIP Workaround</h2>

Official Telegram Bot API accounts do **NOT** support native 1-on-1 MTProto voice or video calls. Bots cannot ring users.

### Why we made this decision:
Because SuperiorChat operates entirely over the Telegram Bot API, we had to engineer an asymmetric web-link bridge:
1. **Initiation**: Calling is strictly **one-way**. The Android app generates a secure 128-bit encrypted link (`call.html?join=UUID&secret=UUID`).
2. **Delivery**: `CallViewModel.kt` sends an `InlineKeyboardMarkup` with a "Join Call" button to the target's chat.
3. **Teardown**: If the recipient ignores it for 45 seconds, the Android app auto-drops the call, and uses `TelegramApi.editMessageText` to surgically delete the "Join Call" button so dead links cannot be scraped or reused.

---

<h2 id="proximity-wakelock">7. Proximity Sensor Earpiece Synchronization</h2>

To mimic a native dialer perfectly, `CallManager.kt` binds to the hardware Proximity Sensor.

### Why we made this decision:
By default, placing the phone to your ear acquires the `PROXIMITY_SCREEN_OFF_WAKE_LOCK`, turning the screen black to prevent accidental touches. However, if a user taps the Speakerphone button, `CallManager` dynamically releases the wakelock. This ensures the screen remains brightly lit while on speaker, allowing the user to interact with the mute and video toggles without fighting the proximity sensor.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>