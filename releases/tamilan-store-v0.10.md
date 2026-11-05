# Tamilan Store 0.10 — background download recovery candidate

User-requested APK transport now belongs to Android DownloadManager rather than the screen. Turning the screen off does not deliberately cancel it. Android may pause for network, power, storage or force-stop restrictions. Reopening Store recovers the selected immutable release and re-verifies completed bytes. Batch installation approvals are not restored after process death: the current file is recoverable, but remaining apps require a new review.

Size, SHA-256, package/version, SDK, signer and installed-version checks remain mandatory before installation. Unverified transport files are never handed to an installer. Only explicit user cancellation removes the system request. No new permissions, wake locks, foreground service or battery-optimization exemption.

Same-key users update normally; do not uninstall or clear local data. This candidate is not installed on a phone; emulator checks do not establish OEM/Doze reliability or Play Protect clearance. No public release is requested yet.

## தமிழ்

திரை அணைந்தாலும் நீங்கள் தொடங்கிய பதிவிறக்கத்தை அங்காடி ரத்து செய்யாது. Android இணையம், மின்சாரம் அல்லது சேமிப்பு காரணமாக இடைநிறுத்தலாம். மீண்டும் திறந்தால் பதிவிறக்க நிலையும் கோப்பும் சரிபார்க்கப்படும். நிறுவலுக்கு உங்கள் ஒப்புதல் தேவை. புதிய அனுமதிகள் இல்லை. நீக்காமல் புதுப்பிக்கவும்; நீக்கினால் உள்ளூர் தரவு அழியலாம். கைப்பேசிச் சோதனை அல்லது Google ஒப்புதல் அல்ல. இன்னும் பொதுவில் வெளியிடப்படவில்லை.
