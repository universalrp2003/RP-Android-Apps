# System App Disabler (சிஸ்டம் செயலி முடக்கி)

**Disable pre-installed bloat safely — no uninstall, no root.**
Package `com.universalrp.appdisabler` · `0.2.0 / code 2` · min 26

Combination of **Shizuku + Canta + Universal Android Debloater** as one RP app:
- **Shizuku** — shell privilege via Wireless debugging (Android 11+)
- **Canta** UX — simple list, search, filter Disabled, batch
- **UAD** DB — per-OEM Safe / Caution / Never-disable labels

**Disable-only:** `setApplicationEnabledSetting(DISABLED_USER)` reversible; 1-tap Enable. No `pm uninstall`, no root, no fully removed APKs. Safer for OTA and banking apps.

## Shizuku setup (2 min, inside app)
App shows Shizuku status card:
- **Get Shizuku** → https://shizuku.rikka.app/download/ (Play + GitHub)
- **How to activate** → https://shizuku.rikka.app/guide/setup/
Steps: Developer options → Wireless debugging → Pair Shizuku → Allow. One pairing per reboot on non-root.

About & RP Apps links come from `shared/updates/SisterApps` (built against a module-local copy,
`appdisabler/app/src/main/java/com/universalrp/updates/SisterApps.kt`, because a shared `srcDir`
would collide).

## Header counts (0.2.0)

The top of the Apps window shows four counts with symbols, each drawn from the same list the cards
use: 📦 installed, ⚙ pre-installed (system), 👤 you added (user apps), ⛔ turned off, ▶ running.
Tapping a tile filters the list to exactly that set. "Running" is read from the process table over
the shell the app already has, so it needs Shizuku; without it the tile shows `—`, never `0`.

## Safety guard (0.2.0)

`DisableManager.blockingReason` refuses to turn off:

* the **keyboard currently in use** (`Settings.Secure.DEFAULT_INPUT_METHOD`),
* a package with an **enabled accessibility service** (`ENABLED_ACCESSIBILITY_SERVICES`),
* anything the UAD database rates `CRITICAL` (as before).

The refusal is checked before any `pm` command runs, and the row explains why instead of failing.
This exists because 0.1.0 let the owner disable a vendor keyboard and parts of Settings with no
warning: losing the keyboard also means losing the ability to pair Shizuku again.

## Testing a candidate build

The current test script is `docs/appdisabler-0.3.0-test-script.md`. It covers the two reports that
matter most: a turned-off app that keeps running after force-close, and a row that used to trust a
local note instead of asking the phone. Install the release candidate over the installed app — it is
signed with the same key — and run the shell check in the Setup tab, which prints the authoritative
`enabled` / `stopped` / `suspended` flags for the package in question.
