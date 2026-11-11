package com.universalrp.tvguard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.tvguard.R
import com.universalrp.tvguard.ai.TvAiConfig
import com.universalrp.tvguard.ai.TvAiProvider
import com.universalrp.tvguard.ai.TvAiSettings

/**
 * Provider, model and key configuration, built for a D-pad.
 *
 * The key field is deliberately a plain field the user fills on the TV itself: TV Guard has
 * no bundled key, and asking a user to read a key out in a chat is never appropriate.
 * Saving encrypts it with an Android Keystore key; if that fails the app says so and does
 * not fall back to storing it in the clear.
 */
@Composable
fun TvAiSettingsView(
    initial: TvAiConfig,
    models: List<String>,
    loadingModels: Boolean,
    storageFailed: Boolean,
    lastKeyLabel: String?,
    onBack: () -> Unit,
    onSave: (TvAiConfig, Boolean) -> Unit,
    onLoadModels: (TvAiConfig) -> Unit,
    onForgetKey: () -> Unit,
) {
    var provider by remember { mutableStateOf(initial.provider) }
    var model by remember { mutableStateOf(initial.model) }
    var key by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    val appContext = LocalContext.current
    var language by remember { mutableStateOf(TvAiSettings.answerLanguage(appContext)) }

    // A key typed here belongs to the currently selected provider only. If the user
    // switches provider without typing a key, nothing is written to that provider's slot.
    val keyTyped = key.isNotBlank()
    val draft = TvAiConfig(
        provider = provider,
        apiKey = if (keyTyped) key else if (provider == initial.provider) initial.apiKey else "",
        model = model,
        baseUrl = baseUrl,
        freeEndpointUrl = initial.freeEndpointUrl,
    )
    // True when the save should write the secret slot for this provider.
    val writeKey = keyTyped

    val back = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusWhenReady(back) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TvFocusButton(
            stringResource(R.string.back_to_tools), onClick = onBack,
            modifier = Modifier.focusRequester(back).testTag("ai-settings-back")
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("ai-settings-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(stringResource(R.string.ai_privacy_notice), color = Color(0xFFFFD180), fontSize = 13.sp)
            }
            item {
                Text(stringResource(R.string.ai_key_never_shared), color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }
            item { SectionHeader(stringResource(R.string.ai_provider)) }
            items(TvAiProvider.entries, key = { it.id }) { entry ->
                SelectRow(
                    label = entry.label,
                    selected = provider == entry,
                    detail = entry.note,
                    tag = "ai-provider-${entry.id}",
                    onClick = {
                        provider = entry
                        model = ""
                        baseUrl = ""
                    }
                )
            }
            if (provider == TvAiProvider.FREE) {
                item {
                    Text(stringResource(R.string.ai_free_note), color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
            item { SectionHeader(stringResource(R.string.ai_model)) }
            if (models.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.ai_models_from_provider, models.size),
                        color = Color(0xFF7DD3FC), fontSize = 12.sp
                    )
                }
                items(models, key = { it }) { id ->
                    SelectRow(
                        label = id,
                        selected = model == id,
                        detail = null,
                        tag = "ai-model-$id",
                        onClick = { model = id }
                    )
                }
            } else if (loadingModels) {
                item { Text(stringResource(R.string.ai_model_loading), color = Color(0xFFCBD5E1), fontSize = 13.sp) }
            } else {
                item {
                    Text(stringResource(R.string.ai_models_none), color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
            item {
                LabeledField(
                    label = stringResource(R.string.ai_model),
                    value = model,
                    onValueChange = { model = it },
                    tag = "ai-model-input",
                )
            }
            item {
                TvFocusButton(
                    stringResource(R.string.ai_model_load),
                    // A key is needed for most providers before the list means anything.
                    onClick = { onLoadModels(draft) },
                    modifier = Modifier.testTag("ai-load-models")
                )
            }
            if (provider.needsKey) {
                item { SectionHeader(stringResource(R.string.ai_api_key)) }
                item {
                    Text(
                        if (initial.keyStored && provider == initial.provider) stringResource(R.string.ai_api_key_saved)
                        else stringResource(R.string.ai_api_key_placeholder),
                        color = if (initial.keyStored && provider == initial.provider) Color(0xFF00E676) else Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                item {
                    LabeledField(
                        label = stringResource(R.string.ai_api_key),
                        value = key,
                        onValueChange = { key = it },
                        tag = "ai-key-input",
                    )
                }
                item {
                    TvFocusButton(
                        stringResource(R.string.ai_key_forget),
                        onClick = { key = ""; onForgetKey() },
                        color = Color(0xFFFF8A80),
                        modifier = Modifier.testTag("ai-forget-key")
                    )
                }
            }
            if (provider == TvAiProvider.CUSTOM) {
                item { SectionHeader(stringResource(R.string.ai_base_url)) }
                item {
                    LabeledField(
                        label = stringResource(R.string.ai_base_url),
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        tag = "ai-base-url-input",
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.ai_answer_language)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("auto" to "Auto", "en" to "English", "ta" to "Tamil").forEach { (value, label) ->
                        TvFocusButton(
                            label,
                            onClick = {
                                language = value
                                TvAiSettings.setAnswerLanguage(appContext, value)
                            },
                            color = if (language == value) Color(0xFF00E5FF) else Color(0xFF475569),
                            modifier = Modifier.testTag("ai-lang-$value")
                        )
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.ai_mic_shortcut_desc),
                    color = Color(0xFF94A3B8), fontSize = 12.sp
                )
            }
            item { SectionHeader(stringResource(R.string.ai_mic_shortcut)) }
            item {
                Text(
                    stringResource(R.string.ai_mic_test),
                    color = Color(0xFFFFD180), fontSize = 13.sp
                )
            }
            item {
                // This is the honest proof step: it shows what the TV actually delivered,
                // rather than claiming a key works before anyone pressed it.
                Text(
                    if (lastKeyLabel == null) stringResource(R.string.ai_mic_test_none)
                    else stringResource(R.string.ai_mic_test_result, lastKeyLabel),
                    color = Color(0xFF00E5FF), fontSize = 14.sp
                )
            }
            item {
                TvFocusButton(
                    stringResource(R.string.ai_save),
                    onClick = { onSave(draft, writeKey) },
                    modifier = Modifier.testTag("ai-save")
                )
            }
            if (storageFailed) {
                item {
                    Text(stringResource(R.string.ai_api_key_storage_failed), color = Color(0xFFFF8A80), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.width(1.dp))
    Text(text, color = Color(0xFF7DD3FC), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
}

@Composable
private fun SelectRow(
    label: String,
    selected: Boolean,
    detail: String?,
    tag: String,
    onClick: () -> Unit,
) {
    // A single focusable row: D-pad focus plus OK selects it. Splitting this into a card and
    // a separate button made the focus order confusing on a remote.
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .focusable(interactionSource = interaction)
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .background(
                when {
                    focused -> Color(0xFF0F2B48)
                    selected -> Color(0xFF0B2237)
                    else -> Color(0xFF111827)
                },
                RoundedCornerShape(10.dp)
            )
            .border(
                when {
                    focused -> 3.dp
                    selected -> 2.dp
                    else -> 1.dp
                },
                when {
                    focused -> Color(0xFF00E5FF)
                    selected -> Color(0xFF0EA5E9)
                    else -> Color(0xFF334155)
                },
                RoundedCornerShape(10.dp)
            )
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Text("✓", color = Color(0xFF00E676), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        if (detail != null) {
            Text(detail, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            modifier = Modifier.fillMaxWidth().testTag(tag)
                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                .padding(10.dp),
        )
    }
}
