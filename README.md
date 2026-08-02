<div align="center">
  <h1>Superior Chat</h1>
  <p><strong>Private, Stealth-First Messaging Powered by Telegram Bot API</strong></p>
  
  <p>
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose">
    <img src="https://img.shields.io/badge/Telegram_Bot_API-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram">
    <img src="https://img.shields.io/badge/License-Apache_2.0-D22128?style=for-the-badge" alt="License">
  </p>
</div>

---

> [!IMPORTANT]
> **Superior Chat** is an Android app designed for maximum privacy. It looks and acts like a normal app on the outside, keeping your conversations **Completely Hidden** and leaving no trace on your personal device.

---

## 📑 Table of Contents
- [🌟 Overview](#overview)
- [📸 Screenshots](#screenshots)
- [🚀 Key Highlights](#features)
- [🤔 How It Works](#how-it-works)
- [🛠️ Instructions & Setup](#setup)
- [📚 Documentation](#documentation)
- [🙌 Acknowledgments](#acknowledgments)
- [⚖️ License](#license)

---

<h2 id="overview">🌟 Overview</h2>

Superior Chat solves a unique privacy challenge: **How to chat securely without keeping a dedicated server or exposing a visible messaging app on your phone.**

Instead of operating custom backend servers, Superior Chat uses the highly reliable Telegram Bot API for messaging:
- 👤 **User A** chats directly inside their official Telegram application via a dedicated bot.
- 👤 **User B** chats inside the custom, hidden Superior Chat app.
- 🚫 **No Middleman Server**: Message histories are managed locally within the hidden app, while relying on Telegram's secure cloud for transport.

---

<h2 id="screenshots">📸 Screenshots</h2>

<p align="center">
  <img src="docs/images/app/chat_screen2.jpg" width="30%" alt="Chat Screen">
  &nbsp;
  <img src="docs/images/app/chatscreen_withreactions.jpg" width="30%" alt="Chat with Reactions">
  &nbsp;
  <img src="docs/images/app/nav_menu.jpg" width="30%" alt="Navigation Menu">
</p>

> 🖼️ Explore more screenshots in the [Chat App](docs/images/app), [Setup App](docs/images/setupapp) and [Flavors](docs/flavors/images/) directories.

---

<h2 id="features">🚀 Key Highlights</h2>

| Feature | Description | Benefit |
|---------|-------------|---------|
| 👻 **Stealth App Disguise** | No launcher icon, or perfectly disguised as functional Weather/System applications. | Invisible to casual snoopers & shoulder-surfers. |
| 🔑 **Secret Access Codes** | Unlocked exclusively via a secret dialer code (`*#*#9131#*#*`), Quick Settings tile, or App Search interception. | Cannot be opened normally from the app drawer. |
| 🛡️ **Camouflaged Alerts** | Notifications appear as harmless carrier alerts or live weather updates based on data states. | Complete privacy even when receiving alerts. |
| ⚡ **Serverless Transport** | Powered entirely by Telegram Bot API long-polling. | Zero hosting costs, 99.9% uptime, no third-party analytics. |
| 📞 **Secure P2P Calling** | Peer-to-Peer WebRTC Voice & Video calling built into the chat interface. | Zero-auth cryptographic rooms with true supply-chain independence. |

👉 **[Click here to see Detailed Features](docs/Features.md)**

---

<h2 id="how-it-works">🤔 How It Works</h2>

Superior Chat acts as a silent client that speaks directly to the Telegram API.

```mermaid
graph LR
    subgraph "Your Device (Stealth)"
        A["📱 Superior Chat App<br/>(Hidden Interface)"]
    end

    subgraph "Cloud Pipe (Serverless)"
        T["☁️ Telegram Bot API<br/>(Encrypted Relay)"]
    end

    subgraph "Partner Device"
        B["💬 Official Telegram App<br/>(Standard Chat)"]
    end

    A -->|"Send Message<br/>(HTTPS POST)"| T
    T -->|"Long Poll Update<br/>(getUpdates)"| A
    T <-->|"Native Bot Chat"| B
```

---

<h2 id="setup">🛠️ Instructions & Setup</h2>

Superior Chat utilizes a **two-app setup system** to ensure zero residual metadata is left behind on the device.

1. **Setup App (`:setupapp`)**: A single-use configuration wizard used to scan QR codes or enter bot tokens. Once configured, it securely passes encrypted credentials to the main app via an RSA-2048 IPC handshake.
2. **Main App (`:app`)**: The core hidden chat application that stays on the device.

To install and set up the apps properly (including creating the Telegram Bot and generating the QR code), please read our complete setup guide:

👉 **[Click here for the Installation & Setup Guide](docs/Instructions.md)**

---

<h2 id="documentation">📚 Documentation</h2>

For detailed technical references, explore the dedicated documentation in this directory:

- 🏗️ **[Architecture](docs/Architecture.md)** — System topology, module breakdown, component dependency graphs, and source trees.
- ⚙️ **[Backend Mechanics](docs/Backend.md)** — Polling loops, network resilience, MediaSync upload/download engine, and background execution.
- ✨ **[Features & Capabilities](docs/Features.md)** — Comprehensive user-facing capabilities, gestures, disguises, and UI interactions.
- 📖 **[Installation Guide](docs/Instructions.md)** — Setup guide & building from source for developers.
- ⚠️ **[Notes & Disclaimers](docs/Notes.md)** — Important known limitations, threat models, and legal disclaimers.
- 📋 **[Version Changelogs](docs/Changelogs.md)** — Detailed version history, release notes, and feature updates.
- 📞 **[WebRTC Calling Engine](webrtc/README.md)** — Headless WebRTC calling architecture, JS bridge, security models, and self-hosting guides.
---
> [!IMPORTANT]
> For complete technical details on how the disguises, entry points, and fake notifications are implemented for each variant, please refer to the specific flavor documentation:
> - **[Captive Portal Details](docs/flavors/CaptivePortal.md)**
> - **[Weather Details](docs/flavors/FlavorWeather.md)**
---

<h2 id="acknowledgments">🙌 Acknowledgments</h2>

- The UI design for the `weather` flavor is adapted from the open-source **[WeatherAppUI](https://github.com/rudram837/WeatherAppUI)** repository by @rudram837.

---

<h2 id="license">⚖️ License</h2>

This project is licensed under the Apache License 2.0. 
For the full license text and terms of use, please view the **[LICENSE](LICENSE)** file.

<br>

<div align="center">
  <sub>Built for privacy and discretion • Apache License 2.0</sub><br>
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</div>
