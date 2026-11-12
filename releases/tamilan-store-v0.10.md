# Tamilan Store 0.10 — background download recovery

User-requested APK transport now belongs to Android DownloadManager rather than the screen. Turning the screen off does not deliberately cancel it. Android may pause for network, power, storage or force-stop restrictions. Reopening Store recovers the selected immutable release and re-verifies completed bytes. Batch installation approvals are not restored after process death: the current file is recoverable, but remaining apps require a new review.

Size, SHA-256, package/version, SDK, signer and installed-version checks remain mandatory before installation. Unverified transport files are never handed to an installer. Only explicit user cancellation removes the system request. No new permissions, wake locks, foreground service or battery-optimization exemption.

Same-key users update normally; do not uninstall or clear local data. The owner reports this candidate now downloads in the background on their phone and authorized publication. Installed-phone hash was not independently matched; emulator checks do not establish all-OEM/Doze reliability or Play Protect clearance.

No independent phone-hash match: exact bytes were not installed on a phone by CI. Owner acceptance is distinct evidence.

## தமிழ்

திரை அணைந்தாலும் நீங்கள் தொடங்கிய பதிவிறக்கத்தை அங்காடி ரத்து செய்யாது. Android இணையம், மின்சாரம் அல்லது சேமிப்பு காரணமாக இடைநிறுத்தலாம். மீண்டும் திறந்தால் பதிவிறக்க நிலையும் கோப்பும் சரிபார்க்கப்படும். நிறுவலுக்கு உங்கள் ஒப்புதல் தேவை. புதிய அனுமதிகள் இல்லை. நீக்காமல் புதுப்பிக்கவும்; நீக்கினால் உள்ளூர் தரவு அழியலாம். கைப்பேசிச் சோதனை அல்லது Google ஒப்புதல் அல்ல. உரிமையாளர் பின்னணிப் பதிவிறக்கம் இயங்குவதாகத் தெரிவித்து வெளியிட அனுமதித்துள்ளார்.
