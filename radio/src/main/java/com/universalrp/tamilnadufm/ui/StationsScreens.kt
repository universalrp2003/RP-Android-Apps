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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// --------------------------------------------------------------------- radio

@Composable
fun RadioScreen(vm: MainViewModel, state: UiState, listState: LazyListState) {
    // v1.14 — the owner asked for a calmer first screen: the search box and the sorting
    // explanation now take one line each and open when tapped, instead of pushing the
    // station list down before the user has chosen anything.
    var searchOpen by rememberSaveable { mutableStateOf(state.query.isNotBlank()) }
    var showSortInfo by rememberSaveable { mutableStateOf(false) }
    var iconView by rememberSaveable { mutableStateOf(false) }
    var selectedIconStation by remember { mutableStateOf<RadioStation?>(null) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
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
    val iconRows = remember(state.visible) { state.visible.chunked(4) }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { station ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceC)
                                .border(1.dp, OutlineC, RoundedCornerShape(14.dp))
                                .clickable { selectedIconStation = station },
                            contentAlignment = Alignment.Center,
                        ) {
                            // Compact mode intentionally shows only the station's artwork or
                            // monogram. Tapping opens a name-and-play confirmation; it never
                            // starts a stream from an accidental icon touch.
                            StationLogo(station, 52.dp)
                        }
                    }
                    repeat(4 - row.size) {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
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
            text = { Text(station.subtitle.ifBlank { "Live stream" }, color = TextSecondary) },
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
