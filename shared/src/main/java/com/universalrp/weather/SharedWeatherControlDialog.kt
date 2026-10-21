package com.universalrp.weather

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private fun bi(tamil: Boolean, en: String, ta: String): String = if (tamil) ta else en

/**
 * Full bilingual setup & live control sheet shared by Ramesh Radio (`allowGps = false`, PIN/place
 * search only + live FM audio ducking!) and Oor Arivippu (`allowGps = true`, automatic GPS + PIN fallback!).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedWeatherControlDialog(
    state: SharedWeatherState,
    tamil: Boolean,
    allowGps: Boolean,
    accentColor: Color,
    ducksMusicNote: Boolean = false,
    onDismiss: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
    onTestVoice: () -> Unit,
    onCyclePreviewAnimation: () -> Unit,
    onSearchPlaces: (String) -> Unit,
    onSelectPlace: (WeatherPlaceCandidate) -> Unit,
    onClearPlace: () -> Unit,
    onSaveTomorrowKey: (String) -> Unit,
    onReadGuideAloud: (String) -> Unit,
    onStopVoice: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var keyExpanded by rememberSaveable { mutableStateOf(false) }
    var apiKeyDraft by rememberSaveable { mutableStateOf("") }

    val sheetTitle = bi(tamil, "Rain & cyclone voice alert", "மழை & புயல் குரல் எச்சரிக்கை")
    val switchTitle = bi(tamil, "Enable rain & storm voice alerts", "மழை & புயல் குரல் எச்சரிக்கை")
    val switchSub = if (ducksMusicNote) {
        bi(
            tamil,
            "Speaks ~30 min before rain (lowers radio volume smoothly, then restores). Zero daily spam.",
            "மழைக்கு ~30 நிமிடத்திற்கு முன் வானொலி ஒலியைக் குறைத்துத் தெளிவாக அறிவித்து மீண்டும் உயர்த்தும்.",
        )
    } else {
        bi(
            tamil,
            "Speaks ~30 min before rain starts. Never sends daily routine weather spam.",
            "மழை தொடங்க ~30 நிமிடத்திற்கு முன் அறிவிக்கும். தினசரி சாதாரண வானிலை தொந்தரவு இல்லை.",
        )
    }
    val checkNowLabel = bi(tamil, "Check now", "இப்போது சரிபார்")
    val testVoiceLabel = bi(tamil, "Test voice", "குரல் சோதனை")
    val previewAnimLabel = bi(tamil, "Preview animation", "அனிமேஷன் பார்")
    val closeLabel = bi(tamil, "Close", "மூடு")

    val locationHeading = bi(tamil, "Location setup", "இட அமைப்பு")
    val locationDesc = when {
        allowGps && state.gpsTurnedOn && state.permissionGranted ->
            bi(
                tamil,
                "Phone GPS is ON — location updates automatically as you travel.",
                "போன் GPS இயக்கத்தில் உள்ளது — பயணிக்கும்போது இடம் தானாகப் புதுப்பிக்கப்படும்.",
            )
        allowGps ->
            bi(
                tamil,
                "GPS is OFF — using saved PIN/village below (search 622301 or alangudi 622301).",
                "GPS அணைந்தால் கீழே சேமித்த PIN/ஊரைப் பயன்படுத்தும் (உதா: 622301 அல்லது alangudi 622301).",
            )
        else ->
            bi(
                tamil,
                "Zero location permission needed — search your PIN code or town once below (e.g. 622301 or alangudi 622301).",
                "இட அனுமதி தேவையில்லை — கீழே உங்கள் PIN எண் அல்லது ஊரை ஒருமுறை தேடிச் சேமியுங்கள் (உதா: 622301).",
            )
    }

    val fieldLabel = bi(tamil, "PIN code or place name", "PIN எண் அல்லது ஊர்ப் பெயர்")
    val fieldHint = bi(tamil, "e.g. 622301 or alangudi 622301", "உதா: 622301 அல்லது alangudi 622301")
    val findLabel = bi(tamil, "Find matching places", "பொருந்தும் ஊர்களைத் தேடு")
    val pickLabel = bi(tamil, "Tap your exact town / village:", "உங்கள் சரியான ஊர் / கிராமத்தைத் தட்டவும்:")
    val savedPrefix = bi(tamil, "Saved place: ", "சேமித்த ஊர்: ")

    val quietRule1 = bi(
        tamil,
        "• Normal 30-min rain alert stays silent during Quiet Hours (10 PM – 7 AM).",
        "• சாதாரண 30 நிமிட மழை முன்னறிவிப்பு இரவு அமைதி நேரத்தில் (10 PM – 7 AM) ஒலிக்காது.",
    )
    val quietRule2 = bi(
        tamil,
        "• Very heavy rain & cyclone alerts announce 24×7 day or night — never skipped!",
        "• மிகக் கனமழை மற்றும் புயல் எச்சரிக்கைகள் இரவு-பகல் 24×7 எப்போதும் ஒலிக்கும்!",
    )

    val tomorrowTitle = bi(
        tamil,
        "Tomorrow.io 1-minute precision (optional)",
        "Tomorrow.io 1-நிமிட துல்லியம் (விருப்பத்தேர்வு)",
    )
    val tomorrowSub = if (state.hasTomorrowKey) {
        bi(tamil, "Personal Tomorrow.io key active (Keystore encrypted)", "Tomorrow.io விசை செயலில் உள்ளது (பாதுகாப்பாகச் சேமிக்கப்பட்டது)")
    } else {
        bi(
            tamil,
            "Tomorrow.io 1-min radar runs automatically by default — optional personal key for extra quota",
            "Tomorrow.io 1-நிமிட ரேடார் விசை இன்றி தானாகவே இயங்குகிறது — கூடுதல் வரம்பிற்கு மட்டும் சொந்த விசை",
        )
    }
    val keyFieldLabel = bi(tamil, "Tomorrow.io API key", "Tomorrow.io API விசை")
    val saveKeyLabel = bi(tamil, "Save key", "விசையைச் சேமி")
    val removeKeyLabel = bi(tamil, "Remove key", "விசையை நீக்கு")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = sheetTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Live preview of the mini floating animation right inside the dialog too!
                if (state.previewSeverity != null || state.severity != WeatherSeverity.CLEAR_OR_NORMAL) {
                    WeatherMiniAlertBanner(
                        severity = state.previewSeverity ?: state.severity,
                        placeName = state.activePlaceName.ifBlank { state.savedPlaceName },
                        minutesUntilRain = state.minutesUntilRain ?: 28,
                        tamil = tamil,
                        isPreview = state.previewSeverity != null,
                        onClick = {},
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(text = switchTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(text = switchSub, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = state.enabled, onCheckedChange = onSetEnabled)
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = onCheckNow,
                        enabled = !state.checkingNow,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    ) {
                        if (state.checkingNow) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                        Spacer(Modifier.width(5.dp))
                        Text(text = checkNowLabel)
                    }
                    OutlinedButton(onClick = onTestVoice) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = testVoiceLabel)
                    }
                    OutlinedButton(onClick = onCyclePreviewAnimation) {
                        Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = previewAnimLabel)
                    }
                }

                if (state.lastCheckedAtMs > 0L) {
                    val checkedClock = WeatherAlertPolicy.format12HourClock(state.lastCheckedAtMs)
                    val checkedStatus = when (state.severity) {
                        WeatherSeverity.CYCLONE_SEVERE ->
                            bi(tamil, "Checked $checkedClock · Urgent cyclone / storm warning!", "சரிபார்த்தது $checkedClock · அவசர புயல் எச்சரிக்கை!")
                        WeatherSeverity.VERY_HEAVY_RAIN ->
                            bi(tamil, "Checked $checkedClock · Very heavy rain warning", "சரிபார்த்தது $checkedClock · மிகக் கனமழை எச்சரிக்கை")
                        WeatherSeverity.RAIN_SOON ->
                            bi(tamil, "Checked $checkedClock · Rain expected within ~30 min", "சரிபார்த்தது $checkedClock · ~30 நிமிடத்தில் மழை")
                        WeatherSeverity.CLEAR_OR_NORMAL ->
                            bi(tamil, "Checked $checkedClock · Clear / normal — staying silent", "சரிபார்த்தது $checkedClock · மழை அறிகுறி இல்லை — அமைதி")
                    }
                    Text(text = checkedStatus, style = MaterialTheme.typography.labelMedium, color = accentColor)
                }

                if (state.statusNote.isNotBlank()) {
                    Text(text = state.statusNote, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD97706))
                }

                HorizontalDivider()

                // Location PIN / Village section
                Text(text = locationHeading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text = locationDesc, style = MaterialTheme.typography.bodySmall)

                if (state.savedPlaceName.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                val title = savedPrefix + state.savedPlaceName
                                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                if (state.savedPlaceDetails.isNotBlank()) {
                                    Text(text = state.savedPlaceDetails, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = onClearPlace) {
                                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = fieldLabel) },
                    placeholder = { Text(text = fieldHint) },
                    trailingIcon = {
                        if (state.searchingPlaces) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { onSearchPlaces(query) }) {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchPlaces(query) }),
                )

                TextButton(
                    onClick = { onSearchPlaces(query) },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = findLabel, color = accentColor)
                }

                if (state.searchResults.isNotEmpty()) {
                    Text(text = pickLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = accentColor)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        state.searchResults.forEach { candidate ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                tonalElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        query = ""
                                        onSelectPlace(candidate)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(text = candidate.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        if (candidate.details.isNotBlank()) {
                                            Text(text = candidate.details, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Quiet Hours summary
                Text(text = quietRule1, style = MaterialTheme.typography.bodySmall)
                Text(text = quietRule2, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)

                HorizontalDivider()

                // Optional Tomorrow.io API key + Official Links + Voice Guide
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { keyExpanded = !keyExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Key, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(text = tomorrowTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(text = tomorrowSub, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(
                        if (keyExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                    )
                }

                if (keyExpanded) {
                    TomorrowApiKeyGuideLinks(
                        tamil = tamil,
                        accentColor = accentColor,
                        onReadGuideAloud = onReadGuideAloud,
                        onStopVoice = onStopVoice,
                    )
                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = keyFieldLabel) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveTomorrowKey(apiKeyDraft)
                                apiKeyDraft = ""
                            },
                            enabled = apiKeyDraft.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        ) {
                            Text(text = saveKeyLabel)
                        }
                        if (state.hasTomorrowKey) {
                            OutlinedButton(
                                onClick = {
                                    apiKeyDraft = ""
                                    onSaveTomorrowKey("")
                                },
                            ) {
                                Text(text = removeKeyLabel)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = closeLabel) }
        },
    )
}
