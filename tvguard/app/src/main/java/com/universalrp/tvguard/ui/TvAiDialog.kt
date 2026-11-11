package com.universalrp.tvguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.universalrp.tvguard.R

/**
 * TV-first question screen.
 *
 * A television has no comfortable keyboard, so both routes are offered: a text field for a
 * typed question (a remote or a paired keyboard can type into it) and a microphone button.
 * Focus order is field -> Ask -> Speak -> Close so the D-pad can reach everything.
 */
@Composable
fun TvAiDialog(
    answer: String?,
    engine: String?,
    busy: Boolean,
    enabled: Boolean,
    notice: String?,
    onAsk: (String) -> Unit,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit,
) {
    var question by remember { mutableStateOf("") }
    val field = remember { FocusRequester() }
    val ask = remember { FocusRequester() }
    val speak = remember { FocusRequester() }
    val close = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) { focusWhenReady(field) }
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.width(720.dp)
                    .background(Color(0xFF111827), RoundedCornerShape(18.dp))
                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(18.dp))
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.ai_title),
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.width(12.dp))
                    if (engine != null) {
                        Text(engine, color = Color(0xFF7DD3FC), fontSize = 12.sp, maxLines = 1)
                    }
                }

                if (!enabled) {
                    Text(
                        stringResource(R.string.ai_disabled_note),
                        color = Color(0xFFFFD180), fontSize = 13.sp
                    )
                }

                Text(stringResource(R.string.ai_hint_typed), color = Color(0xFF94A3B8), fontSize = 12.sp)

                BasicTextField(
                    value = question,
                    onValueChange = { question = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(field)
                        .testTag("ai-question-field")
                        .focusProperties { down = ask; up = close }
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    decorationBox = { inner ->
                        if (question.isEmpty()) {
                            Text(
                                stringResource(R.string.ai_hint_typed),
                                color = Color(0xFF64748B), fontSize = 15.sp
                            )
                        }
                        inner()
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvFocusButton(
                        stringResource(R.string.ai_ask),
                        onClick = { onAsk(question) },
                        modifier = Modifier.focusRequester(ask).testTag("ai-ask")
                            .focusProperties { up = field; right = speak; down = close; left = close }
                    )
                    TvFocusButton(
                        stringResource(R.string.ai_listen),
                        onClick = onSpeak,
                        modifier = Modifier.focusRequester(speak).testTag("ai-speak")
                            .focusProperties { up = field; left = ask; down = close; right = close }
                    )
                    TvFocusButton(
                        stringResource(R.string.ai_close),
                        onClick = onDismiss,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.focusRequester(close).testTag("ai-close")
                            .focusProperties { up = field; left = speak }
                    )
                }

                Spacer(Modifier.height(4.dp))
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.width(22.dp).height(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.ai_waiting), color = Color(0xFFCBD5E1), fontSize = 14.sp)
                    }
                } else if (notice != null) {
                    Text(notice, color = Color(0xFFFFD180), fontSize = 14.sp)
                } else if (answer != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                            .verticalScroll(rememberScrollState())
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(answer, color = Color.White, fontSize = 15.sp)
                    }
                } else {
                    Text(
                        stringResource(R.string.ai_privacy_notice),
                        color = Color(0xFF94A3B8), fontSize = 12.sp
                    )
                }
            }
        }
    }
}
