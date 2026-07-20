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
- [7. Privacy Statement](#privacy)

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

- 📞 **Secret Dialer Code**: Open the application privately by dialing a secret code (`*#*#9131#*#*`) directly into your phone's native dialer.
- 🎛️ **Quick Settings Tile**: Access the chat app via a custom Quick Settings Tile. 
  - **Access Sequence**: Tap the tile `ON`, then `OFF`, and finally `ON and HOLD` to enter the application. (This tile can be disabled from the Application Settings for extra security).

> [!NOTE]
> We have plans to introduce additional, highly discreet access methods in future updates.

---

<h2 id="flavors">5. Build Flavors & Camouflage</h2>

Superior Chat is compiled into different "flavors" (variants) to suit varying levels of privacy needs.

### 🟢 Original Flavor (Normal)
- **Stealth Level**: None.
- **Description**: The standard application with a regular launcher icon and normal notifications. There are no hiding mechanisms. Best for testing or casual use.

### 🔴 Captive Portal Flavor (Hidden)
- **Stealth Level**: High.
- **Description**: The app is completely hidden from the launcher. It is disguised as an **Android System** component (package name: `com.android.connectivity.stats`) and features a standard system Gear icon, making it incredibly hard for snoopers to detect in the app settings list.
- **Notification Camouflage**: Incoming messages trigger harmless-looking system alerts based on the network state:
  - **Idle**: `"[Carrier] - Standard rates apply"`
  - **New Message**: `"[Carrier] - High data usage detected"`
  - **Offline**: `"[Carrier] - Internet not connected"`
  - **API Issues**: `"[Carrier] - Check your data plan"`

> [!NOTE]
> We are planning to add more flavors with even stronger dynamic camouflage mechanisms in future releases.

---

<h2 id="notes">6. Important Notes & Limitations</h2>

| # | Note | Workaround / Action |
|:---:|:---|:---|
| 1 | **Dialer Code Incompatibility** | On heavily customized OEM ROMs, the secret dialer code (`*#*#9131#*#*`) may fail to trigger due to system restrictions. Use the Quick Settings Tile (Captive Portal flavor) instead. |
| 2 | **Battery Optimization Kills Polling** | Android's aggressive battery management may kill the background BotService, delaying incoming messages when the app is closed. | You must grant "Unrestricted" battery usage to the app in your device's App Settings. |
| 3 | **Setup App Metadata Leakage** | Leaving the Setup App installed defeats the purpose of a camouflaged installation. | You **must uninstall** the Setup App immediately after the credentials have been successfully transferred to the Main App. |

---

<h2 id="privacy">7. Privacy Statement</h2>

Superior Chat communicates **exclusively** with the official Telegram Bot API (`api.telegram.org`) using the Bot Token you provide. 
- No data is sent to any third-party servers, analytics platforms, or external endpoints. 
- The developer has absolutely no access to your messages, tokens, or media. 
- All chat history and transferred media remain strictly between your local device storage and the Telegram servers.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>
