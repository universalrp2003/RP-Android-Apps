package com.universalrp.tamilnadufm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.universalrp.tamilnadufm.MainViewModel
import com.universalrp.tamilnadufm.UiState
import com.universalrp.tamilnadufm.data.Category
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState

/** Button-based reordering works with touch, keyboard and TalkBack, not drag-only gestures. */
@Composable
fun TabOrderEditor(vm: MainViewModel, state: UiState, onDismiss: () -> Unit) {
    val tamil = AppLanguageState.current == AppLanguage.TAMIL
    var navigation by remember { mutableStateOf(true) }
    val order = if (navigation) state.navigationOrder else state.categoryOrder
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tamil) "முழுத் தாவல்களின் வரிசை" else "Arrange whole tabs") },
        text = {
            Column {
                Text(if (tamil) "அம்புகளால் வரிசையை மாற்றுங்கள். இந்தப் போனில் தானாகச் சேமிக்கப்படும்; நடக்கும் ஒலிபரப்பு மாறாது."
                    else "Move tabs with the arrows. Saved automatically on this phone; playback and selected tab stay unchanged.")
                Row {
                    TextButton(onClick = { navigation = true }) { Text(if (tamil) "கீழ்த் தாவல்கள்" else "Navigation", color = if (navigation) Saffron else TextSecondary) }
                    TextButton(onClick = { navigation = false }) { Text(if (tamil) "வானொலி வகைகள்" else "Categories", color = if (!navigation) Saffron else TextSecondary) }
                }
                Column(Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                    order.forEachIndexed { index, id ->
                        key(id) {
                            val label = if (navigation) when (id) {
                                "RADIO" -> if (tamil) "வானொலி" else "Radio"
                                "NEWS" -> if (tamil) "செய்திகள்" else "News"
                                "LOCAL" -> if (tamil) "உள்ளூர் இசை" else "Local music"
                                "EQUALIZER" -> if (tamil) "ஒலி சமநிலை" else "Equalizer"
                                else -> if (tamil) "மேலும்" else "More"
                            } else com.universalrp.updates.AppStrings.translate(Category.label(id))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}. $label", modifier = Modifier.weight(1f))
                                TextButton(onClick = { vm.moveTab(navigation, id, -1) }, enabled = index > 0,
                                    modifier = Modifier.size(48.dp).semantics {
                                        contentDescription = if (tamil) "$label முன்னே நகர்த்து" else "Move $label earlier"
                                    }) { Text("↑") }
                                TextButton(onClick = { vm.moveTab(navigation, id, 1) }, enabled = index < order.lastIndex,
                                    modifier = Modifier.size(48.dp).semantics {
                                        contentDescription = if (tamil) "$label பின்னே நகர்த்து" else "Move $label later"
                                    }) { Text("↓") }
                            }
                        }
                    }
                }
                TextButton(onClick = { vm.resetTabOrder(navigation) }) {
                    Text(if (tamil) "இந்தப் பட்டியலின் இயல்பு வரிசை" else "Reset this list to default")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (tamil) "முடிந்தது" else "Done") } },
    )
}
