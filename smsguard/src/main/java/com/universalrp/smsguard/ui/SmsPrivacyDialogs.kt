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
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tamil) "இணைய உயர்தரக் குரல்: உங்கள் தேர்வு" else "Online HD voice: your choice") },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
            Text(
                if (tamil) "உயர்தரக் குரலுக்கு வாசிக்கப்படும் உரை Microsoft speech.platform.bing.com சேவைக்கு அனுப்பப்படும். இணைய சிஸ்டம் குரல் பயன்படுத்தினால் அதன் சேவை வழங்குநருக்கும் உரை செல்லலாம். அனுப்புநர் பெயர், சாதாரண செய்தி உரை மற்றும் பரிவர்த்தனைத் தொகை இதில் இருக்கலாம். இணையக் குரலைத் தேர்ந்தெடுத்தால் பின்னணி SMS அறிவிப்புகளுக்கும் இது பொருந்தும். ரகசியக் குறியீடுகளைத் தடுக்கவும் கணக்கு எண்களை மறைக்கவும் முயல்கிறோம்; எல்லா ரகசியங்களையும் கண்டறிவதற்கு உத்தரவாதம் இல்லை. சமீபத்திய ஒலிகள் போனில் தற்காலிகமாகச் சேமிக்கப்படும்.\n\nமறுத்தால் போனில் உள்ள இணையமில்லா குரல் மட்டும் பயன்படுத்தப்படும். அது இல்லையெனில் குரல் ஒலிக்காது; செய்திகளைத் திரையில் பார்க்கலாம். அனுமதியை Controls பகுதியில் எப்போது வேண்டுமானாலும் திரும்பப் பெறலாம். ஏற்கெனவே அனுப்பிய உரையைத் திரும்பப் பெற முடியாது."
                else "HD voice sends the text being spoken to Microsoft speech.platform.bing.com. A network-backed system voice may instead send text to its own TTS provider. This may include sender names, ordinary message text and transaction amounts, including background SMS announcements. We try to suppress secret-bearing text and mask account numbers; detection is not a guarantee. Recent audio is cached on this phone.\n\nIf you decline, only an installed non-network system voice is used. If none is available there is no speech; messages remain visible. You can withdraw consent in Controls. Withdrawing cannot recall text already sent.",
            )
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://privacy.microsoft.com/privacystatement")))
            }) { Text(if (tamil) "Microsoft தனியுரிமைக் கொள்கை" else "Microsoft privacy statement") }
            }
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
            if (tamil) "சேமித்த மற்றும் புதிய SMS-களைப் படித்து போனிலேயே வரவு-செலவு, பில் மற்றும் இணைப்பு ஆய்வு செய்யும்; அனுமதித்த குரல் அமைப்புக்கு ஏற்ப அறிவிக்கும். இணைய இணைப்புச் சோதனை URL மற்றும் கோரிக்கைத் தகவலை வெளிச் சேவைகளுக்கு அனுப்பும். இணைய HD குரலுக்கு தனி ஒப்புதல் தேவை. Contacts, SIM மற்றும் அனுப்பும் அனுமதி இப்போது கேட்கப்படாது.\n\nஇந்த முக்கியமான SMS அனுமதிகளால் Google Play Protect நிறுவலைத் தடுக்கலாம். பாதுகாப்பை அணைக்கவோ எச்சரிக்கையை மீறவோ வேண்டாம். MMS இல்லை; MMS தேவைப்பட்டால் முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள்."
            else "Read stored and incoming SMS for on-device finance, bills and link analysis, with announcements under your voice settings. Online link checks send URLs and request metadata to external services. Cloud HD speech requires separate consent. Contacts, SIM and sending permissions are not requested here.\n\nThese sensitive SMS permissions can cause a Google Play Protect installation block. Do not disable protection or override a warning. MMS is not implemented: keep a full messaging app as default if you need it.",
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
        ) },
        confirmButton = { TextButton(onClick = onAccept) { Text(if (tamil) "SMS படிக்கும் அனுமதியைப் பார்" else "Review SMS read permissions") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (tamil) "ரத்து" else "Cancel") } },
    )
}

@Composable
fun SmsMessagingLimitNotice(tamil: Boolean, palette: GuardPalette) {
    val context = LocalContext.current
    SectionCard(palette = palette) {
        Text(if (tamil) "SMS-only பயன்முறை மற்றும் தனியுரிமை" else "SMS-only mode and privacy", color = palette.accent)
        Text(
            if (tamil) "MMS பெறுதல்/அனுப்புதல் இல்லை. MMS தேவைப்பட்டால் முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள். குரல் தரவுப் பகிர்வுக்கு தனி ஒப்புதல் தேவை. மூலநிரலை ஆய்வு செய்யலாம்; அது பாதுகாப்புச் சான்றிதழ் அல்ல."
            else "MMS receiving/sending is not implemented. Keep a full messaging app as default if you need MMS. Cloud voice needs separate consent. Source is open for audit, not a safety certificate.",
            color = palette.textMuted, style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/universalrp2003/RP-Android-Apps/blob/main/docs/sms-guard-play-protect.md")))
        }) { Text(if (tamil) "விவரங்கள் மற்றும் மூலநிரல்" else "Details and source audit") }
    }
}


@Composable
fun SmsOnlyDefaultDisclosure(tamil: Boolean, onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tamil) "இயல்புநிலை SMS — MMS வரம்பு" else "Default SMS — MMS limitation") },
        text = { Text(
            if (tamil) "SMS Guard உரை SMS-ஐ அனுப்பவும் பெறவும் முடியும். படங்கள்/வீடியோக்கள் மற்றும் MMS வழிக் குழுச் செய்திகளை அனுப்பவோ பெறவோ முடியாது. இயல்புநிலையாக மாற்றினால் Android அந்த MMS-களை இந்தச் செயலிக்கு மட்டும் அனுப்பலாம்; அவற்றை நீங்கள் இழக்கலாம். அறிவிப்பு வந்தால் எச்சரிப்போம்; உள்ளடக்கத்தைப் பெறவோ மீட்கவோ முடியாது. பின்னர் செயலியை மாற்றினாலும் பழைய MMS திரும்பக் கிடைப்பதற்கு உத்தரவாதம் இல்லை; அனுப்புநர் மீண்டும் அனுப்ப வேண்டியிருக்கலாம்.\n\nMMS தேவைப்பட்டால் ரத்து செய்து உங்கள் முழுமையான செய்திச் செயலியை இயல்புநிலையாக வைத்திருங்கள். SMS மட்டும் போதுமெனில் தொடருங்கள்; அடுத்து Android-ன் உண்மையான இயல்புநிலைத் தேர்வு திறக்கும். Controls-ல் மீண்டும் மாற்றலாம். இந்தத் தேர்வு SMS பதிவுகளை அழிக்காது."
            else "SMS Guard can send and receive text SMS, but cannot send/receive picture, video or MMS-based group messages. As default, Android may route those MMS only here and you may miss them. We can flag an incoming notice, but cannot download or recover the content. Switching apps afterward may not recover an earlier MMS; the sender may need to resend it.\n\nIf you need MMS, cancel and keep your full messaging app as default. If text SMS alone is enough, continue to Android's actual default-app chooser. You can switch back in Controls. This choice does not erase SMS records.",
            modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
        ) },
        confirmButton = { TextButton(onClick = onAccept) {
            Text(if (tamil) "SMS மட்டும் ஏற்கிறேன் — தொடர்க" else "Accept SMS-only — continue")
        } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (tamil) "ரத்து" else "Cancel") } },
    )
}
