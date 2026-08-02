<div align="center">
  <h1>🛡️ Security, Privacy & Threat Models</h1>
  <p><strong>Breakdown of sandbox boundaries, known vulnerabilities, and architectural defenses</strong></p>
</div>

---

> [!WARNING]
> **No System is 100% Secure**: We do not market this engine as impenetrable. Relying on default public infrastructure carries inherent risks (downtime, supply-chain tampering, metadata logging). This document details the WebRTC implementation's known vulnerabilities and the exact code defenses in place to mitigate them.

---

## 📑 Quick Navigation

- [1. Android Sandbox Boundaries (The JS Bridge)](#sandbox)
- [2. The Open Source Security Model](#open-source)
- [3. Threat Scenarios & Engine Defenses](#threat-models)
- [4. IP Exposure & P2P Characteristics](#ip-privacy)
- [5. Infrastructure & Supply Chain Risks](#supply-chain)

---

<h2 id="sandbox">1. Android Sandbox Boundaries (The JS Bridge)</h2>

Because the WebRTC engine runs in JavaScript, it is completely isolated from the Android operating system via a WebView sandbox (`CallEngine.kt`).

### Operational Reality:
- 🛑 **No Filesystem Access**: The web engine **cannot** read the Android SQLite database, Telegram bot tokens, app preferences, or photo gallery. It is completely blind to the rest of the application.
- 🚧 **Strict Bridge Enforcement**: The `JavascriptInterface` (`app.js` → `CallEngine.kt`) only accepts predefined string actions (e.g., `"connected"`, `"audio_level"`). It cannot execute arbitrary Android system commands.
- 🌍 **Origin Lock**: Camera and microphone access (`WebChromeClient`) is strictly hardcoded to the verified URL saved in `Prefs.webrtcBaseUrl`. If the WebView navigates away to an unverified site, Android instantly severs hardware access.

---

<h2 id="open-source">2. The Open Source Security Model</h2>

SuperiorChat is 100% open-source across both the Android application and the WebRTC signaling engine. This architectural transparency provides critical security advantages:

- 🔍 **Auditability (No Security by Obscurity)**: Security researchers can independently verify that there are no hidden backdoors, undocumented APIs, or obfuscated telemetry being sent to third parties. The 128-bit cryptographic secret generation (`CallManager.kt`) and the connection rejection logic (`webrtc.js`) are fully visible and verifiable.
- 🏗️ **True Supply Chain Independence**: Open source enables true self-hosting. Users are not simply changing a URL in the app; they have the ability to compile the Android app from source and deploy the exact same WebRTC static engine to their own private infrastructure. This completely severs any reliance on proprietary servers or the maintainer's default infrastructure.

---

<h2 id="threat-models">3. Threat Scenarios & Engine Defenses</h2>

The following outlines theoretical attacks that apply to WebRTC systems, and how the codebase defends against them.

### Threat A: Zero-Click WebRTC Hijacking & Scraping
- **The Vulnerability**: Free PeerJS signaling servers are public. Attackers can write scrapers to listen for active PeerIDs and attempt to force a WebRTC call to an Android device, attempting to turn on the camera and microphone without consent (Zero-Click).
- **The Defense**: `CallManager.kt` generates a 128-bit cryptographic `secret` UUID alongside the Room ID. `webrtc.js` intercepts all incoming `call` and `connection` events. If the incoming payload does not contain a `metadata.secret` that perfectly matches the host's expected secret, the connection is instantly closed before media tracks are requested.

### Threat B: Origin Spoofing & Malicious Hosts
- **The Vulnerability**: If a user is tricked into pointing the app to a malicious server in **Settings → Call Configuration**, an attacker could serve a modified `webrtc.js` designed to silently stream the camera to a third-party server.
- **The Defense**: The Android app performs a strict HTTP GET pre-flight check before launching the WebView. It parses the HTML payload for `<title>Superiorchat Connect</title>` or `id="ui-layer"`. If the server returns an unexpected payload (like a phishing page or a raw directory), the app instantly blocks the connection.

### Threat C: DataChannel Payload Injection (XSS)
- **The Vulnerability**: An attacker sends malicious payloads through the WebRTC `DataChannel` (used for video sync) to crash the host or execute XSS.
- **The Defense**: The `dataConn.on('data')` listener rigidly parses incoming JSON. It only acts on predefined `MSG.VIDEO_STATE` and `MSG.FACING_MODE` types. Malformed JSON or injected JavaScript is swallowed in an empty `catch` block and ignored.

### Threat D: "Zombie" Link Re-use
- **The Vulnerability**: A Telegram user forwards the "Join Call" link to a public group, allowing unauthorized users to click the link hours after the call ended.
- **The Defense**: `CallViewModel.kt` uses the Telegram Bot API (`editMessageText`) to surgically delete the inline keyboard button the second the call drops. Furthermore, `webrtc.js` executes a pre-join DataChannel ping (`checkHostActive`)—if the host is inactive, the web UI refuses to render the call screen.

---

<h2 id="ip-privacy">4. IP Exposure & P2P Characteristics</h2>

WebRTC is fundamentally a Peer-to-Peer (P2P) technology. There is no central server proxying the audio/video by default.

- 📍 **Public IP Reveal**: Both participants (the Android host and the Telegram guest) **can see each other's public IP address**. A guest can inspect network traffic in their browser console to find the host's location.
- 📡 **Google STUN Logging**: The engine relies on Google's public STUN (`stun.l.google.com:19302`) to punch through routers. Google can log the IP addresses of devices using this service.
- 🔌 **Signaling Metadata**: The public PeerJS signaling broker sees the IP addresses of both devices during the initial handshake (though it cannot decrypt the media streams).

**The Mitigation**: To achieve anonymity, users **must** self-host a TURN server (Coturn) to act as a proxy relay, preventing peers from seeing each other's direct IPs.

---

<h2 id="supply-chain">5. Infrastructure & Supply Chain Risks</h2>

The default static pages are hosted on free-tier platforms (Vercel) and use free public signaling (PeerJS). 

**Infrastructure Constraints**:
- **Quota Exhaustion**: If the app experiences a massive spike in traffic, Vercel may suspend the static pages. Default calls across all users will fail until quotas reset.
- **Maintainer Compromise**: If the project maintainer's GitHub or Vercel account is compromised, an attacker could theoretically push malicious JavaScript to all users relying on the default URL.

This is why **Self-Hosting is heavily recommended for strict security requirements**. Deploying the static files to a personal hosting account eliminates these centralized supply chain risks entirely. 

> 🚀 **Self-Hosting Instructions**: View [Deployment.md](Deployment.md) for step-by-step guides on deploying the engine and custom TURN servers.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>