# Android TV Guard (தொலைக்காட்சி காவலன்)

**Package:** `com.universalrp.tvguard`  
**Compatibility:** Android 8.0 / API 26 or later (optimized for Android TV, Google TV, and AOSP TV boxes).  
**Navigation:** Remote D-pad, air mouse, and USB mouse controls with high-contrast focus states; key delivery varies by TV and remote.

---

## Overview

Some Android TV boxes (including models reported as Allwinner, Rockchip or Amlogic) can report specifications that conflict with other system readings. A mismatch deserves investigation, but software-visible reports alone cannot prove the installed storage chip or authenticate the Android version. Preinstalled apps and capabilities also vary by device.

**Android TV Guard** is a free, open-source, zero-ad TV utility and clean launcher that:
1. **Shows hardware evidence** from Linux kernel, filesystem and Android reports, with warnings for selected inconsistencies.
2. **Lists debloating options** through Root removal (on rooted boxes) or Wireless ADB commands (on unrooted boxes); review each choice before removal.
3. **Provides a TV launcher** with independently optional memory, storage, CPU and network indicators.

---

## Lightweight launcher and Expert controls

TV Guard defaults to **launcher only**: the app grid, favorites, hidden-app management, Android settings, default-home chooser and About remain available. The status header/pills, screenshots, Wi-Fi hub, remote controls, hardware audit, Debloater, network diagnostics, RAM boost and running-app scan are all **off by default**. Existing favorites, hidden apps and files are not cleared. Optional code remains packaged in the APK, and Android itself may run services; this does not guarantee a particular memory saving.

To turn features on, open **Tools & Settings → Expert settings**, focus **Expert mode** and press OK. The master switch reveals a separately saved switch for each feature. Use the D-pad and OK to change them. Turning Expert off pauses all extras without erasing their selections; turning it back on restores the saved selections, and a previously enabled **file hub can start automatically**. To keep it off, turn off its own switch before leaving Expert mode.

- The **status strip** is one switch (`STATUS_OVERLAY`), not one per reading: processor load, free RAM, free storage, temperature and internet quality all appear together on a single movable line, and there is no switch that shows only some of them. It samples only while TV Guard is started; network sampling makes TCP requests to public DNS and common local-router addresses.
- **No AI, voice or system assistant.** Per owner request (Nov 2026) TV Guard is AI-free. The previous `voice_helper`, `system_assistant`, and the five per-reading pill switches (`cpu_pill`, `ram_pill`, `storage_pill`, `network_pill`, `temp_pill`) were removed completely. Old saved keys are ignored on load (see `TvFeatureSettingsTest.theRemovedFeaturesNoLongerExist`). There is no microphone permission, no speech recognizer, and no AI request — unlike Live Guard which keeps its optional AI (see below).
- Screenshot capture and the **Wi-Fi file hub** are independent. Disabling screenshots does not delete existing files, which an enabled hub can still expose. Browser remote controls require the hub, and remote capture/boost also require their own switches. Turning off the hub or Expert closes the listener and active connections; an action already handed to Android or root may not be reversible.
- Cancellation of polls and scans is requested when the launcher is stopped or the feature is turned off; a bounded I/O call already in progress may take a moment to finish. A chosen file hub remains reachable while the activity exists, even if another app is in front, and stops when disabled or the activity is destroyed. No reset or uninstall is needed.

> **Live Guard is different:** Live Guard (`cleaner`) **keeps** its optional AI — news analysis, translation, `AiClient`, `AssistantScreen` and the “optional AI analysis with your own key” About text are intentional and unchanged. Only TV Guard had AI removed.

**Security:** the optional hub has **no authentication or encryption**. Anyone who can reach the TV on the local network may view, upload or delete files in Downloads and saved screenshots; if browser remote is enabled, they can also send controls and installation prompts. Use it only on a trusted network and switch it off when finished. Review files before installing them.

## Core Capabilities

### 1. Hardware Truth Auditor (Fake Buster)
- **RAM Truth:**
  - Reads kernel-managed physical memory directly from `/proc/meminfo` (`MemTotal`, `MemFree`, `MemAvailable`, `Cached`, `Buffers`, `SwapTotal`).
  - Flags selected discrepancies between Android and kernel RAM reports; reserved memory and firmware can also affect the readings.
- **Storage evidence:**
  - Reads a candidate whole-disk sector count from `/sys/block` or `/proc/partitions`, and compares it with the reported `/data` filesystem size. The screen identifies the source and highlights a substantial mismatch; a candidate may be removable or may not back `/data`.
  - Shows `/data` totals and available bytes as reported, listed filesystem sizes, and a **rough accounting remainder** when the inputs allow one. The remainder is not measured writable free space: other partitions, reserved blocks, shared mounts or modified reports can change the result. Missing or impossible inputs produce “Unavailable”.
  - GB and GiB differ: a nominal 16 GB disk is approximately 14.9 GiB before partitioning. A report of 32 GB alongside a candidate of about 16 GB is worth investigating, not proof that a particular 7.2 GB is writable. No destructive fill test is performed.
