package com.universalrp.smsguard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun CloudSpeechDisclosure(tamil: Boolean, onChoice: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tamil) "இணைய உயர்தரக் குரல்: உங்கள் தேர்வு" else "Online HD voice: your choice") },
        text = {
            Text(
                if (tamil) "உயர்தரக் குரலுக்கு வாசிக்கப்படும் உரை Microsoft speech.platform.bing.com சேவைக்கு அனுப்பப்படும். அனுப்புநர் பெயர், சாதாரண செய்தி உரை மற்றும் பரிவர்த்தனைத் தொகை இதில் இருக்கலாம். இணையக் குரலைத் தேர்ந்தெடுத்தால் பின்னணி SMS அறிவிப்புகளுக்கும் இது பொருந்தும். ரகசியக் குறியீடுகளைத் தடுக்கவும் கணக்கு எண்களை மறைக்கவும் முயல்கிறோம்; எல்லா ரகசியங்களையும் கண்டறிவதற்கு உத்தரவாதம் இல்லை. சமீபத்திய ஒலிகள் போனில் தற்காலிகமாகச் சேமிக்கப்படும்.\n\nமறுத்தால் போனில் உள்ள இணையமில்லா குரல் மட்டும் பயன்படுத்தப்படும். அது இல்லையெனில் குரல் ஒலிக்காது; செய்திகளைத் திரையில் பார்க்கலாம். அனுமதியை Controls பகுதியில் எப்போது வேண்டுமானாலும் திரும்பப் பெறலாம். ஏற்கெனவே அனுப்பிய உரையைத் திரும்பப் பெற முடியாது."
                else "HD voice sends the text being spoken to Microsoft speech.platform.bing.com. This may include sender names, ordinary message text and transaction amounts, including background SMS announcements. We try to suppress secret-bearing text and mask account numbers; detection is not a guarantee. Recent audio is cached on this phone.\n\nIf you decline, only an installed non-network system voice is used. If none is available there is no speech; messages remain visible. You can withdraw consent in Controls. Withdrawing cannot recall text already sent.",
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { TextButton(onClick = { onChoice(true) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (tamil) "உரையை அனுப்பி HD குரலை அனுமதி" else "Allow text sharing for HD voice")
        } },
        dismissButton = { TextButton(onClick = { onChoice(false) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(if (tamil) "வேண்டாம் — உள்ளூர் குரல் மட்டும்" else "Decline — on-device voice only")
        } },
    )
}

@Composable
fun SmsReadDisclosure(tamil: Boolean, onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tamil) "SMS அணுகல் எதற்காக?" else "Why SMS access?") },
        text = { Text(
            if (tamil) "சேமித்த மற்றும் புதிய SMS-களைப் படித்து போனிலேயே வரவு-செலவு, பில் மற்றும் இணைப்பு ஆய்வு செய்யும்; அனுமதித்த குரல் அமைப்புக்கு ஏற்ப அறிவிக்கும். இணைய இணைப்புச் சோதனை URL மற்றும் கோரிக்கைத் தகவலை வெளிச் சேவைகளுக்கு அனுப்பும். இணைய HD குரலுக்கு தனி ஒப்புதல் தேவை. Contacts, SIM மற்றும் அனுப்பும் அனுமதி இப்போது கேட்கப்படாது.\n\nஇந்த முக்கியமான SMS அனுமதிகளால் Google Play Protect நிறுவலைத் தடுக்கலாம். பாதுகாப்பை அணைக்கவோ எச்சரிக்கையை மீறவோ வேண்டாம். MMS பெறுதல் முழுமையாக இல்லை; உங்கள் முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள்."
            else "Read stored and incoming SMS for on-device finance, bills and link analysis, with announcements under your voice settings. Online link checks send URLs and request metadata to external services. Cloud HD speech requires separate consent. Contacts, SIM and sending permissions are not requested here.\n\nThese sensitive SMS permissions can cause a Google Play Protect installation block. Do not disable protection or override a warning. Incoming MMS is incomplete: keep your full messaging app as default.",
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
        ) },
        confirmButton = { TextButton(onClick = onAccept) { Text(if (tamil) "SMS படிக்கும் அனுமதியைப் பார்" else "Review SMS read permissions") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (tamil) "ரத்து" else "Cancel") } },
    )
}

@Composable
fun SmsSecurityNotice(tamil: Boolean, palette: GuardPalette) {
    val context = LocalContext.current
    SectionCard(palette = palette) {
        Text(if (tamil) "Play Protect நிலை: தீர்க்கப்படவில்லை" else "Play Protect status: unresolved", color = palette.accent)
        Text(
            if (tamil) "SMS அணுகல் காரணமாக நிறுவல் தடுக்கப்படலாம். Google ஒப்புதல் கிடைத்ததாக உறுதி இல்லை. பாதுகாப்பை அணைக்க வேண்டாம். மூலநிரலை யாரும் ஆய்வு செய்யலாம்; திறந்த மூலநிரல் பாதுகாப்பு உத்தரவாதம் அல்ல. MMS பெறுதல் முழுமையாக இல்லை."
            else "Installation may be blocked because of sensitive SMS access. No Google clearance is claimed. Do not disable protection. Source is open for audit, not a safety guarantee. Incoming MMS is incomplete.",
            color = palette.textMuted, style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/universalrp2003/RP-Android-Apps/blob/main/docs/sms-guard-play-protect.md")))
        }) { Text(if (tamil) "விவரங்கள் மற்றும் மூலநிரல்" else "Details and source audit") }
    }
}
