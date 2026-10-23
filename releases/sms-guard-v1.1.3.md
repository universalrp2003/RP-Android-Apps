## SMS Guard (குறுஞ்செய்தி காவலன்) v1.1.3 — Instant Screen-ON Voice Stop, Global Stop Button, Newest-First Threads & Full Govt/Ration/Exam Voice Readout

**Package:** `com.universalrp.smsguard` · **Version:** `1.1.3` (`versionCode = 9`) · **Minimum Android:** Android 8.0+ (API 26)
**Verification Note:** Built, linted, unit-tested, and verified via CI static/manifest/signature checks; this exact candidate build was not installed on a physical phone prior to GitHub release publication (`0` new permissions added over `v1.1.2`).

### What's New in v1.1.3
- **Instant Auto-Stop on Screen ON (`Intent.ACTION_SCREEN_ON`):**
  - Turning ON the phone screen while any SMS voice announcement is playing now **immediately stops** voice playback (`MediaPlayer` + Android `TextToSpeech` + audio focus) so reading on screen is completely effortless.
- **Compact `⏹ Stop` (`⏹ நிறுத்து`) Button Across Every Window:**
  - Whenever voice is actively playing, a compact red **`⏹ Stop` / `⏹ நிறுத்து`** pill appears in the persistent top bar across **every window** (`Messages`, opened conversation threads, `Shield`, `Finance`, `Controls`, `Guide`, `About`) as well as inside open thread headers.
- **Newest / Latest Messages Always at the Top (`DESC`):**
  - Opened multi-message conversation threads (e.g., `ICICI Bank`, `SBI`, `TNEPDS`, `TNSEDU`, personal chats), Inbox threads, Finance transactions, and Shield link histories now display **latest messages strictly at the top (`index 0`)**.
- **Full Announcement by Default for Government, Ration Shop (`TNEPDS`), School Exam Results (`TNSEDU`) & Non-Bank Senders:**
  - Enabled full non-OTP normal/government SMS voice announcements **ON by default** (including automatic one-time migration from `v1.1.2`).
  - Expanded Tamil & English TRAI DLT department recognition (`TNEPDS` Ration Shop, `TNSEDU` / `TNDGE` School Education & Exam Results, `TNEB/TANGEDCO`, `TWAD/CMWSSB` Water, `Indane/Bharatgas/HP Gas`, `TNSTC/SETC`, `TNeGA/e-Sevai`, `TNPSC`, `UIDAI`, `India Post`, `Parivahan`, etc.) plus smart body-content detection and natural speech expansion for Ration Shop items/bills (`P.Oil`, `pkt`, `kg`, `Rs.25/-`) and full HSC/SSLC exam mark-sheets (all subjects + total + Pass, up to 90s / 950 chars without mid-message cutoff).
  - Preserves **100% OTP privacy silence**, `CallGuard`, Quiet Hours queue, NextDNS compatibility, and `₹0` outgoing NEFT beneficiary settlement handling.
