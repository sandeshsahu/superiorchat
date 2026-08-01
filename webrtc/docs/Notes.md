<div align="center">
  <h1>📝 Important Developer Notes & Gotchas</h1>
  <p><strong>Critical considerations, stealth rules, and operational gotchas for SuperiorChat WebRTC</strong></p>
</div>

---

> [!IMPORTANT]
> This document collects essential operational notes, hardware gotchas, stealth constraints, and developer rules learned during the development of SuperiorChat WebRTC.

---

## 📑 Quick Navigation

- [1. Stealth & Camouflage Constraints](#stealth-notes)
- [2. Hardware & Audio Routing Gotchas](#hardware-notes)
- [3. Web & Rendering Gotchas](#web-notes)
- [4. Configuration & Server Fallback Notes](#config-notes)
- [5. Telegram Bot Limitations & Call Flow Notes](#telegram-notes)
- [6. Related Documentation](#related-docs)

---

<h2 id="stealth-notes">1. Stealth & Camouflage Constraints</h2>

- 🚫 **No OS-Level Picture-in-Picture (PiP)**: Android's native `enterPictureInPictureMode()` must **never** be used for calls. Doing so creates a floating window on the home screen when minimized, exposing the secret app. All PiP is handled in-app within the Compose layout.
- ⏹️ **Auto-Hangup on App Minimize**: When the app reaches the `ON_STOP` state (minimized), calls are immediately terminated (`endCall()`). Running calls in a background service triggers Android's green camera/microphone privacy dots in the status bar, which compromises stealth.
- 🛡️ **Quiet Crash-Proofing**: Fatal exceptions during calls trigger a quiet process exit (`exitProcess(2)`) rather than an Android system crash dialog, keeping the secret app unexposed.

> 📐 **Design Rationale**: Read more about stealth architectural choices in [Decisions.md](Decisions.md#stealth-pip).

---

<h2 id="hardware-notes">2. Hardware & Audio Routing Gotchas</h2>

- 🎧 **VoIP Audio Mode (`MODE_IN_COMMUNICATION`)**: `CallManager.kt` sets `AudioManager.mode` to `MODE_IN_COMMUNICATION`.
- 👂 **Default Earpiece & Proximity Suppression**: Calls default to the phone earpiece (`setSpeakerphone(false)`). The proximity sensor automatically turns off the screen when held to the ear, but is suppressed when Loudspeaker is activated so users can interact with controls.
- 📱 **Android 12+ Audio Routing (API 31+)**: `CallManager` uses modern `setCommunicationDevice` for Loudspeaker and `clearCommunicationDevice()` for Earpiece/Bluetooth default routing. Legacy `am.isSpeakerphoneOn` is strictly isolated to API < 31 to prevent Bluetooth SCO blockages.
- 🔇 **Media Playback Auto-Stop**: Initiating a call triggers `AudioPlayer.stop()` to immediately halt active voice notes or media playback before setting up call audio focus.

> 🛠️ **Troubleshooting**: Solutions for missing audio or camera locks are available in [Troubleshoot.md](Troubleshoot.md#media-missing).

---

<h2 id="web-notes">3. Web & Rendering Gotchas</h2>

- 📐 **CSS Layout Synchronization**: Compose renders the WebView as a background layer. The camera preview position in `assets/css/call.css` (`body.is-host #localVideo`) **must** mathematically match the padding of `LocalCameraBox` in `CallScreen.kt` (`top = 160.dp, end = 16.dp`).
- ⚡ **Zero-Lag Camera Flip**: Flipping cameras uses `RTCRtpSender.replaceTrack()` to swap video tracks on the fly. Dropping and renegotiating the WebRTC connection creates severe latency and network drops.
- 🔗 **Zombie Link Prevention**: The Telegram web client performs a silent pre-flight DataChannel ping (`checkHostActive`) before rendering the "Join Call" button. If the host is offline, the dead link is safely marked as expired.

> 🏗️ **Architecture Details**: View the full bridge event protocol in [Architecture.md](Architecture.md#bridge).

---

<h2 id="config-notes">4. Configuration & Server Fallback Notes</h2>

- 🌐 **Dynamic Domain Overrides**: Custom URLs configured in **App Settings → Call Configuration** override default fallback domains until **Reset to Default** is tapped.
- 🔒 **Pre-Flight Validation**: Pre-flight HTTP GET checks test candidate domains (`?host=UUID&secret=UUID`), verifying an HTTP 200 response and inspecting the HTML payload for `<title>Superiorchat Connect</title>` or `id="ui-layer"` to ensure a valid engine before connecting.
- 🔄 **Persistent Fallback Updating**: If a fallback URL succeeds during validation while the primary saved URL fails, `CallManager` saves the working domain to `Prefs.webrtcBaseUrl` so future calls directly prioritize the active server.
- 🛑 **Mid-Validation Cancellation Guard**: If the user cancels the call dialog during pre-flight checks, `CallManager` intercepts the state change (`CallState.ENDING`/`IDLE`) and aborts background HTTP validation.
- 📄 **Source Code Fallbacks**: Default server fallback lists are stored in `app/src/main/res/values/webrtc_urls.xml`. Developers compiling custom builds should update this array with their own domains.

> 🚀 **Self-Hosting**: Step-by-step guides for deploying custom servers are available in [Deployment.md](Deployment.md).

---

<h2 id="telegram-notes">5. Telegram Bot Limitations & Call Flow Notes</h2>

- 🤖 **No Native Telegram Bot Calling**: Telegram Bot API accounts cannot place or receive native 1-on-1 MTProto VoIP calls.
- 📲 **One-Way Call Initiation**: Calling is strictly **one-way**. Only the SuperiorChat Android app user can start a call. The Telegram user **cannot** call the Android app.
- 🚦 **UI Initiation State Pipeline (`AppNav.kt`)**: Call initiation moves through explicit UI states: `CONFIRMATION` → `VALIDATING` → `INITIALIZING_HARDWARE` → `SENDING_LINK` → `SUCCESS`/`FAILED_SENDING`.
- 🔗 **WebRTC Invitation Link Workaround**: When calling, the Android app generates a secure 128-bit encrypted join link (`call.html?join=UUID&secret=UUID`), uses public PeerJS signaling, and sends it as an inline keyboard button ("Join Call") to the Telegram user.
- ⏱️ **Join vs Timeout Lifecycles**:
  - **If the recipient joins**: Both sides connect via WebRTC and direct peer-to-peer audio/video flows.
  - **If the recipient ignores or doesn't join**: The 45-second watchdog timer in the app expires, the call auto-hangs up (`NO_ANSWER`), and the Telegram message is updated to remove/disable the Join button so dead links cannot be reused.
- 🔄 **ICE Recovery Guard**: `markConnected()` checks `_callState`. If the call is already `ACTIVE`, it treats subsequent bridge triggers as ICE network recoveries rather than restarting duration timers.
- 📌 **Minimization & StatusPill**: Minimizing an active call retains the `CallScreen` composition layer in `AppNav.kt` while surfacing a top-center `StatusPill` overlay for single-tap call restoration.

---

<h2 id="related-docs">6. Related Documentation</h2>

Explore our complete documentation suite:

| Document | Purpose |
|---|---|
| 🏗️ **[Architecture.md](Architecture.md)** | WebRTC engine structure, JavaScript bridge, and DOM layout layers |
| ⚡ **[Backend.md](Backend.md)** | PeerJS signaling, ICE candidate exchange, and DataChannel protocol |
| 🛡️ **[Security.md](Security.md)** | WebView sandbox limits, secret verification, and privacy threat models |
| 🚀 **[Deployment.md](Deployment.md)** | How to deploy your own WebRTC engine on Vercel, Cloudflare Pages, or VPS |
| 🛠️ **[Troubleshoot.md](Troubleshoot.md)** | Solutions for connection timeouts, permission denials, and server popups |
| 📐 **[Decisions.md](Decisions.md)** | Architectural Decision Records (ADR) detailing design choices and stealth rules |

---

<br>
<p align="center">
  <sub>SuperiorChat WebRTC Calling Architecture</sub>
</p>
