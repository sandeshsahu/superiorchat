<h1 align="center">
  Installation Guide
</h1>

<p align="center">
  <strong>Download & Installation Instructions for Superior Chat</strong>
</p>

---

> [!NOTE]
> **Superior Chat supports two setup modes:**
> - **App to Telegram (Standard)**: **Person B (Client)** chats inside the hidden Superior Chat app, while **Person A (Admin)** chats directly through the official Telegram bot's direct messages (DM). Only Person B installs the hidden app.
> - **App to App (Dual Stealth)**: Both **Person A** and **Person B** install the hidden Superior Chat app on their devices. This unlocks extra features like mutual in-app incoming call popups, bidirectional stealth, and full in-app media management.
>
> For step-by-step instructions on configuring your bots and generating setup QR codes for either mode, refer to the **[Setup Guide](SetupGuide.md)**.

---

## Table of Contents
- [Download: Pre-built APKs](#download)
- [Google Play Protect Notice](#play-protect)
- [Choosing & Installing Your App Flavor](#installing-the-app)
  - [Option A: Invisible Flavors (Captive Portal & Play Support)](#option-a)
  - [Option B: Weather Flavor (Disguised)](#option-b)
  - [Option C: Original Flavor (Standard / Testing)](#option-c)
- [Building From Source (Developers)](#building-from-source)

---

<h2 id="download">📥 Download Pre-built APKs</h2>

Before you begin, download the APK files for your desired camouflage flavor from our official releases.

> [!IMPORTANT]
> Currently, 3 camouflaged flavors of the app are available:
> - **[Captive Portal](flavors/CaptivePortal.md)**: Completely invisible with no app icon; disguises notifications as carrier system alerts.
> - **[Play Support](flavors/PlaySupport.md)**: Completely invisible with no app icon; disguises notifications as Google Play updates.
> - **[Weather](flavors/FlavorWeather.md)**: Looks and functions like a real weather application with real-time weather forecasts.
> - **Original**: Standard app icon with visible branding (best for testing and development).

### Understanding the File Names
Downloadable APKs follow a simple pattern: `[Type]-[Flavor]-release.apk`

- **Type**:
  - `app`: The main secret chat application.
  - `setupapp`: A temporary helper app. Used by **Person A (Admin)** to generate setup QR codes, and used by **anyone** installing iconless flavors (`captiveportal` or `playsupport`) to install and launch the hidden app for the first time. The main app prompts you to uninstall the Setup App once configured.
- **Flavor**: The visual disguise (`weather`, `playsupport`, `captiveportal`, or `original`).

### Which File Do You Need?
- **For Invisible Flavors (Captive Portal / Play Support)**: Download `setupapp-[flavor]-release.apk`. The setup app bundles and installs the hidden app for you.
- **For Weather Flavor**: Simply download `app-weather-release.apk` directly.
- **For Original Flavor**: Download `app-original-release.apk` directly.
- **For Admin QR Generation (Person A)**: Download any `setupapp-[flavor]-release.apk` to generate connection QR codes in Admin Mode.

👉 **[Download Latest APKs from GitHub Releases](https://github.com/sandeshsahu/superiorchat/releases)**

---

<h2 id="play-protect">🛡️ Google Play Protect Notice</h2>

> **Why you might see a "Harmful App" warning during installation**
> 
> Because this app is distributed as an open-source APK (not hosted on Google Play Store), **Google Play Protect may display a warning.** This happens because the app requests standard permissions for messaging, media sharing, and uses the Telegram Bot API for communication.
> 
> This is an automated Android system check. **You have full control over how you proceed**:
> 
> 1. **Audit & Build Yourself:** This project is completely open-source. You can inspect the entire codebase and compile your own APKs from source.
> 2. **Use Pre-Built Releases:** Official signed APKs are automatically built directly from source code by GitHub's secure, isolated build environment whenever a new release tag is pushed.
> 
> If you choose to install the pre-built APK and see the warning, tap **More Details $\rightarrow$ Install Anyway**.

---

<h2 id="installing-the-app">📱 Choosing & Installing Your App Flavor</h2>

Choose the flavor that best matches your stealth requirements:

### Option A: Invisible Flavors (Captive Portal & Play Support)
These flavors have **no launcher icon** in your phone's app drawer and require the Setup App for their initial installation and first launch.

1. **Install Setup App**: Download and install `setupapp-captiveportal-release.apk` or `setupapp-playsupport-release.apk`.
2. **Step 1 (Install Main App)**: Open the Setup App in **Client Mode** (default) and tap to install the bundled camouflage app.
3. **Step 2 (Scan QR Code)**: Tap **Scan QR Code**, scan your setup QR code, and enter the 4-digit PIN.
4. **Step 3 (Wake Up & Launch)**: Tap the button to launch the hidden app for the first time. This binds your credentials.
5. **Cover Your Tracks**: The app will prompt you to uninstall the Setup App. Tap **Uninstall** so no trace remains!
6. **Accessing the App**: Open your app anytime using your secret dialer code (default: `*#*#9131#*#*`) or Quick Settings tile sequence (`On` $\rightarrow$ `Off` $\rightarrow$ `On` $\rightarrow$ `Hold` within 3 seconds).

<br>
<p align="center">
  <img src="images/setupapp/client_mode/clientmode_step1.jpg" width="30%" alt="Client Mode Step 1">
  &nbsp;
  <img src="images/setupapp/client_mode/clientmode_step2.jpg" width="30%" alt="Client Mode Step 2">
  &nbsp;
  <img src="images/setupapp/client_mode/clientmode_step3.jpg" width="30%" alt="Client Mode Step 3">
</p>
<p align="center">
  <b>📂 <a href="images/setupapp/client_mode/">View Client Mode Screenshots Directory</a></b>
</p>

---

### Option B: Weather Flavor (Disguised)
This flavor looks, opens, and behaves exactly like a real weather forecast application. It can be installed directly without the Setup App.

1. **Install Main App**: Download and install `app-weather-release.apk`.
2. **Access the Secret Chat**: Open the Weather app. Tap the city search bar at the top, type `superior chat` (or your custom access word), and press the **Search / Enter** key on your keyboard.
3. **Scan QR Code**: Tap the **Scanner button on top of the chat screen**, scan your setup QR code, and enter your 4-digit PIN.
4. **Start Chatting**: You are completely set up and ready to chat!

<br>
<p align="center">
  <img src="flavors/images/flavor_weather/1.jpg" width="30%" alt="Weather UI Facade">
  &nbsp;
  <img src="flavors/images/flavor_weather/2.jpg" width="30%" alt="7-Day Forecast">
  &nbsp;
  <img src="flavors/images/flavor_weather/3.jpg" width="30%" alt="Search Interception">
</p>
<p align="center">
  <b>📂 <a href="flavors/images/flavor_weather/">View Weather Flavor Screenshots Directory</a></b>
</p>

---

### Option C: Original Flavor (Standard / Testing)
This version has a standard app icon and is simpler to navigate (recommended for testing and development).

1. **Install Main App**: Download and install `app-original-release.apk`.
2. **Open & Scan**: Open the app from your app drawer, tap the **Scanner button on top of the chat screen**, and scan your setup QR code.
3. **Start Chatting**: You are connected!

---

<h2 id="building-from-source">🛠️ Building From Source (Developers)</h2>

If you prefer to compile the applications yourself instead of using pre-built APKs, run the following Gradle commands in Android Studio or your terminal:

### Compiling the Apps

| App Flavor | Gradle Command | Description |
|------------|----------------|-------------|
| **Original** | `./gradlew app:assembleOriginalDebug` | Builds the standard visible chat app. |
| **Captive Portal** | `./gradlew app:assembleCaptivePortalDebug` | Builds the invisible chat app (carrier alert camouflage). |
| **Play Support** | `./gradlew app:assemblePlaySupportDebug` | Builds the invisible chat app (Google Play promotion camouflage). |
| **Weather** | `./gradlew app:assembleWeatherDebug` | Builds the disguised weather app (live context). |
| **Setup App** | `./gradlew setupapp:assembleCaptivePortalDebug`<br>`./gradlew setupapp:assemblePlaySupportDebug`<br>`./gradlew setupapp:assembleWeatherDebug`| Builds the Setup wizard application with the respective hidden flavor. |

> [!WARNING]
> **Important Note for Setup App Compilation:**
> When building the Setup App for a camouflaged flavor, you **must** first build the `app:assemble<Flavor>Debug` APK. Once built, copy that resulting APK file into the `setupapp\src\main\assets` folder and name it `superiorchat.apk` **before** running the `setupapp` build command. This bundles the hidden app inside the Setup wizard!

<br>

> [!TIP]
> 💬 **Need help, demo videos, or instruction guides?**  
> Check out video walkthroughs and get support on our official Telegram channel: **[@SuperiorChatGithub](https://t.me/SuperiorChatGithub)**.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>
