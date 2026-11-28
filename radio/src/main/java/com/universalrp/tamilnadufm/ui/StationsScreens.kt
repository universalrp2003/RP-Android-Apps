package com.universalrp.tamilnadufm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.HelpOutline
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.universalrp.tamilnadufm.MainViewModel
import com.universalrp.tamilnadufm.UiState
import com.universalrp.tamilnadufm.data.Category
import com.universalrp.tamilnadufm.data.RadioStation
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import com.universalrp.updates.AppStrings
import kotlinx.coroutines.launch

// --------------------------------------------------------------------- radio

@Composable
fun RadioScreen(vm: MainViewModel, state: UiState, listState: LazyListState) {
    // v1.14 — the owner asked for a calmer first screen: the search box and the sorting
    // explanation now take one line each and open when tapped, instead of pushing the
    // station list down before the user has chosen anything.
    var searchOpen by rememberSaveable { mutableStateOf(state.query.isNotBlank()) }
    var showSortInfo by rememberSaveable { mutableStateOf(false) }
    var iconView by rememberSaveable { mutableStateOf(true) }
    var selectedIconStation by remember { mutableStateOf<RadioStation?>(null) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    val screenScope = rememberCoroutineScope()
    var showArtists by rememberSaveable { mutableStateOf(false) }
    if (showArtists) {
        BackHandler { showArtists = false }
        ArtistScreen(onBack = { showArtists = false }, onSelect = { artist ->
            vm.setCategory(Category.ALL)
            vm.setQuery(artist.query)
            searchOpen = true
            showArtists = false
            screenScope.launch { listState.scrollToItem(0) }
        })
        return
    }
    if (showGuide) {
        BackHandler { showGuide = false }
        com.universalrp.updates.GuideScreen(
            content = com.universalrp.updates.AppGuides.rameshRadio,
            tamil = AppLanguageState.current == AppLanguage.TAMIL,
            onBack = { showGuide = false },
            accent = Saffron,
        )
        return
    }
    // Cache rows in composition, not in the non-composable LazyListScope builder.
    val gridColumns = StationGridLayout.columns(LocalConfiguration.current.screenWidthDp, LocalDensity.current.fontScale)
    val iconRows = remember(state.visible, gridColumns) { state.visible.chunked(gridColumns) }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Ramesh Radio",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Saffron,
                        fontWeight = FontWeight.Bold,
                    )

                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceHigh,
                ) {
                    // v1.15 — this pill used to print "இன்று" in English too. The word now
                    // follows the chosen language; the number never does.
                    val dataMb = state.todayDataUsageMb
                    val todayWord = AppStrings.translate("Today")
                    val text = if (dataMb >= 1024.0) {
                        "%.2f GB %s".format(dataMb / 1024.0, todayWord)
                    } else {
                        "%.1f MB %s".format(dataMb, todayWord)
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = Saffron,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
                val radioHeaderContext = androidx.compose.ui.platform.LocalContext.current
                IconButton(
                    onClick = { com.universalrp.tamilnadufm.RadioWeatherMonitor.openDialog(radioHeaderContext) },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        Icons.Filled.Cloud,
                        contentDescription = AppStrings.translate("Rain & cyclone voice alert"),
                        tint = Saffron,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { showGuide = true }, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Filled.HelpOutline,
                        contentDescription = AppStrings.translate("Guide"),
                        tint = Saffron,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        item {
            if (searchOpen) {
                SearchBar(vm, state, onSearchDirectory = { vm.searchDirectory() })
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceHigh)
                        .border(1.dp, OutlineC, RoundedCornerShape(14.dp))
                        .clickable { searchOpen = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Search stations, city or language",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = AppStrings.translate("Directory"),
                        tint = Teal,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { vm.searchDirectory() },
                    )
                }
            }
        }

        item {
            DraggableCategoryTabs(
                ids = state.categoryOrder,
                selected = state.category,
                onSelect = vm::setCategory,
                onDrop = vm::dropCategoryTab,
                onMove = { id, delta -> vm.moveTab(false, id, delta) },
            )
        }

        item {
            TextButton(onClick = { showArtists = true }) {
                Text(if (AppLanguageState.current == AppLanguage.TAMIL) "கலைஞர்கள் & பிரபலமான பாடல்கள் →" else "Artists & popular songs →", color = Saffron)
            }
        }

        // Continue where you left off
        val last = vm.lastPlayed()
        if (last != null && state.query.isBlank() &&
            (state.category == Category.ALL || state.category == Category.TAMIL)
        ) {
            item {
                PanelCard(
                    Modifier
                        .fillMaxWidth()
                        .clickable { vm.play(last) },
                    color = SurfaceHigh,
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Saffron,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Continue listening",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                            Text(
                                last.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.query.isBlank()) "${state.visible.size} stations"
                    else "${state.visible.size} matches",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = { iconView = !iconView },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (iconView) Icons.Filled.ViewList else Icons.Filled.GridView,
                        contentDescription = AppStrings.translate(
                            if (iconView) "Switch to list view" else "Switch to icon view",
                        ),
                        tint = Saffron,
                    )
                }
                IconButton(onClick = { vm.searchDirectory() }) {
                    Icon(
                        Icons.Filled.Language,
                        contentDescription = AppStrings.translate("Live directory"),
                        tint = Teal,
                    )
                }
            }
        }

        if (state.query.isBlank() && state.category in setOf(Category.ALL, Category.TAMIL, Category.TAMIL_FM)) {
            item {
                // One line, opened only when wanted — the same pattern the owner asked for on
                // the Oor Arivippu screens instead of a paragraph above every list.
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showSortInfo = !showSortInfo }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "How this list is sorted",
                            style = MaterialTheme.typography.labelSmall,
                            color = Saffron,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(if (showSortInfo) "  ▴" else "  ▾",
                            style = MaterialTheme.typography.labelSmall, color = Saffron)
                    }
                    if (showSortInfo) {
                        Text(
                            "Verified streams stay first; the all-stations list prioritizes Tamil, and Radio Browser votes sort each group.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        if (state.visible.isEmpty()) {
            item {
                PanelCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Nothing here yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        IconChip(Icons.Filled.Refresh, "Search the directory") {
                            vm.searchDirectory()
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Try another category or clear the search box. The live directory has " +
                                "far more stations than the shipped list, including town and city stations.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        if (iconView) {
            items(iconRows, key = { row -> row.first().url }) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { station ->
                        StationGalleryCard(
                            station = station,
                            playing = state.nowPlaying?.url == station.url,
                            modifier = Modifier.weight(1f),
                            onOpen = { selectedIconStation = station },
                        )
                    }
                    repeat(gridColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else {
            items(state.visible, key = { it.url }) { station ->
                StationRow(
                    station = station,
                    playing = state.nowPlaying?.url == station.url,
                    favourite = state.favourites.contains(station.url.lowercase()),
                    onPlay = { vm.play(station) },
                    onFavourite = { vm.toggleFavourite(station) },
                    onShare = { vm.shareStation(station) },
                    onSave = { vm.saveDirectoryStation(station) },
                    onDelete = { vm.removeStation(station) },
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    selectedIconStation?.let { station ->
        AlertDialog(
            onDismissRequest = { selectedIconStation = null },
            title = { Text(station.name, color = TextPrimary) },
            text = {
                Column {
                    Text(station.subtitle.ifBlank { AppStrings.translate("Live stream") }, color = TextSecondary)
                    if (station.referenceName.isNotBlank() && !station.referenceName.equals(station.name, ignoreCase = true)) {
                        Text(
                            if (AppLanguageState.current == AppLanguage.TAMIL)
                                "படத்தில் இருக்கக்கூடிய பெயர்: ${station.referenceName} (உறுதி செய்யப்படவில்லை)"
                            else "Possible reference name: ${station.referenceName} (not confirmed)",
                            color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (com.universalrp.tamilnadufm.data.RadioStreamPolicy.heldForReview(station.url)) {
                        Text(if (AppLanguageState.current == AppLanguage.TAMIL) "வழங்குநர் ஒலிபரப்பு பரிசீலனையில்" else "Provider stream held for review", color = Warn)
                    } else if (!station.verified) {
                        Text(if (AppLanguageState.current == AppLanguage.TAMIL) "சரிபார்க்கப்படாத ஒலிபரப்பு" else "Stream not verified", color = Warn)
                    }
                    val favourite = state.favourites.contains(station.url.lowercase())
                    Row {
                        IconButton(onClick = { vm.toggleFavourite(station) }) {
                            Icon(if (favourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = AppStrings.translate(if (favourite) "Remove favourite" else "Add favourite"),
                                tint = if (favourite) Gold else TextSecondary)
                        }
                        IconButton(onClick = { vm.shareStation(station) }) {
                            Icon(Icons.Filled.Share, contentDescription = AppStrings.translate("Share"), tint = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedIconStation = null
                    vm.play(station)
                }) { Text(AppStrings.translate("Play"), color = Saffron) }
            },
            dismissButton = {
                TextButton(onClick = { selectedIconStation = null }) {
                    Text(AppStrings.translate("Cancel"), color = TextSecondary)
                }
            },
        )
    }
}

/** Picture above a readable name, inspired by the owner's reference, without copying its artwork. */
@Composable
private fun StationGalleryCard(station: RadioStation, playing: Boolean, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.clip(shape).background(Color.White)
            .border(if (playing) 2.dp else 1.dp, if (playing) Saffron else OutlineC, shape)
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        StationArtwork(station, Modifier.fillMaxWidth().aspectRatio(1.55f), decorative = true)
        Box(Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(horizontal = 5.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center) {
            Text(station.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18151C), textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * v1.13 — the search row stays one line tall.
 *
 * The station-list button used to sit inside the text field as a full chip ("நிலையப் பட்டியல்"),
 * which took almost half the width and made the Tamil hint wrap onto three lines. The button is
 * now its own compact icon square beside the field (same action, same icon), and the hint is
 * pinned to a single ellipsised line so no language can stretch the field again.
 */
@Composable
fun SearchBar(vm: MainViewModel, state: UiState, onSearchDirectory: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = { vm.setQuery(it) },
            placeholder = {
                Text(
                    "Station, city or language",
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Saffron,
                unfocusedBorderColor = OutlineC,
                focusedContainerColor = SurfaceHigh,
                unfocusedContainerColor = SurfaceHigh,
                cursorColor = Saffron,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
        )
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceHigh)
                .border(1.dp, OutlineC, RoundedCornerShape(14.dp))
                .clickable(onClick = onSearchDirectory),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Language,
                contentDescription = AppStrings.translate("Directory"),
                tint = Teal,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Shared station row: logo, name, meta pills, favourite star and a play tap. */
@Composable
fun StationRow(
    station: RadioStation,
    playing: Boolean,
    favourite: Boolean,
    onPlay: () -> Unit,
    onFavourite: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    PanelCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        color = if (playing) SurfaceHigh else SurfaceC,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationLogo(station, 46.dp)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (playing) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Saffron)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        station.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (playing) Saffron else TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (station.referenceName.isNotBlank() && !station.referenceName.equals(station.name, ignoreCase = true)) {
                    Text(
                        if (AppLanguageState.current == AppLanguage.TAMIL)
                            "படத்தில் இருக்கக்கூடிய பெயர்: ${station.referenceName} (உறுதி செய்யப்படவில்லை)"
                        else "Possible reference name: ${station.referenceName} (not confirmed)",
                        color = TextSecondary, style = MaterialTheme.typography.labelSmall,
                    )
                }
                if (com.universalrp.tamilnadufm.data.RadioStreamPolicy.heldForReview(station.url)) {
                    Text(if (AppLanguageState.current == AppLanguage.TAMIL) "வழங்குநர் ஒலிபரப்பு பரிசீலனையில்" else "Provider stream held for review",
                        color = Warn, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!station.verified) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Warn,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        station.subtitle.ifBlank { "Live stream" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconChip(
                icon = Icons.Filled.Share,
                label = "",
                tint = TextSecondary,
            ) {
                onShare()
            }

            Spacer(Modifier.width(6.dp))

            if (station.isCustom) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = AppStrings.translate("Remove"),
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onDelete),
                )
                Spacer(Modifier.width(6.dp))
            }

            Icon(
                if (favourite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = AppStrings.translate(if (favourite) "Remove favourite" else "Add favourite"),
                tint = if (favourite) Gold else TextSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onFavourite),
            )
        }
    }
}

// ---------------------------------------------------------------------- news

@Composable
fun NewsScreen(vm: MainViewModel, state: UiState, listState: LazyListState) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text(
                    "News radio",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "World news in English and Tamil news, around the clock.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        item {
            val newsCategories = setOf(Category.ALL_NEWS, Category.TAMIL_NEWS,
                Category.WORLD_NEWS, Category.INDIA_NEWS, Category.ENGLISH)
            val chips = state.categoryOrder.filter { it in newsCategories }
            DraggableCategoryTabs(
                ids = chips,
                selected = state.category,
                onSelect = vm::setCategory,
                onDrop = vm::dropCategoryTab,
                onMove = { id, delta ->
                    val target = chips.getOrNull(chips.indexOf(id) + delta)
                    if (target != null) vm.dropCategoryTab(id, target, delta > 0)
                },
            )
        }

        item {
            PanelCard(Modifier.fillMaxWidth(), color = SurfaceHigh) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Live news stations",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "The shipped list has world and English news channels. Ask the directory " +
                            "for more — it is community maintained and much larger than any list " +
                            "an app can carry.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconChip(Icons.Filled.Refresh, "World news (English)") {
                            vm.setCategory(Category.WORLD_NEWS)
                            vm.discoverNews("english")
                        }
                        IconChip(Icons.Filled.Refresh, "Tamil news", tint = Teal) {
                            vm.setCategory(Category.TAMIL_NEWS)
                            vm.discoverNews("tamil")
                        }
                        IconChip(Icons.Filled.Refresh, "Indian news", tint = Gold) {
                            vm.setCategory(Category.INDIA_NEWS)
                            vm.discoverNews("hindi")
                        }
                    }
                }
            }
        }

        item {
            SectionTitle("${state.visible.size} stations in this list")
        }

        if (state.visible.isEmpty()) {
            item {
                PanelCard(Modifier.fillMaxWidth()) {
                    Text(
                        "No news stations in this category yet. Tap one of the refresh buttons " +
                            "above to pull live results from the directory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
        }

        items(state.visible, key = { it.url }) { station ->
            StationRow(
                station = station,
                playing = state.nowPlaying?.url == station.url,
                favourite = state.favourites.contains(station.url.lowercase()),
                onPlay = { vm.play(station) },
                onFavourite = { vm.toggleFavourite(station) },
                onShare = { vm.shareStation(station) },
                onSave = { vm.saveDirectoryStation(station) },
                onDelete = { vm.removeStation(station) },
            )
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ----------------------------------------------------------- directory sheet

@Composable
fun DirectorySheet(vm: MainViewModel, state: UiState) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Live directory",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "radio-browser.info • community run, no account needed",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                Icon(
                    Icons.Filled.Close,
                    contentDescription = AppStrings.translate("Close"),
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { vm.closeDirectory() },
                )
            }

            if (state.directoryBusy) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Saffron)
                }
            }

            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // url+name: still stable across refreshes, but two entries that share a
                // stream URL can no longer collide and crash the list.
                items(state.directoryResults, key = { it.url + "|" + it.name }) { station ->
                    PanelCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StationLogo(station, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    station.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    station.subtitle.ifBlank { "Live stream" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconChip(Icons.Filled.PlayArrow, "Play", tint = Saffron) {
                                vm.playDirectory(station)
                            }
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = AppStrings.translate("Save"),
                                tint = Teal,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { vm.saveDirectoryStation(station) },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}


/** Artist cards are searches, not pre-labelled playable streams. */
@Composable
private fun ArtistScreen(onBack: () -> Unit, onSelect: (ArtistShortcut) -> Unit) {
    var credits by rememberSaveable { mutableStateOf(false) }
    val tamil = AppLanguageState.current == AppLanguage.TAMIL
    val columns = StationGridLayout.columns(LocalConfiguration.current.screenWidthDp, LocalDensity.current.fontScale)
    val rows = remember(columns) { ArtistBrowse.artists.chunked(columns) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(if (tamil) "← திரும்ப" else "← Back", color = Saffron) }
                Text(if (tamil) "கலைஞர்கள்" else "Artists", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                TextButton(onClick = { credits = true }) { Text(if (tamil) "பட உதவி" else "Photo credits", color = Saffron) }
            }
        }
        item {
            Text(if (tamil) "ஒரு படத்தைத் தட்டினால் நிலையப் பெயர்களில் தேடும். பாடல்கள் அல்லது நேரடி ஒலிபரப்பு இருப்பது உறுதி அல்ல. மேலும் தேட வானொலி பட்டியல் பொத்தானைப் பயன்படுத்தவும்."
                 else "Tap an artist to search station names. A card does not guarantee a live station or a particular song. Use the directory button to search further.",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        items(rows, key = { it.first().name }) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { artist ->
                    val artwork = remember(artist) { RadioStation(id = artist.name, name = artist.name, url = "", favicon = artist.photo) }
                    StationGalleryCard(artwork, playing = false, modifier = Modifier.weight(1f), onOpen = { onSelect(artist) })
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
    if (credits) {
        val uri = LocalUriHandler.current
        val creditRows = listOf(
            Triple("Ilaiyaraaja — Sriram Narasimhan / Silverscreen; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Ilaiyaraaja_at_Merku_Thodarchi_Malai_Press_Meet.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("A. R. Rahman — Dani Charles / Silverscreen", "https://commons.wikimedia.org/wiki/File:AR_Rahman_At_The_%E2%80%98Marvel_Anthem%E2%80%99_Launch.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("S. P. Balasubrahmanyam — Aravind Sivaraj", "https://commons.wikimedia.org/wiki/File:S._P._Balasubrahmanyam_in_2013.JPG", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("K. S. Chithra — Jayanthjwala", "https://commons.wikimedia.org/wiki/File:KS_Chithra.jpg", "https://creativecommons.org/licenses/by-sa/4.0/"),
            Triple("P. Susheela — Kris Iyer; cropped by Commons", "https://commons.wikimedia.org/wiki/File:With_P_Susheela_(cropped).jpg", "https://creativecommons.org/publicdomain/zero/1.0/"),
            Triple("K. J. Yesudas — Anees Kodiyathur", "https://commons.wikimedia.org/wiki/File:Kj_yesudas.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Anirudh — Surajram Kumaravel", "https://commons.wikimedia.org/wiki/File:Anirudh_Ravichander.jpg", "https://creativecommons.org/licenses/by/3.0/"),
            Triple("Hariharan — E P Sajeevan", "https://commons.wikimedia.org/wiki/File:A_Hariharan.JPG", "https://creativecommons.org/licenses/by-sa/4.0/"),
            Triple("S. Janaki — Rishabh Tatiraju", "https://commons.wikimedia.org/wiki/File:S_Janaki_in_Pune,_India_2007.JPG", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Mano — subhadra devan", "https://commons.wikimedia.org/wiki/File:Playback_singer_Mano.JPG", "https://creativecommons.org/licenses/by-sa/4.0/"),
            Triple("Harris Jayaraj — Wikipraveennew", "https://commons.wikimedia.org/wiki/File:Harris_Jayaraj.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("D. Imman — Silverscreen Media Inc.", "https://commons.wikimedia.org/wiki/File:D_Imman_at_Panjumittai_Audio_Launch.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Vijay Antony — Kumarcd", "https://commons.wikimedia.org/wiki/File:VijayAntony.JPG", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Deva — Silverscreen Media Inc.; watermark removed by Commons", "https://commons.wikimedia.org/wiki/File:Music_Director_Deva_at_KJ_Yesudas_50_Concert_Press_Meet.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Vidyasagar — Prajeesh Raj; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Vidyasagar_at_Anarkali_celebration.jpg", "https://creativecommons.org/licenses/by-sa/4.0/"),
            Triple("Sujatha — Lijesh K; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Sujatha_(cropped).jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Vani Jayaram — Bollywood Hungama; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Vani_Jairam_2014_FF_(cropped).jpg", "https://creativecommons.org/licenses/by/3.0/"),
            Triple("Kamal Haasan — Bollywood Hungama; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Kamal_at_61st_FF_(cropped).jpg", "https://creativecommons.org/licenses/by/3.0/"),
            Triple("Rajinikanth — Dani Charles / Silverscreen; watermark removed by Commons", "https://commons.wikimedia.org/wiki/File:Rajinikanth_Felicitates_Writer_Kalaignanam.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Dhanush — Dani Charles / Silverscreen", "https://commons.wikimedia.org/wiki/File:Dhanush_at_the_%E2%80%98Asuran%E2%80%99_Success_Meet.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Suriya — Epsajeevan", "https://commons.wikimedia.org/wiki/File:Soorya_sivakumar.jpg", "https://creativecommons.org/licenses/by-sa/4.0/"),
            Triple("Yuvan Shankar Raja — Silverscreen Media Inc.", "https://commons.wikimedia.org/wiki/File:Yuvan_Shankar_Raja_exclusive_HQ_Photos_Silverscreen.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Ajith — Shridhar Devalla", "https://commons.wikimedia.org/wiki/File:Ajith_Kumar_at_Irungattukottai_Race_Track.jpg", "https://creativecommons.org/licenses/by/2.0/"),
            Triple("Vijay — Silverscreen Media Inc.; watermark removed by Commons", "https://commons.wikimedia.org/wiki/File:Vijay_at_Puli_Audio_Launch.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Vijayakanth — Silverscreen Media Inc.", "https://commons.wikimedia.org/wiki/File:Vijaykanth_at_the_Sagaptham_Audio_Launch.jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("M. S. Viswanathan — Kannanshanmugam, Shanmugam Studio, Kollam", "https://commons.wikimedia.org/wiki/File:M.s.v.JPG", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Sivaji Ganesan — Mohan V Raman; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Sivaji_Ganesan_1.jpg", "https://creativecommons.org/licenses/by-sa/4.0/"),
            Triple("Vadivelu — Silverscreen Media Inc.; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Vadivelu_1_at_Trailer_%26_HD_Songs_Launch_of_Kaththi_Sandai_(cropped).jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Vivek — Dani Charles / Silverscreen; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Vivek_at_the_Ezhumin_Press_Meet_(headshot).jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("Na. Muthukumar — Dani Charles / Silverscreen; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Na_Muthukumar_at_Touring_Talkies_Audio_Launch_(cropped).jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
            Triple("M. G. Ramachandran — oil painting by Rajasekharan Parameswaran", "https://commons.wikimedia.org/wiki/File:MGR_-OIL_PAINTING_BY_RAJASEKHARAN.jpg", "https://creativecommons.org/licenses/by-sa/4.0/"),
            Triple("Santhanam — Dani Charles / Silverscreen; cropped by Commons", "https://commons.wikimedia.org/wiki/File:Santhanam_at_the_%E2%80%98A1%E2%80%99_Press_Meet_(cropped).jpg", "https://creativecommons.org/licenses/by-sa/3.0/"),
        )
        AlertDialog(onDismissRequest = { credits = false }, title = { Text(if (tamil) "பட உதவி" else "Photo credits") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(if (tamil) "படங்கள் விக்கிமீடியா காமன்ஸிலிருந்து பெறப்படுகின்றன; காட்டுவதற்காக அளவு மாற்றப்படுகிறது, செயலி கோப்புகளை மாற்றாது." else "Images from Wikimedia Commons; display is resized, files are not modified by the app.")
                    creditRows.forEach { (label, source, licence) ->
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { uri.openUri(source) }) { Text(if (tamil) "மூலம்" else "Source") }
                            TextButton(onClick = { uri.openUri(licence) }) { Text(if (tamil) "உரிமம்" else "Licence") }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { credits = false }) { Text("OK") } })
    }
}
