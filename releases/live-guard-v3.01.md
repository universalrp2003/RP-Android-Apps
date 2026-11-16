# Live Guard 3.01 — radio monitor crash fix

Fixes the Android 15/16 foreground-service timeout identified in the owner's Live Guard 3.00 crash report. The user-enabled continuous radio/signal monitor is local monitoring, not a data transfer; it now uses the appropriate foreground service type and stops safely if Android calls its timeout callback. Existing radio monitoring and the shared notification remain available.

Update over 3.00 without uninstalling or clearing data. The package and signing key are unchanged; version code is 106. Android 16 emulator testing verified an in-place update with synthetic data and a shortened dataSync timeout; Android 15 was also tested. The owner approved publishing without another phone test. This specific 3.01 APK has not been verified on the owner's Xiaomi; other crashes may have different causes.

## தமிழ்

Live Guard 3.00-ல் வந்த Android 15/16 radio monitor timeout காரணமான திடீர் நிறுத்தம் சரி செய்யப்பட்டது. இது இணையத் தரவு பரிமாற்றம் அல்ல; பயனர் இயக்கிய signal / 4G-5G கண்காணிப்பு. Timeout வந்தாலும் செயலியைப் பாதுகாப்பாக நிறுத்தும் ஏற்பாடு உள்ளது.

3.00 மீது நேரடியாகப் புதுப்பிக்கவும்; uninstall அல்லது தரவு அழிக்க வேண்டாம். பதிப்பு குறியீடு 106; அதே package மற்றும் signing key. Android emulator சோதனைகள் செய்யப்பட்டன; இந்தப் பதிப்பு உரிமையாளரின் கைபேசியில் இன்னும் சோதிக்கப்படவில்லை.
