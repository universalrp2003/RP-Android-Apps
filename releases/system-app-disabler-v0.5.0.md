## System App Disabler 0.5.0 — Android TV candidate

- Adds a TV launcher entry, remote focus indicators and a TV-specific Shizuku setup guide, while retaining the phone launcher.
- The launcher guard refuses to disable a HOME app unless another enabled, exported and visibly unsuspended HOME app can be verified. When that cannot be checked, it fails closed. This cannot guarantee the alternative will work or that recovery is possible.
- A device-owner warning is shown when Android exposes one to the current user; an unavailable management check is reported. Other users, profiles and provider management can be hidden, so no warning does not prove a device is unmanaged.
- The owner reported that the earlier candidate worked with the remote on their TV setup. This wording-corrected build was tested with a same-key update from 0.4.1 and remote navigation on a disposable TV emulator; **it has not been retested on the physical TV**. The earlier installed hash was not independently read back. The tests do not disable real packages or prove Shizuku works on a particular TV.
- Disabling essential packages can make the device unusable. Review one change at a time, and do not bypass Android security prompts or uninstall to force an update.

## தமிழ்

தொலைக்காட்சியில் திறக்கும் வழி, ரிமோட் தேர்வுக் குறி மற்றும் தொலைக்காட்சிக்கான அமைப்பு வழிகாட்டி சேர்க்கப்பட்டுள்ளன; கைபேசியில் திறக்கும் வழியும் தொடர்கிறது.

வேறு இயக்கக்கூடிய முகப்புச் செயலியை உறுதிசெய்ய முடியாவிட்டால் முகப்புச் செயலியை முடக்க அனுமதிக்காது. மாற்றுச் செயலி வேலை செய்யும் என்றோ மீட்பு சாத்தியம் என்றோ உறுதி இல்லை. சாதன நிர்வாகத் தகவல் மறைக்கப்பட்டிருக்கலாம்; எச்சரிக்கை இல்லை என்பதால் நிர்வாகம் இல்லை என்று கருத வேண்டாம்.

சோதனைத் தொலைக்காட்சிச் சூழலில் புதுப்பிப்பும் ரிமோட் வழிசெலுத்தலும் சரிபார்க்கப்படுகின்றன. உண்மையான தொலைக்காட்சியில் இந்தத் துல்லியமான உருவாக்கம் சோதிக்கப்பட்டதாகக் கூறவில்லை. முக்கிய ஆப்களை முடக்குவது சாதனத்தைப் பயன்படுத்த முடியாத நிலைக்கு இட்டுச் செல்லலாம்.
