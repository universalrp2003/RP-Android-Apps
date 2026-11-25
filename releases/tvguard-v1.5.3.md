## Android TV Guard 1.5.3 — optional Shizuku diagnostics

- Tools & Settings → Shizuku diagnostics opens a separate read-only screen with an on/off control. When enabled, it connects to an already running Shizuku service and asks for TV Guard's own Shizuku permission if needed. Installing Shizuku alone cannot start its service: follow Shizuku's root or ADB/wireless-debugging instructions, and restart the service after reboot when required. Turning diagnostics off does not stop Shizuku itself.
- The screen shows visible process rows, process RSS, device RAM and data-filesystem storage. The optional pill adds the number of visible installed packages matched to visible process names while connected; this is an approximation, not a complete count of active apps. RSS includes shared pages, and filesystem size is not proof of physical capacity. Unavailable values remain unavailable.
- The CPU pill shows sampled non-idle processor **load**, not free capacity. The recently-used fallback no longer invents per-app RAM estimates.
- The owner reported the candidate working on their TV and shared screenshots of live process details and the pill. The installed TV's APK hash was not independently read back. The exact published APK is the candidate tested on a disposable emulator; real Shizuku 13.6.0 was tested separately on a debug build of the same source. Devices and Shizuku versions may differ; the process API used here is deprecated and may become unavailable in a future release.
- **Security audit limitation remains:** partial reads can omit installers, make an unavailable HOME lookup look like no default, or treat a missing USB ADB setting as off. A clear result is not proof of security; check Android settings directly.
- Update in place only if Android accepts the existing signature. Do not uninstall, clear app data, or bypass security warnings to force an update.

## தமிழ்

Tools & Settings → Shizuku diagnostics பகுதியில் விருப்பமான, படிக்க மட்டும் பயன்படும் விவரங்களையும் இயக்க / நிறுத்த கட்டுப்பாட்டையும் காணலாம். Shizuku ஏற்கனவே இயங்கினால் TV Guard அதனுடன் இணைந்து தனக்கான அனுமதியைக் கேட்கும். Shizuku சேவையைத் தொடங்க அதன் சொந்த root அல்லது ADB வழிமுறைகளைப் பின்பற்றவும்.

காணக்கூடிய செயல்முறைகள், நினைவகம், சேமிப்பிடம் மற்றும் பொருந்திய தொகுப்புகளின் தோராயமான எண்ணிக்கை காட்டப்படும்; இது இயங்கும் அனைத்து ஆப்புகளின் துல்லியமான எண்ணிக்கை அல்ல. CPU இப்போது பயன்பாட்டு சுமையைக் காட்டுகிறது. பாதுகாப்புச் சோதனை முழுமையான பாதுகாப்புக்கான உறுதியல்ல; அமைப்புகளை நேரடியாகச் சரிபார்க்கவும். ஆப்பை நீக்காமலும் தரவை அழிக்காமலும் புதுப்பிக்கவும்.
