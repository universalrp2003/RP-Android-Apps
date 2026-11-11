package com.universalrp.tvguard.web

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.universalrp.tvguard.capture.ScreenCaptureManager
import com.universalrp.tvguard.debloat.DebloatEngine
import com.universalrp.tvguard.memory.MemoryTrimmer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object LocalWebTransferServer {

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var serverJob: Job? = null
    @Volatile private var features: HubFeatures = HubFeatures()
    private val clients = ConcurrentHashMap<Socket, Job>()

    @Synchronized
    fun configure(features: HubFeatures) {
        val previous = this.features
        this.features = features
        // Abort in-flight requests as well as rejecting future ones when a control
        // is revoked. The file hub itself remains available if it is still enabled.
        if ((previous.remote && !features.remote) ||
            (previous.screenshot && !features.screenshot) ||
            (previous.ramBoost && !features.ramBoost)
        ) closeClients()
    }

    private fun closeClients() {
        clients.forEach { (socket, job) ->
            job.cancel()
            runCatching { socket.close() }
        }
    }
    var activePort: Int = 8080
        private set

    val isRunning: Boolean
        get() = serverSocket?.isClosed == false && serverJob?.isActive == true

    fun getLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Throwable) {}
        return "127.0.0.1"
    }

    fun getServerUrl(): String {
        return "http://${getLocalIp()}:$activePort"
    }

    @Synchronized
    fun start(context: Context, preferredPort: Int = 8080): Boolean {
        if (isRunning) return true

        val portsToTry = listOf(preferredPort, 8081, 8888, 8000)
        for (p in portsToTry) {
            try {
                serverSocket = ServerSocket(p)
                activePort = serverSocket!!.localPort
                break
            } catch (_: Throwable) {
                serverSocket = null
            }
        }

        val s = serverSocket ?: return false
        // Set the parent before launching so isRunning cannot race the accept loop.
        val parent = SupervisorJob()
        serverJob = parent
        CoroutineScope(Dispatchers.IO + parent).launch {
            try {
                while (isActive && !s.isClosed) {
                    val client = try { s.accept() } catch (_: Exception) { break }
                    // Lazy start lets completion close the socket even if its parent
                    // is cancelled just before the handler has a chance to run.
                    val job = launch(start = CoroutineStart.LAZY) {
                        try { runInterruptible(Dispatchers.IO) { handleClient(context.applicationContext, client) } }
                        catch (_: Exception) { /* Disconnected, revoked or stopped. */ }
                    }
                    clients[client] = job
                    job.invokeOnCompletion {
                        clients.remove(client, job)
                        runCatching { client.close() }
                    }
                    job.start()
                }
            } finally {
                runCatching { s.close() }
                parent.cancel()
                synchronized(this@LocalWebTransferServer) {
                    if (serverSocket === s) { serverSocket = null; serverJob = null }
                }
            }
        }
        return true
    }

    @Synchronized
    fun stop() {
        features = HubFeatures()
        serverJob?.cancel()
        runCatching { serverSocket?.close() }
        closeClients()
        serverSocket = null
        serverJob = null
    }

    private fun handleClient(context: Context, socket: Socket) {
        socket.use { sock ->
            sock.soTimeout = 30000
            val input = BufferedInputStream(sock.getInputStream())
            val output = BufferedOutputStream(sock.getOutputStream())

            val rawHeaders = readHeaderString(input)
            if (rawHeaders.isBlank()) return

            val lines = rawHeaders.split("\r\n")
            val requestLine = lines.firstOrNull() ?: return
            val requestParts = requestLine.split(" ")
            if (requestParts.size < 2) return

            val method = requestParts[0].uppercase(Locale.ROOT)
            val fullUri = requestParts[1]
            val path = fullUri.substringBefore("?")
            val query = if (fullUri.contains("?")) fullUri.substringAfter("?") else ""
            val queryParams = parseQueryParams(query)

            // Hiding a dashboard button is not enough: reject direct requests too.
            if (!isRunning || !features.allows(path)) {
                sendJsonResponse(output, 403, "{\"success\":false,\"error\":\"Feature disabled\"}")
                return
            }

            // Extract headers
            val headersMap = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val headerLine = lines[i]
                val colonIdx = headerLine.indexOf(':')
                if (colonIdx > 0) {
                    headersMap[headerLine.substring(0, colonIdx).trim().lowercase(Locale.ROOT)] =
                        headerLine.substring(colonIdx + 1).trim()
                }
            }

            when {
                method == "GET" && (path == "/" || path == "/index.html") -> {
                    serveDashboard(context, output)
                }

                method == "GET" && path == "/download" -> {
                    val filename = queryParams["file"] ?: ""
                    serveFileDownload(filename, output, asAttachment = true)
                }

                method == "GET" && path == "/view" -> {
                    val filename = queryParams["file"] ?: ""
                    serveFileDownload(filename, output, asAttachment = false)
                }

                method == "POST" && path == "/upload" -> {
                    val contentType = headersMap["content-type"] ?: ""
                    val contentLength = headersMap["content-length"]?.toLongOrNull() ?: 0L
                    handleUpload(context, input, output, contentType, contentLength, queryParams)
                }

                method == "POST" && path == "/api/screenshot" -> {
                    handleApiScreenshot(context, output)
                }

                method == "POST" && path == "/api/boost" -> {
                    val res = MemoryTrimmer.trim(context)
                    sendJsonResponse(output, 200, "{\"success\":true,\"freedMb\":${res.freedMb}}")
                }

                method == "POST" && path == "/api/install" -> {
                    val filename = queryParams["file"] ?: ""
                    val ok = triggerApkInstall(context, filename)
                    sendJsonResponse(output, 200, "{\"success\":$ok}")
                }

                (method == "POST" || method == "GET") && path == "/api/key" -> {
                    val code = queryParams["code"]?.toIntOrNull() ?: 0
                    if (code > 0) {
                        val ok = sendKeyEvent(code)
                        sendJsonResponse(output, 200, "{\"success\":$ok,\"code\":$code}")
                    } else {
                        sendJsonResponse(output, 400, "{\"error\":\"Missing or invalid code\"}")
                    }
                }

                (method == "POST" || method == "GET") && path == "/api/text" -> {
                    val text = queryParams["text"] ?: ""
                    if (text.isNotBlank()) {
                        val ok = sendTextToTv(text)
                        sendJsonResponse(output, 200, "{\"success\":$ok}")
                    } else {
                        sendJsonResponse(output, 400, "{\"error\":\"Empty text\"}")
                    }
                }

                (method == "POST" || method == "GET") && path == "/api/launch" -> {
                    val app = queryParams["app"] ?: ""
                    val ok = launchAppByName(context, app)
                    // The browser already knows the requested name; do not splice
                    // untrusted query text into JSON (quotes or newlines break it).
                    sendJsonResponse(output, 200, "{\"success\":$ok}")
                }

                (method == "POST" || method == "GET") && path == "/delete" -> {
                    val filename = queryParams["file"] ?: ""
                    deleteFile(filename)
                    sendRedirect(output, "/")
                }

                else -> {
                    send404(output)
                }
            }
        }
    }

    private fun readHeaderString(input: InputStream): String {
        val baos = ByteArrayOutputStream()
        var lastFour = 0
        while (true) {
            val b = input.read()
            if (b == -1) break
            baos.write(b)
            if (baos.size() > 16_384) return "" // Reject unbounded request/part headers.
            lastFour = (lastFour shl 8) or (b and 0xFF)
            if (lastFour == 0x0D0A0D0A) break // \r\n\r\n
        }
        return baos.toString(StandardCharsets.UTF_8.name())
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (query.isBlank()) return map
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                map[key] = value
            }
        }
        return map
    }

    private fun sanitizeFilename(name: String): String {
        return File(name).name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
    }

    private fun getDownloadsDir(): File {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** The hosted app store page. A web update adds an app; no APK needs to change. */
    private val STORE_URL = "https://universalrp2003.github.io/RP-Android-Apps/"

    /**
     * The store card shown above the remote control. Kept as its own function so a unit test can
     * check the link and the promise it makes without standing up the HTTP server.
     */
    internal fun storeCardHtml(): String = buildString {
        append("<div class=\"card\">")
        append("<h3>\uD83E\uDDED RP App Store</h3>")
        append("<p style=\"font-size: 13px; color: var(--muted); margin-bottom: 14px;\">Browse every RP app, download an APK, or open the install prompt that appears in the file list above.</p>")
        append("<a class=\"btn btn-outline\" style=\"display:block; text-align:center; text-decoration:none;\" href=\"" + STORE_URL + "\" target=\"_blank\" rel=\"noopener\">Open the RP App Store</a>")
        append("<p style=\"font-size: 12px; color: var(--muted); margin-top: 10px;\">The store is a web page, so it always lists the newest apps. Nothing needs reinstalling to see a new one.</p>")
        append("</div>")
    }

    private fun isTemporaryUpload(filename: String): Boolean =
        filename.startsWith(".tvguard-upload-") && filename.endsWith(".part")

    private fun findFile(rawFilename: String): File? {
        val filename = existingHubFileName(rawFilename) ?: return null
        if (isTemporaryUpload(filename)) return null
        for (directory in listOf(ScreenCaptureManager.getScreenshotDir(), getDownloadsDir())) {
            val file = File(directory, filename)
            // Do not follow a shared-storage symlink out of the advertised folders.
            val insideDirectory = try { file.canonicalFile.parentFile == directory.canonicalFile } catch (_: Exception) { false }
            if (insideDirectory && file.isFile) return file
        }
        return null
    }

    fun sendKeyEvent(keyCode: Int): Boolean {
        val rootRes = DebloatEngine.executeRootCommand("input keyevent $keyCode")
        return rootRes.isSuccess || executeInput("keyevent", keyCode.toString())
    }

    fun sendTextToTv(text: String): Boolean {
        // Android 'input text' interprets spaces as %s
        val formatted = text.replace(" ", "%s")
        // Treat browser text as data, not a root shell expression (including $ and backticks).
        val rootRes = DebloatEngine.executeRootCommand("input text ${shellQuote(formatted)}")
        return rootRes.isSuccess || executeInput("text", formatted)
    }

    /** Starting a process does not prove Android accepted its input event. */
    private fun executeInput(vararg arguments: String): Boolean {
        var process: Process? = null
        try {
            process = ProcessBuilder(listOf("input") + arguments).redirectErrorStream(true).start()
            return process.waitFor(4, TimeUnit.SECONDS) && process.exitValue() == 0
        } catch (interrupted: InterruptedException) {
            throw interrupted
        } catch (_: Exception) {
            return false
        } finally {
            process?.destroyForcibly()
        }
    }

    fun launchAppByName(context: Context, appNameOrPkg: String): Boolean {
        val pm = context.packageManager
        val lower = appNameOrPkg.lowercase(Locale.ROOT)
        if (lower == "settings") {
            return try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (_: Throwable) { false }
        }

        val targetPkgs = when (lower) {
            "smarttube", "smart tube" -> listOf("org.smarttube.stable", "com.teamsmart.videomanager.tv")
            "youtube" -> listOf("com.google.android.youtube.tv", "com.google.android.youtube")
            "browser", "duckduckgo" -> listOf("com.duckduckgo.mobile.android", "org.mozilla.tv.firefox", "com.android.chrome")
            else -> listOf(appNameOrPkg)
        }

        for (pkg in targetPkgs) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    ?: pm.getLeanbackLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            } catch (_: Exception) {
                // An app can disappear or reject the launch after it was queried.
            }
        }
        return false
    }

    private fun serveDashboard(context: Context, out: OutputStream) {
        val policy = features
        val screenshots = ScreenCaptureManager.listScreenshots()
        val downloads = getDownloadsDir().listFiles { f ->
            f.isFile && !f.name.startsWith("TvGuard_Screenshot_") && !isTemporaryUpload(f.name)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val ip = getLocalIp()

        val html = buildString {
            append("<!DOCTYPE html><html lang=\"en\"><head>")
            append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            append("<title>Android TV Guard | Wi-Fi Manager</title>")
            append("<style>")
            append(":root { --bg: #0B0F19; --card: #131B2B; --border: #1E293B; --cyan: #00E5FF; --text: #F8FAFC; --muted: #94A3B8; --green: #00E676; --danger: #FF5252; }")
            append("* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }")
            append("body { background: var(--bg); color: var(--text); padding: 20px; }")
            append(".container { max-width: 920px; margin: 0 auto; }")
            append("header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 24px; }")
            append(".logo { font-size: 22px; font-weight: bold; color: var(--cyan); display: flex; align-items: center; gap: 8px; }")
            append(".device-pill { background: #1E293B; padding: 6px 14px; border-radius: 20px; font-size: 13px; color: var(--muted); }")
            append(".grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px; }")
            append("@media (max-width: 700px) { .grid { grid-template-columns: 1fr; } }")
            append(".card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 18px; margin-bottom: 20px; }")
            append(".card h3 { font-size: 16px; color: var(--cyan); margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }")
            append(".btn { display: inline-block; background: var(--cyan); color: #0B0F19; border: none; font-weight: bold; padding: 10px 18px; border-radius: 8px; cursor: pointer; text-decoration: none; font-size: 14px; transition: 0.15s; }")
            append(".btn:hover { opacity: 0.9; transform: scale(1.02); }")
            append(".btn-danger { background: var(--danger); color: white; }")
            append(".btn-outline { background: transparent; border: 1px solid var(--cyan); color: var(--cyan); }")
            append(".drop-zone { border: 2px dashed #334155; border-radius: 10px; padding: 24px; text-align: center; cursor: pointer; margin-bottom: 12px; }")
            append(".drop-zone.dragover { border-color: var(--cyan); background: #17243B; }")
            append("table { width: 100%; border-collapse: collapse; margin-top: 10px; font-size: 14px; }")
            append("th, td { text-align: left; padding: 10px; border-bottom: 1px solid var(--border); }")
            append("th { color: var(--muted); font-size: 12px; text-transform: uppercase; }")
            append(".action-links a, .action-links button { margin-right: 8px; font-size: 12px; text-decoration: none; color: var(--cyan); background: transparent; border: 1px solid var(--border); padding: 4px 8px; border-radius: 4px; cursor: pointer; }")
            append(".action-links a:hover, .action-links button:hover { border-color: var(--cyan); }")
            append(".remote-btn { background: #1E293B; color: var(--text); border: 1px solid #334155; border-radius: 8px; padding: 12px 14px; font-size: 13px; font-weight: bold; cursor: pointer; transition: 0.15s; user-select: none; touch-action: manipulation; }")
            append(".remote-btn:active { background: var(--cyan); color: #0B0F19; transform: scale(0.95); }")
            append(".remote-btn-ok { background: var(--cyan); color: #0B0F19; min-width: 54px; }")
            append(".remote-btn-power { background: rgba(255, 82, 82, 0.2); color: var(--danger); border-color: var(--danger); }")
            append("</style></head><body>")
            append("<div class=\"container\">")
            append("<header>")
            append("<div class=\"logo\">🛡️ Android TV Guard <span style=\"font-size: 13px; color: var(--muted); font-weight: normal;\">Wi-Fi File Hub</span></div>")
            append("<div class=\"device-pill\">📺 ").append(htmlEscape(model)).append(" (").append(htmlEscape(ip)).append(")</div>")
            append("</header>")

            // Grid Cards
            append("<div class=\"grid\">")

            // Card 1: Upload
            append("<div class=\"card\" style=\"margin-bottom:0;\">")
            append("<h3>📥 Upload APK or Files to TV</h3>")
            append("<p style=\"font-size: 13px; color: var(--muted); margin-bottom: 12px;\">Send APKs, videos, or files directly to TV Download folder without any USB drive or cable.</p>")
            append("<form action=\"/upload\" method=\"POST\" enctype=\"multipart/form-data\">")
            append("<div class=\"drop-zone\" id=\"dropZone\">")
            append("<input type=\"file\" name=\"file\" id=\"fileInput\" style=\"display:none;\" onchange=\"updateFileName(this)\">")
            append("<div id=\"filePrompt\">📁 Click to select file or drag & drop here</div>")
            append("<div id=\"fileNameDisplay\" style=\"margin-top: 8px; font-weight: bold; color: var(--green);\"></div>")
            append("</div>")
            append("<button type=\"submit\" class=\"btn\" style=\"width: 100%;\">🚀 Upload Directly to TV</button>")
            append("</form>")
            append("</div>")

            // Card 2: only actions explicitly enabled in Expert settings.
            if (policy.remote && (policy.screenshot || policy.ramBoost)) {
            append("<div class=\"card\" style=\"margin-bottom:0;\">")
            append("<h3>📸 Quick TV Maintenance</h3>")
            append("<p style=\"font-size: 13px; color: var(--muted); margin-bottom: 16px;\">Control TV maintenance and capture screenshots remotely from your phone.</p>")
            append("<div style=\"display: flex; gap: 10px; flex-direction: column;\">")
            if (policy.screenshot) append("<button onclick=\"triggerScreenshot()\" class=\"btn btn-outline\" style=\"width: 100%; text-align: center;\">📸 Take TV Screenshot Now</button>")
            if (policy.ramBoost) append("<button onclick=\"triggerBoost()\" class=\"btn btn-outline\" style=\"width: 100%; text-align: center;\">⚡ Boost TV RAM Remotely</button>")
            append("</div>")
            append("<div id=\"actionResult\" style=\"margin-top: 14px; font-size: 13px; font-weight: bold;\"></div>")
            append("</div>")
            }

            append("</div>") // End grid

            // Card 3: the app store page, opened from this hub so the phone never has to
            // remember an address. The store is a hosted web page, so adding an app is a
            // web update, not a new APK.
            append(storeCardHtml())

            // Card 4: Mobile Virtual Remote Control & Touchpad / Keyboard
            if (policy.remote) {
            append("<div class=\"card\">")
            append("<h3>🎮 Mobile Virtual TV Remote & Keyboard</h3>")
            append("<p style=\"font-size: 13px; color: var(--muted); margin-bottom: 14px;\">Control TV D-Pad navigation, volume, apps, and send typed phone text to the TV.</p>")
            append("<div style=\"display: flex; flex-wrap: wrap; gap: 20px; justify-content: center; align-items: flex-start;\">")

            // D-Pad
            append("<div style=\"display: flex; flex-direction: column; align-items: center; gap: 6px;\">")
            append("<div style=\"font-size: 11px; color: var(--muted); text-transform: uppercase; margin-bottom: 4px;\">D-Pad Directional</div>")
            append("<button class=\"remote-btn\" onclick=\"sendKey(19)\" style=\"width: 54px;\">▲</button>")
            append("<div style=\"display: flex; gap: 6px;\">")
            append("<button class=\"remote-btn\" onclick=\"sendKey(21)\" style=\"width: 54px;\">◀</button>")
            append("<button class=\"remote-btn remote-btn-ok\" onclick=\"sendKey(23)\">OK</button>")
            append("<button class=\"remote-btn\" onclick=\"sendKey(22)\" style=\"width: 54px;\">▶</button>")
            append("</div>")
            append("<button class=\"remote-btn\" onclick=\"sendKey(20)\" style=\"width: 54px;\">▼</button>")
            append("</div>")

            // Navigation & Volume
            append("<div style=\"display: flex; flex-direction: column; gap: 8px; min-width: 140px;\">")
            append("<div style=\"font-size: 11px; color: var(--muted); text-transform: uppercase;\">System Keys</div>")
            append("<div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 8px;\">")
            append("<button class=\"remote-btn\" onclick=\"sendKey(4)\">↩ Back</button>")
            append("<button class=\"remote-btn\" onclick=\"sendKey(3)\">🏠 Home</button>")
            append("<button class=\"remote-btn\" onclick=\"sendKey(82)\">☰ Menu</button>")
            append("<button class=\"remote-btn remote-btn-power\" onclick=\"sendKey(26)\">⏻ Sleep</button>")
            append("</div>")
            append("<div style=\"font-size: 11px; color: var(--muted); text-transform: uppercase; margin-top: 6px;\">Volume</div>")
            append("<div style=\"display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 6px;\">")
            append("<button class=\"remote-btn\" onclick=\"sendKey(24)\">🔊 +</button>")
            append("<button class=\"remote-btn\" onclick=\"sendKey(25)\">🔉 -</button>")
            append("<button class=\"remote-btn\" onclick=\"sendKey(164)\">🔇</button>")
            append("</div>")
            append("</div>")

            // Quick App Launchers
            append("<div style=\"display: flex; flex-direction: column; gap: 8px; min-width: 150px;\">")
            append("<div style=\"font-size: 11px; color: var(--muted); text-transform: uppercase;\">Quick App Launch</div>")
            append("<button class=\"remote-btn\" style=\"text-align: left; padding: 8px 12px;\" onclick=\"launchApp('smarttube')\">📺 SmartTube</button>")
            append("<button class=\"remote-btn\" style=\"text-align: left; padding: 8px 12px;\" onclick=\"launchApp('youtube')\">▶ YouTube</button>")
            append("<button class=\"remote-btn\" style=\"text-align: left; padding: 8px 12px;\" onclick=\"launchApp('browser')\">🌐 DuckDuckGo</button>")
            append("<button class=\"remote-btn\" style=\"text-align: left; padding: 8px 12px;\" onclick=\"launchApp('settings')\">⚙️ TV Settings</button>")
            append("</div>")

            // Phone Keyboard Input
            append("<div style=\"display: flex; flex-direction: column; gap: 8px; flex: 1; min-width: 220px;\">")
            append("<div style=\"font-size: 11px; color: var(--muted); text-transform: uppercase;\">Phone Keyboard to TV</div>")
            append("<input type=\"text\" id=\"tvTextInput\" placeholder=\"Type search, URL, or text here...\" style=\"background: #0B0F19; border: 1px solid var(--border); color: var(--text); padding: 10px; border-radius: 8px; font-size: 14px;\" onkeydown=\"if(event.key==='Enter') sendText()\">")
            append("<div style=\"display: flex; gap: 8px;\">")
            append("<button class=\"btn\" style=\"flex: 1;\" onclick=\"sendText()\">⌨️ Send to TV</button>")
            append("<button class=\"btn btn-outline\" onclick=\"sendKey(67)\">⌫ Del</button>")
            append("</div>")
            append("<div id=\"remoteFeedback\" style=\"font-size: 12px; color: var(--green); height: 16px; margin-top: 4px;\"></div>")
            append("</div>")

            append("</div>") // End remote flex
            append("</div>") // End Remote Card
            }

            // Card 5: Screenshots Table (saved in /sdcard/Download/Screenshots/)
            append("<div class=\"card\">")
            append("<h3>📸 TV Screenshots (/sdcard/Download/Screenshots/) (").append(screenshots.size).append(" files)</h3>")
            if (screenshots.isEmpty()) {
                append("<p style=\"color: var(--muted); font-size: 13px; padding: 14px 0; text-align: center;\">No screenshots saved yet.</p>")
            } else {
                append("<table><thead><tr><th>Screenshot Name</th><th>Size</th><th>Date</th><th>Actions</th></tr></thead><tbody>")
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                for (file in screenshots) {
                    val sizeMb = String.format(Locale.US, "%.2f MB", file.length().toDouble() / (1024.0 * 1024.0))
                    val dateStr = sdf.format(Date(file.lastModified()))

                    val encodedName = hubUrlComponent(file.name)
                    append("<tr>")
                    append("<td>🖼️ ").append(htmlEscape(file.name)).append("</td>")
                    append("<td>").append(sizeMb).append("</td>")
                    append("<td>").append(dateStr).append("</td>")
                    append("<td class=\"action-links\">")
                    append("<a href=\"/download?file=").append(encodedName).append("\">Download</a>")
                    append("<a href=\"/view?file=").append(encodedName).append("\" target=\"_blank\">View</a>")
                    append("<a href=\"/delete?file=").append(encodedName).append("\" style=\"color: var(--danger);\">Delete</a>")
                    append("</td>")
                    append("</tr>")
                }
                append("</tbody></table>")
            }
            append("</div>")

            // Card 6: Downloads & APKs Table (saved in /sdcard/Download/)
            append("<div class=\"card\">")
            append("<h3>📦 TV Downloads & Uploaded APKs (/sdcard/Download/) (").append(downloads.size).append(" files)</h3>")
            if (downloads.isEmpty()) {
                append("<p style=\"color: var(--muted); font-size: 13px; padding: 14px 0; text-align: center;\">No uploaded files or APKs in /sdcard/Download.</p>")
            } else {
                append("<table><thead><tr><th>File Name</th><th>Size</th><th>Date</th><th>Actions</th></tr></thead><tbody>")
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                for (file in downloads) {
                    val isApk = file.name.endsWith(".apk", ignoreCase = true)
                    val sizeMb = String.format(Locale.US, "%.2f MB", file.length().toDouble() / (1024.0 * 1024.0))
                    val dateStr = sdf.format(Date(file.lastModified()))

                    val encodedName = hubUrlComponent(file.name)
                    append("<tr>")
                    append("<td>").append(if (isApk) "📦 " else "📄 ").append(htmlEscape(file.name)).append("</td>")
                    append("<td>").append(sizeMb).append("</td>")
                    append("<td>").append(dateStr).append("</td>")
                    append("<td class=\"action-links\">")
                    append("<a href=\"/download?file=").append(encodedName).append("\">Download</a>")
                    if (isApk && policy.remote) {
                        append("<button data-file=\"").append(encodedName)
                            .append("\" onclick=\"installApk(decodeURIComponent(this.dataset.file))\">Install on TV</button>")
                    }
                    append("<a href=\"/delete?file=").append(encodedName).append("\" style=\"color: var(--danger);\">Delete</a>")
                    append("</td>")
                    append("</tr>")
                }
                append("</tbody></table>")
            }
            append("</div>")

            // JavaScript
            append("<script>")
            append("const dropZone = document.getElementById('dropZone');")
            append("const fileInput = document.getElementById('fileInput');")
            append("dropZone.onclick = () => fileInput.click();")
            append("dropZone.ondragover = (e) => { e.preventDefault(); dropZone.classList.add('dragover'); };")
            append("dropZone.ondragleave = () => dropZone.classList.remove('dragover');")
            append("dropZone.ondrop = (e) => { e.preventDefault(); dropZone.classList.remove('dragover'); if (e.dataTransfer.files.length) { fileInput.files = e.dataTransfer.files; updateFileName(fileInput); } };")
            append("function updateFileName(input) { if (input.files.length) { document.getElementById('fileNameDisplay').innerText = 'Selected: ' + input.files[0].name; } }")
            append("function triggerScreenshot() {")
            append("  const res = document.getElementById('actionResult'); res.innerText = 'Capturing TV screenshot...'; res.style.color = '#00E5FF';")
            append("  fetch('/api/screenshot', { method: 'POST' }).then(r => r.json()).then(d => {")
            append("    if(d.success) { res.innerText = '✅ Screenshot saved: ' + d.filename; res.style.color = '#00E676'; setTimeout(() => location.reload(), 1200); }")
            append("    else { res.innerText = '❌ Failed: ' + (d.error || 'Unknown error'); res.style.color = '#FF5252'; }")
            append("  }).catch(e => { res.innerText = '❌ Error: ' + e; res.style.color = '#FF5252'; });")
            append("}")
            append("function triggerBoost() {")
            append("  const res = document.getElementById('actionResult'); res.innerText = 'Trimming TV memory...'; res.style.color = '#00E5FF';")
            append("  fetch('/api/boost', { method: 'POST' }).then(r => r.json()).then(d => {")
            append("    if(d.success) { res.innerText = '✅ RAM Boosted! Freed ' + d.freedMb + ' MB'; res.style.color = '#00E676'; }")
            append("    else { res.innerText = '❌ Failed to boost RAM'; res.style.color = '#FF5252'; }")
            append("  }).catch(e => { res.innerText = '❌ Error: ' + e; res.style.color = '#FF5252'; });")
            append("}")
            append("function installApk(name) {")
            append("  if(!confirm('Launch package installer on TV for ' + name + '?')) return;")
            append("  fetch('/api/install?file=' + encodeURIComponent(name), { method: 'POST' }).then(r => r.json()).then(d => {")
            append("    alert(d.success ? 'Install prompt opened on TV!' : 'Could not launch installer');")
            append("  });")
            append("}")
            append("function sendKey(code) {")
            append("  const fb = document.getElementById('remoteFeedback');")
            append("  fetch('/api/key?code=' + code, { method: 'POST' }).then(r => r.json()).then(d => {")
            append("    if (fb) { fb.innerText = d.success ? 'Sent key: ' + code : 'TV did not accept the key'; setTimeout(() => { fb.innerText = ''; }, 1500); }")
            append("  }).catch(e => { if (fb) fb.innerText = 'Error sending key'; });")
            append("}")
            append("function sendText() {")
            append("  const input = document.getElementById('tvTextInput');")
            append("  const text = input.value;")
            append("  if (!text) return;")
            append("  const fb = document.getElementById('remoteFeedback');")
            append("  fetch('/api/text?text=' + encodeURIComponent(text), { method: 'POST' }).then(r => r.json()).then(d => {")
            append("    if (fb) { fb.innerText = d.success ? 'Sent text to TV: \"' + text + '\"' : 'TV did not accept the text'; setTimeout(() => { fb.innerText = ''; }, 2000); }")
            append("    if (d.success) input.value = '';")
            append("  }).catch(e => { if (fb) fb.innerText = 'Error sending text'; });")
            append("}")
            append("function launchApp(name) {")
            append("  const fb = document.getElementById('remoteFeedback');")
            append("  fetch('/api/launch?app=' + encodeURIComponent(name), { method: 'POST' }).then(r => r.json()).then(d => {")
            append("    if (fb) { fb.innerText = d.success ? 'Launched ' + name : 'App not found'; setTimeout(() => { fb.innerText = ''; }, 2000); }")
            append("  }).catch(e => { if (fb) fb.innerText = 'Launch error'; });")
            append("}")
            append("</script>")

            append("</div></body></html>")
        }

        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun serveFileDownload(rawFilename: String, out: OutputStream, asAttachment: Boolean) {
        val file = findFile(rawFilename)
        if (file == null || !file.exists() || !file.isFile) {
            send404(out)
            return
        }

        val filename = file.name
        val mimeType = when {
            filename.endsWith(".png", ignoreCase = true) -> "image/png"
            filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            filename.endsWith(".apk", ignoreCase = true) -> "application/vnd.android.package-archive"
            filename.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            filename.endsWith(".zip", ignoreCase = true) -> "application/zip"
            else -> "application/octet-stream"
        }

        val asciiName = sanitizeFilename(filename).take(180).ifBlank { "download" }
        val disposition = if (asAttachment) "attachment; filename=\"$asciiName\"; filename*=UTF-8''${hubUrlComponent(filename)}" else "inline"
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: $mimeType\r\n" +
                "X-Content-Type-Options: nosniff\r\n" +
                "Content-Length: ${file.length()}\r\n" +
                "Content-Disposition: $disposition\r\n" +
                "Connection: close\r\n\r\n"

        out.write(header.toByteArray(StandardCharsets.UTF_8))
        FileInputStream(file).use { input ->
            val buf = ByteArray(32768)
            var n: Int
            while (input.read(buf).also { n = it } != -1) {
                out.write(buf, 0, n)
            }
        }
        out.flush()
    }

    /** Upload to a temporary file first: a stopped hub must not truncate an existing file. */
    private fun handleUpload(
        context: Context,
        input: InputStream,
        output: OutputStream,
        contentType: String,
        contentLength: Long,
        queryParams: Map<String, String>
    ) {
        var temp: File? = null
        try {
            require(contentLength > 0) { "Content-Length required" }
            val limited = ContentLengthInputStream(input, contentLength)
            var filename = queryParams["filename"] ?: queryParams["file"] ?: "upload_${System.currentTimeMillis()}"
            val multipart = contentType.contains("multipart/form-data", ignoreCase = true)
            val boundary = if (multipart) contentType.substringAfter("boundary=", "").trim().trim('"') else ""
            if (multipart) {
                require(boundary.isNotBlank() && boundary.length <= 200) { "Invalid multipart boundary" }
                val headers = readHeaderString(limited)
                require(headers.startsWith("--$boundary\r\n")) { "Invalid multipart header" }
                filename = "filename=\"([^\"]+)\"".toRegex().find(headers)?.groupValues?.get(1) ?: filename
            }
            filename = sanitizeFilename(filename).take(180).takeUnless { it.isBlank() || it == "." || it == ".." }
                ?: "upload_${System.currentTimeMillis()}"
            temp = File.createTempFile(".tvguard-upload-", ".part", getDownloadsDir())
            FileOutputStream(temp).use { fileOutput ->
                val buffer = ByteArray(32_768)
                var n: Int
                while (limited.read(buffer).also { n = it } != -1) {
                    if (Thread.currentThread().isInterrupted) throw InterruptedException("Upload cancelled")
                    fileOutput.write(buffer, 0, n)
                }
                fileOutput.flush()
            }
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Upload cancelled")
            if (multipart) trimMultipartEnding(temp, boundary)
            val saved = SafeUploadStorage.publish(temp, getDownloadsDir(), filename)
            try { android.media.MediaScannerConnection.scanFile(context.applicationContext, arrayOf(saved.absolutePath), null, null) }
            catch (_: Exception) { }
            sendRedirect(output, "/")
        } catch (_: Exception) {
            // A disconnect or a feature switch may have closed the socket.
            runCatching { sendJsonResponse(output, 400, "{\"success\":false,\"error\":\"Upload incomplete\"}") }
        } finally {
            temp?.delete()
        }
    }

    /** Stops at the declared length instead of waiting for the browser to close. */
    private class ContentLengthInputStream(private val source: InputStream, private var remaining: Long) : InputStream() {
        override fun read(): Int {
            if (remaining == 0L) return -1
            val value = source.read()
            if (value == -1) throw EOFException("Incomplete upload")
            remaining--
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (remaining == 0L) return -1
            val count = source.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
            if (count == -1) throw EOFException("Incomplete upload")
            remaining -= count
            return count
        }
    }

    /** Accept the one-file form used by the dashboard and remove its closing delimiter. */
    private fun trimMultipartEnding(file: File, boundary: String) {
        val marker = "\r\n--$boundary--\r\n".toByteArray(StandardCharsets.ISO_8859_1)
        java.io.RandomAccessFile(file, "rw").use { raf ->
            require(raf.length() >= marker.size) { "Incomplete multipart body" }
            val tail = ByteArray(marker.size)
            raf.seek(raf.length() - marker.size)
            raf.readFully(tail)
            require(tail.contentEquals(marker)) { "Invalid multipart ending" }
            raf.setLength(raf.length() - marker.size)
        }
    }

    private fun handleApiScreenshot(context: Context, out: OutputStream) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val targetFile = File.createTempFile("TvGuard_Screenshot_${timestamp}_", ".png", ScreenCaptureManager.getScreenshotDir())
        var saved = false
        try {
            val path = shellQuote(targetFile.absolutePath)
            val result = DebloatEngine.executeRootCommand("screencap -p $path && chmod 664 $path")
            if (result.isSuccess && targetFile.length() > 1024) {
                saved = true
                ScreenCaptureManager.notifyMediaScanner(context, targetFile)
                sendJsonResponse(out, 200, "{\"success\":true,\"filename\":\"${targetFile.name}\"}")
            } else {
                sendJsonResponse(out, 500, "{\"success\":false,\"error\":\"Root screencap unavailable from web daemon\"}")
            }
        } finally {
            if (!saved) targetFile.delete()
        }
    }

    private fun triggerApkInstall(context: Context, rawFilename: String): Boolean {
        val file = findFile(rawFilename)
        if (file == null || !file.exists() || !file.isFile) return false

        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun deleteFile(rawFilename: String) {
        val file = findFile(rawFilename)
        if (file != null && file.exists() && file.isFile) {
            file.delete()
        }
    }

    private fun sendJsonResponse(out: OutputStream, code: Int, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val statusText = when (code) { 200 -> "OK"; 400 -> "Bad Request"; 403 -> "Forbidden"; else -> "Internal Server Error" }
        val header = "HTTP/1.1 $code $statusText\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun sendRedirect(out: OutputStream, location: String) {
        val header = "HTTP/1.1 302 Found\r\n" +
                "Location: $location\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.flush()
    }

    private fun send404(out: OutputStream) {
        val body = "404 Not Found"
        val header = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${body.length}\r\n" +
                "Connection: close\r\n\r\n$body"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.flush()
    }
}
