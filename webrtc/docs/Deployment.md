<div align="center">
  <h1>🚀 Deployment & Self-Hosting Guide</h1>
  <p><strong>Step-by-step instructions to host the SuperiorChat WebRTC engine for free</strong></p>
</div>

---

The default public servers are provided strictly as a convenience. While we prioritize privacy, relying on third-party infrastructure always carries a non-zero risk. We highly recommend deploying the open-source **`webrtc/`** engine to your own trusted hosting provider.

---

## 📑 Quick Navigation

- [Part 1: Hosting the Frontend (Static Client)](#part-1)
- [Part 2: Advanced Infrastructure (Signaling & TURN)](#part-2)
- [Part 3: Configuring the Android App](#part-3)

---

<h2 id="part-1">🌐 Part 1: Hosting the Frontend (Static Client)</h2>

The WebRTC engine is a pure static HTML/JS application. It requires **no build step**.

### Option A: Vercel / Cloudflare Pages / Netlify (Easiest)
The most reliable method is linking your Git repository directly to a modern host.
1. Create a new project in your hosting dashboard.
2. Connect your GitHub/GitLab repository.
3. Set the **Root Directory** to `webrtc/`.
4. **Framework Preset**: `None` or `Static HTML`.
5. **Build Command**: *(Leave blank)*.
6. Click **Deploy**. Your site will be live instantly (e.g., `https://call.yourdomain.com`).

### Option B: Cloudflare Pages (Direct Upload - No Git Required)
If you don't want to use Git, you can upload the folder directly to Cloudflare's free CDN:
1. Open [Cloudflare Dashboard](https://dash.cloudflare.com) → **Workers & Pages**.
2. Click **Create Application → Pages → Upload assets**.
3. Drag and drop the `webrtc/` folder directly from your computer.
4. Click **Deploy**. Cloudflare generates your domain (e.g. `https://your-app.pages.dev`).

### Option C: GitHub Pages
1. Go to your repository **Settings → Pages**.
2. Under **Source**, choose `Deploy from a branch` and select `main`.
3. Set the directory to `/root` (if the repo only contains the WebRTC engine) or use a GitHub Action to target the `webrtc/` folder.

### Option D: Manual VPS (Nginx)
Copy the `webrtc/` folder to `/var/www/superiorchat` on your server and configure Nginx:
```nginx
server {
    listen 443 ssl;
    server_name call.yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/call.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/call.yourdomain.com/privkey.pem;

    root /var/www/superiorchat;
    index index.html;
}
```
> [!IMPORTANT]
> **HTTPS Required**: Browsers completely block camera and microphone access on non-secure (`http://`) connections. You **must** serve your domain over HTTPS.

---

<h2 id="part-2">🔒 Part 2: Advanced Infrastructure (100% Self-Hosted Privacy)</h2>

By default, the static frontend relies on public PeerJS cloud servers for signaling and Google for STUN. For total privacy and reliability on strict networks (VPNs/Carrier NATs), you must self-host these backend components.

### 2.1 Self-Hosting the PeerJS Signaling Server
The signaling server exchanges the initial cryptographic handshakes. It does *not* touch audio/video data.
1. Install Node.js on a VPS.
2. Install the PeerJS server globally: `npm install -g peer`
3. Run the server on port 9000: `peerjs --port 9000 --key peerjs --path /myapp`
4. Secure it behind Nginx with an SSL certificate.

### 2.2 Self-Hosting STUN / TURN (Coturn)
STUN discovers IPs; TURN relays media if strict firewalls block P2P connections. Without a TURN server, calls may fail on corporate VPNs or symmetric NATs.
1. Install Coturn on an Ubuntu VPS: `sudo apt install coturn`
2. Edit `/etc/turnserver.conf`:
   ```ini
   listening-port=3478
   tls-listening-port=5349
   realm=turn.yourdomain.com
   user=secureuser:strongpassword
   ```
3. Start the server: `sudo systemctl restart coturn`

### 2.3 Linking Custom Servers in `webrtc.js`
Once your private backend is running, edit `webrtc/assets/js/webrtc.js` to point away from the public defaults:

```javascript
// 1. Update the PeerJS configuration
this.peer = new Peer(this.hostId, {
    host: 'signaling.yourdomain.com',
    port: 443,
    path: '/myapp',
    secure: true
});

// 2. Add your custom STUN/TURN servers to the ICE configuration
const customConfig = {
    'iceServers': [
        { urls: 'stun:stun.yourdomain.com:3478' },
        {
            urls: 'turn:turn.yourdomain.com:3478',
            username: 'secureuser',
            credential: 'strongpassword'
        }
    ]
};
```

---

<h2 id="part-3">📱 Part 3: Configuring the Android App</h2>

After deploying your frontend, point the Android app to your custom domain:

1. Open **SuperiorChat App**.
2. Go to **Settings → Call Configuration**.
3. Enter your custom server URL (e.g., `https://call.yourdomain.com`).
   - *Note: Provide only the base domain. Do not include `/call.html`.*
4. Tap **Save**. The app will validate your server URL automatically.

> [!NOTE]
> **🛡️ Hardcoding Backup URLs (For Developers)**
> 
> If you are compiling your own APK, you should permanently add your custom domains to the fallback array. This ensures the app can always connect even if the primary user-configured server goes down.
> 
> Edit `app/src/main/res/values/webrtc_urls.xml`:
> ```xml
> <?xml version="1.0" encoding="utf-8"?>
> <resources>
>     <string-array name="webrtc_fallback_urls">
>         <!-- Add your custom domains here -->
>         <item>https://primary.yourdomain.com</item>
>         <item>https://backup.yourdomain.com</item>
>     </string-array>
> </resources>
> ```
> During the "Race to Connect" validation, the `CallManager` will automatically shuffle and test these URLs if the primary connection fails.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>