- **OS & framework evidence:**
  - Compares `Build.VERSION.RELEASE` with `Build.VERSION.SDK_INT` and probes Android framework classes, including the Android 14 `GrammaticalInflectionManager` marker. For example, a reported Android 14 with API 29 is inconsistent.
  - A mismatch may indicate modified or customized firmware; these system-provided observations cannot authenticate the exact OS base or prove spoofing. Android 15/16 are not classified as fake merely because an Android 14 class exists.
- **SoC & GPU Profile:**
  - Extracts CPU implementer, part numbers, and architecture from `/proc/cpuinfo`.
  - Provides a chipset/GPU hint from reported board strings and device nodes. These are not independent chip identification or a queried GPU renderer.
- **Root & Security State:**
  - Probes standard `su` paths, tests root command execution (`su -c id`), and inspects SELinux enforcement status.

### 2. TV Bloatware Cleaner & Debloater
- **Categorized App Scanner:**
  - 🟢 **Safe to Remove:** Known pirate streaming players (HD Streamz), third-party stores (Uptodown), ad-heavy installers (XAPKS), vendor telemetry/backdoors (`com.adups.fota`, `com.allwinner.service`).
  - 🟡 **User Discretion:** Utility apps, alternative launchers (Bare Launcher, Simple TV Launcher), browsers, third-party file managers.
  - 🔴 **Core System Protected:** System UI, Leanback Settings, Google Play Services, input method (Keyboard). Protected from accidental deletion.
- **Multi-Engine Removal:**
  - **Root Engine:** For rooted boxes (like boxes with "Root permission" enabled in Advanced Settings), executes `pm uninstall -k --user 0 <package>` or `pm disable-user`. One-click removal directly with remote OK button.
  - **Wireless / USB ADB Engine:** For unrooted boxes, displays the TV box's local IP address and generates exact copy-paste terminal commands (`adb connect <ip>:5555`, `adb shell pm uninstall ...`).
  - **Standard Intent Engine:** Native Android uninstall dialog for user-installed APKs.

### 3. Clean TV Home Launcher Mode
- Optional Leanback default home launcher (`CATEGORY_LEANBACK_LAUNCHER` & `CATEGORY_HOME`).
- Direct **"Set Default Launcher"** action prompt for simple one-click home replacement.
- Optional high-contrast digital clock, date, and individually enabled status pills:
  - Available RAM in GiB, or a mismatch warning when readings conflict.
  - Reported filesystem available space, or a mismatch warning when readings conflict.
  - CPU usage calculated from successive `/proc/stat` counter samples (or `--` if unavailable), and core count.
  - Wi-Fi/internet ping latency when available.
- Optional one-click **RAM Boost** (requests stopping background processes and may drop kernel page caches if root is active; results vary).
- Optional **Take Screenshot:** Saves PNGs under Downloads/Screenshots, immediately or after three seconds. Root `screencap` may capture the TV screen; without root, PixelCopy/Canvas captures only the TV Guard window, subject to device restrictions.
- **Original App Icons:** Renders original, authentic high-resolution application icons for all installed apps in favorites row and app grid.
- D-pad friendly favorites and installed apps grid, with a separate focus-trapped options dialog.


### Remote Home and dialogs
- In **Tools & Settings → Default Launcher**, choose TV Guard as Android's default Home app. A system Home launch resets Tools/subviews and in-app dialogs, scrolls to the app grid and focuses its first available app. If no apps are available, the Tools tile is focused. Returning from other apps depends on Android/OEM Home routing and the chosen default launcher.
- D-pad focus alone no longer changes tabs. Press OK to select a tab. Hold OK on an app tile or use Menu to open options; Up/Down/Left/Right stay in the dialog. Back closes the dialog.
- **No voice or AI here.** The previous Voice helper and System assistant sections were removed with the AI feature. About & RP Apps still includes the expandable Tamil/English guide, project credits and sister-app links, but no assistant.

**Device checks:** On the target TV, set the default Home app; press Home from an external app, Tools/Debloater, an app-options dialog and while already on Home; verify the first app is visibly focused each time. Check all four directions in app options and Down through Debloater. No microphone or voice check needed — that feature was removed.

### 4. Local Wi-Fi File Hub (Zero-USB Transfer)
- Opt-in embedded HTTP server, trying port `8080` first (e.g. `http://192.168.10.104:8080`). It is off by default and has no password or encryption; see the security warning above.
- Allows transferring APKs and files directly from any phone, laptop, or tablet on the same Wi-Fi network without USB flash drives or cables.
- Web dashboard features:
  - **APK & File Upload:** Drag-and-drop uploads into `/sdcard/Download`; installation prompts additionally require browser remote controls.
  - **TV Screenshots Explorer:** Browse and download TV screenshots directly to phone or laptop.
  - **Remote TV Actions:** With browser remote and the relevant individual switches enabled, trigger screenshot capture or RAM Boost from your browser.
  - **Storage Permission:** One-click helper for granting Android All Files Access (`MANAGE_EXTERNAL_STORAGE`).

