package com.universalrp.podcast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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

    val shows = remember { mutableStateOf(BuiltInShows.all()) }
    // Per show, so one dead feed cannot freeze the whole screen.
    val loads = remember { mutableStateMapOf<String, Load>() }
    var openId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var newUrl by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var playerRevision by remember { mutableIntStateOf(PodcastState.revision) }

    // The player card has to refresh as playback changes state, so it polls rather than being
    // pushed to. 400ms is frequent enough to feel immediate and cheap enough to ignore.
    LaunchedEffect(Unit) {
        while (true) {
            playerRevision = PodcastState.revision
            delay(400)
        }
    }

    /**
     * Fetches one feed. Never throws: a network failure is a status the screen shows, not an
     * exception that kills the coroutine and takes the screen's state with it.
     */
    suspend fun fetch(show: PodcastShow): PodcastShow = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .callTimeout(20, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(show.feedUrl).build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val title = RssReader.parseTitle(body)
                val episodes = RssReader.parseEpisodes(body)
                show.copy(title = title, episodes = episodes)
            }
        } catch (error: Exception) {
            // An empty episode list is what the UI reads as "nothing came back", and the FAILED
            // marker set by the caller is what makes it say so out loud.
            show.copy(episodes = emptyList())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header() }

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
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val url = newUrl.trim()
                                        if (!url.startsWith("http", ignoreCase = true)) {
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
                                TextButton(onClick = {
                                    adding = false
                                    addError = null
                                }) {
                                    Text(context.getString(R.string.feed_cancel), color = Muted)
                                }
                            }
                        }
                    }
                }
            }
            for (show in shows.value) {
                item(key = "show-${show.id}") {
                    ShowCard(
                        show = show,
                        load = loads[show.id] ?: Load.IDLE,
                        onOpen = {
                            openId = show.id
                            if (loads[show.id] == null) loads[show.id] = Load.LOADING
                        },
                        onRetry = { loads[show.id] = Load.LOADING },
                        onRemove = if (show.builtIn) null else {
                            {
                                store.removeFeedUrl(show.feedUrl)
                                shows.value = shows.value.filterNot { it.id == show.id }
                                loads.remove(show.id)
                            }
                        },
                    )
                }
            }
            item {
                Text(
                    text = context.getString(R.string.feed_note),
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            val show = shows.value.firstOrNull { it.id == openId }
            if (show == null) {
                // The show was removed while open. Nothing to show, so go back.
                openId = null
            } else {
                item(key = "detail-${show.id}") {
                    EpisodeCard(
                        show = show,
                        load = loads[show.id] ?: Load.IDLE,
                        onBack = { openId = null },
                        onRetry = { loads[show.id] = Load.LOADING },
                        onPlay = { episode ->
                            PodcastState.markBuffering(episode.title)
                            PodcastPlaybackService.play(context, episode.audioUrl, episode.title)
                        },
                    )
                }
            }
        }

        item { NowPlayingCard(context, playerRevision) }
        item { Spacer(Modifier.height(24.dp)) }
    }

    // The fetch. Keyed on the load state of the open show, so it starts exactly once per attempt and
    // a retry is just setting the state back to LOADING.
    LaunchedEffect(openId, loads[openId]) {
        val id = openId ?: return@LaunchedEffect
        if (loads[id] != Load.LOADING) return@LaunchedEffect
        val target = shows.value.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val loaded = fetch(target)
        shows.value = shows.value.map { if (it.id == id) loaded else it }
        loads[id] = if (loaded.episodes.isEmpty()) Load.FAILED else Load.OK
    }
}

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
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = show.title,
                fontSize = 17.sp,
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
                Load.FAILED -> Text(
                    text = context.getString(R.string.feed_load_failed),
                    fontSize = 12.sp,
                    color = ErrorColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
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

@Composable
private fun EpisodeCard(
    show: PodcastShow,
    load: Load,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (Episode) -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = show.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
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
                        text = context.getString(R.string.feed_load_failed),
                        fontSize = 14.sp,
                        color = ErrorColor,
                    )
                    Text(
                        text = context.getString(R.string.feed_load_failed_hint),
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
                        show.episodes.forEach { episode ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = episode.title,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                    )
                                    val length = formatDuration(episode.durationSeconds)
                                    if (length.isNotEmpty()) {
                                        Text(text = length, fontSize = 11.sp, color = Muted)
                                    }
                                }
                                TextButton(onClick = { onPlay(episode) }) {
                                    Text(
                                        text = context.getString(R.string.play),
                                        color = Accent,
                                        fontSize = 13.sp,
                                    )
                                }
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

@Composable
private fun NowPlayingCard(context: android.content.Context, revision: Int) {
    // Re-reading the state when the revision changes is what makes this card recompose as
    // playback moves between buffering, playing, paused and failed.
    val state = remember(revision) { PodcastState.status }
    if (!PodcastState.isActive() && state != PodcastState.Status.FAILED) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = PodcastState.nowPlayingTitle(),
                fontSize = 15.sp,
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
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
private val Accent = Color(0xFFB388FF)
private val Muted = Color(0xFFB9AECB)
private val ErrorColor = Color(0xFFFF8A80)
