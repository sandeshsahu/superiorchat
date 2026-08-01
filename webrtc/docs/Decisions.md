<div align="center">
  <h1>📐 Architecture Decision Records (ADR)</h1>
  <p><strong>Design choices, tradeoffs, and stealth rules for SuperiorChat WebRTC</strong></p>
</div>

---

> [!NOTE]
> This document explains **why** SuperiorChat was designed the way it is. It details why we chose a Headless WebView over native libraries, how our visual overlay works, and how our stealth protection rules operate.

---

## 📑 Quick Navigation

- [1. Why Use Headless WebView Instead of Native WebRTC?](#why-not-native)
- [2. The Background Video Layer Architecture](#ui-rendering)
- [3. Automatic Server Fallback & Reliability](#load-balancing)
- [4. Proximity Sensor & Earpiece Sync](#proximity-wakelock)
- [5. Stealth Rule: Why No Android OS Picture-in-Picture?](#stealth-pip)
- [6. Stealth Rule: Automatic Call Teardown on App Minimize](#stealth-background)
- [7. Telegram Bot Limitations & WebRTC Link Workaround](#telegram-limitation)
- [8. Related Documentation](#related-docs)

---

<h2 id="why-not-native">1. Why Use Headless WebView Instead of Native WebRTC?</h2>

Most traditional Android calling apps compile Google's native WebRTC C++ library (`org.webrtc`). SuperiorChat intentionally avoids this in favor of a **Headless WebView** running WebRTC via JavaScript.

### Key Advantages:
1. 📦 **Zero Added APK Size**: Native C++ WebRTC libraries add 30 MB–50 MB of binary bloat to an app. By using Android's built-in system WebView, SuperiorChat adds **0 MB** to the app download size.
2. ⚡ **Instant Server-Side Updates**: Updating native WebRTC requires compiling a new APK and pushing app updates to every user. With our web-powered engine, signaling fixes, STUN updates, and security patches take effect instantly upon loading.
3. 🔄 **100% Host-Guest Parity**: The Android caller and the Telegram web guest run the exact same WebRTC engine code, eliminating cross-platform compatibility bugs.

> 🏗️ **Architecture Details**: Learn how the Android app communicates with the web engine in [Architecture.md](Architecture.md#bridge).

---

<h2 id="ui-rendering">2. The Background Video Layer Architecture</h2>

### The Challenge
Extracting high-performance video frames out of a browser `<video>` tag into native Android Compose code causes significant frame drops and battery drain.

### Our Solution
Instead of copying pixel buffers:
1. The WebView (`AndroidView`) is rendered as a transparent background layer filling the screen.
2. The web engine positions the local camera preview in the upper corner using CSS (`call.css`).
3. The native Jetpack Compose UI controls are drawn **directly on top** of the WebView.

This creates the seamless experience of a native calling interface while preserving maximum video performance and battery efficiency.

> ⚠️ **Layout Coupling**: If you modify camera preview dimensions, you must update `assets/css/call.css` as documented in [Architecture.md](Architecture.md#css-coupling).

---

<h2 id="load-balancing">3. Automatic Server Fallback & Reliability</h2>

Rather than relying on a single central server, SuperiorChat uses an automatic client-side server fallback list:

When initiating a call, the app pings available signaling servers sequentially:
1. If the primary web domain is online, it connects immediately.
2. If a domain is blocked by a network, experiencing an outage, or rate-limited, the app automatically switches to the next fallback domain seamlessly.

This provides high availability without relying on single points of failure.

> 🚀 **Self-Hosting**: Learn how to deploy your own custom server nodes in [Deployment.md](Deployment.md).

---

<h2 id="proximity-wakelock">4. Proximity Sensor & Earpiece Sync</h2>

To provide a natural phone calling experience:
- **Proximity Sensor**: When you raise the phone to your ear during a call, the screen turns off automatically to prevent accidental touches.
- **Smart Loudspeaker Toggle**: Tapping the speakerphone button instantly disables the proximity sensor, keeping the screen on so you can use the call controls.

---

<h2 id="stealth-pip">5. Stealth Rule: Why No Android OS Picture-in-Picture?</h2>

Android supports system-level Picture-in-Picture (PiP), which creates a floating video box on your phone's home screen when you exit an app.

### Why We Prohibit OS-Level PiP:
SuperiorChat is designed with a strong focus on privacy and camouflage. If the app used Android's OS-level PiP, minimizing the app would leave a floating call window on your home screen, immediately revealing the hidden app.

Instead, we use a custom **in-app floating camera box** that remains strictly inside the app interface.

---

<h2 id="stealth-background">6. Stealth Rule: Automatic Call Teardown on App Minimize</h2>

If an active call is ongoing and the user minimizes the app or switches to another application, the call **immediately hangs up**.

### Why We Enforce Auto-Hangup:
1. **Privacy Indicators**: Android displays permanent green camera and microphone dots in the system status bar whenever an app accesses media in the background, which would compromise stealth.
2. **Camouflage Compatibility**: Auto-hangup ensures background calls never conflict with app lock screens or camouflage modes (like the fake Calculator or Weather interface).

---

<h2 id="telegram-limitation">7. Telegram Bot Limitations & WebRTC Link Workaround</h2>

### The Limitation
Official Telegram Bot API accounts do **NOT** support native 1-on-1 voice or video calls (Telegram bots cannot place or receive native MTProto VoIP calls).

### Our Workaround Architecture
Because SuperiorChat operates serverlessly using Telegram Bot API for transport, we engineered a WebRTC web-link calling mechanism:

1. **Strictly One-Way Call Initiation**: Calling is strictly **one-way**. Only the SuperiorChat Android app user can initiate a call to the Telegram recipient. The Telegram recipient **cannot** initiate a call back to the bot, as Telegram bots cannot receive incoming call webhooks or trigger ring notifications.
2. **Dynamic Invitation Links**: Tapping "Call" generates a secure, single-use 128-bit encrypted WebRTC link (`call.html?join=UUID&secret=UUID`) and sends it to the Telegram chat via an inline keyboard button ("Join Call").
3. **Connection Lifecycle**:
   - **If the recipient taps "Join Call"**: Their browser opens the static WebRTC client, connects to the PeerJS signaling broker, and establishes a direct peer-to-peer audio/video stream with the Android app.
   - **If the recipient ignores or doesn't join**: The Android app's 45-second watchdog timer expires, the call auto-hangs up (`NO_ANSWER`), and the inline button is automatically updated/removed so dead links cannot be clicked later.

> 🛡️ **Security Overview**: Read our complete threat model and privacy design in [Security.md](Security.md).

---

<h2 id="related-docs">8. Related Documentation</h2>

For complete technical specifications, troubleshooting steps, and deployment guides:

| Document | Purpose |
|---|---|
| 🏗️ **[Architecture.md](Architecture.md)** | WebRTC engine structure, JavaScript bridge, and DOM layout layers |
| ⚡ **[Backend.md](Backend.md)** | PeerJS signaling, ICE candidate exchange, and DataChannel protocol |
| 🛡️ **[Security.md](Security.md)** | WebView sandbox boundaries, secret verification, and privacy threat models |
| 🚀 **[Deployment.md](Deployment.md)** | How to deploy your own WebRTC engine on Vercel, Cloudflare Pages, or VPS |
| 🛠️ **[Troubleshoot.md](Troubleshoot.md)** | Resolving connection timeouts, permission blocks, and network errors |
| 📝 **[Notes.md](Notes.md)** | Essential developer notes, hardware gotchas, and stealth constraints |

---

<br>
<p align="center">
  <sub>SuperiorChat WebRTC Calling Architecture</sub>
</p>
