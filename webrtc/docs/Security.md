<div align="center">
  <h1>🛡️ Security, Privacy & Blind-Spot Audit</h1>
  <p><strong>Transparent breakdown of security boundaries, threat models, and real-world vulnerabilities</strong></p>
</div>

---

> [!WARNING]
> **No System is 100% Secure**: While SuperiorChat is built on a Zero-Trust sandbox design, relying on default public infrastructure or maintainer-hosted static pages carries real-world risks. This document transparently discloses all security boundaries, blind spots, and threat scenarios so you can make informed privacy choices.

---

## 📑 Quick Navigation

- [1. Security Sandbox (What is Protected)](#sandbox)
- [2. Known Blind Spots & Real-World Risks](#blind-spots)
- [3. Threat Scenarios & Defenses](#threat-models)
- [4. IP Addresses & Network Tracking](#ip-privacy)
- [5. Why Self-Hosting is Strongly Urged](#self-hosting)
- [6. Related Documentation](#related-docs)

---

<h2 id="sandbox">1. Security Sandbox (What is Protected)</h2>

SuperiorChat runs voice and video calls inside an isolated web environment (WebView) within Android.

### What the Sandbox Guarantees:
- 🔒 **Zero Native File Access**: The web calling engine **cannot** read your Telegram bot token, chat history, SQLite database, photos, or files.
- 🛑 **Strict Bridge Restrictions**: The web engine can only send predefined event strings to Android (`"connected"`, `"ended"`, `"audio_level"`). It cannot execute arbitrary Android native code or shell commands.
- 🌐 **Domain-Level Permission Lock**: Camera and microphone access are granted **exclusively** to your configured domain. If the WebView is redirected to an untrusted domain, Android automatically denies hardware permissions.

---

<h2 id="blind-spots">2. Known Blind Spots & Real-World Risks</h2>

We refuse to market this system as "impenetrable." Here are the actual blind spots and risks when using default configurations:

### ⚠️ Blind Spot A: Free-Tier Hosting Quota Exhaustion
The default static WebRTC pages (`call.html`) provided with the app are hosted on free-tier platforms (Vercel, Cloudflare Pages, Netlify).
- **The Risk**: Free hosting accounts have strict bandwidth, build, and request limits. If traffic spikes or quotas are hit, the hosting provider will shut down or suspend the static pages.
- **The Impact**: Default calls across all app installs will instantly fail with "Invalid URL" or server error popups until the next billing cycle or until users switch to self-hosted URLs.

### ⚠️ Blind Spot B: Maintainer Account or Domain Compromise
If the project maintainer's hosting account, GitHub repository, or custom domain is ever compromised, expired, or abandoned:
- **The Risk**: An attacker gaining access to the static hosting account could modify `webrtc.js` or `app.js` to serve tampered JavaScript.
- **What is NOT compromised**: The Android sandbox still prevents the attacker from reading your phone files, messages, or Telegram tokens.
- **What IS at risk**: Because the Android app trusts the verified domain for camera/mic access, a tampered script could theoretically open secondary WebRTC data channels or stream camera feeds to an unauthorized third party during an active call.

### ⚠️ Blind Spot C: Public Signaling Server Outages & Rate Limits
By default, the app uses the free public PeerJS broker (`0.peerjs.com`) to coordinate call setup.
- **The Risk**: Public signaling brokers can crash, experience WebSocket drops, or enforce IP rate limits without notice, causing calls to get stuck on "Waiting...".

---

<h2 id="threat-models">3. Threat Scenarios & Defenses</h2>

### Scenario A: Call Hijacking & Eavesdropping
- **Threat**: An attacker guesses a call Room ID and attempts to join the call stream to listen in.
- **Defense**: Every call uses a 128-bit cryptographically random Room ID and Secret key (`?host=UUID&secret=UUID`). The host verifies the secret before accepting connections. Once connected, all subsequent incoming join attempts are locked out and disconnected immediately.

### Scenario B: Supply Chain Tampering
- **Threat**: Default static hosting assets are altered or intercepted.
- **Defense**: Users can completely eliminate this risk by deploying their own copy of the open-source `webrtc/` folder to their personal free hosting account and entering their custom domain in **Settings → Call Configuration**.

---

<h2 id="ip-privacy">4. IP Addresses & Network</h2>

### Peer-to-Peer IP Exposure
WebRTC connects audio and video directly between your phone and the Telegram guest.
- 📍 **Public IP Reveal**: Both participants can see each other's public IP address using basic network tools.
- 📡 **STUN Server Logging**: The web engine uses Google's public STUN servers (`stun.l.google.com:19302`) for NAT lookup. Google can log the IP addresses that query its STUN server.
- 🔌 **Signaling Metadata**: The public PeerJS broker sees the IP addresses of both devices during the initial handshake (though it cannot view call media).

### How to Fix IP Exposure:
- Deploy your own private PeerJS signaling server (`peerjs --port 9000`).
- Configure a private **TURN Relay Server** in `config.js` to route all media through a proxy and hide IP addresses entirely.

---

<h2 id="self-hosting">5. Why Self-Hosting is Strongly Urged</h2>

The default public static pages exist **strictly for convenience and quick testing**. 

Relying on default maintainer infrastructure carries ongoing risks of bandwidth exhaustion, domain changes, or third-party outages.

**We strongly urge all users to deploy the static `webrtc/` folder to their own free account on Vercel, Cloudflare Pages, or Netlify.** It takes under 2 minutes, costs nothing, and gives you 100% control over your uptime, privacy, and security.

> 🚀 **Self-Hosting Instructions**: Step-by-step setup guides for Vercel, Cloudflare Pages, Netlify, and VPS are available in [Deployment.md](Deployment.md).

---

<h2 id="related-docs">6. Related Documentation</h2>

| Document | Purpose |
|---|---|
| 🚀 **[Deployment.md](Deployment.md)** | Step-by-step guide to hosting your own static WebRTC engine |
| 🛠️ **[Troubleshoot.md](Troubleshoot.md)** | Solutions for quota limits, server failures, and permission blocks |
| ⚡ **[Backend.md](Backend.md)** | Explanation of PeerJS signaling, ICE candidate exchange, and DataChannel |
| 🏗️ **[Architecture.md](Architecture.md)** | Technical breakdown of Android WebView layering and JS bridge |
| 📐 **[Decisions.md](Decisions.md)** | Architectural Decision Records (ADR) detailing design choices and stealth rules |
| 📝 **[Notes.md](Notes.md)** | Essential developer notes, hardware gotchas, and stealth constraints |

---

<br>
<p align="center">
  <sub>SuperiorChat WebRTC Security & Privacy Audit</sub>
</p>
