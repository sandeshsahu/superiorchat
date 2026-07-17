# Architecture & Component Design

This document outlines the architectural topology, module segregation, directory structure, and data routing mechanics of **Superior Chat**. The application relies entirely on an asymmetric client-to-bot-to-client relay loop using the Telegram Bot API as a serverless transport layer, backed by local persistent storage and a high-security initialization workflow.

---

## 1. System Topology & Asymmetric Design

Superior Chat eliminates central database costs and hosting footprints by delegating transport orchestration to Telegram. The system is entirely distributed across two disparate interfaces:

* **Production Client (`:app`):** Runs the core package `com.mobile.superiorchat`. It operates as a fully local, database-driven Android client utilizing Android Architecture Components (MVVM, Jetpack Compose, StateFlow, and Room). It ingests incoming traffic via long polling and pushes outgoing data directly via HTTPS POST.
* **Target/Owner Client:** Utilizes the native, official Telegram desktop or mobile app to read incoming messages relayed by the bot and send commands/responses back to the bot queue.

```text
+----------------------------+               +----------------------------+
|  Client A (Custom App)     |               |  Client B (Telegram App)   |
|  Package: superiorchat     |               |  Standard Native Chat UI   |
+-------------+--------------+               +-------------+--------------+
              |                                            ^
  Outbound:   | sendMessage                                | Inbound:
  HTTPS POST  |                                            | Telegram Native
              v                                            | Message Delivery
+-------------+--------------------------------------------+--------------+
|                          Telegram Bot API Cloud                         |
|                 Acts purely as a serverless network pipe                |
+-------------+--------------------------------------------+--------------+
              |                                            ^
  Inbound:    | getUpdates                                 | Outbound:
  Long Poll   |                                            | Message / Command
              v                                            |
+-------------+--------------+                             |
|  Bot Sync Buffer (Queue)   |-----------------------------+
+----------------------------+
```

## 2. Directory and File Structure (Multi-Flavor)

We maintain a compact, highly cohesive directory layout. Do **not** flood the repository with single-purpose classes or redundant utility files. The architecture leverages **Product Flavors** to compile different disguises from the same core codebase.

The main application resides under the package `com.mobile.superiorchat` in the `app` module:

