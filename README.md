<h1 align="center">
  Superior Chat
</h1>

<p align="center">
  <strong>Stealthy, Serverless, End-to-End Android Chat via Telegram</strong>
</p>

<p align="center">
  <img alt="Android" src="https://img.shields.io/badge/Android-10%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img alt="Telegram API" src="https://img.shields.io/badge/Telegram-Bot_API-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" />
</p>

---

Superior Chat is a highly stealthy, serverless chat application built on Android. To eliminate maintenance costs and preserve absolute privacy, it utilizes the **Telegram Bot API** strictly as a transport layer. **Client A** chats via a custom, fully-featured, Room-backed Android UI, while **Client B** chats directly through their standard Telegram application with the configured bot. 

> [!WARNING]  
> **DISCLAIMER:** This project was built strictly as a technical experiment in serverless Android communication and stealth UX design. Users are responsible for complying with all applicable laws and regulations. I do not authorize its use for any malicious purposes. If you choose to use this software, you bear sole and absolute responsibility for any legal consequences. 

---
<!-- 
## 📸 Application Snapshots

<p align="center">
  <img src="docs/img/1.jpg" width="30%" />
  <img src="docs/img/2.jpg" width="30%" />
  <img src="docs/img/3.jpg" width="30%" />
</p>

*More snapshots are available in the [`docs/img/`](docs/img/) directory.*

--- -->

## ✨ Highlights

| Category | Capability |
|:---|:---|
| **Serverless Architecture** | No third-party backend or database. Telegram acts entirely as the secure transport pipe between Client A and Client B. |
| **Absolute Stealth** | The main application has no launcher icon and hides completely from the app drawer. It is accessed exclusively by dialing `*#*#9131#*#*` in the native phone dialer. |
| **Intruder Defense** | Incoming API updates are strictly filtered by Telegram Chat ID. Updates from any unauthorized chat are dropped instantly to prevent spam or intrusion. |
| **Media & Queueing** | Handles text, photos, and voice notes (`.m4a`) flawlessly. Features instant media sync, offline queueing, and network recovery. |
| **Rate Limiting** | Implements an intentional delay of 3 messages per second to prevent Telegram API flood limits. |
| **Setup Application** | A temporary "burner" setup app provides a secure UI for injecting bot credentials via AES/GCM encrypted IPC intents, before permanently uninstalling itself. |

---

## 🚀 Setup & Configuration

Because Superior Chat operates in "Invisible Mode" from the moment it is installed, a temporary **Setup Application** (`:setupapp`) is bundled with the project to handle initial configuration securely.

### Client B (Target/Owner) Side
1. Create a new bot using `@botfather` in Telegram and start the bot.
2. Copy the **Bot Token** and your personal **Chat/Owner ID**.
3. Send these credentials to Client A securely.

### Client A (Android App) Side
1. Install and open the **Setup Application**.
2. The setup app will automatically install the main application (`com.mobile.superiorutils`) without a launcher icon, ensuring it is completely hidden.
3. Enter Client B's Chat ID and Bot Token into the setup UI.
4. Open the main application via the setup app's handshake prompt. 
5. Follow the system prompt to **uninstall the setup application**, erasing all configuration footprints from the device.
6. The app will launch the modern chat interface. For future access, simply dial `*#*#9131#*#*`.

---

## 🏗️ Architecture & Responsibilities 

The application is built using a compact, highly cohesive MVVM directory layout under the `com.mobile.superiorutils` package. While Telegram handles data transport, the local app is fully responsible for UI, local storage, security, and media management.

*   **`bot/`:** Wraps the OkHttp-based Telegram API and manages the long-polling loop (`BotSync.kt`) and secret local notifications.
*   **`core/` & `data/`:** Houses the application graph, Room Database configuration, and `EncryptedSharedPreferences` for secure credential persistence.
*   **`media/`:** Manages concurrency-safe foreground queueing and background uploads/downloads via WorkManager (`MediaWorker.kt`).
*   **`service/`:** Background polling services (`BotService.kt`)
*   **`ui/`:** Native Jetpack Compose UI (Material 3) featuring a rich chat bubble feed, voice recording interfaces, and camouflaged diagnostic logs.
*   **`utils/`:** Contains stealth mechanisms, including the `CodeReceiver.kt` (intercepts the dialer code) and `CryptoUtils.kt` (AES/GCM/NoPadding 256-bit symmetric cryptography for secure IPC handshakes).

---

## 🛠️ Tech Stack

| Layer | Technology |
|:---|:---|
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM with ViewModel & StateFlow |
| **Networking** | `OkHttp3` & native `HttpURLConnection` |
| **Security** | `androidx.security:security-crypto` (EncryptedSharedPreferences) & AES-256 GCM |
| **Persistence** | Room Database |
| **Background** | Foreground Service, WorkManager, BroadcastReceiver |

---

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0** with the **Commons Clause** condition. 

This means:
- ✅ You can view, modify, and use this code.
- ❌ **You CANNOT sell this software** or provide it as a paid service.
- ❌ This project cannot be used for commercial distribution or corporate earnings.

See the full [LICENSE](LICENSE) file for exact terms.