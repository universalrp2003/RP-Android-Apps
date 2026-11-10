# Android TV Guard (தொலைக்காட்சி காவலன்)

**Package:** `com.universalrp.tvguard`  
**Compatibility:** Android 8.0 / API 26 or later (optimized for Android TV, Google TV, and AOSP TV boxes).  
**Navigation:** 100% remote D-pad, air mouse, and USB mouse compatible with high-contrast glowing focus states.

---

## Overview

Budget Android TV boxes (Allwinner H313/H616, Rockchip RK3318/RK3528, Amlogic S905W/X) frequently ship with spoofed specifications—falsely advertising "Android 14, 4GB RAM, 64GB ROM" when the actual hardware is Android 9/10, 1GB–2GB RAM, and 16GB eMMC storage. In addition, many ship preloaded with ad-injecting streaming apps, third-party APK stores, and background telemetry services.

**Android TV Guard** is a free, open-source, zero-ad TV utility and clean launcher that:
1. **Unmasks fake specs** with low-level Linux kernel and hardware block device evidence.
2. **Debloats TV boxes** safely via one-click Root removal (on rooted boxes) or step-by-step Wireless ADB commands (on unrooted boxes).
3. **Restores speed** by providing a lightweight, ad-free TV home launcher with real-time memory, storage, and network health monitors.

---

## Core Capabilities

### 1. Hardware Truth Auditor (Fake Buster)
- **RAM Truth:**
  - Reads kernel-managed physical memory directly from `/proc/meminfo` (`MemTotal`, `MemFree`, `MemAvailable`, `Cached`, `Buffers`, `SwapTotal`).
  - Safe, non-destructive bounded memory allocation probe (allocates 32 MB chunks with memory page touches) to test real physical memory commit limits.
  - Automatically flags spoofed RAM (e.g. system claiming 4 GB when kernel `MemTotal` is $\le 2$ GB).
- **Storage Truth:**
  - Reads the hardware eMMC flash controller sector count from `/sys/block/mmcblk0/size`.
  - Calculates true physical flash capacity (e.g. $31,080,448 \text{ sectors} \times 512\text{ bytes} = 15.91\text{ GB}$, exposing 16 GB eMMC chips like SK hynix `H26M51002KPR`).
  - Reads `/proc/partitions` and partition filesystem stats (`/data`, `/system`, `/vendor`, `/cache`).
  - Flags spoofed internal storage (e.g. system claiming 32 GB or 64 GB on a 16 GB physical chip).
- **OS & Framework Truth:**
  - Compares `Build.VERSION.RELEASE` against `Build.VERSION.SDK_INT` and runtime class reflection (`android.app.GrammaticalInflectionManager`, `android.os.ext.SdkExtensions`, `android.media.AudioPlaybackCaptureConfiguration`).
  - Exposes the true underlying base OS (e.g. Android 10 Q / API 29 spoofed as Android 14).
  - Inspects kernel release timestamp and warns against impossible future build dates.
- **SoC & GPU Profile:**
  - Extracts CPU implementer, part numbers, and architecture from `/proc/cpuinfo`.
  - Queries GPU renderer (e.g. ARM Mali-G31) and identifies true board platform (e.g. Allwinner H313/H616).
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
- High-contrast digital clock, date, and live status pills:
  - Live RAM usage (`%.1f GB Free (xx% Used)`).
  - Live Flash Storage free space.
  - Live Wi-Fi signal and ping latency.
  - Live Root state badge.
- One-click **RAM Boost** (trims background app caches and drops kernel page caches if root is active).
- D-pad friendly favorites row and clean installed apps grid. Zero ads, zero background tracking, $< 30\text{ MB}$ memory footprint.

### 4. Internet & Wi-Fi Quality Monitor
- Active connection interface (Wi-Fi 5GHz / 2.4GHz or Ethernet).
- Signal strength (RSSI dBm), link negotiation speed (Mbps), local IP, and gateway route.
- Live TCP latency probe to Google DNS (`8.8.8.8`) and Cloudflare DNS (`1.1.1.1`).
- Quality badges: Excellent ($< 45\text{ ms}$), Good ($45\text{--}110\text{ ms}$), Fair ($110\text{--}220\text{ ms}$), Poor ($> 220\text{ ms}$).

---

## தமிழ் (Tamil)

**தொலைக்காட்சி காவலன் (Android TV Guard)** என்பது ஆண்ட்ராய்டு டிவி பெட்டிகளுக்கான திறந்த மூல, விளம்பரமற்ற பாதுகாப்பு மற்றும் தூய முகப்பு செயலியாகும்.

- **வன்பொருள் உண்மை தணிக்கை:** போலி ரேம் (RAM), போலி சேமிப்பகம் (Storage), போலி ஆண்ட்ராய்டு பதிப்பு ஆகியவற்றை லினக்ஸ் கர்னல் மற்றும் இஎம்எம்சி (eMMC) தொகுதி அளவுகள் மூலம் துல்லியமாகக் கண்டறிந்து உண்மை விவரங்களை வெளிப்படுத்துகிறது.
- **தேவையற்ற செயலிகள் நீக்கம் (Debloater):** முன்கூட்டியே நிறுவப்பட்ட ஆபத்தான விளம்பர செயலிகள், தேவையற்ற ஸ்ட்ரீமிங் செயலிகளை ரூட் (Root) அல்லது வயர்லெஸ் ஏடிபி (Wireless ADB) மூலம் எளிதாக நீக்க உதவுகிறது.
- **தூய டிவி முகப்பு (Clean Launcher):** குறைந்த நினைவக பயன்பாடு, நேரடி ரேம் மற்றும் சேமிப்பக தகவல், இணைய பிங் தரம் மற்றும் ரிமோட் மூலம் எளிதாக இயக்கக்கூடிய வண்ணக் குறியீடுகள் கொண்ட முகப்புத் திரை.
- **இணைய வேகத் தணிக்கை:** வைஃபை சிக்னல் வலிமை (RSSI), இணைப்பு வேகம் மற்றும் நேரடி தாமத (Ping) சோதனை.
