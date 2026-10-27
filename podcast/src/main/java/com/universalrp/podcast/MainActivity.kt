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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
 * Tamil-first and deliberately plain. This app is for listening while doing something else, so the
 * screen does one thing - pick an episode - and gets out of the way.
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

@Composable
private fun PodcastScreen() {
    val context = LocalContext.current
    val store = remember { PodcastStore(context) }

    var shows by remember { mutableStateOf(BuiltInShows.all()) }
    var openShow by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var newUrl by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(PodcastState.revision) }

    LaunchedEffect(Unit) {
        while (true) {
            revision = PodcastState.revision
            delay(400)
        }
    }

    // A feed is fetched once per show and cached in memory for the life of the screen.
    suspend fun load(show: PodcastShow): PodcastShow = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(show.feedUrl).build()
        val client = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val title = RssReader.parseTitle(body)
            val episodes = RssReader.parseEpisodes(body)
            show.copy(title = title, episodes = episodes)
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

        if (openShow == null) {
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
                            androidx.compose.material3.OutlinedTextField(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                label = { Text(context.getString(R.string.feed_url_hint)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val url = newUrl.trim()
                                        if (url.startsWith("http", true) &&
                                            store.addFeedUrl(url)
                                        ) {
                                            shows = shows + PodcastShow(
                                                id = url, title = url, feedUrl = url,
                                                builtIn = false,
                                            )
                                            newUrl = ""
                                            adding = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                                ) { Text(context.getString(R.string.feed_add_confirm)) }
                                TextButton(onClick = { adding = false }) {
                                    Text(context.getString(R.string.feed_cancel), color = Muted)
                                }
                            }
                        }
                    }
                }
            }
            items@ for (show in shows) {
                item {
                    ShowCard(
                        show = show,
                        busy = busy,
                        onOpen = {
                            openShow = show.id
                            busy = true
                        },
                        onRemove = if (show.builtIn) null else {
                            {
                                store.removeFeedUrl(show.feedUrl)
                                shows = shows.filterNot { it.id == show.id }
                            }
                        },
                    )
                }
            }
        } else {
            val show = shows.firstOrNull { it.id == openShow }
            if (show == null) {
                openShow = null
            } else {
                item {
                    EpisodeListCard(
                        show = show,
                        onBack = { openShow = null },
                        onPlay = { episode ->
                            PodcastState.markBuffering(episode.title)
                            PodcastPlaybackService.play(context, episode.audioUrl, episode.title)
                        },
                    )
                }
            }
        }

        item { NowPlayingCard(context) }
        item { Spacer(Modifier.height(24.dp)) }
    }

    // Fetch the episodes of the show that was just opened.
    LaunchedEffect(openShow, busy) {
        val id = openShow ?: return@LaunchedEffect
        if (!busy) return@LaunchedEffect
        val show = shows.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val loaded = load(show)
        shows = shows.map { if (it.id == id) loaded else it }
        busy = false
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
    busy: Boolean,
    onOpen: () -> Unit,
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
            if (show.episodes.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.episode_count, show.episodes.size),
                    fontSize = 12.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpen,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent),
                ) { Text(context.getString(R.string.tab_shows)) }
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text(context.getString(R.string.feed_cancel), color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeListCard(
    show: PodcastShow,
    onBack: () -> Unit,
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
            if (show.episodes.isEmpty()) {
                Text(
                    text = context.getString(R.string.loading),
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
                            Text(context.getString(R.string.play), color = Accent, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onBack) {
                Text(context.getString(R.string.feed_cancel), color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NowPlayingCard(context: android.content.Context) {
    val state = PodcastState.status
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
            Text(text = statusText, fontSize = 12.sp, color = Muted, modifier = Modifier.padding(top = 2.dp))
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