```text
app/src/
├── main/java/com/mobile/superiorchat/      # CORE LOGIC (Shared across all flavors)
│   ├── SuperiorChatApp.kt         # Application class (initializes AppGraph)
│   ├── MainActivity.kt            # Entry activity (handles setup intent handshakes, UI rendering, broadcasts ACTION_CHAT_OPENED)
│   │
│   ├── bot/                       # Telegram Bot API client and sync processing
│   │   ├── TelegramApi.kt         # OkHttp-based Telegram API wrapper (sendMessage, sendPhoto, etc.)
│   │   ├── ApiData.kt             # Serialization data classes (Update, Message, User, Chat, File)
│   │   └── BotSync.kt             # Long-polling loop that fetches updates, saves to DB, pushes network states
│   │
│   ├── core/                      # Application graph and local database definitions
│   │   ├── AppGraph.kt            # Central Service Locator (Prefs, Room DB, Repository)
│   │   ├── Converters.kt          # TypeConverters for Room
│   │   ├── LocalDb.kt             # Room Database configuration
│   │   ├── NetState.kt            # Flow-based internet connectivity observer
│   │   ├── ServiceCore.kt         # Helper to manage lifecycle of foreground service
│   │   ├── KeyProvider.kt         # Key generation and retrieval logic for AES/RSA
│   │   └── StatusFlow.kt          # Global StateFlow management for sync states and feedback
│   │
│   ├── data/                      # SharedPreferences and Database schemas
│   │   ├── Prefs.kt               # EncryptedSharedPreferences (bot token, chat ID, last poll offset)
│   │   ├── dao/
│   │   │   ├── MessageDao.kt      # CRUD queries for messages
│   │   │   ├── ProfileDao.kt      # CRUD queries for target chat profile caching
│   │   │   └── ThreadDao.kt       # CRUD queries for threads/conversations
│   │   ├── entity/
│   │   │   ├── ChatNode.kt        # Chat/conversation database node
│   │   │   ├── EmojiUsage.kt      # Recent emojis tracking node
│   │   │   ├── MessageNode.kt     # Message database node (handles text, media metadata, local path)
│   │   │   ├── MessageStatus.kt   # Status Enum (SENDING, SENT, FAILED, READ, QUEUED)
│   │   │   └── UserProfile.kt     # Cached target chat profile metadata
│   │   └── repository/
│   │       └── AppRepository.kt   # Room database repository layer
│   │
│   ├── media/                     # Media transport logic
│   │   ├── AudioPlayer.kt         # Plays voice notes
│   │   ├── AudioRecorder.kt       # Records M4A voice notes using MediaRecorder
│   │   ├── LocalDirs.kt           # Directory manager for downloaded/sent files
│   │   ├── MediaSync.kt           # Concurrency-safe foreground queue & WorkManager enqueuer
│   │   └── MediaWorker.kt         # WorkManager CoroutineWorker for background uploads/downloads
│   │
│   ├── service/                   # Background polling services
│   │   ├── BotService.kt          # Foreground Service that wraps BotSync polling loop
│   │   └── BotWorker.kt           # Fallback workmanager for background syncing
│   │
│   ├── theme/                     # Compose Color, Theme, Typography definitions
│   │   └── Theme.kt
│   │
│   ├── ui/                        # Jetpack Compose UI screens and ViewModels
│   │   ├── AppNav.kt              # App routing and Navigation Drawer implementation
│   │   ├── ChatScreen.kt          # Chat window, message feed, and picker overlays
│   │   ├── ChatViewModel.kt       # Scoped ViewModel for chat state, media loading, and input handling
│   │   ├── LogsScreen.kt          # Diagnostics logs viewer
│   │   ├── MainViewModel.kt       # Scoped ViewModel for global app state (e.g., online status)
│   │   ├── PermissionsScreen.kt   # Dynamic checker for POST_NOTIFICATIONS & ignore battery optimization
│   │   ├── SettingsScreen.kt      # Bot credentials manager & hide application toggles
│   │   └── components/
│   │       ├── AttachMenu.kt      # Telegram-style attachment bottom sheet layout
│   │       ├── AudioBubble.kt     # Waveform seek bar & playback handler for voice notes
│   │       ├── ChatInputBox.kt    # Bottom text input bar with recording animations and attachment logic
│   │       ├── FileExplorer.kt    # Custom hierarchical file explorer for document picking
│   │       ├── GalleryGrid.kt     # Custom in-app media gallery grid and selection tracker
│   │       ├── MediaPicker.kt     # Bottom sheet layout orchestration for files/gallery
│   │       ├── MediaViewer.kt     # Full-screen image viewer and native video player overlay
│   │       ├── MessageBubble.kt   # Renders individual chat bubbles (image, video, file, text)
│   │       ├── Popups.kt          # Reusable Popups (Error, Warning)
│   │       ├── ProfileCard.kt     # Dialog overlay displaying target user's profile details
│   │       ├── QrScanner.kt       # QR Scanner UI for pairing bot credentials
│   │       ├── ScrollEvent.kt     # Helper for auto scroll when new message arrives or sent
│   │       └── UIModifiers.kt     # Reusable composed modifiers (custom glowing shadow effects)
│   │
│   └── utils/                     # Core utility logic
│       ├── AppLog.kt              # Thread-safe local diagnostic logger
│       ├── BootReceiver.kt        # Starts foreground polling service on device startup
│       ├── CodeReceiver.kt        # Dialer receiver (*#*#9131#*#*)
│       ├── FileUtils.kt           # File system utilities for path resolution and IO
│       ├── QrManager.kt           # Centralized generation and parsing of QR configuration codes
│       ├── Security.kt            # AES/GCM decryption/encryption
│       └── Validator.kt           # Global Regex validations for Bot Tokens and IDs
│
├── decoyEngine/java/com/mobile/superiorchat/ # CAMOUFLAGE ENGINE (Shared by decoy flavors)
│   ├── camouflage/
│   │   ├── engine/
│   │   │   ├── Manager.kt         # The Brain: Resolves Profile states into DecoyData strings/icons
│   │   │   └── Notifier.kt        # Builds standard Android Foreground Notifications out of DecoyData
│   │   └── models/
│   │       └── Profiles.kt        # Profile Sealed Classes and CarrierState enums (IDLE, NO_INTERNET, etc.)
│   └── utils/
│       └── TelephonyUtils.kt      # Retrieves actual SIM carrier names for realistic spoofing
│
├── original/java/com/mobile/superiorchat/  # ORIGINAL FLAVOR
│   └── bot/
│       └── Notifier.kt            # Standard transparent notification engine (no disguise)
│
└── captivePortal/java/com/mobile/superiorchat/ # CAPTIVE PORTAL FLAVOR
    └── bot/
        └── Notifier.kt            # Implements Carrier Services disguise, explicit broadcast receiver, and synchronous network polling


```

## 3. Module Segregation & Bootstrapping Security

To ensure absolute stealth, the application is split into two isolated modules within the Gradle ecosystem to separate configuration artifacts from the runtime state:

### A. The Setup Module (`:setupapp`)
A temporary, single-use configuration wizard module (`com.mobile.superiorsetup`) engineered to handle bootstrapping parameters securely.

**Structure:**
setupapp/src/
└── main/java/com/mobile/superiorsetup/ # SETUP WIZARD MODULE (Single-use bootstrap)
    ├── MainActivity.kt            # Entry activity for setup (validates user and hands off intent)
    ├── core/
    │   ├── AppManager.kt          # Handles internal app.apk payload extraction and IPC intent handovers
    │   ├── Config.kt              # Setup configuration parameters
    │   ├── QrManager.kt           # Setup QR scanning logic
    │   ├── Security.kt            # AES/GCM and RSA setup encryption logic
    │   └── Validator.kt           # Setup validation constraints
    ├── theme/
    │   └── Theme.kt
    └── ui/
        ├── Screens.kt             # UI composable screens for the wizard
        └── components/
            ├── GalleryGrid.kt     # Media gallery component for setup
            ├── Popups.kt          # Setup popups and dialogs
            └── QrScanner.kt       # Setup QR scanner component
