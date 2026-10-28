package com.universalrp.podcast

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.Intent
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Slider
import androidx.compose.material3.darkColorScheme
import com.universalrp.updates.AppGuides
import com.universalrp.updates.GuideScreen
import com.universalrp.updates.SuiteAbout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * The one screen: shows, episodes, and the player.
 *
 * Tamil-first and deliberately plain, because this app is for listening while doing something else.
 *
 * The state machine is the part worth reading. The first release had a single `busy` flag that was
 * set when a show was opened and cleared only when the fetch succeeded. A feed that could not be
 * reached - which was every built-in feed, because the URLs in the first release were invented -
 * left the flag set forever, so the screen sat on "loading" and neither the back button nor the
 * cancel button did anything. The owner reported exactly that.
 *
 * So: the load state lives per show in a map, every path through the fetch sets it exactly once,
 * and the screen the user is looking at is a single nullable field that any button can clear. There
 * is no state that a failed network call can leave stuck.
 *
 * The second release added two more things the owner found. A host that blocks a request answers
 * with HTML, and HTML has a `<title>`, so the reader used to show "Attention Required! |
 * Cloudflare" as the name of a podcast; the fetch now refuses a body that is not a feed and says so.
 * And the screen was a wall of text with no artwork, which is not what a podcast player looks like;
 * shows now carry their published cover art and episodes are laid out as a numbered playlist with
 * durations, which is the shape every other player of this kind uses.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.universalrp.updates.AppLanguageState.initialize(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, onPrimary = Bg, background = Bg, onBackground = Color.White, surface = CardBg, onSurface = Color.White, onSurfaceVariant = Muted)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg) {
                    PodcastScreen()
                }
            }
        }
    }
}

/** Where a feed fetch got to, per show. */
private enum class Load { IDLE, LOADING, OK, FAILED }