### 5. Internet & Wi-Fi Quality Monitor
- Active connection interface (Wi-Fi 5GHz / 2.4GHz or Ethernet).
- Signal strength (RSSI dBm), link negotiation speed (Mbps), local IP, and gateway route.
- Live TCP latency probe to Google DNS (`8.8.8.8`) and Cloudflare DNS (`1.1.1.1`).
- Quality badges: Excellent ($< 45\text{ ms}$), Good ($45\text{--}110\text{ ms}$), Fair ($110\text{--}220\text{ ms}$), Poor ($> 220\text{ ms}$).

---

## தமிழ் (Tamil)

**தொலைக்காட்சி காவலன் (Android TV Guard)** என்பது ஆண்ட்ராய்டு டிவி பெட்டிகளுக்கான திறந்த மூல, விளம்பரமற்ற பாதுகாப்பு மற்றும் தூய முகப்பு செயலியாகும்.

- **வன்பொருள் சான்றுகள்:** கர்னல், சேமிப்பகம் மற்றும் Android அறிக்கைகளில் சாத்தியமான முரண்பாடுகளைக் காட்டுகிறது. இவை சிப்பின் உண்மை அளவையோ Android அடிப்படைப் பதிப்பையோ தனியாக உறுதிப்படுத்தாது.
- **தேவையற்ற செயலிகள் நீக்கம் (Debloater):** முன்கூட்டியே நிறுவப்பட்ட ஆபத்தான விளம்பர செயலிகள், தேவையற்ற ஸ்ட்ரீமிங் செயலிகளை ரூட் (Root) அல்லது வயர்லெஸ் ஏடிபி (Wireless ADB) மூலம் எளிதாக நீக்க உதவுகிறது.
- **எளிய முகப்பு:** முதலில் முகப்பு மட்டும். கருவிகள் → நிபுணர் அமைப்புகளில் தேவையான RAM, சேமிப்பகம், CPU, இணையம், படம் மற்றும் பிற வசதிகளைத் தனித்தனியாக இயக்கலாம். **TV Guard இல் AI/குரல் இல்லை** — உரிமையாளர் கோரிக்கையில் நீக்கப்பட்டது. Live Guard இல் AI தொடர்கிறது. நிபுணர் முறையை நிறுத்தினால் தேர்வுகள் சேமிக்கப்பட்டு கூடுதல் வேலை நிற்கும்; மீண்டும் இயக்கினால் பழைய கோப்பு மையமும் தொடங்கலாம். குறிப்பிட்ட நினைவகச் சேமிப்பு உறுதி இல்லை.
- **திரைப்படம் எடுத்தல் (Screenshot):** டிவி திரையை நேரடியாக `/sdcard/Download` கோப்புறையில் சேமிக்கும் வசதி (உடனடி மற்றும் 3 நொடி தாமத முறை).
- **வைஃபை கோப்பு மையம்:** முதலில் நிறுத்தத்தில் இருக்கும். இயக்கினால் கடவுச்சொல்லோ குறியாக்கமோ இல்லை; உள்ளூர் வலையில் உள்ளவர்கள் Downloads மற்றும் பழைய படங்களைப் பார்க்க, பதிவேற்ற அல்லது நீக்க முடியும். நம்பகமான வலையில் மட்டும் பயன்படுத்தி முடிந்ததும் நிறுத்தவும்.
- **இணைய வேகத் தணிக்கை:** வைஃபை சிக்னல் வலிமை (RSSI), இணைப்பு வேகம் மற்றும் நேரடி தாமத (Ping) சோதனை.

## Verification

The focused Android integration tests use a **disposable TV emulator** and include a harmless launchable fixture in the test APK. They temporarily reset and restore optional-feature preferences; they do not change the system default launcher, favorites, hidden apps or remove apps. For a debug-signed emulator run (do not install it over a user's release installation), use:

```sh
./gradlew -PtvguardTestBuildType=debug :tvguard:connectedDebugAndroidTest
```

The manually dispatched TV Guard candidate workflow also runs these tests on an API 34 Android TV emulator. They exercise D-pad movement and an explicitly delivered Home intent; they do **not** establish that a physical TV remote or OEM system forwards Home/assistant keys. The normal build checks are `:tvguard:testDebugUnitTest :tvguard:lintDebug :tvguard:assembleDebug`.

The ordinary CI debug APK is **emulator-only**: its temporary signing key can change between runs, so it cannot reliably update an existing installation. A manually requested `signed_candidate` run instead requires the existing pinned signing identity and is designed to verify the exact non-debuggable candidate on an emulator, including a synthetic same-key update that preserves favorites, hidden apps, optional choices and a private file. The synthetic older fixture is never offered as a candidate. A successful run is an **unpublished test candidate**, not physical-TV or publication approval.

The signed candidate keeps package `com.universalrp.tvguard`, version name `1.0`, and version code `2`. Future updates must use the **same signing key** and a higher version code; an older APK signed with a different temporary debug key cannot be updated in place by this candidate. Uninstalling such a copy deletes its private app data, including favorites and settings.
