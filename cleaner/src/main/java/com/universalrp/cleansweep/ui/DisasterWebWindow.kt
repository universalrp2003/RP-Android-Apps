package com.universalrp.cleansweep.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/** Actual provider pages. No injected map, credentials, location or JavaScript/native bridge. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun DisasterWebWindow(url: String, placeLabel: String = "", onClose: () -> Unit) {
    if (!disasterWebAllowed(url)) return
    val browser = LocalUriHandler.current
    val context = LocalContext.current
    val zoomPrefs = remember { context.getSharedPreferences("disaster_web_page_zoom", Context.MODE_PRIVATE) }
    var selected by rememberSaveable(url) { mutableStateOf(DisasterWebPolicy.provider(url)) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var zoomMenu by remember { mutableStateOf(false) }
    // Retain a bounded, lazily-created WebView per tab. Detaching/re-attaching, not reloading,
    // preserves provider-owned map zoom/layers/position during tab switches and Expand.
    val pages = remember(url) { mutableMapOf<DisasterWebPolicy.Provider, WebView>() }
    val addresses = remember(url) { mutableStateMapOf<DisasterWebPolicy.Provider, String>() }
    val failures = remember(url) { mutableStateMapOf<DisasterWebPolicy.Provider, String>() }
    val zooms = remember { mutableStateMapOf<DisasterWebPolicy.Provider, Int>() }
    var web by remember { mutableStateOf<WebView?>(null) }
    fun back() { if (web?.canGoBack() == true) web?.goBack() else onClose() }
    DisposableEffect(url) {
        onDispose {
            pages.values.forEach { page ->
                (page.parent as? android.view.ViewGroup)?.removeView(page)
                page.stopLoading(); page.destroy()
            }
            pages.clear(); web = null
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(
        usePlatformDefaultWidth = false, dismissOnBackPress = false)) {
        BackHandler { back() }
        Surface(Modifier.fillMaxWidth(if (expanded) 1f else .96f).fillMaxHeight(if (expanded) 1f else .80f),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { back() }) { Text(watchWords("Back", "பின்செல்")) }
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) watchWords("Compact", "சிறிதாக்கு") else watchWords("Expand", "பெரிதாக்கு"))
                    }
                    TextButton(onClick = onClose) { Text(watchWords("Close", "மூடு")) }
                }
                Row(Modifier.fillMaxWidth()) {
                    DisasterWebPolicy.Provider.entries.forEach { provider ->
                        val isCurrent = selected == provider
                        TextButton(onClick = { selected = provider }, modifier = Modifier.weight(1f).semantics { this.selected = isCurrent },
                            colors = ButtonDefaults.textButtonColors(contentColor = if (selected == provider)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text(provider.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                val currentUrl = addresses[selected] ?: if (selected == DisasterWebPolicy.provider(url)) url else selected.home
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { if (disasterWebAllowed(currentUrl)) browser.openUri(currentUrl) }) {
                        Text(watchWords("Open in browser", "உலாவியில் திற"), style = MaterialTheme.typography.labelSmall)
                    }
                    Box {
                        TextButton(onClick = { zoomMenu = true }) {
                            Text(watchWords("Page zoom", "பக்க அளவு") + " ${zooms[selected] ?: zoomPrefs.getInt(selected.name, 100)}%",
                                style = MaterialTheme.typography.labelSmall)
                        }
                        DropdownMenu(expanded = zoomMenu, onDismissRequest = { zoomMenu = false }) {
                            DisasterWebPolicy.pageZoomChoices.forEach { percent ->
                                DropdownMenuItem(text = { Text("$percent%") }, onClick = {
                                    val current = zooms[selected] ?: 100
                                    web?.zoomBy(percent.toFloat() / current.coerceAtLeast(1))
                                    zoomMenu = false
                                })
                            }
                        }
                    }
                }
                if (placeLabel.isNotBlank()) Text(watchWords("Selected place: ", "தேர்ந்தெடுத்த இடம்: ") + placeLabel,
                    Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelSmall)
                Text(Uri.parse(currentUrl).host.orEmpty(), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelSmall)
                Text(watchWords("Choose the place and map zoom on the website. Map view stays while this window is open; page zoom is saved.",
                    "இடத்தையும் வரைபட அளவையும் தளத்தில் தேர்ந்தெடுக்கவும். சாளரம் திறந்திருக்கும் வரை வரைபடம் நீடிக்கும்; பக்க அளவு சேமிக்கப்படும்."),
                    Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.labelSmall)
                failures[selected]?.let { Text(it, Modifier.padding(8.dp), style = MaterialTheme.typography.labelSmall) }
                AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { FrameLayout(it) }, update = { frame ->
                    val page = pages.getOrPut(selected) {
                        val provider = selected
                        WebView(frame.context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setGeolocationEnabled(false)
                            settings.setSupportMultipleWindows(false)
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.safeBrowsingEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            setInitialScale(DisasterWebPolicy.initialScale(zoomPrefs.getInt(provider.name, 100), resources.displayMetrics.density))
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val target = DisasterWebPolicy.secureNavigation(request.url.toString())
                                    if (target == null) {
                                        if (request.isForMainFrame) failures[provider] = watchWords(
                                            "This link cannot open here. Use the provider page in your browser.",
                                            "இந்த இணைப்பை இங்கே திறக்க முடியாது. தளத்தை உலாவியில் திறக்கவும்.")
                                        return true
                                    }
                                    // Upgrade the provider's own legacy links, not the allowlist.
                                    if (target != request.url.toString()) { view.loadUrl(target); return true }
                                    return false
                                }
                                override fun onPageStarted(view: WebView, next: String, favicon: android.graphics.Bitmap?) {
                                    if (!disasterWebAllowed(next)) {
                                        view.stopLoading()
                                        failures[provider] = watchWords("Navigation blocked. Use the provider page in your browser.", "இணைப்பு தடுக்கப்பட்டது. தளத்தை உலாவியில் திறக்கவும்.")
                                    } else {
                                        addresses[provider] = next
                                        failures.remove(provider)
                                    }
                                }
                                override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
                                    val percent = DisasterWebPolicy.savedPageZoom((100 * newScale / view.resources.displayMetrics.density).roundToInt())
                                    zooms[provider] = percent
                                    zoomPrefs.edit().putInt(provider.name, percent).apply()
                                }
                                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                    if (request.isForMainFrame) failures[provider] = watchWords("Website could not load. No safety conclusion can be drawn.",
                                        "தளம் திறக்கவில்லை. பாதுகாப்பு முடிவு கூற முடியாது.")
                                }
                                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                                    if (request.isForMainFrame) failures[provider] = watchWords("Website returned an error. Try your browser.", "தளம் பிழை காட்டுகிறது. உலாவியில் முயலவும்.")
                                }
                                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                                    handler.cancel()
                                    failures[provider] = watchWords("Secure connection failed. The page was blocked.", "பாதுகாப்பான இணைப்பு தோல்வி. பக்கம் தடுக்கப்பட்டது.")
                                }
                            }
                            loadUrl(if (provider == DisasterWebPolicy.provider(url)) url else provider.home)
                        }
                    }
                    if (frame.getChildAt(0) !== page) {
                        web?.onPause()
                        frame.removeAllViews()
                        frame.addView(page, FrameLayout.LayoutParams(-1, -1))
                        web = page
                        page.onResume()
                    }
                })
            }
        }
    }
}