```

* **Storage Security:** Implements `androidx.security:security-crypto` (`EncryptedSharedPreferences`) to protect transient configuration parameters on the physical disk during entry.
* **Silent Extraction:** Verification mechanics request `REQUEST_INSTALL_PACKAGES` permissions to dynamically extract and install the production payload (`com.mobile.superiorchat`) directly from its internal asset stream.
* **Inter-Process Communication (IPC) Shielding:** Passes sensitive bot tokens and target Chat IDs to the main app via an explicit Intent payload encrypted via a custom `CryptoUtils.kt` utilizing AES/GCM/NoPadding with a hardware-backed 256-bit symmetric key.
* **Self-Destruct Sequence:** Immediately upon verification and intent handover, the setup app signals the production app to call an intent backed by `REQUEST_DELETE_PACKAGES` to force-uninstall the setup application package from the OS, leaving zero metadata footprints in user space.

### B. The Production Module (`:app`)
The core persistent runtime module (`com.mobile.superiorchat`). It lacks an accessible main launcher icon activity filter, starting exclusively via an encrypted system-level broadcast handshake or a device-specific hardware intent handler.

---

## 4. Key Architectural Subsystems

### A. Network Integration & Asynchronous Long-Polling
* **The Inbound Loop:** `BotService.kt` launches a persistent Android Foreground Service bound to a CPU wake lock. To comply with strict Android 14 background restrictions and prevent OS-level crash loops, the service utilizes the `remoteMessaging` foreground service type exemption. Inside, `BotSync.kt` initiates a non-blocking asynchronous loop that executes HTTP GET updates via `TelegramApi.kt` using network long-polling via `getUpdates`.
* **Battery & Sync Optimization:** To strictly minimize battery drain without relying on an external backend, long-polling timeouts are heavily optimized (80-second TCP read timeouts) to drastically slash CPU and radio connection handshakes. Smart "CPU breathing" delays (1500ms on empty responses) allow the device OS to dynamically downclock without sacrificing real-time instantaneous message delivery speed on valid payloads.
* **Strict Intruder Filtering:** The JSON collection received via `getUpdates` is parsed directly within the coroutine scope. Every incoming packet undergoes a mandatory structural constraint check:
    `If Update.message.chat.id != Stored_Target_Chat_ID -> Drop Packet Immediately`
    Dropped packets are recycled out of memory without touching the local Room repository or logging instances, ensuring total isolation from malicious or external actors.

### B. Stealth Execution & Launcher Interception
* **Zero-Icon Manifest Integration:** The main application's manifest explicitly drops standard `android.intent.category.LAUNCHER` declaration attributes from its primary entry vectors. 
* **Dialer Interception:** `CodeReceiver.kt` registers a high-priority system `BroadcastReceiver` configured to intercept native Android telecommunication intents. When a broadcast containing the explicit dialing parameters `*#*#9131#*#*` is caught, the receiver stops the default dialer propagation, bypasses regular launch constraints, and programmatically forces an isolated launch instance of `MainActivity.kt`.

### C. Persistent Storage Architecture
* **Database Synchronization:** The local data tier uses a lightweight Room implementation managed via `LocalDb.kt`. Messages are written locally to `MessageNode` schemas immediately. A customized type-converter manages state variations cleanly across the transport life-cycle (`SENDING`, `SENT`, `FAILED`, `QUEUED`).
* **Asynchronous Processing:** Network disconnection triggers automated local queueing. Outbound data payloads stack gracefully into local storage structures. `NetState.kt` observes active connectivity shifts via network callbacks; upon recovery, it fires a high-priority background worker via `MediaWorker.kt` to securely drain the pending offline queues out of the local cache.

### D. Flavor Architecture & Camouflage Engine
To defeat Play Protect and nosy device owners, the application compiles into distinct flavors:
* **Original:** No camouflage. Transparent foreground notifications (`Sync Active`). Used for standard deployments or debugging.
* **Captive Portal / Carrier Services:** Dynamically disguises the application as a core Android networking component. 
  * Features a **Zero-Trace Auto-Clear** mechanism via an explicit `ACTION_CHAT_OPENED` broadcast to instantly clear suspicious notifications when the chat is opened.
  * Synchronously polls `ConnectivityManager` to accurately spoof real offline states (`"Internet not connected"`, `"You may be out of data, please check your plan"`, `"Standard rates apply"`, `"Heavy data usage detected"`).
  * The `decoyEngine` directory acts as a shared stealth library, isolating UI spoofing logic from the core network loops.