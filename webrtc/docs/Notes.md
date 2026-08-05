<div align="center">
  <h1>Important Developer Notes & Gotchas</h1>
  <p><strong>Critical considerations, stealth rules, and operational gotchas for SuperiorChat WebRTC</strong></p>
</div>

---

> [!IMPORTANT]
> This document collects essential operational notes, hardware gotchas, stealth constraints, and developer rules strictly derived from the actual SuperiorChat WebRTC and Android implementation.

---

## 📑 Quick Navigation

- [1. Stealth & Security Constraints](#stealth)
- [2. Hardware & Audio Routing Gotchas](#hardware)
- [3. WebRTC & PeerJS Constraints (webrtc.js)](#webrtc)
- [4. Telegram Link Lifecycle](#telegram)
- [5. WebView Sandbox Rules (CallEngine.kt)](#webview)
- [6. Development Progress & Commit History](#commits)

---

<h2 id="stealth">1. Stealth & Security Constraints</h2>

- 🚫 **No OS-Level Picture-in-Picture (PiP)**: Android's native `enterPictureInPictureMode()` must **never** be used for calls. Doing so creates a floating window on the home screen when minimized, exposing the secret app. All PiP is handled strictly within the Jetpack Compose layout using dynamic Drag Physics (`CallScreen.kt`).
- ⏹️ **Auto-Hangup on App Minimize**: When the app reaches the `ON_STOP` state (minimized), calls must immediately be terminated. Running calls in a background service triggers Android's green camera/microphone privacy dots in the system status bar, completely compromising stealth.
- 🔒 **Cryptographic Call Validation**: `webrtc.js` rigorously guards both `call` and `connection` events. If a connecting peer's `metadata.secret` does not perfectly match the host's expected 128-bit UUID secret, the connection is instantly closed. This mathematically prevents third-party attackers from scraping PeerJS to hijack active host WebRTC engines.

---

<h2 id="hardware">2. Hardware & Audio Routing Gotchas</h2>

- 🎧 **VoIP Audio Mode (`MODE_IN_COMMUNICATION`)**: `CallManager.kt` enforces `AudioManager.mode = MODE_IN_COMMUNICATION`. If left in `MODE_NORMAL`, audio routing breaks entirely and WebRTC audio suffers severe dropouts.
- 👂 **Proximity Sensor vs Speakerphone**: The proximity sensor natively acquires `PROXIMITY_SCREEN_OFF_WAKE_LOCK` to turn off the screen when near the ear. However, `CallManager` actively suppresses this wakelock the moment Loudspeaker (`isSpeakerphoneOn`) is toggled, ensuring users can interact with the screen freely.
- 📱 **Android 12+ Audio Routing (API 31+)**: Legacy `AudioManager.isSpeakerphoneOn` fails to route Bluetooth audio correctly on API 31+. Therefore, Android 12+ strictly relies on `setCommunicationDevice()`, prioritizing `TYPE_BT_SCO (7)` and `TYPE_BT_A2DP (8)` using raw integer literals to bypass known Kotlin toolchain compilation issues.

---

<h2 id="webrtc">3. WebRTC & PeerJS Constraints (webrtc.js)</h2>

- ⚡ **Zero-Lag Camera Flips**: `flipCamera()` executes `RTCRtpSender.replaceTrack()` to instantly swap video tracks at the hardware level. We do not tear down or renegotiate the WebRTC SDP connection, preventing dropped frames and connection lag.
- ⏱️ **ICE Zombie Watchdog**: If a guest's internet drops hard, WebRTC fires an ICE `disconnected` state. `webrtc.js` begins a strict 15-second watchdog timer. If the network does not recover (`connected` or `completed`), the Android host is forced to drop the call to prevent permanent battery-draining zombie wakelocks.
- 🔊 **Audio Visualizer**: The real-time volume indicator calculates sensitivity by reading frequencies from an `AnalyserNode` and transmitting the `audio_level` JSON event over the JS Bridge. The Android UI (`CallScreen.kt`) binds this float value to the avatar glow ring.

---

<h2 id="telegram">4. Telegram Link Lifecycle</h2>

- 📲 **One-Way Initiation**: Telegram Bot API accounts cannot place MTProto VoIP calls. Calling is strictly one-way (Android → Telegram).
- 🔗 **Link Expiration & Dead Links**: To prevent "zombie links", `webrtc.js` fires a silent DataChannel ping (`checkHostActive`) to ensure the host is alive before rendering the Join button on the web. 
- 🧹 **Telegram Message Cleanup**: `CallViewModel.kt` listens for the `ENDING` state. Once a call drops, it dynamically calls `TelegramApi.editMessageText` to wipe the Inline Keyboard (`replyMarkup`) from the target's Telegram chat, physically deleting the "Join Call" button and updating the text to "Call Ended" with the exact formatted duration.

---

<h2 id="webview">5. WebView Sandbox & UI Rendering Rules</h2>

- 🌍 **Strict Origin Whitelisting**: `CallEngine.kt` uses `WebChromeClient` to explicitly verify that `request.origin` matches the exact URL validated in `Prefs.webrtcBaseUrl`. Any origin mismatches result in an instant `request.deny()` for camera and microphone permissions.
- ✅ **Pre-Flight Validation**: Before launching a call, `CallManager` tests candidate URLs with an HTTP GET. It specifically parses the HTML payload, looking for `<title>Superiorchat Connect</title>` or `id="ui-layer"`. If missing, the app assumes the server is hijacked/offline and falls back to a backup domain from `webrtc_urls.xml`.

> [!IMPORTANT]
> **📐 CSS Layout Synchronization**
> Jetpack Compose renders the WebView as a full-screen background layer. To perfectly align the native `LocalCameraBox` border with the underlying WebRTC video, the CSS rules (`style.css` `.show-local-pip`) **must** mathematically match the Compose padding in `CallScreen.kt` (`top = 160.dp, end = 16.dp`). Any desync will cause the native border to float away from the actual video stream.

---

<h2 id="commits">6. Development Progress & Commit History</h2>

All development iterations, WebKit optimization progress, bug fixes, and feature releases for the WebRTC signaling and web client codebase (`superiorchat-connect`) are publicly tracked on GitLab.

- 🦊 **GitLab Repository**: **[gitlab.com/sandeshsahu/superiorchat-connect](https://gitlab.com/sandeshsahu/superiorchat-connect)**
- 📜 **Full Commit Log**: Track every step-by-step technical commit, single-sink WebRTC performance fixes, and cross-browser Picture-in-Picture enhancements directly on the **[GitLab Commit History](https://gitlab.com/sandeshsahu/superiorchat-connect/-/commits/main)**.

> [!TIP]
> **Tracing Technical Iterations**:
> Every WebRTC milestone—including WebKit RTCP sink conflict resolutions, pre-warmed `readyState` PiP handling, and Telegram WebApp integration—is committed with comprehensive technical logs in the repository commit history.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>