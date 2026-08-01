<div align="center">
  <h1>🚀 Deployment & Self-Hosting Guide</h1>
  <p><strong>Step-by-step instructions to host the SuperiorChat WebRTC engine for free</strong></p>
</div>

---

The default public servers are provided strictly as a convenience for users who do not want the headache of configuring infrastructure. While we prioritize privacy and security immensely, relying on third-party infrastructure always carries a non-zero risk in the distant future.

We highly recommend deploying the open-source **webrtc/** repository to your own trusted hosting provider (Vercel, Netlify, Cloudflare Pages, etc.). It takes less than 2 minutes and is completely free.

---

## 📑 Quick Navigation

- [1. Connecting GitHub / GitLab (Recommended for All Platforms)](#git-integration)
- [2. Vercel Deployment](#vercel)
- [3. Cloudflare Pages Deployment](#cloudflare)
- [4. Netlify Deployment](#netlify)
- [5. GitHub Pages Deployment](#github)
- [6. Manual VPS Setup (Nginx / Apache)](#vps)
- [7. Configuring the Android App](#android-config)
- [8. Related Documentation](#related-docs)

---

<h2 id="git-integration">1. Connecting GitHub / GitLab (Recommended)</h2>

The easiest and most reliable way to host the engine is by connecting your GitHub or GitLab repository directly to your web hosting provider. This enables automatic deployment whenever you update your code.

### Standard Git Import Settings:
- **Repository**: Select your `SuperiorChat` repository (or a dedicated `webrtc` repository).
- **Branch**: Select `main` (or `master`).
- **Root Directory / Framework Preset**: Set Root Directory to `webrtc/` (or choose **Other / Static HTML**).
- **Build Command**: *Leave blank* (it is a pure static site).
- **Output Directory**: `.` (or *leave default*).

---

<h2 id="vercel">2. Vercel Deployment</h2>

Vercel is the default hosting provider for public SuperiorChat nodes.

### Method A: Connect GitHub / GitLab (Easiest)
1. Go to [Vercel Dashboard](https://vercel.com/dashboard) and click **Add New → Project**.
2. Connect your GitHub or GitLab account and select your repository.
3. In **Root Directory**, click **Edit** and select the `webrtc` folder.
4. Leave Build & Output Settings as default.
5. Click **Deploy**. Vercel will provide your live URL (e.g. `https://your-app.vercel.app`).

### Method B: Vercel CLI
```bash
cd webrtc
npm i -g vercel
vercel --prod
```

---

<h2 id="cloudflare">3. Cloudflare Pages Deployment</h2>

Cloudflare Pages offers a fast global CDN with unlimited bandwidth on its free tier.

### Method A: Connect GitHub / GitLab
1. Open [Cloudflare Dashboard](https://dash.cloudflare.com) → **Workers & Pages**.
2. Click **Create Application → Pages → Connect to Git**.
3. Select your repository and production branch (`main`).
4. Set **Root directory** to `webrtc`.
5. Framework preset: Select **None** / **Static Site**.
6. Click **Save and Deploy**. Cloudflare generates your domain (e.g. `https://your-app.pages.dev`).

### Method B: Direct Folder Upload
1. Go to **Workers & Pages → Create Application → Pages → Upload assets**.
2. Drag and drop the `webrtc/` folder directly from your computer and click **Deploy**.

---

<h2 id="netlify">4. Netlify Deployment</h2>

### Method A: Connect GitHub / GitLab
1. Log in to [Netlify](https://app.netlify.com) and click **Add new site → Import an existing project**.
2. Choose **GitHub** or **GitLab** and authorize access to your repo.
3. Set **Base directory** to `webrtc`.
4. Leave build command blank and click **Deploy**.

### Method B: Netlify CLI
```bash
cd webrtc
npm install -g netlify-cli
netlify deploy --prod
```

---

<h2 id="github">5. GitHub Pages Deployment</h2>

If you host your code on GitHub, you can publish the web page directly from your repo settings:

1. Go to your repository **Settings → Pages**.
2. Under **Source**, choose `Deploy from a branch`.
3. Select the `main` branch.
4. Set directory to `/root` (if your repo contains only the WebRTC engine) or use a GitHub Action to publish the `webrtc/` subfolder.
5. Click **Save**. Your site will be published at `https://<username>.github.io/<repo>/`.

---

<h2 id="vps">6. Manual VPS Setup (Nginx / Apache)</h2>

If you own a VPS (Virtual Private Server) and prefer self-managed infrastructure:

1. Copy the contents of the `webrtc/` folder to `/var/www/superiorchat` on your server.
2. Configure Nginx:
   ```nginx
   server {
       listen 443 ssl;
       server_name call.yourdomain.com;
       
       ssl_certificate /etc/letsencrypt/live/call.yourdomain.com/fullchain.pem;
       ssl_certificate_key /etc/letsencrypt/live/call.yourdomain.com/privkey.pem;

       root /var/www/superiorchat;
       index index.html;

       location / {
           try_files $uri $uri/ =404;
       }
   }
   ```

> [!IMPORTANT]
> **HTTPS Required**: Browsers block camera and microphone access on non-secure (`http://`) connections. You **must** serve your domain over HTTPS using a valid SSL certificate (e.g. Let's Encrypt).

---

<h2 id="android-config">7. Configuring the Android App</h2>

After deploying your server, point your Android app to your custom domain:

1. Open **SuperiorChat App**.
2. Go to **Settings → Call Configuration**.
3. Enter your custom server URL (e.g. `https://your-app.vercel.app` or `https://call.yourdomain.com`).
   - *Note: Provide only the base domain. Do not include `/call.html`.*
4. Tap **Save**. The app will validate your server URL automatically.

---

<h2 id="related-docs">8. Related Documentation</h2>

For complete technical specifications, architecture diagrams, and security models:

| Document | Purpose |
|---|---|
| 🏗️ **[Architecture.md](Architecture.md)** | WebRTC engine structure, JavaScript bridge, and DOM layout layers |
| ⚡ **[Backend.md](Backend.md)** | PeerJS signaling, ICE candidate exchange, and DataChannel protocol |
| 🛡️ **[Security.md](Security.md)** | WebView sandbox limits, secret verification, and privacy threat models |
| 🛠️ **[Troubleshoot.md](Troubleshoot.md)** | Solutions for connection timeouts, permission denials, and server popups |
| 📐 **[Decisions.md](Decisions.md)** | Architectural Decision Records (ADR) detailing design choices and stealth rules |
| 📝 **[Notes.md](Notes.md)** | Essential developer notes, hardware gotchas, and stealth constraints |

---

<br>
<p align="center">
  <sub>SuperiorChat WebRTC Calling Architecture</sub>
</p>
