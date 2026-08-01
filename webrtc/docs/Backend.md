<div align="center">
  <h1>⚡ Backend & Networking Mechanics</h1>
  <p><strong>Signaling, peer discovery, ICE NAT punching, and media stream pipelines</strong></p>
</div>

---

> [!NOTE]
> SuperiorChat uses a **peer-to-peer (P2P)** architecture. Audio and video flow directly between devices without passing through a central server. The "backend" consists of public signaling helpers, network connection setup (ICE/STUN), and encrypted data channels.

---

## 📑 Quick Navigation

- [1. Signaling & Peer Discovery](#signaling)
- [2. Connecting Through Firewalls (STUN/ICE)](#ice)
- [3. Secret Verification & Security](#security)
- [4. Real-Time DataChannel Sync](#datachannel)
- [5. Media Stream Quality & Processing](#media)
- [6. Network Resilience & Auto-Recovery](#resilience)
- [7. Related Documentation](#related-docs)

---

<h2 id="signaling">1. Signaling & Peer Discovery</h2>

Before two devices can stream audio and video directly to each other, they must introduce themselves over the internet. This setup phase is called **signaling**.

- **Signaling Helper (PeerJS)**: The system uses WebSocket signaling to exchange connection data.
- **Host Registration**: The Android host registers with a unique, randomly generated ID.
- **Guest Join**: The Telegram user opens the invitation link containing the host's ID and connects automatically.
- **Reconnection Handling**: If the signaling WebSocket drops momentarily, the active audio/video call continues without interruption while signaling reconnects in the background.

> 🏗️ **Architecture Details**: To see how the WebRTC engine connects to the Android UI, see [Architecture.md](Architecture.md#topology).

---

<h2 id="ice">2. Connecting Through Firewalls (STUN/ICE)</h2>

Devices on mobile networks or home Wi-Fi are usually behind routers and firewalls. WebRTC uses **ICE** and **STUN** servers to help devices find their public internet addresses and establish a direct connection.

- **STUN Server**: Discovers your public IP address so the recipient device knows where to send audio and video.
- **Default STUN**: Uses Google's free STUN service (`stun:stun.l.google.com:19302`).
- **Connection Monitor**: The system actively monitors connection health. If the network drops completely, a 15-second timer starts. If the network recovers, the call continues; if not, the call safely ends.

> 🛡️ **Privacy Context**: Read about IP visibility and how to set up a private relay server in [Security.md](Security.md#ip-privacy).

---

<h2 id="security">3. Secret Verification & Security</h2>

To prevent unauthorized users from guessing call IDs and attempting to join, every call URL includes a hidden 128-bit cryptographic secret parameter.

When a guest attempts to connect:
1. The host inspects the connection request.
2. If the guest's secret key does not match the host's secret key, the connection is instantly rejected.
3. Once a call is active, all new incoming join requests are locked out automatically.

> 🛡️ **Security Details**: Read the full Zero-Trust threat model in [Security.md](Security.md#threat-models).

---

<h2 id="datachannel">4. Real-Time DataChannel Sync</h2>

In addition to audio and video streams, a lightweight encrypted **DataChannel** runs directly between both devices. It handles instant status updates:

| Message Type | Purpose |
|---|---|
| `VIDEO_STATE` | Notifies the other participant when you turn your camera on or off so their UI updates immediately. |
| `FACING_MODE` | Informs the recipient when you switch between front and rear cameras so the video stream mirrors correctly. |

---

<h2 id="media">5. Media Stream Quality & Processing</h2>

### Audio Enhancements
Audio streams are processed directly by browser audio engines to ensure clarity:
- 🔊 **Echo Cancellation**: Prevents feedback loops when using speakers.
- 🎙️ **Noise Suppression**: Filters out background noise.
- 🎚️ **Auto Gain Control**: Automatically balances voice volume levels.

### Zero-Lag Camera Flipping
Switching between front and back cameras updates the active video stream instantly without tearing down or reconnecting the call session.

---

<h2 id="resilience">6. Network Resilience & Auto-Recovery</h2>

- **Pre-Flight Server Validation (`CallManager.kt`)**: Before opening the call interface, `CallManager` executes pre-flight HTTP GET checks (`call.html?host=UUID&secret=UUID`) with a 3000ms timeout against candidate servers. It verifies an HTTP 200 response and validates HTML contents (`<title>Superiorchat Connect</title>` or `id="ui-layer"`). If a fallback server succeeds, `CallManager` automatically persists it as the active base URL in preferences.
- **Pre-Flight Host Check**: Before rendering the guest call screen, the browser silently checks if the host is online and ready. If the host disconnected, the guest sees a clean "Call Ended" message immediately.
- **Offline Warning Banner**: If your internet drops during a call, a red notification banner alerts you immediately while the app attempts to restore the connection.

> 🛠️ **Troubleshooting**: If you experience connection drops or "Network Error" popups, follow the fixes in [Troubleshoot.md](Troubleshoot.md#stuck-waiting).

---

<h2 id="related-docs">7. Related Documentation</h2>

For complete architecture details, deployment options, and troubleshooting steps, refer to our other guides:

| Document | Purpose |
|---|---|
| 🏗️ **[Architecture.md](Architecture.md)** | WebRTC engine structure, JavaScript bridge, and DOM layout layers |
| 🛡️ **[Security.md](Security.md)** | WebView sandbox boundaries, secret verification, and privacy protection |
| 🚀 **[Deployment.md](Deployment.md)** | How to deploy your own signaling engine on Vercel, Cloudflare, or VPS |
| 🛠️ **[Troubleshoot.md](Troubleshoot.md)** | Step-by-step solutions for call timeouts, network errors, and missing video |
| 📐 **[Decisions.md](Decisions.md)** | Architectural Decision Records (ADR) detailing design choices and stealth rules |
| 📝 **[Notes.md](Notes.md)** | Essential developer notes, hardware gotchas, and stealth constraints |

---

<br>
<p align="center">
  <sub>SuperiorChat WebRTC Calling Architecture</sub>
</p>
