package com.universalrp.cleansweep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.TextButton
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.Screen
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.tr
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.AccentViolet
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary

@Composable
fun AboutScreen(state: com.universalrp.cleansweep.UiState, vm: MainViewModel) {
    val sourceContext = androidx.compose.ui.platform.LocalContext.current
    // The shared guide: same compact, expandable Tamil+English text in all six apps.
    var showGuide by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (showGuide) {
        com.universalrp.updates.GuideScreen(
            content = com.universalrp.updates.AppGuides.liveGuard,
            tamil = Lang.isTamil,
            onBack = { showGuide = false },
            accent = AccentCyan,
            surface = SurfaceHigh,
            contentColor = TextPrimary,
            mutedColor = TextSecondary,
        )
        return
    }
    val installedVersion = androidx.compose.runtime.remember {
        runCatching { sourceContext.packageManager.getPackageInfo(sourceContext.packageName, 0).versionName }.getOrNull() ?: "unknown"
    }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.goBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Home"), tint = TextSecondary)
            }
            Text(
                tr("About") + " " + tr(Lang.appName()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ------------------------------------------------------ who made this
            androidx.compose.material3.TextButton(onClick = {
                runCatching { sourceContext.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/universalrp2003/RP-Android-Apps"))) }
            }) { Text("Source code · GNU GPL v3") }
            LiveGuardUpdateCard()
            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        tr("Contributors"),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(listOf(AccentCyan, AccentViolet))
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "ர",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF04202A),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            // Always both scripts so the project contributor is recognisable
                            // whichever language the menu is in.
                            Text(
                                "ரமேஷ் பிரதாப்",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Ramesh prathap .R",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val context = LocalContext.current
                    TextButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_SENDTO,
                                        Uri.parse("mailto:universalrp2003@gmail.com"),
                                    )
                                )
                            } catch (e: Exception) {
                                // No mail app: the address is on screen to copy.
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Email,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "universalrp2003@gmail.com",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        tr("Project development contributor: Ramesh prathap .R"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        tr("AI software-assistance contributor: Arena.ai Agent Mode"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    TextButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/universalrp2003"),
                                    )
                                )
                            } catch (e: Exception) {
                                // No browser: the address is on screen to copy.
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Public,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "github.com/universalrp2003",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LiveGuardAboutPolicy.projectLinks(com.universalrp.updates.SisterApps.projectLinks()).forEach { (label, url) ->
                        TextButton(
                            onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                        ) {
                            Text(
                                "🔗 " + tr(label),
                                color = AccentCyan,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Free software under the GNU GPL v3") + "\n" +
                            "github.com/universalrp2003/RP-Android-Apps",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                    )
                    Text(
                        tr("App name") + ": " + Lang.appName(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    TextButton(onClick = { showGuide = true }) {
                        Text(
                            if (Lang.isTamil) "வழிகாட்டி — எப்படிப் பயன்படுத்துவது" else "Guide — how to use this app",
                            color = AccentCyan,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Other RP Ideas Android Apps"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentCyan)
                    Spacer(Modifier.height(2.dp))
                    Text(tr("App releases"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    // Other apps only, including DailyVault once in its correct section.
                    LiveGuardAboutPolicy.releaseLinks(com.universalrp.updates.SisterApps.releaseLinks()).forEach { (label, url) ->
                        TextButton(onClick = {
                            runCatching { sourceContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        }) { Text("🔗 " + tr(label), color = AccentCyan) }
                    }
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Live Guard v$installedVersion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "A privacy-friendly cleaner for every Android phone — Redmi and other Xiaomi " +
                            "phones (HyperOS / MIUI), Samsung One UI, Oppo, Vivo, Realme, OnePlus, " +
                            "Motorola, Nokia, Tecno and stock Android 8+. It finds residual temp files, " +
                            "thumbnail caches, duplicate files, leftover APK installers, empty folders, " +
                            "old downloads and large files — then removes only what you confirm.\n\n" +
                            "New in 2.0: live battery, temperature, voltage, charging current and watt " +
                            "readings; an installed-app list with usage records, package and permission details" +
                            "; a security review of permissions and system settings; a Wi-Fi scanner " +
                            "that shows how many devices share your network; and optional AI analysis " +
                            "with your own key (or a free keyless option). The Accessibility service " +
                            "stays gone, and nothing is uploaded unless you tap Analyse.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Security, contentDescription = null, tint = AccentViolet)
                        Spacer(Modifier.width(10.dp))
                        Text(tr("Privacy"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        watchWords(
                            "• Local scans and cleanup run on your phone. News, weather, HD speech, updates, speed tests and optional AI can use the internet; enabled background watches can also check automatically.\n\n" +
                                "• Weather providers receive coordinates for enabled places. AI providers receive the information you choose to send. HD speech sends announcement text to its voice provider; generated clips may be cached locally.\n\n" +
                                "• Android permissions control access. Live Guard has no Accessibility service and does not silently tap inside other apps. Files are removed only after selection and confirmation.",
                            "• உள்ளூர் ஆய்வும் சுத்தமும் உங்கள் கைபேசியில் நடக்கும். செய்திகள், வானிலை, HD குரல், புதுப்பிப்பு, வேகச் சோதனை மற்றும் விருப்ப AI இணையத்தைப் பயன்படுத்தலாம்; இயக்கப்பட்ட பின்னணி கண்காணிப்பும் தானாகச் சரிபார்க்கலாம்.\n\n" +
                                "• இயக்கப்பட்ட இடங்களின் ஆயத்தொலைவு வானிலை வழங்குநருக்கு அனுப்பப்படும். நீங்கள் தேர்ந்தெடுக்கும் தகவல்கள் AI வழங்குநருக்குச் செல்லும். HD குரலுக்கு அறிவிப்பு உரை குரல் வழங்குநருக்கு அனுப்பப்படும்; உருவாக்கப்பட்ட ஒலி கைபேசியில் சேமிக்கப்படலாம்.\n\n" +
                                "• அணுகலை Android அனுமதிகள் கட்டுப்படுத்தும். Live Guard-ல் Accessibility சேவை இல்லை; பிற செயலிகளில் தானாகத் தொடாது. கோப்புகளைத் தேர்ந்தெடுத்து உறுதிசெய்த பின்னரே நீக்கும்."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(watchWords("Android support", "Android ஆதரவு"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentViolet,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        watchWords(
                            "Designed for Android 8 and later. Features and background behavior can vary by phone, Android version, permissions and battery restrictions. Review selected files before confirming deletion.",
                            "Android 8 மற்றும் அதற்குப் பிந்தைய பதிப்புகளுக்காக வடிவமைக்கப்பட்டது. கைபேசி, Android பதிப்பு, அனுமதிகள், பேட்டரி கட்டுப்பாடுகளுக்கு ஏற்ப வசதிகளும் பின்னணி செயல்பாடும் மாறலாம். நீக்குவதை உறுதிசெய்யும் முன் தேர்ந்தெடுத்த கோப்புகளைச் சரிபார்க்கவும்."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Installing & updating (Play Protect)"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        watchWords(
                            "• Download updates from a source you trust. Play Protect may show a warning; read it and decide for yourself. A warning or its absence is not a safety guarantee.\n\n" +
                                "• Compatible updates normally retain app data. If Android rejects an update, check the package and signing identity. Do not uninstall or clear data as a first step: uninstalling can remove your saved choices and data.",
                            "• நம்பகமான மூலத்திலிருந்து புதுப்பிப்புகளைப் பதிவிறக்கவும். Play Protect எச்சரிக்கை காட்டினால் படித்து நீங்களே முடிவெடுக்கவும். எச்சரிக்கை இருப்பதோ இல்லாததோ பாதுகாப்புக்கான உத்தரவாதம் அல்ல.\n\n" +
                                "• இணக்கமான புதுப்பிப்புகள் பொதுவாகச் செயலித் தரவைப் பாதுகாக்கும். Android புதுப்பிப்பை மறுத்தால் தொகுப்பையும் கையொப்ப அடையாளத்தையும் சரிபார்க்கவும். முதலில் செயலியை நீக்கவோ தரவை அழிக்கவோ வேண்டாம்: நீக்கினால் சேமித்த தேர்வுகளும் தரவும் அழியலாம்."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Why some things can't be cleaned"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        watchWords(
                            "Android protects other apps' data. To clear another app's cache, Live Guard opens its storage page and you choose Clear cache. Android also restricts Android/data and Android/obb. Cleanup reports space from actual file operations; it does not promise to boost RAM or cool the phone.",
                            "Android பிற செயலிகளின் தரவைப் பாதுகாக்கிறது. மற்றொரு செயலியின் தற்காலிகச் சேமிப்பை அழிக்க Live Guard அதன் சேமிப்பு பக்கத்தைத் திறக்கும்; நீங்கள் Clear cache தேர்வு செய்ய வேண்டும். Android/data மற்றும் Android/obb-க்கும் Android கட்டுப்பாடுகள் உள்ளன. உண்மையான கோப்பு நடவடிக்கைகளின் இடமே சுத்த அறிக்கையில் காட்டப்படும்; RAM அதிகரிப்பையோ குளிர்விப்பையோ உறுதியளிக்காது."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }

            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(tr("Suggested routine"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "1. Tap “Scan & clean junk” and review what's selected.\n" +
                            "2. Check Large files and Old downloads — unselect anything you want to keep.\n" +
                            "3. Clean, then scan once more to sweep the folders left empty.\n" +
                            "4. Open App cache cleaner once a week for apps like Instagram, YouTube or WhatsApp.\n" +
                            "5. Stuck on something? Ask the on-device assistant — it answers from your " +
                            "real numbers, offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            // v2.6: a crash used to be invisible — "CleanSweep keeps stopping" with nothing
            // to act on. The app now keeps the last one on the phone and shows it here.
            if (state.lastCrash.isNotBlank()) {
                PanelCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            tr("Last crash"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.universalrp.cleansweep.ui.theme.WarnAmber,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            tr(
                                "The last time Live Guard stopped by itself. Nothing is sent " +
                                    "anywhere — this is here so you can read it out or screenshot it."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.lastCrash.take(1400),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = { vm.clearCrashReport() }) {
                            Text(
                                tr("Clear"),
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
