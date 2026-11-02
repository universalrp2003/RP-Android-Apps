package com.universalrp.cleansweep.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Actual provider website; never reconstructed tracks, injected location, or app/secret JS bridges. */
internal fun disasterWebAllowed(url: String): Boolean = runCatching {
    val uri = java.net.URI(url)
    uri.scheme == "https" && uri.rawUserInfo == null && uri.port in setOf(-1, 443) &&
        uri.host in setOf("www.gdacs.org", "gdacs.org", "rsmcnewdelhi.imd.gov.in", "sachet.ndma.gov.in")
}.getOrDefault(false)

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun DisasterWebWindow(url: String, onClose: () -> Unit) {
    if (!disasterWebAllowed(url)) return
    val browser = LocalUriHandler.current
    var web by remember { mutableStateOf<WebView?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    DisposableEffect(url) { onDispose { web?.stopLoading(); web?.destroy(); web = null } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { if (web?.canGoBack() == true) web?.goBack() else onClose() }) { Text(watchWords("Back", "பின்செல்")) }
                    TextButton(onClick = { browser.openUri(url) }) { Text(watchWords("Open in browser", "உலாவியில் திற")) }
                    TextButton(onClick = onClose) { Text(watchWords("Close", "மூடு")) }
                }
                Text(Uri.parse(url).host.orEmpty(), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelSmall)
                if (failed) Text(watchWords("Website could not load. Retry in your browser; no safety conclusion can be drawn.",
                    "இணையதளம் திறக்கவில்லை. உலாவியில் முயலவும்; பாதுகாப்பு முடிவு கூற முடியாது."), Modifier.padding(16.dp))
                AndroidView(modifier = Modifier.weight(1f).fillMaxWidth(), factory = { context ->
                    WebView(context).apply {
                        web = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setGeolocationEnabled(false)
                        settings.setSupportMultipleWindows(false)
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.safeBrowsingEnabled = true
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                !disasterWebAllowed(request.url.toString())
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                if (request.isForMainFrame) failed = true
                            }
                            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                                handler.cancel(); failed = true
                            }
                        }
                        loadUrl(url)
                    }
                })
            }
        }
    }
}
