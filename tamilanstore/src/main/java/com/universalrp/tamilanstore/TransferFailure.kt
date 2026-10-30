package com.universalrp.tamilanstore

/** Local failure stages only; never expose raw network/provider exceptions or claim installation. */
enum class TransferFailure {
    DOWNLOAD, SIZE, HASH, APK, IDENTITY, SDK, SIGNER, INSTALLED_SIGNER, DOWNGRADE, STORAGE;

    fun message(tamil: Boolean): String = when (this) {
        DOWNLOAD -> if (tamil) "பதிவிறக்கம் முடியவில்லை. இணையம் மற்றும் சேமிப்பிடத்தைச் சரிபார்த்து மீண்டும் முயலவும். நிறுவப்படவில்லை." else "Download interrupted. Check your connection and storage, then retry. Nothing installed."
        SIZE -> if (tamil) "கோப்பின் அளவு பொருந்தவில்லை. நிறுவப்படவில்லை; மீண்டும் பதிவிறக்கவும்." else "APK size does not match the release. Nothing installed; download again."
        HASH -> if (tamil) "கோப்பின் சரிபார்ப்பு மதிப்பு பொருந்தவில்லை. நிறுவப்படவில்லை; மீண்டும் பதிவிறக்கவும்." else "APK checksum does not match the release. Nothing installed; download again."
        APK -> if (tamil) "APK விவரங்களைப் படிக்க முடியவில்லை. நிறுவப்படவில்லை; வெளியீட்டு விவரத்தைப் பார்க்கவும்." else "Cannot read APK details. Nothing installed; check the release details."
        IDENTITY -> if (tamil) "செயலியின் பெயர் அல்லது பதிப்பு பொருந்தவில்லை. நிறுவல் தடுக்கப்பட்டது." else "APK package or version does not match the catalog. Installation blocked."
        SDK -> if (tamil) "இந்த APK-க்கு புதிய Android பதிப்பு தேவை. நிறுவல் தடுக்கப்பட்டது." else "This APK requires a newer Android version. Installation blocked."
        SIGNER -> if (tamil) "பதிவிறக்கிய APK-யின் கையொப்பம் நம்பகமான வெளியீட்டுடன் பொருந்தவில்லை. நிறுவல் தடுக்கப்பட்டது; வெளியீட்டு விவரத்தைப் பார்க்கவும்." else "Downloaded APK signer does not match the trusted release certificate. Installation blocked; check the release details."
        INSTALLED_SIGNER -> if (tamil) "நிறுவிய செயலியும் புதிய APK-யும் வெவ்வேறு கையொப்பங்கள் கொண்டவை. நேரடியாகப் புதுப்பிக்க முடியாது. செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம்." else "Installed app and new APK have different signing identities. A direct update is blocked. Do not uninstall or clear app data."
        DOWNGRADE -> if (tamil) "நிறுவிய பதிப்பை இந்தக் கோப்பால் பாதுகாப்பாகப் புதுப்பிக்க முடியாது. நிறுவல் தடுக்கப்பட்டது." else "This file cannot safely update the installed version. Installation blocked."
        STORAGE -> if (tamil) "சரிபார்த்த கோப்பைச் சேமிக்க முடியவில்லை. சேமிப்பிடத்தைச் சரிபார்த்து மீண்டும் முயலவும். நிறுவப்படவில்லை." else "Cannot save the verified APK. Check free storage and retry. Nothing installed."
    }
}

internal class TransferVerificationException(val reason: TransferFailure) : IllegalStateException(reason.name)
internal fun verifyGate(condition: Boolean, failure: TransferFailure) {
    if (!condition) throw TransferVerificationException(failure)
}