@Composable
private fun PodcastScreen() {
    val context = LocalContext.current
    val store = remember { PodcastStore(context) }
    val downloads = remember { PodcastDownloads(context.applicationContext) }
    val downloadScope = rememberCoroutineScope()

    // The built-ins, plus anything the owner added before. The URLs are kept on disk, so a feed
    // added in one session is still there in the next; seeding from the built-ins alone lost them.
    val shows = remember {
        mutableStateOf(
            BuiltInShows.all() + store.userFeedUrls().map { url ->
                PodcastShow(id = url, title = url, feedUrl = url, builtIn = false)
            },
        )
    }
    // Per show, so one dead feed cannot freeze the whole screen.
    val loads = remember { mutableStateMapOf<String, Load>() }
    var openId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var newUrl by remember { mutableStateOf("") }
    var showQuery by rememberSaveable { mutableStateOf("") }
    var categoryFilter by rememberSaveable { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var playerRevision by remember { mutableIntStateOf(PodcastState.revision) }
    var page by rememberSaveable { mutableStateOf("") }
    var pendingUrl by rememberSaveable { mutableStateOf("") }
    var pendingTitle by rememberSaveable { mutableStateOf("") }
    var pendingPosition by rememberSaveable { mutableIntStateOf(0) }
    var pendingOffline by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        if (pendingUrl.isNotBlank()) PodcastPlaybackService.play(context, pendingUrl, pendingTitle, pendingPosition, pendingOffline)
        pendingUrl = ""
    }
    fun playEpisode(url: String, title: String, position: Int = 0, offlineOnly: Boolean = false,
                    queue: List<PodcastStore.QueueEntry>? = null) {
        if (queue != null) store.saveQueue(queue, offlineOnly)
        else if (store.queue().none { it.url == url }) store.saveQueue(listOf(PodcastStore.QueueEntry(url, title)), offlineOnly)
        val useOffline = offlineOnly || (queue == null && store.queueOfflineOnly)
        val prefs = context.getSharedPreferences("podcast_permissions", android.content.Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !prefs.getBoolean("asked", false)) {
            pendingUrl = url
            pendingTitle = title
            pendingPosition = position
            pendingOffline = useOffline
            prefs.edit().putBoolean("asked", true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else PodcastPlaybackService.play(context, url, title, position, useOffline)
    }
    BackHandler(enabled = page.isNotEmpty() || openId != null || adding) {
        when {
            page.isNotEmpty() -> page = ""
            adding -> adding = false
            else -> openId = null
        }
    }
    if (page == "guide") {
        GuideScreen(AppGuides.podcast, com.universalrp.updates.AppLanguageState.current == com.universalrp.updates.AppLanguage.TAMIL, { page = "" })
        return
    }
    if (page == "about") {
        SuiteAbout("podcast", "Tamil-Podcast-Player") { page = "" }
        return
    }

    // The player card has to refresh as playback changes state, so it polls rather than being
    // pushed to. 400ms is frequent enough to feel immediate and cheap enough to ignore.
    LaunchedEffect(Unit) {
        while (true) {
            playerRevision = PodcastState.revision
            delay(400)
        }
    }

    if (page == "downloads") {
        Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
            OfflineDownloadsScreen(
                downloads = downloads,
                onBack = { page = "" },
                onPlay = { entry, queue -> playEpisode(entry.url, entry.title, offlineOnly = true,
                    queue = queue.map { PodcastStore.QueueEntry(it.url, it.title) }) },
                onBeforeDelete = { entry ->
                    if (store.lastPlayed() == entry.url && PodcastState.isActive()) PodcastPlaybackService.stop(context)
                },
                modifier = Modifier.weight(1f),
            )
            NowPlayingCard(context, playerRevision)
        }
        return
    }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header() }
            item {
                val bookmark = remember(playerRevision) { store.bookmark() }
                if (bookmark != null && !PodcastState.isActive()) {
                    Button(onClick = { playEpisode(bookmark.url, bookmark.title, bookmark.positionMs) }, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("▶ தொடர்ந்து கேள் / Continue listening")
                            Text(bookmark.title, fontSize = 13.sp, maxLines = 2)
                            Text(formatDuration(bookmark.positionMs / 1000), fontSize = 12.sp)
                        }
                    }
                }
                if (permissionDenied) {
                    Text("அறிவிப்பு அனுமதி மறுக்கப்பட்டது; கேட்பது தொடரும். / Listening is available without notification permission.", color = Muted, fontSize = 13.sp)
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                    }) { Text("அறிவிப்பு அமைப்புகள் / Notification settings") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { page = "guide" }) { Text("வழிகாட்டி / Guide") }
                    OutlinedButton(onClick = { page = "about" }) { Text("விவரம் / Update") }
                }
            }

            item {
                OutlinedButton(onClick = { page = "downloads" }, modifier = Modifier.fillMaxWidth()) {
                    Text("பதிவிறக்கங்கள் / Downloads")
                }
            }

            if (openId == null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(onClick = { adding = !adding }) {
                            Text(context.getString(R.string.add_feed))
                        }
                    }
                }
                if (adding) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                OutlinedTextField(
                                    value = newUrl,
                                    onValueChange = {
                                        newUrl = it
                                        addError = null
                                    },
                                    label = { Text(context.getString(R.string.feed_url_hint)) },
                                    singleLine = true,
                                    isError = addError != null,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                addError?.let { message ->
                                    Text(
                                        text = message,
                                        fontSize = 12.sp,
                                        color = ErrorColor,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val url = newUrl.trim()
                                            if (!PlaybackPolicy.isWebAudio(url)) {
                                                addError = context.getString(R.string.feed_add_failed)
                                            } else if (store.addFeedUrl(url)) {
                                                shows.value = shows.value + PodcastShow(
                                                    id = url,
                                                    title = url,
                                                    feedUrl = url,
                                                    builtIn = false,
                                                )
                                                newUrl = ""
                                                adding = false
                                                addError = null
                                            } else {
                                                // Either a duplicate or a URL that is already there.
                                                addError = context.getString(R.string.feed_already)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                    ) { Text(context.getString(R.string.feed_add_confirm)) }
                                    TextButton(onClick = { adding = false }) {
                                        Text(
                                            context.getString(R.string.feed_cancel),
                                            color = Muted,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text("உலகெங்குமுள்ள தமிழ் நிகழ்ச்சிகளின் தேர்ந்தெடுத்த பட்டியல்; முழு இணையத்தின் பட்டியல் அல்ல. வெளியீட்டாளரின் உள்ளடக்கம் மாறலாம்; அனைத்தும் சிறுவர்களுக்கானவை அல்ல. / A curated worldwide Tamil catalogue, not the entire web. Publisher content may change; not all shows are for children.", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = showQuery,
                        onValueChange = { showQuery = it },
                        label = { Text("நிகழ்ச்சிகளைத் தேடு / Search shows") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            OutlinedButton(onClick = { categoryFilter = "" }) {
                                Text(if (categoryFilter.isEmpty()) "✓ அனைத்தும் / All" else "அனைத்தும் / All")
                            }
                        }
                        PodcastCategory.entries.forEach { category ->
                            item {
                                OutlinedButton(onClick = { categoryFilter = category.name }) {
                                    Text((if (categoryFilter == category.name) "✓ " else "") + category.label)
                                }
                            }
                        }
                    }
                }
                val selected = shows.value.filter { show ->
                    (categoryFilter.isEmpty() || BuiltInShows.category(show.id).name == categoryFilter) &&
                        show.title.contains(showQuery.trim(), ignoreCase = true)
                }
                if (selected.isEmpty()) item { Text("நிகழ்ச்சிகள் இல்லை. / No matching shows.", color = Muted) }
                selected.groupBy { BuiltInShows.category(it.id) }.forEach { (category, categoryShows) ->
                    item { Text(category.label, color = Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    categoryShows.forEach { show ->
                        item {
                            ShowCard(
                                show = show,
                                load = loads[show.id] ?: Load.IDLE,
                                onOpen = {
                                    openId = show.id
                                    loads[show.id] = Load.LOADING
                                },
                                onRetry = { loads[show.id] = Load.LOADING },
                                onRemove = if (show.builtIn) {
                                    null
                                } else {
                                    {
                                        store.removeFeedUrl(show.feedUrl)
                                        shows.value = shows.value.filterNot { it.id == show.id }
                                        loads.remove(show.id)
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                val show = shows.value.firstOrNull { it.id == openId }
                if (show == null) {
                    // The show was removed while its screen was open. Back is the only way out, and it
                    // is always available.
                    item {
                        TextButton(onClick = { openId = null }) {
                            Text(context.getString(R.string.back), color = Muted, fontSize = 13.sp)
                        }
                    }
                } else {
                    item {
                        EpisodeCard(
                            show = show,
                            load = loads[show.id] ?: Load.IDLE,
                            onBack = { openId = null },
                            onRetry = { loads[show.id] = Load.LOADING },
                            onPlay = { episode -> playEpisode(episode.audioUrl, episode.title,
                                queue = show.episodes.map { PodcastStore.QueueEntry(it.audioUrl, it.title) }) },
                            onDownload = { episode ->
                                downloadScope.launch {
                                    val result = runCatching { withContext(Dispatchers.IO) { downloads.enqueue(episode.audioUrl, episode.title) } }
                                    val message = if (result.isSuccess) "பதிவிறக்கங்கள் பகுதியில் நிலையைப் பார்க்கவும். வைஃபைக்காகக் காத்திருக்கலாம். / See Downloads for status; it may be waiting for Wi-Fi."
                                        else "பதிவிறக்கத்தைத் தொடங்க முடியவில்லை. பதிவிறக்கச் சேவை, சேமிப்பிடம் மற்றும் 100 கோப்பு வரம்பைச் சரிபார்க்கவும். / Could not start. Check the download service, storage and 100-file limit."
                                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
        // Transport stays visible even after selecting an episode far down a long playlist.
        NowPlayingCard(context, playerRevision)
    }

    // The fetch. Keyed on the load state of the open show, so it starts exactly once per attempt and
    // a retry is just setting the state back to LOADING.
    LaunchedEffect(openId, loads[openId]) {
        val id = openId ?: return@LaunchedEffect
        if (loads[id] != Load.LOADING) return@LaunchedEffect
        val target = shows.value.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val loaded = fetch(target)
        shows.value = shows.value.map { if (it.id == id) loaded else it }
        loads[id] = if (loaded.problem == null) Load.OK else Load.FAILED
    }
}

/**
 * Fetches one feed. Never throws: a network failure is a status the screen shows, not an exception
 * that kills the coroutine and takes the screen's state with it.
 *
 * Three separate things are checked, because they need three different words on screen. The call
 * can fail (UNREACHABLE). The server can refuse it (UNREACHABLE). And the server can answer with
 * something that is not a feed at all - a login page, a Cloudflare challenge - which used to be
 * parsed as a show and named after the HTML title of the block page.
 *
 * A User-Agent is sent because several podcast hosts answer a request that carries none with a
 * challenge page rather than with the feed.
 */
private suspend fun fetch(show: PodcastShow): PodcastShow = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(show.feedUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext show.copy(episodes = emptyList(), problem = FeedProblem.UNREACHABLE)
            }
            val body = response.body?.byteStream()?.use { FeedDownload.readPrefix(it) }.orEmpty()
            if (!RssReader.looksLikeFeed(body)) {
                // The link works, but it is not a podcast feed. Retrying it will fail forever, so
                // the screen must say that rather than "check your connection".
                return@withContext show.copy(episodes = emptyList(), problem = FeedProblem.NOT_A_FEED)
            }
            show.copy(
                title = if (show.builtIn) show.title else RssReader.parseTitle(body),
                episodes = RssReader.parseEpisodes(body),
                imageUrl = RssReader.parseImage(body),
                problem = null,
            )
        }
    } catch (error: Exception) {
        // An empty episode list is what the UI reads as "nothing came back", and the FAILED marker
        // set by the caller is what makes it say so out loud.
        show.copy(episodes = emptyList(), problem = FeedProblem.UNREACHABLE)
    }
}

/**
 * Identifies the app to podcast hosts.
 *
 * Buzzsprout - one of the hosts this app's built-in feeds live on - answers a request with no
 * User-Agent with a Cloudflare challenge page instead of the feed, and that page's HTML title then
 * appeared as the name of a podcast. Naming the client is the ordinary thing a feed reader does.
 */
private const val USER_AGENT = "RPAndroidPodcastPlayer/1.3 (+https://github.com/universalrp2003/RP-Android-Apps)"

@Composable
private fun Header() {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = context.getString(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = context.getString(R.string.what_it_does_body),
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/**
 * Cover art, loaded from the feed's own image.
 *
 * No image library: OkHttp is already a dependency and this is one small bitmap. A show that
 * publishes no artwork, or whose artwork will not load, gets its first letter on a tinted tile
 * instead of an empty grey box, so the list still has something to look at.
 */
@Composable
private fun CoverArt(imageUrl: String?, monogram: String, size: Dp) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .callTimeout(15, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(imageUrl).header("User-Agent", USER_AGENT).build()
                client.newCall(request).execute().use { response ->
                    val bytes = response.body?.bytes() ?: return@use null
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            } catch (error: Exception) {
                null
            }
        }
    }
    val art = bitmap
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(TileBg),
        contentAlignment = Alignment.Center,
    ) {
        if (art != null) {
            Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size),
            )
        } else {
            Text(
                text = monogram,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
            )
        }
    }
}

@Composable
private fun ShowCard(
    show: PodcastShow,
    load: Load,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            CoverArt(
                imageUrl = show.imageUrl,
                monogram = show.title.trim().take(1),
                size = 76.dp,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = show.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = if (show.builtIn) {
                        context.getString(R.string.builtin_shows)
                    } else {
                        context.getString(R.string.your_shows)
                    },
                    fontSize = 12.sp,
                    color = Accent,
                    modifier = Modifier.padding(top = 2.dp),
                )
                when (load) {
                    Load.OK -> Text(
                        text = context.getString(R.string.episode_count, show.episodes.size),
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Load.FAILED -> {
                        val message =
                            if (show.problem == FeedProblem.NOT_A_FEED) {
                                context.getString(R.string.feed_add_failed)
                            } else {
                                context.getString(R.string.feed_load_failed)
                            }
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = ErrorColor,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Load.LOADING -> Text(
                        text = context.getString(R.string.loading),
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Load.IDLE -> Unit
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpen,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) { Text(context.getString(R.string.open_show)) }
                    if (load == Load.FAILED) {
                        TextButton(onClick = onRetry) {
                            Text(context.getString(R.string.retry), color = Accent, fontSize = 13.sp)
                        }
                    }
                    if (onRemove != null) {
                        TextButton(onClick = onRemove) {
                            Text(context.getString(R.string.remove), color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    show: PodcastShow,
    load: Load,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (Episode) -> Unit,
    onDownload: (Episode) -> Unit,
) {
    val context = LocalContext.current
    var query by remember(show.id) { mutableStateOf("") }
    var visible by remember(show.id, query) { mutableIntStateOf(30) }
    val filtered = remember(show.episodes, query) { show.episodes.filter { it.title.contains(query.trim(), ignoreCase = true) } }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoverArt(
                    imageUrl = show.imageUrl,
                    monogram = show.title.trim().take(1),
                    size = 64.dp,
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = show.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    TextButton(onClick = onBack, modifier = Modifier.padding(top = 2.dp)) {
                        Text(context.getString(R.string.back), color = Accent, fontSize = 13.sp)
                    }
                }
            }
            when (load) {
                Load.LOADING -> Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Accent,
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = context.getString(R.string.loading),
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Load.FAILED -> Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = if (show.problem == FeedProblem.NOT_A_FEED) {
                            context.getString(R.string.feed_add_failed)
                        } else {
                            context.getString(R.string.feed_load_failed)
                        },
                        fontSize = 14.sp,
                        color = ErrorColor,
                    )
                    Text(
                        text = if (show.problem == FeedProblem.NOT_A_FEED) {
                            context.getString(R.string.feed_not_a_feed_hint)
                        } else {
                            context.getString(R.string.feed_load_failed_hint)
                        },
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        ) { Text(context.getString(R.string.retry)) }
                        TextButton(onClick = onBack) {
                            Text(context.getString(R.string.back), color = Muted, fontSize = 13.sp)
                        }
                    }
                }
                Load.OK, Load.IDLE -> {
                    if (show.episodes.isEmpty()) {
                        Text(
                            text = context.getString(R.string.no_episodes),
                            fontSize = 13.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        // A numbered playlist, which is the shape every podcast player uses: the
                        // position, the title, how long it is and when it arrived. The previous
                        // layout was an unnumbered list of titles with a play link beside each,
                        // which is what made it read as "not organised".
                        Text(
                            text = context.getString(R.string.episode_count, show.episodes.size),
                            fontSize = 12.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("பகுதிகளைத் தேடு / Search episodes") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (filtered.isEmpty()) Text("பொருந்தும் பகுதி இல்லை / No matching episodes", color = Muted)
                        filtered.take(visible).forEachIndexed { index, episode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TileBg),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Accent,
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                                    Text(
                                        text = episode.title,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                    )
                                    val length = formatDuration(episode.durationSeconds)
                                    val date = formatPubDate(episode.publishedAt)
                                    val meta = listOf(length, date).filter { it.isNotEmpty() }
                                        .joinToString(" · ")
                                    if (meta.isNotEmpty()) {
                                        Text(
                                            text = meta,
                                            fontSize = 11.sp,
                                            color = Muted,
                                            modifier = Modifier.padding(top = 1.dp),
                                        )
                                    }
                                }
                                Column {
                                    TextButton(onClick = { onPlay(episode) }) {
                                        Text(context.getString(R.string.play), color = Accent, fontSize = 13.sp)
                                    }
                                    TextButton(onClick = { onDownload(episode) }) {
                                        Text("பதிவிறக்கு / Download", color = Accent, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        if (filtered.size > visible) {
                            OutlinedButton(onClick = { visible += 30 }, modifier = Modifier.fillMaxWidth()) {
                                Text("மேலும் பகுதிகள் / More episodes")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onBack) {
                Text(context.getString(R.string.back), color = Muted, fontSize = 13.sp)
            }
        }
    }
}

/** Tamil month names, short form, for the episode list. */
private val TAMIL_MONTHS = listOf(
    "ஜன", "பிப்", "மார்", "ஏப்", "மே", "ஜூன்",
    "ஜூலை", "ஆக", "செப்", "அக்", "நவ", "டிச",
)

/**
 * "27 அக் 2026", or empty when the feed published no date.
 *
 * Done by hand rather than with `SimpleDateFormat` so it needs no locale on the device and cannot
 * be wrong about the timezone: the instant is read in the phone's own zone, which is the zone the
 * person holding it is in.
 */
internal fun formatPubDate(millis: Long): String {
    if (millis <= 0L) return ""
    val zone = java.util.TimeZone.getDefault()
    val shifted = millis + zone.getOffset(millis)
    val days = Math.floorDiv(shifted, 86_400_000L).toInt()
    // Civil date from days since the epoch, the inverse of the algorithm in RssReader.parseDate.
    var z = days + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val year = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = doy - (153 * mp + 2) / 5 + 1
    val month = if (mp < 10) mp + 3 else mp - 9
    val finalYear = if (month <= 2) year + 1 else year
    return "$day ${TAMIL_MONTHS[month - 1]} $finalYear"
}

@Composable
private fun NowPlayingCard(context: android.content.Context, revision: Int) {
    // Re-reading the state when the revision changes is what makes this card recompose as
    // playback moves between buffering, playing, paused and failed.
    val store = remember(context) { PodcastStore(context) }
    var continuous by remember { mutableStateOf(store.continuous) }
    val state = remember(revision) { PodcastState.status }
    if (!PodcastState.isActive() && state != PodcastState.Status.FAILED) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = PodcastState.nowPlayingTitle(),
                fontSize = 15.sp,
                maxLines = 2,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            val statusText = when (state) {
                PodcastState.Status.BUFFERING -> context.getString(R.string.status_buffering)
                PodcastState.Status.PLAYING -> context.getString(R.string.status_playing)
                PodcastState.Status.PAUSED -> context.getString(R.string.status_paused)
                PodcastState.Status.FAILED -> context.getString(R.string.status_failed)
                PodcastState.Status.STOPPED -> context.getString(R.string.status_stopped)
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = Muted,
                modifier = Modifier.padding(top = 2.dp),
            )
            TextButton(onClick = { continuous = !continuous; store.continuous = continuous }) {
                Text(if (continuous) "தொடர்ந்து கேள்: இயக்கம் / Continuous: on" else "தொடர்ந்து கேள்: நிறுத்தம் / Continuous: off", color = Muted)
            }
            Row {
                TextButton(onClick = { PodcastPlaybackService.previous(context) }) { Text("முந்தைய பகுதி / Previous", color = Muted) }
                TextButton(onClick = { PodcastPlaybackService.next(context) }) { Text("அடுத்த பகுதி / Next", color = Muted) }
            }
            val duration = PodcastState.durationMs
            val position = PodcastState.positionMs.coerceIn(0, duration.coerceAtLeast(0))
            var dragging by remember { mutableStateOf<Float?>(null) }
            Slider(
                value = dragging ?: position.toFloat(),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { PodcastPlaybackService.seek(context, it.toLong()) }
                    dragging = null
                },
                valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                enabled = duration > 0 && state != PodcastState.Status.BUFFERING,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "இயக்க நேரத்தை மாற்று / Seek playback position" },
            )
            Text("${formatDuration((dragging?.toInt() ?: position) / 1000)} / ${formatDuration(duration / 1000)}", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { PodcastPlaybackService.seek(context, position - 15_000L) }, modifier = Modifier.semantics { contentDescription = "பதினைந்து நொடிகள் பின்னோக்கி / Rewind 15 seconds" }, enabled = duration > 0) { Text("↶ 15") }
                when (state) {
                    PodcastState.Status.PLAYING -> Button(
                        onClick = { PodcastPlaybackService.pause(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) { Text(context.getString(R.string.pause)) }
                    PodcastState.Status.PAUSED -> Button(
                        onClick = { PodcastPlaybackService.resume(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                    ) { Text(context.getString(R.string.play)) }
                    else -> Unit
                }
                OutlinedButton(onClick = { PodcastPlaybackService.seek(context, position + 15_000L) }, modifier = Modifier.semantics { contentDescription = "பதினைந்து நொடிகள் முன்னோக்கி / Forward 15 seconds" }, enabled = duration > 0) { Text("15 ↷") }
            }
            Row {
                if (PodcastState.isActive() || state == PodcastState.Status.FAILED) {
                    TextButton(onClick = { PodcastPlaybackService.stop(context) }) {
                        Text(context.getString(R.string.stop), color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private val Bg = Color(0xFF0C0714)
private val CardBg = Color(0xFF1A1226)
private val TileBg = Color(0xFF2A1E3D)
private val Accent = Color(0xFFB388FF)
private val Muted = Color(0xFFB9AECB)
private val ErrorColor = Color(0xFFFF8A80)
