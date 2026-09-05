# Setup Guide

<p align="center">
  <strong>Complete Step-by-Step Connection Instructions for Superior Chat</strong>
</p>

---

<div align="center">

| Mode | Who Uses What? | Key Benefit | Difficulty |
| :--- | :--- | :--- | :---: |
| **[Option 1: App to Telegram](#option-1-app-to-telegram-easiest)** | **Person A**: Official Telegram<br>**Person B**: Disguised Chat App | Easiest setup • No group needed | ⭐ *Beginner* |
| **[Option 2: App to App](#option-2-app-to-app-both-on-superior-chat)** | **Person A**: Disguised Chat App<br>**Person B**: Disguised Chat App | Both chat inside app • Mutual call popups | 🚀 *Advanced* |

</div>

> [!NOTE]
> If you have not installed the application on your phone yet, please read the **[Installation Guide](Installation.md)** first.

---

## 📑 Table of Contents
- [🟢 Option 1: App to Telegram (Easiest)](#option-1-app-to-telegram-easiest)
  - [🤖 Step 1: Create the Telegram Bot (Person A)](#step-1-create-the-telegram-bot-person-a)
  - [🔐 Step 2: Generate the Setup QR Code (Person A)](#step-2-generate-the-setup-qr-code-person-a-for-person-b)
  - [📱 Step 3: Connect on Person B's Device](#step-3-connect-on-person-bs-device)
- [🚀 Option 2: App to App (Both on Superior Chat)](#option-2-app-to-app-both-on-superior-chat)
  - [🔄 How App-to-App Mode Works](#how-app-to-app-mode-works)
  - [🤖 Step 1: Create Bots & Configure Settings via @BotFather Web Page](#step-1-create-bots--configure-settings-via-botfather-web-page)
  - [👥 Step 2: Create Private Telegram Group & Promote Both Bots to Admin](#step-2-create-private-telegram-group--promote-both-bots-to-admin)
  - [⚡ Step 3: Generate Dual QRs in Setup App (Person A / Admin)](#step-3-generate-dual-qrs-in-setup-app-person-a--admin)
  - [📲 Step 4: Scan the QRs to Connect Both Devices](#step-4-scan-the-qrs-to-connect-both-devices)
- [📞 Configuring Voice & Video Calls (Optional)](#configuring-voice--video-calls-optional)

---

## 🟢 Option 1: App to Telegram (Easiest)

In this mode, **Person A** chats directly inside the official Telegram app, while **Person B** chats inside the disguised Superior Chat app. Person A acts as the Administrator and sets up the bridge.

### 🤖 Step 1: Create the Telegram Bot (Person A)

1. Open your official Telegram app and search for **[@BotFather](https://t.me/BotFather)**.
2. Send the command `/newbot` to create a new bot.
3. Follow the prompts to choose a display name and a username ending in `bot` (e.g., `my_secret_chat_bot`).
4. Once created, BotFather will give you an **API Token** (e.g., `123456789:ABCdefGhIJKlmNoPQRsTUvwxyz`). Keep this safe!
5. Open a chat with your newly created bot on Telegram and press **Start** (or send `/start`).
6. Retrieve your personal **Telegram User ID** (forward any message to `@userinfobot` or `@MissRose_bot` to get your numeric ID).

### 🔐 Step 2: Generate the Setup QR Code (Person A) for Person B

Person A uses the **Superior Setup App** to safely encrypt these credentials into a scannable QR code:

1. Download and open the **Setup App** on your device.
2. Tap the **Admin Mode** toggle located at the top right (below the step indicator).
3. Select **"I will chat on Telegram (Standard Mode)"** and continue to Step 2.
4. Enter the **Bot Token** and **Your Telegram User ID** (Chat ID).
5. **Enable PIN Protection (Recommended)**: If PIN protection is enabled, the QR code is securely encrypted with a 4-digit PIN. Share this 4-digit PIN with Person B so they can import it.
6. Share the generated encrypted QR code with **Person B** to scan, or display it on screen for Person B to scan.

> [!WARNING]
> **Direct Mode Vulnerability**: If you disable "Require PIN" to generate a Direct Mode QR (`DIR_QR`), the app falls back to a default key. Because this project is open-source, anyone who gets hold of an un-PINned QR code could decrypt it and gain access to your Telegram bot. Always use PIN protection! See the [Threat Model in Notes.md](Notes.md#threat-model) for details.

<p align="center">
  <img src="images/setupapp/admin_mode/adminmode_step1.jpg" width="30%" alt="Admin Mode Step 1">
  &nbsp;
  <img src="images/setupapp/admin_mode/adminmode_step3.jpg" width="30%" alt="Admin Mode Step 3">
  &nbsp;
  <img src="images/setupapp/admin_mode/adminmode_step6.jpg" width="30%" alt="Generate QR Code">
</p>
<p align="center">
  <b>📂 <a href="images/setupapp/admin_mode/">View Admin Mode Screenshots Directory</a></b>
</p>

### 📱 Step 3: Connect on Person B's Device

Person B opens Superior Chat (or Setup App in Client Mode) and imports the **Client QR** code (**Three ways to connect**):

1. 📲 **Using Setup App (Captive Portal & Play Support)**: In Client Mode Step 2, tap **Scan QR Code**, scan the QR, enter the 4-digit PIN, and tap **Wake Up & Launch**.
2. 📷 **Using Weather or Original Flavor**: Simply open the app and tap the **Scanner button on top of the chat screen** to scan the QR code directly!
3. ⌨️ **Manual Entry**: In Settings → Credentials, Person B can also manually paste the Bot Token and Person A's Telegram User ID.

---

## 🚀 Option 2: App to App (Both on Superior Chat)

In this mode, **both Person A and Person B chat directly inside Superior Chat**. Neither user uses the official Telegram app for messaging.

> [!TIP]
> **Prefer Video Walkthroughs?**  
> Complete video guides demonstrating each step of the App-to-App setup are available on our official Telegram Group: **[@SuperiorChatGithub](https://t.me/SuperiorChatGithub)**.

### 🔄 How App-to-App Mode Works

Instead of chatting directly with a bot in a 1-on-1 DM, two bots communicate inside a private Telegram group:
- 🤖 **Two Separate Bots**: One bot represents **Person A (Admin)**, and another bot represents **Person B (Partner)**.
- 🔄 **Bot-to-Bot Communication Mode**: Enabled in `@BotFather`'s web page so both bots can see and reply to each other's messages inside the group.
- 👥 **Private Telegram Group**: Acts as the secure, private communication bridge connecting both bots.
- ⚡ **Setup App Dual QRs**: Automatically packs all credentials into two separate QR codes:
  - 📱 **Client QR**: Applied on Person B's phone (enables *Route Messages*).
  - 🛡️ **Admin QR**: Applied on Person A's phone (enables *I Am Admin*).

### 🤖 Step 1: Create Bots & Configure Settings via @BotFather Web Page

> [!IMPORTANT]
> Telegram's **Bot-to-Bot Communication Mode is NOT available through standard chat commands or bot buttons**. It is **only available inside BotFather's Web Page**! Everything—creating both bots and configuring their settings—can be done directly inside this web page.

1. Open **[@BotFather](https://t.me/BotFather)** in Telegram.
2. Tap the **Open** button on the bottom-left (located right where the text input box is) to launch the **BotFather Web Page**.
3. **Create Two Bots** inside the web page:
   - Create **Bot 1** (for your partner, e.g., `alice_chat_bot`) and save its API token.
   - Create **Bot 2** (for yourself, e.g., `bob_admin_bot`) and save its API token.
4. **Configure Bot Settings** inside the web page (repeat for both bots):
   - Select your bot → open **Bot Settings**:
     - **Bot-to-Bot Communication Mode**: **Enable** *(mandatory for bots to receive each other's messages in a group)*.
     - **Group Privacy**: **Turn OFF / Disable** *(mandatory for bots to read group messages)*.
     - **Allow Groups?**: Ensure it is **Turned ON**.

<p align="center">
  <img src="images/setupapp/admin_mode/apptoapp_botfather1.jpg" width="30%" alt="Launch BotFather Web Page">
  &nbsp;
  <img src="images/setupapp/admin_mode/apptoapp_botfather2.jpg" width="30%" alt="Create New Bot in Web Page">
  &nbsp;
  <img src="images/setupapp/admin_mode/apptoapp_botfather3.jpg" width="30%" alt="Enable Bot to Bot Mode & Disable Group Privacy">
</p>

### 👥 Step 2: Create Private Telegram Group & Promote Both Bots to Admin

1. In Telegram, create a new **Private Group** and enable **Visible History** (Chat history for new members: Visible).
2. Add **both bots** (`alice_chat_bot` and `bob_admin_bot`) to this group.
3. Open Group Settings → **Administrators** → Promote **BOTH bots to Administrator** (grant them full permissions: send messages, embed links, manage chat, etc.).
4. Retrieve your **Group Chat ID** (starts with `-100...`). You can easily find it by adding `@MissRose_bot` to the group and sending the command `/id`.

### ⚡ Step 3: Generate Dual QRs in Setup App (Person A / Admin)

Person A (Admin) uses the **Superior Setup App** to verify the entire configuration live and generate both QR codes:

1. 🛡️ **Admin Mode**: Toggle **Admin Mode** at the top right of the Setup App.
2. 🎛️ **Setup Hub**: Tap the **Setup QR Code** action card and continue.
3. 💬 **Choose Chat Mode**: Select **"I will also chat on Superior Chat (App-to-App Mode)"** and continue.
4. 🔍 **2-Bot Credentials & Live Verification**:
   - Enter **Partner's Bot Token** (her bot token).
   - Enter **Your Bot Token** (your admin bot token).
   - Enter **Group Chat ID** (e.g., `-1001234567890`).
   - Tap **Verify**: The Setup App checks Telegram live to verify that:
     - Both tokens are active and usernames are extracted.
     - Both bots are inside the group.
     - **Both bots are Administrators**.
     - **Group Privacy is disabled** on both bots.
   - Once all checks pass with green checkmarks, tap **Continue**.
5. 🎭 **Stealth & Disguise Customization**:
   - Optionally configure stealth access parameters (e.g., custom Weather search word, secret dialer code `*#*#<code>#*#*`, and screen security).
6. 🔐 **Security & Dual QR Generation**:
   - Keep **Require PIN** enabled (recommended 4-digit PIN).
   - Optional: Enable **Hard Lock** if you want to permanently lock Admin Settings on partner's phone.
   - Tap **Generate QR Code**.
   - The app displays **Dual QR Codes** with a tab selector:
     - 📱 **Partner QR (Client QR)**: For Person B's phone.
     - 🛡️ **Your QR (Admin QR)**: For Person A's phone.
   - Save both QR codes to your gallery or keep them on screen.

<p align="center">
  <img src="images/setupapp/admin_mode/adminmode_a2a_step3.jpg" width="30%" alt="Choose App-to-App Mode">
  &nbsp;
  <img src="images/setupapp/admin_mode/adminmode_a2a_step4.jpg" width="30%" alt="Live Telegram Verification">
  &nbsp;
  <img src="images/setupapp/admin_mode/adminmode_a2a_step6.jpg" width="30%" alt="Dual QR Generation">
</p>
<p align="center">
  <b>📂 <a href="images/setupapp/admin_mode/">View Admin Mode Screenshots Directory</a></b>
</p>

### 📲 Step 4: Scan the QRs to Connect Both Devices

The universal scanner automatically detects the role encoded in each QR code and provisions the application instantly:

* 🛡️ **Person A (Admin Device)**: Open Superior Chat, tap the **Scanner button on top of the chat screen**, scan your **Admin QR**, and enter the 4-digit PIN. Your device is configured and ready as the Admin!
* 📱 **Person B (Partner Device)**: Open Superior Chat, tap the **Scanner button on top of the chat screen**, scan the **Partner QR (Client QR)**, and enter the 4-digit PIN. Her device is connected and ready to chat!

> [!NOTE]
> If using invisible camouflage flavors (*Captive Portal* or *Play Support*) on Person B's phone, she can also use the **Setup App in Client Mode** to install the camouflage chat application and scan the Partner QR in Step 2.

---

## 📞 Configuring Voice & Video Calls (Optional)

Superior Chat features a secure, peer-to-peer WebRTC calling engine. By default, it connects through public infrastructure, but you can configure it for maximum privacy.

1. **Test Default Calls**: Open a chat and press the **Call** button. If it connects successfully, you are ready to go!
2. **Configure Custom Server (Recommended)**: For true security and privacy, you should not rely on the default public servers.
   - Go to **Application page → App Settings → Call Configuration**.
   - Enter your self-hosted WebRTC domain (e.g., `https://call.yourdomain.com`).
   - Need help hosting? Read the **[WebRTC Deployment Guide](../webrtc/docs/Deployment.md)**.

---

<div align="center">

### 💬 Need Help or Full Video Guides?
Stuck on any step, need troubleshooting assistance, or want to watch complete setup and demo videos?  
👉 **Check our Telegram Group: [@SuperiorChatGithub](https://t.me/SuperiorChatGithub)**

</div>

---

<div align="center">
  <sub>Built with ❤️ by <a href="https://gitlab.com/sandeshsahu">@sandeshsahu</a></sub>
</div>
