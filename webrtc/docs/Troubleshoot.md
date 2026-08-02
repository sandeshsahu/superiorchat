<div align="center">
  <h1>🛠️ Troubleshooting, Quota & Diagnostic Guide</h1>
  <p><strong>Transparent resolution guide for server failures, quota limits, connection drops, and permissions</strong></p>
</div>

---

> [!WARNING]
> **Default Static Pages are NOT Guaranteed 100% Uptime**: Default call URLs provided in the app rely on free-tier hosting (Vercel, Cloudflare Pages, Netlify) managed by the project maintainer. If free usage quotas are exceeded or accounts are suspended, default calls will fail until users configure their own self-hosted domain.

---

## 📑 Quick Navigation

- [1. Immediate "Invalid URL" or "Network Error" Popups](#network-error)
- [2. Call Stuck on "Waiting..."](#stuck-waiting)
- [3. Connected, But No Audio or Video](#media-missing)
- [4. Camera Preview Misaligned or Distorted](#ui-alignment)
- [5. Complete Blind-Spots & Real-World Risks](#real-world-risks)

---

<h2 id="network-error">1. Immediate "Invalid URL" or "Network Error" Popups</h2>

### What is happening?
An error popup appears immediately when you tap the Call button, and the call fails before connecting.

### Real-World Causes & Blind Spots:
1. **Free-Tier Quota Exhaustion**: The default static WebRTC pages hosted on free services (Vercel/Cloudflare Pages) have reached their monthly bandwidth or request limits, causing the host to temporarily disable the deployment.
2. **Maintainer Account / Domain Suspension**: The maintainer's hosting account or domain was suspended, updated, or decommissioned.
3. **ISP / Regional Blocking**: Your network operator, Wi-Fi firewall, or country ISP is blocking the default domain.
4. **No Internet**: Your Android device has no active internet connection.
5. **Custom URL Typo**: You entered an invalid or Non-Functional `URL` in **Settings → Call Configuration**.

### How to Fix It:
- **Automatic Fallback Validation**: `CallManager` automatically pings candidate fallback servers if the saved URL is unreachable. If all servers fail, the app presents a diagnostic popup with an option to jump directly to Settings and reset to default.
- **Deploy Your Own Free Static Engine (Strongly Recommended)**: Do not rely on default maintainer infrastructure. Deploy the open-source `webrtc/` folder to your own free Vercel, Cloudflare Pages, or Netlify account (takes 2 minutes) and enter your URL in **Settings → Call Configuration**.
- **Reset to Default**: Go to **Settings → Call Configuration** and tap **Reset to Default** to restore standard fallback domains.
- **Check Internet**: Ensure Wi-Fi or Mobile Data is active and connected.

> 🚀 **Self-Hosting Guide**: Step-by-step setup guides for Vercel, Cloudflare Pages, Netlify, and VPS are available in [Deployment.md](Deployment.md).

---

<h2 id="stuck-waiting">2. Call Stuck on "Waiting..."</h2>

### What is happening?
You tap "Call", but the screen stays on "Waiting..." and automatically hangs up after 45 seconds.

### Real-World Causes & Blind Spots:
1. **Public Signaling Broker Outage**: The free public PeerJS server (`0.peerjs.com`) dropped WebSockets, crashed, or rate-limited your IP address.
2. **Strict Firewall / UDP Blocking**: You or the recipient is on a corporate, hotel, or VPN network that blocks direct peer-to-peer UDP traffic.
3. **Recipient Missed the Link**: The Telegram user did not click the "Join Call" button before the 45-second timeout expired.

### How to Fix It:
- **Switch Networks**: Turn off VPN or switch between Wi-Fi and Mobile Data.
- **Re-try the Call**: Prompt the Telegram recipient to click the link as soon as it appears.
- **Host Private Signaling**: For mission-critical reliability, run your own private PeerJS server (`peerjs --port 9000`).

---

<h2 id="media-missing">3. Connected, But No Audio or Video</h2>

### What is happening?
The call connects and the timer starts, but you cannot hear sound or see video.

### Real-World Causes:
1. **Camera Locked by Another App**: Android allows only one app to access the camera at a time. If another app (or an in-app QR scanner) locked the camera, WebRTC video fails with `NotReadableError`.
2. **Browser Permissions Denied**: The Telegram guest clicked "Deny" when their browser prompted for Camera/Microphone access.
3. **Domain Security Lock**: The app forcibly blocks microphone/camera access if the web engine is loaded from an unverified or redirected domain.

### How to Fix It:
- **Close Media Apps**: Force close other camera/audio apps (Zoom, Instagram, Camera) and restart SuperiorChat.
- **Grant Browser Permissions**: The Telegram recipient must open browser site settings, set Camera/Microphone to **Allow**, and refresh the page.
- **Check Custom URL**: Ensure your URL in **Settings → Call Configuration** matches your verified domain.

---

<h2 id="ui-alignment">4. Camera Preview Misaligned or Distorted</h2>

### What is happening?
The local camera preview on Android does not align with the video frame, resulting in cut-off video or misplaced borders.

### Cause:
The camera container padding in the Android app (`CallScreen.kt`) and the web stylesheet (`assets/css/call.css`) must match mathematically.

### How to Fix It:
If modifying source code:
1. Check `LocalCameraBox` padding in `CallScreen.kt` (e.g. `top = 160.dp, end = 16.dp`).
2. Update `#localVideo` in `assets/css/call.css` to match (`top: calc(160px + env(safe-area-inset-top, 0px)); right: 16px;`).

---

<h2 id="real-world-risks">5. Summary of Real-World Risks & Blind-Spots</h2>

| Risk Factor | Reality / Impact | Recommended Mitigation |
|---|---|---|
| **Free Hosting Quotas** | Default maintainer static pages can hit Vercel/Cloudflare limits and fail | **Self-host** the `webrtc/` folder on your own free account |
| **Domain / Account Takeover** | If maintainer domain expires or hosting is compromised, static JS could be altered | **Self-host** on your own trusted domain & account |
| **Public Signaling Failure** | `0.peerjs.com` can crash or rate-limit IPs without notice | Run a **private PeerJS server** (`peerjs --port 9000`) |
| **P2P IP Exposure** | WebRTC media connections reveal your public IP address to the Telegram caller | Use a **private TURN Relay Server** |

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>