<div align="center">
  <h1>Superiorchat Connect</h1>
  <p><strong>Headless WebRTC Calling Engine for SuperiorChat</strong></p>
  
  <p>
    <img src="https://img.shields.io/badge/PeerJS-000000?style=for-the-badge&logo=peerjs&logoColor=white" alt="PeerJS">
    <img src="https://img.shields.io/badge/WebRTC-333333?style=for-the-badge&logo=webrtc&logoColor=white" alt="WebRTC">
    <img src="https://img.shields.io/badge/Javascript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JS">
  </p>
</div>

---

> [!IMPORTANT]
> **Superiorchat Connect** is the web engine that powers encrypted voice and video calls for SuperiorChat. It runs silently inside an Android WebView during calls while providing a standalone web UI for Telegram recipients.

---

## 📑 Table of Contents
- [1. Visual Roles (Host vs. Guest)](#overview)
- [2. Android ↔ JavaScript Bridge Protocol](#bridge)
- [3. Camera Layout Coordinates](#css-coupling)
- [4. Deployment & Self-Hosting](#deployment)
- [5. Documentation Directory](#docs)

---

<h2 id="overview">1. Visual Roles (Host vs. Guest)</h2>

The web application adapts dynamically based on URL parameters:

- 📱 **Host Mode (Android App)**: Loaded with `call.html?host=<UUID>&secret=<UUID>`. The HTML UI elements are hidden. The native Android Compose layout displays the calling controls while the web page processes audio and video in the background.
- 💬 **Guest Mode (Telegram Browser)**: Loaded with `call.html?join=<UUID>&secret=<UUID>`. Displays a full web calling interface (buttons, timers, avatars) for the Telegram recipient.

> 📐 **Design Rationale**: Learn why we chose a web engine over native compiled libraries in [docs/Decisions.md](docs/Decisions.md#why-not-native).

---

<h2 id="bridge">2. Android ↔ JavaScript Bridge Protocol</h2>

### Android → JavaScript (Commands Sent to Engine)
- `window.androidToggleMute()` — Mutes or unmutes local microphone
- `window.androidToggleVideo()` — Toggles local camera stream
- `window.androidToggleSwapVideo()` — Swaps local and remote video positions
- `window.androidFlipCamera()` — Switches between front and back camera
- `window.androidEndCall()` — Terminates the call session

### JavaScript → Android (Events Sent to App)
- `onWebRTCEvent("ready")` — Connected to signaling server
- `onWebRTCEvent("connected")` — Call active and streaming media
- `onWebRTCEvent("audio_level")` — Mic volume meter for avatar pulse effect
- `onWebRTCEvent("ended")` — Call disconnected

> 🏗️ **Full Protocol**: View the detailed event table and lifecycle sequence in [docs/Architecture.md](docs/Architecture.md#bridge).

---

<h2 id="css-coupling">3. Camera Layout Coordinates ⚠️</h2>

> [!WARNING]
> The Android app (`CallScreen.kt`) draws native controls over the web view layer. **The local camera position in `assets/css/call.css` must match the Compose layout coordinates**:
> ```css
> body.is-host #localVideo {
>     top: calc(160px + env(safe-area-inset-top, 0px));
>     right: 16px;
>     width: 108px;
>     height: 148px;
> }
> ```

---

<h2 id="deployment">4. Deployment & Self-Hosting</h2>

Superiorchat Connect is a completely static web application that requires no backend database or Node.js server. 

> 🚀 **Self-Hosting Guide**: Follow our step-by-step instructions for deploying via GitHub/GitLab integration to Vercel, Cloudflare Pages, Netlify, or VPS in [docs/Deployment.md](docs/Deployment.md).

---

<h2 id="docs">5. Documentation Directory</h2>

Explore our complete documentation suite:

| Document | Purpose |
|---|---|
| 🏗️ **[Architecture.md](docs/Architecture.md)** | WebRTC engine structure, directory layout, JS bridge, and DOM layers |
| ⚡ **[Backend.md](docs/Backend.md)** | PeerJS signaling, ICE candidate exchange, and DataChannel protocol |
| 🛡️ **[Security.md](docs/Security.md)** | WebView sandbox limits, secret verification, and privacy threat models |
| 🚀 **[Deployment.md](docs/Deployment.md)** | How to deploy your own WebRTC engine on Vercel, Cloudflare Pages, or VPS |
| 🛠️ **[Troubleshoot.md](docs/Troubleshoot.md)** | Solutions for connection timeouts, permission denials, and server popups |
| 📐 **[Decisions.md](docs/Decisions.md)** | Architectural Decision Records (ADR) detailing design choices and stealth rules |
| 📝 **[Notes.md](docs/Notes.md)** | Essential developer notes, hardware gotchas, and stealth constraints |

---

<br>
<p align="center">
  <sub>SuperiorChat WebRTC Calling Engine</sub>
</p>
