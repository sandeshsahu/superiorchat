<h1 align="center">
  Installation & Setup Guide
</h1>

<p align="center">
  <strong>Complete Step-by-Step Instructions for Superior Chat</strong>
</p>

---

> [!NOTE]
> Superior Chat is a two-way system. **Person A** uses the official Telegram app, while **Person B** uses the custom Superior Chat app (or vice-versa). They communicate through a shared Telegram Bot.

---

## Table of Contents
- [Download: Pre-built APKs](#download)
- [Phase 1: Telegram Side (Person A)](#phase-1)
- [Phase 2: App Side (Person B)](#phase-2)
- [Phase 3: Building From Source (Developers)](#phase-3)

---

<h2 id="download">📥 Download Pre-built APKs</h2>

Before you begin, you must download the necessary APK files from our official GitLab repository.

> [!IMPORTANT]
> Currently 2 camouflaged flavors of the app are available for download, which are **[Captive Portal](flavors/CaptivePortal.md)** and **[Weather](flavors/FlavorWeather.md)**.

**Naming of flavors**:
- **Flavor** = Available Camoflouged Flavor (e.g. `CaptivePortal`)
- `[Flavor] App_Release.apk` - flavor of the setup app (`captiveportal`) and app (`weather`).
- `[Original] App_Release.apk` - non-hidden, standard version of the app

👉 **[Download Latest APKs from GitHub Releases](https://github.com/sandeshsahu/superiorchat/releases)**

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


**What to download:**
- `[Flavor]SetupApp_Release.apk` (Needed by Person A to generate the QR code, and Person B to install the Camouflage app, only if person b uses `captive portal` flavor. If person b uses `weather` flavor no need of setup app for him/her).
- `[Original]App_Release.apk` (Only needed if Person B wants the non-hidden, standard version).

---

<h2 id="phase-1">🤖 Phase 1: Telegram Side (Person A)</h2>

Person A (the person using the official Telegram app) needs to create the communication bridge (the Bot) and generate a secure QR code for Person B.

### Step 1: Create the Telegram Bot
1. Open your Telegram app and search for **[@BotFather](https://t.me/BotFather)**.
2. Send the command `/newbot` to create a new bot.
3. Follow the prompts to choose a name and username for your bot.
4. Once created, BotFather will give you an **API Token** (e.g., `123456789:ABCdefGhIJKlmNoPQRsTUvwxyz`). Keep this safe!
5. Start a chat with your new bot.
6. Note down your personal **User ID** (You can easily get this by forwarding a message to `@userinfobot` or `@MissRose_bot`).

### Step 2: Generate the Setup QR Code
Person A uses the Superior Setup App to safely pack these credentials into an encrypted QR code.
1. Download and open the **Setup App** on your device.
2. Tap the **Admin Mode** toggle located at the top right (below the step indicator).
3. Proceed to **Step 2** and enter the Bot Token and User ID you got from Telegram.
4. Save the generated encrypted QR code or scan it directly on Person B's phone.

<br>
<p align="center">
  <img src="images/setupapp/admin_mode/adminmode_step1.jpg" width="30%" alt="Admin Mode Step 1">
  &nbsp;
  <img src="images/setupapp/admin_mode/adminmode_step2.jpg" width="30%" alt="Admin Mode Step 2">
  &nbsp;
  <img src="images/setupapp/admin_mode/adminmode_generateqr.jpg" width="30%" alt="Generated QR Code">
</p>

---

<h2 id="phase-2">📱 Phase 2: App Side (Person B)</h2>

Person B will install the actual Superior Chat application. There are three different "Flavors" of the app you can choose from depending on your need for stealth.

### Option A: Captive Portal Flavor (System Camouflage)
This version hides itself entirely and requires the Setup App to install and configure.

1. **Install Setup App**: Download and install the `Setup App` APK.
2. **Client Mode**: Open it and ensure it is in **Client Mode** (default).
3. **Install Main App**: When prompted during Step 1, click to install the "Fake-Launcher" (Camouflage) chat application.
4. **Scan Credentials**: Continue to Step 2. Scan the encrypted QR code provided by Person A (or manually type the credentials).
5. **Wake Up & Bind**: Continue to Step 3. Tap the button to open the Main app from within the Setup App. This locks in your credentials.
6. **Cover Your Tracks**: The main app will now ask you to uninstall the Setup App. Choose **Uninstall** to remove all traces of configuration.
7. **Start Chatting**: Use your secret dialer code (`*#*#9131#*#*`), Quick Settings tile, or App Search Interception (for [weather flavor](flavors/FlavorWeather.md)) to access the app going forward!

<br>
<p align="center">
  <img src="images/setupapp/client_mode/clientmode_step1.jpg" width="30%" alt="Client Mode Step 1">
  &nbsp;
  <img src="images/setupapp/client_mode/clientmode_step2.jpg" width="30%" alt="Client Mode Step 2">
  &nbsp;
  <img src="images/setupapp/client_mode/clientmode_step3.jpg" width="30%" alt="Client Mode Step 3">
</p>

### Option B: Weather Flavor (App Camouflage)
This version perfectly mimics a functional weather application and does not require the Setup App.

1. **Install Main App**: Download the latest "Weather" release APK and install it.
2. **Access Hidden Engine**: Open the "Weather" app. Tap the search bar, type `superior chat` (or your custom access word), and hit the search/enter key on your keyboard.
3. **Open Settings**: The hidden chat engine will launch. Go directly to App Settings.
4. **Enter Credentials**: Fill in your Bot Token and Chat ID (the QR Code scanner is also available).
5. **Start Chatting**: You are ready to go!

<br>
<p align="center">
  <img src="flavors/images/flavor_weather/1.jpg" width="30%" alt="Weather UI Facade">
  &nbsp;
  <img src="flavors/images/flavor_weather/2.jpg" width="30%" alt="7-Day Forecast">
  &nbsp;
  <img src="flavors/images/flavor_weather/3.jpg" width="30%" alt="Search Interception">
</p>

### Option C: Original Flavor (Non-Camouflage)
This version has a standard app icon and is simpler to set up (recommended for testing).

1. **Install Main App**: Download the latest "Original" release APK and install it.
2. **Open Settings**: Launch the app from your app drawer and go directly to App Settings.
3. **Enter Credentials**: Fill in your Bot Token and Chat ID or Scan the QR Code.
4. **Start Chatting**: You are ready to go!

---

<h2 id="phase-3">🛠️ Phase 3: Building From Source (Developers)</h2>

If you prefer to compile the application yourself instead of downloading the pre-built APKs, you can use the following Gradle commands in Android Studio or your terminal.

### Compiling the Apps

| App Flavor | Gradle Command | Description |
|------------|----------------|-------------|
| **Original** | `./gradlew app:assembleOriginalDebug` | Builds the standard visible chat app. |
| **Captive Portal** | `./gradlew app:assembleCaptivePortalDebug` | Builds the camouflaged invisible chat app (carrier alerts). |
| **Weather** | `./gradlew app:assembleWeatherDebug` | Builds the disguised weather app (live context). |
| **Setup App** | `./gradlew setupapp:assembleCaptivePortalDebug` `./gradlew setupapp:assembleWeatherDebug`| Builds the Setup wizard application with the respective hidden flavor. |

> [!WARNING]
> **Important Note for Setup App Compilation:**
> When building the Setup App for a camouflaged flavor, you **must** first build the `app:assemble<Flavor>Debug` APK. Once built, copy that resulting APK file into the `setupapp\src\main\assets` folder and name it `superiorchat.apk` **before** running the `setupapp` build command. This bundles the hidden app inside the Setup wizard!
---

<p align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</p>