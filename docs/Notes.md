<h1 align="center">
  Notes & Disclaimers
</h1>

<p align="center">
  <strong>Important Disclaimers, Known Limitations & Threat Models</strong>
</p>

---

> [!CAUTION]
> Please read this document carefully before relying on Superior Chat for sensitive communications.

---

## Table of Contents
- [1. Development Background](#background)
- [2. Disclaimer of Liability](#liability)
- [3. Threat Model & Accepted Risks](#threat-model)
- [4. App Access & Entry Methods](#access)
- [5. Build Flavors & Camouflage](#flavors)
- [6. Important Notes & Limitations](#notes)
- [7. Google Play Protect Warning](#play-protect)
- [8. Call System & Security Notes](#call-notes)
- [9. Privacy Statement](#privacy)

---

<h2 id="background">1. Development Background</h2>

This application was developed as an open-source, high-privacy, stealth messaging solution designed specifically for couples and close partners. It was built with the assistance of Google's Antigravity AI to prioritize seamless User Experience (UX) while maintaining strong on-device discretion. 

While every effort has been made to polish the codebase, there may still be undiscovered bugs. **Use this application carefully.**

---

<h2 id="liability">2. Disclaimer of Liability</h2>

The creator of this application assumes **no responsibility or liability** for any damages or consequences resulting from its use. By using Superior Chat, you accept full responsibility for any outcomes, which includes but is not limited to:

- 🚫 Misuse of the application
- 💔 Personal, relational, or legal issues
- 📉 Accidental data loss or lost messages
- 🔓 Privacy breach or exposure of sensitive information

> [!WARNING]
> **Compliance Warning**: It is your strict responsibility to comply with all local, state, and federal regulations regarding privacy and encrypted communications in your jurisdiction. The creator is not responsible for any misuse of this application.

---

<h2 id="threat-model">3. Threat Model & Accepted Risks</h2>

Superior Chat is built to protect against a specific threat model. It is important to understand what the app **does** and **does not** protect against.

- **Target Adversary**: "Casual snoopers", nosy friends, or people who happen to briefly shoulder-surf or look at your unlocked phone.
- **Out of Scope (Accepted Risk)**: Highly technical attackers or forensic experts with prolonged physical access to an *unlocked* device. If a forensic expert has your unlocked phone, they can bypass stealth measures, find the dialer code, dump the APK, and read local databases. We are *not* fighting this attacker.
- **QR Code Encryption (AES-GCM)**: The Setup App encrypts your Bot Token into a QR code. However, because this app is open-source, the AES decryption key is publicly visible on GitHub. **This is an accepted risk.** The encryption exists solely to stop automated image scanners or accidental scans from exposing the plaintext token, *not* to defeat targeted cryptographic attacks.

---

<h2 id="access">4. App Access & Entry Methods</h2>

To maintain strict stealth, Superior Chat provides hidden entry points depending on the flavor you install. Currently, there are two primary methods to access the hidden chat interface:

- 📞 **Secret Dialer Code**: (CaptivePortal flavor only) See [CaptivePortal.md](flavors/CaptivePortal.md) for access instructions.
- 🎛️ **Quick Settings Tile**: (CaptivePortal flavor only) See [CaptivePortal.md](flavors/CaptivePortal.md) for access instructions.
- 🔍 **App Search Interception**: (Weather flavor only) See [FlavorWeather.md](flavors/FlavorWeather.md) for access instructions.
> [!NOTE]
> We have plans to introduce additional, highly discreet access methods in future updates.

---

<h2 id="flavors">5. Build Flavors & Camouflage</h2>

Superior Chat is compiled into different "flavors" (variants) to suit varying levels of privacy needs.

### 🟢 Original Flavor (Normal)
- **Stealth Level**: None.
- **Description**: The standard application with a regular launcher icon and normal notifications. There are no hiding mechanisms. Best for testing or casual use.

### 🔵 Weather Flavor (Camouflaged)
- **Stealth Level**: Advanced.
- **Details**: Full details regarding this flavor's disguise, intercept mechanisms, and notification camouflage can be found in [FlavorWeather.md](flavors/FlavorWeather.md).

### 🔴 Captive Portal Flavor (Hidden + Camouflaged)
- **Stealth Level**: Maximum.
- **Details**: Full details regarding this flavor's system decoy identity, Quick Settings tile access, and carrier notification camouflage can be found in [CaptivePortal.md](flavors/CaptivePortal.md).


> [!NOTE]
> We are planning to add more flavors with even stronger dynamic camouflage mechanisms in future releases.

---

<h2 id="notes">6. Important Notes & Limitations</h2>

| # | Note | Description | Action / Workaround |
|:---:|:---|:---|:---|
| 1 | **Dialer Code Incompatibility** | Secret dialer code (`*#*#9131#*#*`) may fail on customized OEM ROMs due to system restrictions. | See [CaptivePortal.md](flavors/CaptivePortal.md) for fallback entry methods. |
| 2 | **Battery Optimization** | Android's aggressive battery management may kill background BotService polling. | Grant "Unrestricted" battery usage in device App Settings. |
| 3 | **Setup App Metadata Leakage** | Leaving the Setup App installed on the device defeats the camouflage. | **Uninstall the Setup App** immediately after configuration. |
| 4 | **Private Chat ID** | Adding the bot to group or public channels exposes message activity to group members. | Use a **private 1-on-1 Chat ID** with the bot to keep privacy 100% intact. |
| 5 | **Call Prerequisites** | Calling requires active internet, verified credentials, and Camera/Microphone permissions. | Check status indicators in **Application page $\rightarrow$ App Settings**. |
| 6 | **Automatic Server Fallback** | If the primary call server fails, `CallManager` tests backup fallback servers automatically. | Reset to default or customize URLs in **Application page $\rightarrow$ App Settings**. |
| 7 | **Background Call Termination** | To maintain strict camouflage, if the app is minimized or sent to the background during an active call, the call is instantly terminated. | Stay in the app during active calls. We deliberately avoid native OS Picture-in-Picture. |
| 8 | **WebRTC IP Exposure** | Because the call system is Peer-to-Peer, both participants can theoretically discover each other's public IP address. | Host your own TURN server to proxy the connection if strict anonymity is required. |
| 9 | **Fake Video Mute (UI-Level Hide)** | Muting video via the UI only hides the video player. The camera hardware remains active and streaming to prevent PeerJS SDP crashes and remote "frozen video" bugs. | We prioritize connection stability. We may find a safe hardware-level fix for this in upcoming updates. |

---

<h2 id="play-protect">7. Google Play Protect Warning</h2>

> [!IMPORTANT]
> **Why you might see a "Harmful App" warning during installation**
> 
> I have poured a lot of hard work and passion into this project to deliver a seamless, beautifully designed application.
> 
> Because this app is only available as open-source (not from Play Store), **Google Play Protect may flag this application as harmful.** This is an automated security warning from Android.
> 
> I cannot control Google's automated flagging. Therefore, **the choice is entirely in your hands**:
> 
> 1. **Verify it yourself:** This project is completely open-source. You have access to the complete source code, and you are highly encouraged to audit it, compile it yourself, and use your own builds.
> 2. **Use the provided releases:** If you don't want to build it yourself, you can use the signed APKs provided in the Releases section. 
> 
> I am not forcing anyone to use my provided APKs. This project is the result of a personal vision and a strong, relentless drive to make these ideas work in the real world. If you choose to install the pre-built APK and see the warning, simply click **More Details -> Install Anyway**.

---

<h2 id="call-notes">8. Call System & Security Notes</h2>

For security boundaries, threat models, developer notes, and self-hosting instructions regarding the voice/video calling feature, refer to the calling documentation suite:

- 🛡️ **[Calling Security & Privacy Audit](../webrtc/docs/Security.md)**
- 📝 **[Calling Developer Notes & Limitations](../webrtc/docs/Notes.md)**
- 📁 **[Full Calling Documentation Suite](../webrtc/docs/)**

### Key Usage Guidelines:
- 🌐 **Browser Recommendation**: For optimal WebRTC performance, zero-lag video rendering, and full hardware support, always use the **Google Chrome** browser when joining call links.
- 📱 **Telegram Side Background Requirement**: When joining a call from Telegram, do not switch apps or move the browser tab to the background without tapping the **Minimize** (PiP) button first. Moving to the background without Picture-in-Picture prevents audio/video access and may disconnect the call.

> [!CAUTION]
> **Third-Party Infrastructure Risk (WebRTC Calls)**
> The default static webpages and signaling servers used for the WebRTC calling feature are provided **strictly as a convenience**. Using the maintainer's default servers (or any unknown third-party server) is **strictly not recommended** for privacy-critical use cases. A malicious host owner can easily inject code to steal your cryptographic call secrets, secretly log your activity, or severely compromise your privacy. The maintainer and developers assume **no responsibility** for privacy breaches caused by relying on third-party infrastructure. You **must** self-host your own static web pages and signaling server for guaranteed security. 
> 
> See the step-by-step self-hosting guide in **[Deployment.md](../webrtc/docs/Deployment.md)**.

---

<h2 id="privacy">9. Privacy Statement</h2>

Superior Chat communicates **exclusively** with the official Telegram Bot API (`api.telegram.org`) using the Bot Token you provide. 
- The developer has absolutely no access to your messages, tokens, or media. 
- All chat history and transferred media remain strictly between your local device storage and the Telegram servers.
- **Flavor Exceptions (Weather)**: See the data privacy and third-party API usage guarantees in [FlavorWeather.md](flavors/FlavorWeather.md).

> [!TIP]
> **Private Chat Privacy Recommendation**:
> It is **strongly recommended** to use your own private 1-on-1 Chat ID with the bot. Avoid adding the bot to public or private group chats, as group members could see bot activity. Keeping communication strictly within a direct 1-on-1 private chat ensures the other person's privacy remains completely intact.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>
