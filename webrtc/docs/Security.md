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

<h2 id="open-source">2. The Open Source Security Model & Encryption</h2>

SuperiorChat is 100% open-source across both the Android application and the WebRTC signaling engine. This architectural transparency provides critical security advantages:

- 🔒 **End-to-End Encryption (E2EE)**: All WebRTC audio, video, and data channels are End-to-End Encrypted by default using DTLS-SRTP. The signaling server (even if it is a third-party public server) is only used to exchange connection coordinates (SDP). The server owner **cannot** decrypt, listen to, or view your media streams.
- 🔍 **Auditability (No Security by Obscurity)**: Security researchers can independently verify that there are no hidden backdoors, undocumented APIs, or obfuscated telemetry being sent to third parties. The 128-bit cryptographic secret generation (`CallManager.kt`) and the connection rejection logic (`webrtc.js`) are fully visible and verifiable.
- 🏗️ **True Supply Chain Independence**: Open source enables true self-hosting. Users are not simply changing a URL in the app; they have the ability to compile the Android app from source and deploy the exact same WebRTC static engine to their own private infrastructure. This completely severs any reliance on proprietary servers or the maintainer's default infrastructure.

---

<h2 id="threat-models">3. Threat Scenarios & Engine Defenses</h2>

The following outlines theoretical attacks that apply to WebRTC systems, and how the codebase defends against them.

### Threat A: Zero-Click WebRTC Hijacking & Scraping
- **The Vulnerability**: Free PeerJS signaling servers are public. Attackers can write scrapers to listen for active PeerIDs and attempt to force a WebRTC call to an Android device, attempting to turn on the camera and microphone without consent (Zero-Click).
- **The Defense**: `CallManager.kt` generates a 128-bit cryptographic `secret` UUID alongside the Room ID. `webrtc.js` intercepts all incoming `call` and `connection` events. If the incoming payload does not contain a `metadata.secret` that perfectly matches the host's expected secret, the connection is instantly closed before media tracks are requested.

### Threat B: Origin Spoofing & Malicious Hosts
- **The Vulnerability**: If a user points the app to a malicious server in **Application page $\rightarrow$ App Settings $\rightarrow$ Call Configuration**, that server controls the entire WebRTC engine. The host can serve a modified `webrtc.js` designed to silently stream the camera/microphone to a third-party server, steal call metadata, or log connections.
- **The Defense (Accepted Risk)**: The app **cannot** protect you from a server you explicitly configure. The built-in URL verification only checks for accidental typos (like entering a dead link), it does *not* defend against a malicious host forging a valid response.
- **The Only Solution**: Do **NOT** use unknown WebRTC pages. For true security, you must deploy your own WebRTC pages from a private repository that only you control. Even the default servers provided by this repository's maintainer should be considered a potential risk. If you choose not to self-host, you assume 100% of the responsibility for any privacy breaches or data leaks caused by the server owner.

### Threat C: DataChannel Payload Injection (XSS)
- **The Vulnerability**: An attacker sends malicious payloads through the WebRTC `DataChannel` (used for video sync) to crash the host or execute XSS.
- **The Defense**: The `dataConn.on('data')` listener rigidly parses incoming JSON. It only acts on predefined `MSG.VIDEO_STATE` and `MSG.FACING_MODE` types. Malformed JSON or injected JavaScript is swallowed in an empty `catch` block and ignored.

### Threat D: "Zombie" Link Re-use
- **The Vulnerability**: A Telegram user forwards the "Join Call" link to a public group, allowing unauthorized users to click the link hours after the call ended.
- **The Defense**: `CallViewModel.kt` uses the Telegram Bot API (`editMessageText`) to surgically delete the inline keyboard button the second the call drops. Furthermore, `webrtc.js` executes a pre-join DataChannel ping (`checkHostActive`)—if the host is inactive, the web UI refuses to render the call screen.

### Threat E: The "Fake Video Mute" Privacy Leak
- **The Vulnerability**: When a user taps "Disable Video", the app currently only hides the video player on the screen. It does **not** physically stop the camera hardware. The camera continues to capture and send live video over the network to the other person on the call. The person on the other end could theoretically inspect their browser's hidden network data to view your "muted" video feed without your knowledge.
- **The Defense (Currently Pending)**: We temporarily rely on this "visual-only" mute due to a strict limitation in our connection engine (PeerJS). If a call is started in "Audio Only" mode (without initially turning on the camera), PeerJS configures an audio-only connection. If the user later decides to turn on their camera mid-call, PeerJS struggles to dynamically add a new video stream to the active call and usually crashes. Therefore, the app is forced to turn on the camera from the very first second to keep the video connection open, even if the user has their video muted. I am currently researching a way to implement a safe hardware-level fix for this issue in future updates without breaking the connection.

---

<h2 id="ip-privacy">4. IP Exposure & P2P Characteristics</h2>

WebRTC is fundamentally a Peer-to-Peer (P2P) technology. There is no central server proxying the audio/video by default.

- 📍 **Public IP Reveal**: Both participants (the Android host and the Telegram guest) **can see each other's public IP address**. A guest can inspect network traffic in their browser console to find the host's location.
- 📡 **Google STUN Logging**: The engine relies on Google's public STUN (`stun.l.google.com:19302`) to punch through routers. Google can log the IP addresses of devices using this service.
- 🔌 **Signaling Metadata**: The public PeerJS signaling broker sees the IP addresses of both devices during the initial handshake (though it cannot decrypt the media streams).

**The Mitigation (TURN Servers)**: By default, SuperiorChat only uses STUN servers, meaning connections are strictly P2P and your IP is exposed. To achieve true anonymity, you **must** deploy your own Coturn (TURN) server and manually add it to `webrtc/assets/js/call/config.js`. A TURN server acts as a proxy relay, routing all media traffic through itself so peers cannot see each other's direct IP addresses.

---

<h2 id="supply-chain">5. Infrastructure & Supply Chain Risks</h2>

> [!CAUTION]
> **Using the default hosted pages provided by the maintainer (or any unknown third party) is STRICTLY NOT RECOMMENDED for privacy-critical use cases.** 

The default static pages and signaling servers are provided **strictly as a convenience** for testing. While we prioritize your privacy, relying on third-party infrastructure always carries a non-zero risk. 

**Why you must self-host:**
- 🕵️ **Malicious Code Injection**: The owner of the host (or a hacker who compromises their account) can secretly modify the JavaScript (`app.js`) to steal your cryptographic call secrets straight from your browser.
- 📜 **Covert Logging**: They can quietly inject logging mechanisms to track when you call, how long you call, and who you connect with.
- 🚫 **Quota Exhaustion**: Free-tier public servers (like Vercel or PeerJS Cloud) may crash or suspend service unexpectedly if traffic spikes.

### Disclaimer of Liability
The maintainer and developers of SuperiorChat are **NOT RESPONSIBLE** for any privacy breaches, data leaks, or service interruptions that occur if you choose to rely on the default or third-party infrastructure. You assume all risks associated with using someone else's server.

> [!IMPORTANT]
> To guarantee true privacy and eliminate supply-chain risks, **users must deploy their own static webpage and signaling server.**
> 
> 🚀 **Self-Hosting Instructions**: Read the step-by-step guide in [Deployment.md](Deployment.md) to set up your own secure infrastructure in minutes.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>